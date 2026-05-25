package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSAssignmentExpression
import com.intellij.lang.javascript.psi.JSBlockStatement
import com.intellij.lang.javascript.psi.JSConditionalExpression
import com.intellij.lang.javascript.psi.JSExpressionStatement
import com.intellij.lang.javascript.psi.JSIfStatement
import com.intellij.lang.javascript.psi.JSReturnStatement
import com.intellij.lang.javascript.psi.JSVarStatement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsTernaryHandler : SplitJoinHandler {

    private sealed class Context {
        data class Declaration(val varStmt: JSVarStatement, val name: String, val ternary: JSConditionalExpression) : Context()
        data class Assignment(val exprStmt: JSExpressionStatement, val lhs: String, val ternary: JSConditionalExpression) : Context()
        data class Return(val retStmt: JSReturnStatement, val ternary: JSConditionalExpression) : Context()
    }

    override fun canSplit(element: PsiElement): Boolean {
        val ctx = element.classify()
        if (ctx == null) {
            val ternary = element.ternaryAncestor() ?: return false
            return !ternary.containsComment()
        }
        return !ctx.containerStmt().containsComment()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val ifStmt = element.ifAncestor() ?: return false
        if (ifStmt.containsComment()) return false
        return ifStmt.classifyJoinable() != null
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        when (val ctx = element.classify() ?: return) {
            is Context.Declaration -> {
                val (cond, then, els) = ctx.ternary.parts() ?: return
                val replacement = "let ${ctx.name};\nif ($cond) { ${ctx.name} = $then; } else { ${ctx.name} = $els; }"
                context.replaceWithLeadingWhitespace(ctx.varStmt, replacement)
            }
            is Context.Assignment -> {
                val (cond, then, els) = ctx.ternary.parts() ?: return
                val replacement = "if ($cond) { ${ctx.lhs} = $then; } else { ${ctx.lhs} = $els; }"
                context.replaceWithLeadingWhitespace(ctx.exprStmt, replacement)
            }
            is Context.Return -> {
                val (cond, then, els) = ctx.ternary.parts() ?: return
                val replacement = "if ($cond) { return $then; } else { return $els; }"
                context.replaceWithLeadingWhitespace(ctx.retStmt, replacement)
            }
        }
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val ifStmt = element.ifAncestor() ?: return
        val info = ifStmt.classifyJoinable() ?: return
        when (info) {
            is JoinShape.DeclThenAssign -> {
                val replacement = "${info.kind} ${info.name} = ${info.cond} ? ${info.then} : ${info.els};"
                val range = info.varStmt.textRange.union(ifStmt.textRange).withLeadingWhitespace(info.varStmt)
                val prefix = range.leadingTextBefore(info.varStmt)
                context.replace(range, prefix + replacement)
            }
            is JoinShape.PlainAssign -> {
                context.replaceWithLeadingWhitespace(ifStmt, "${info.lhs} = ${info.cond} ? ${info.then} : ${info.els};")
            }
            is JoinShape.Return -> {
                context.replaceWithLeadingWhitespace(ifStmt, "return ${info.cond} ? ${info.then} : ${info.els};")
            }
        }
    }

    private fun PsiElement.classify(): Context? {
        val ternary = ternaryAncestor() ?: return null
        var node: PsiElement? = ternary.parent
        while (node != null && node !is PsiFile) {
            when (node) {
                is JSVarStatement -> {
                    val v = node.variables.singleOrNull() ?: return null
                    if (v.initializerOrStub !== ternary) return null
                    val name = v.name ?: return null
                    return Context.Declaration(node, name, ternary)
                }
                is JSExpressionStatement -> {
                    val assign = node.expression as? JSAssignmentExpression ?: return null
                    if (assign.rOperand !== ternary) return null
                    val lhs = assign.lOperand?.text?.trim() ?: return null
                    return Context.Assignment(node, lhs, ternary)
                }
                is JSReturnStatement -> {
                    if (node.expression !== ternary) return null
                    return Context.Return(node, ternary)
                }
            }
            node = node.parent
        }
        return null
    }

    private fun PsiElement.ternaryAncestor(): JSConditionalExpression? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is JSConditionalExpression) return node
            node = node.parent
        }
        return null
    }

    private fun JSConditionalExpression.parts(): Triple<String, String, String>? {
        val cond = condition?.text?.trim() ?: return null
        val thenExpr = thenBranch?.text?.trim() ?: return null
        val elseExpr = elseBranch?.text?.trim() ?: return null
        return Triple(cond, thenExpr, elseExpr)
    }

    private fun Context.containerStmt(): PsiElement = when (this) {
        is Context.Declaration -> varStmt
        is Context.Assignment -> exprStmt
        is Context.Return -> retStmt
    }

    private sealed class JoinShape {
        data class DeclThenAssign(
            val varStmt: JSVarStatement,
            val kind: String,
            val name: String,
            val cond: String,
            val then: String,
            val els: String,
        ) : JoinShape()

        data class PlainAssign(val lhs: String, val cond: String, val then: String, val els: String) : JoinShape()
        data class Return(val cond: String, val then: String, val els: String) : JoinShape()
    }

    private fun PsiElement.ifAncestor(): JSIfStatement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is JSIfStatement) return node
            node = node.parent
        }
        return null
    }

    private fun JSIfStatement.classifyJoinable(): JoinShape? {
        val cond = condition?.text?.trim() ?: return null
        val thenBranch = thenBranch ?: return null
        val elseBranch = elseBranch ?: return null
        val thenStmt = unwrapSingleStatement(thenBranch) ?: return null
        val elseStmt = unwrapSingleStatement(elseBranch) ?: return null

        if (thenStmt is JSReturnStatement && elseStmt is JSReturnStatement) {
            val thenExpr = thenStmt.expression?.text?.trim() ?: return null
            val elseExpr = elseStmt.expression?.text?.trim() ?: return null
            return JoinShape.Return(cond, thenExpr, elseExpr)
        }

        val thenAssign = (thenStmt as? JSExpressionStatement)?.expression as? JSAssignmentExpression ?: return null
        val elseAssign = (elseStmt as? JSExpressionStatement)?.expression as? JSAssignmentExpression ?: return null
        val thenLhs = thenAssign.lOperand?.text?.trim() ?: return null
        val elseLhs = elseAssign.lOperand?.text?.trim() ?: return null
        if (thenLhs != elseLhs) return null
        val thenExpr = thenAssign.rOperand?.text?.trim() ?: return null
        val elseExpr = elseAssign.rOperand?.text?.trim() ?: return null

        val prev = prevSignificantSibling() as? JSVarStatement
        if (prev != null && prev.variables.size == 1) {
            val v = prev.variables[0]
            if (v.name == thenLhs && v.initializerOrStub == null) {
                val kind = prev.declarationKindLeaf() ?: return null
                return JoinShape.DeclThenAssign(prev, kind, thenLhs, cond, thenExpr, elseExpr)
            }
        }

        return JoinShape.PlainAssign(thenLhs, cond, thenExpr, elseExpr)
    }

    private fun unwrapSingleStatement(node: PsiElement): PsiElement? {
        if (node !is JSBlockStatement) return node
        val statements = node.children.filter {
            it !is PsiWhiteSpace &&
                it.text != "{" &&
                it.text != "}" &&
                !it.text.isBlank()
        }
        return statements.singleOrNull()
    }

    private fun JSVarStatement.declarationKindLeaf(): String? {
        var c = firstChild
        while (c != null) {
            val t = c.text
            if (t == "let" || t == "var" || t == "const") return t
            c = c.nextSibling
        }
        return null
    }

    private fun PsiElement.prevSignificantSibling(): PsiElement? {
        var s = prevSibling
        while (s != null && (s is PsiWhiteSpace || s.text.isBlank())) s = s.prevSibling
        return s
    }

    private fun SplitJoinContext.replaceWithLeadingWhitespace(element: PsiElement, replacement: String) {
        val range = element.rangeWithLeadingWhitespace()
        replace(range, range.leadingTextBefore(element) + replacement)
    }

    private fun PsiElement.rangeWithLeadingWhitespace(): TextRange =
        textRange.withLeadingWhitespace(this)

    private fun TextRange.withLeadingWhitespace(element: PsiElement): TextRange {
        var start = startOffset
        var prev = element.prevSibling
        while (prev != null && (prev is PsiWhiteSpace || prev.text.isBlank())) {
            start = prev.textRange.startOffset
            prev = prev.prevSibling
        }
        return TextRange(start, endOffset)
    }

    private fun TextRange.leadingTextBefore(element: PsiElement): String {
        val doc = element.containingFile.viewProvider.document ?: return ""
        return doc.getText(TextRange(startOffset, element.textRange.startOffset))
    }
}
