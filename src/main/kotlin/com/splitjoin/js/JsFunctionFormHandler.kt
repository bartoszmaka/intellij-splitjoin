package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSBlockStatement
import com.intellij.lang.javascript.psi.JSReturnStatement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsFunctionFormHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val fn = element.functionExpressionAncestor() ?: return false
        if (fn.containsComment()) return false
        return fn.isFunctionForm() && !fn.isGenerator() && !fn.isNamed()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val fn = element.functionExpressionAncestor() ?: return false
        if (fn.containsComment()) return false
        return fn.isArrowForm()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val fn = element.functionExpressionAncestor() ?: return
        val async = fn.asyncPrefix()
        val params = fn.parameterListText() ?: return
        val body = fn.blockBody() ?: return
        val arrowBody = arrowBodyFromBlock(body)
        val rebuilt = "$async$params => $arrowBody"
        context.replaceWithLeadingWhitespace(fn, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val fn = element.functionExpressionAncestor() ?: return
        val async = fn.asyncPrefix()
        val params = fn.parameterListText() ?: return
        val arrowBodyText = fn.arrowBodyText() ?: return
        val blockText = if (arrowBodyText.trimStart().startsWith("{")) {
            arrowBodyText.trim()
        } else {
            "{ return $arrowBodyText; }"
        }
        val rebuilt = "${async}function $params $blockText"
        context.replaceWithLeadingWhitespace(fn, rebuilt)
    }

    private fun PsiElement.functionExpressionAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node.javaClass.simpleName == "TypeScriptFunctionExpressionImpl") return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.isFunctionForm(): Boolean {
        var c = firstChild
        while (c != null) {
            if (c is PsiWhiteSpace) {
                c = c.nextSibling
                continue
            }
            if (c.javaClass.simpleName == "JSAttributeListImpl") {
                c = c.nextSibling
                continue
            }
            return c.text == "function"
        }
        return false
    }

    private fun PsiElement.isArrowForm(): Boolean {
        var c = firstChild
        while (c != null) {
            if (c.text == "=>") return true
            c = c.nextSibling
        }
        return false
    }

    private fun PsiElement.isGenerator(): Boolean {
        var c = firstChild
        while (c != null) {
            if (c.text == "*") return true
            c = c.nextSibling
        }
        return false
    }

    private fun PsiElement.isNamed(): Boolean {
        var sawFunction = false
        var c = firstChild
        while (c != null) {
            if (c is PsiWhiteSpace) {
                c = c.nextSibling
                continue
            }
            val text = c.text
            if (text == "function") {
                sawFunction = true
                c = c.nextSibling
                continue
            }
            if (sawFunction) {
                if (text == "*") {
                    c = c.nextSibling
                    continue
                }
                if (c.javaClass.simpleName == "TypeScriptParameterListImpl") return false
                return true
            }
            c = c.nextSibling
        }
        return false
    }

    private fun PsiElement.asyncPrefix(): String {
        var c = firstChild
        while (c != null) {
            if (c.javaClass.simpleName == "JSAttributeListImpl" && c.text.contains("async")) return "async "
            c = c.nextSibling
        }
        return ""
    }

    private fun PsiElement.parameterListText(): String? {
        var c = firstChild
        while (c != null) {
            if (c.javaClass.simpleName == "TypeScriptParameterListImpl") return c.text
            c = c.nextSibling
        }
        return null
    }

    private fun PsiElement.blockBody(): JSBlockStatement? {
        var c = firstChild
        while (c != null) {
            if (c is JSBlockStatement) return c
            c = c.nextSibling
        }
        return null
    }

    private fun arrowBodyFromBlock(block: JSBlockStatement): String {
        val statements = block.children.filter {
            it !is PsiWhiteSpace &&
                it.text != "{" &&
                it.text != "}" &&
                !it.text.isBlank()
        }
        if (statements.size == 1 && statements[0] is JSReturnStatement) {
            val ret = statements[0] as JSReturnStatement
            val expr = ret.expression?.text?.trim()
            if (expr != null) return expr
        }
        return block.text
    }

    private fun PsiElement.arrowBodyText(): String? {
        var sawArrow = false
        var c = firstChild
        while (c != null) {
            if (c.text == "=>") {
                sawArrow = true
                c = c.nextSibling
                continue
            }
            if (sawArrow) {
                if (c is PsiWhiteSpace) {
                    c = c.nextSibling
                    continue
                }
                return c.text
            }
            c = c.nextSibling
        }
        return null
    }

    private fun SplitJoinContext.replaceWithLeadingWhitespace(element: PsiElement, replacement: String) {
        val range = element.rangeWithLeadingWhitespace()
        replace(range, range.leadingTextBefore(element) + replacement)
    }

    private fun PsiElement.rangeWithLeadingWhitespace(): TextRange {
        var start = textRange.startOffset
        var prev = prevSibling
        while (prev != null && (prev is PsiWhiteSpace || prev.text.isBlank())) {
            start = prev.textRange.startOffset
            prev = prev.prevSibling
        }
        return TextRange(start, textRange.endOffset)
    }

    private fun TextRange.leadingTextBefore(element: PsiElement): String {
        val doc = element.containingFile.viewProvider.document ?: return ""
        return doc.getText(TextRange(startOffset, element.textRange.startOffset))
    }
}
