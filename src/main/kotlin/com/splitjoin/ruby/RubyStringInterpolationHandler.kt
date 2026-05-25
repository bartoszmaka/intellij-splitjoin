package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.basicTypes.stringLiterals.RExpressionSubstitutionImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.basicTypes.stringLiterals.baseString.RDStringLiteralImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RCompoundStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.expressions.RMathBinExpressionImpl

class RubyStringInterpolationHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val literal = element.dStringAncestor() ?: return false
        if (literal.containsComment()) return false
        return literal.substitutions().isNotEmpty()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val chain = element.plusChainRoot() ?: return false
        if (chain.containsComment()) return false
        val terms = chain.flattenPlusChain() ?: return false
        // Require at least one string term AND at least one expression term.
        if (terms.none { it.isString }) return false
        if (terms.none { !it.isString }) return false
        return terms.size >= 2
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val literal = element.dStringAncestor() ?: return
        val rebuilt = rebuildAsConcat(literal)
        context.replace(literal, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val chain = element.plusChainRoot() ?: return
        val terms = chain.flattenPlusChain() ?: return
        val rebuilt = rebuildAsInterpolation(terms)
        context.replace(chain, rebuilt)
    }

    // ---------- split direction ----------

    private fun rebuildAsConcat(literal: RDStringLiteralImpl): String {
        val text = literal.text
        // Identify substitution offsets relative to literal start.
        val substitutions = literal.substitutions()
        val literalStart = literal.textRange.startOffset

        // Content is everything between the opening and closing double-quote.
        val contentStart = 1                       // skip opening "
        val contentEnd = text.length - 1           // skip closing "

        val terms = mutableListOf<String>()
        var cursor = contentStart
        for (sub in substitutions) {
            val subStart = sub.textRange.startOffset - literalStart
            val subEnd = sub.textRange.endOffset - literalStart
            if (subStart > cursor) {
                val staticPart = text.substring(cursor, subStart)
                terms += "\"$staticPart\""
            }
            val expr = sub.expressionText() ?: continue
            terms += "$expr.to_s"
            cursor = subEnd
        }
        if (cursor < contentEnd) {
            val staticPart = text.substring(cursor, contentEnd)
            terms += "\"$staticPart\""
        }
        return terms.joinToString(" + ")
    }

    private fun RDStringLiteralImpl.substitutions(): List<RExpressionSubstitutionImpl> {
        val result = mutableListOf<RExpressionSubstitutionImpl>()
        var child = firstChild
        while (child != null) {
            if (child is RExpressionSubstitutionImpl) result += child
            child = child.nextSibling
        }
        return result
    }

    private fun RExpressionSubstitutionImpl.expressionText(): String? =
        children.filterIsInstance<RCompoundStatementImpl>().firstOrNull()?.text?.trim()

    // ---------- join direction ----------

    private data class Term(val text: String, val isString: Boolean)

    private fun RMathBinExpressionImpl.flattenPlusChain(): List<Term>? {
        // Only `+` operator chains qualify.
        if (!isPlusOperation()) return null
        val terms = mutableListOf<Term>()
        if (!collectInto(terms)) return null
        return terms
    }

    private fun RMathBinExpressionImpl.collectInto(terms: MutableList<Term>): Boolean {
        if (!isPlusOperation()) return false
        // Children: [left, op-leaf, right]. Filter to non-trivial children.
        val significant = children.filter { it !is PsiWhiteSpace }
        if (significant.size != 2) return false
        val (left, right) = significant[0] to significant[1]
        if (!collectTerm(left, terms)) return false
        if (!collectTerm(right, terms)) return false
        return true
    }

    private fun collectTerm(node: PsiElement, terms: MutableList<Term>): Boolean {
        if (node is RMathBinExpressionImpl && node.isPlusOperation()) {
            return node.collectInto(terms)
        }
        val text = node.text.trim()
        val isString = node is RDStringLiteralImpl
        terms += Term(text, isString)
        return true
    }

    private fun RMathBinExpressionImpl.isPlusOperation(): Boolean {
        // The operator is a leaf token among the children. Look for "+" text.
        var child = firstChild
        while (child != null) {
            if (child !is PsiWhiteSpace && child.text == "+") return true
            child = child.nextSibling
        }
        return false
    }

    private fun rebuildAsInterpolation(terms: List<Term>): String {
        val sb = StringBuilder("\"")
        for (term in terms) {
            if (term.isString) {
                // Strip surrounding double-quotes from the literal text.
                sb.append(term.text.trim('"'))
            } else {
                // Strip trailing .to_s if present.
                val expr = term.text.removeSuffix(".to_s")
                sb.append("#{").append(expr).append("}")
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    // ---------- ancestor lookup ----------

    private fun PsiElement.dStringAncestor(): RDStringLiteralImpl? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is RDStringLiteralImpl) return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.plusChainRoot(): RMathBinExpressionImpl? {
        // Walk up to the OUTERMOST `+` chain (the one whose parent is not also a + chain).
        var node: PsiElement? = this
        var outermost: RMathBinExpressionImpl? = null
        while (node != null && node !is PsiFile) {
            if (node is RMathBinExpressionImpl) outermost = node
            node = node.parent
        }
        return outermost?.takeIf { it.isPlusOperation() }
    }
}
