package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSBinaryExpression
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsTemplateLiteralHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val tmpl = element.templateAncestor() ?: return false
        if (tmpl.containsComment()) return false
        if (tmpl.isTagged()) return false
        return tmpl.hasSubstitution()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val chain = element.plusChainRoot() ?: return false
        if (chain.containsComment()) return false
        val terms = chain.flattenPlusChain() ?: return false
        return terms.any { it.isString } && terms.any { !it.isString }
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tmpl = element.templateAncestor() ?: return
        val rebuilt = rebuildAsConcat(tmpl) ?: return
        context.replaceWithLeadingWhitespace(tmpl, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val chain = element.plusChainRoot() ?: return
        val terms = chain.flattenPlusChain() ?: return
        val rebuilt = rebuildAsTemplate(terms)
        context.replaceWithLeadingWhitespace(chain, rebuilt)
    }

    private fun rebuildAsConcat(tmpl: PsiElement): String? {
        val terms = mutableListOf<String>()
        val static = StringBuilder()
        var child = tmpl.firstChild?.nextSibling

        while (child != null && child.text != "`") {
            if (child.text == "$" && child.nextSibling?.text == "{") {
                if (static.isNotEmpty()) {
                    terms += "'" + static.toString() + "'"
                    static.setLength(0)
                }
                child = child.nextSibling?.nextSibling
                val expr = StringBuilder()
                while (child != null && child.text != "}") {
                    expr.append(child.text)
                    child = child.nextSibling
                }
                if (child?.text != "}") return null
                terms += expr.toString().trim()
                child = child.nextSibling
            } else {
                static.append(child.text)
                child = child.nextSibling
            }
        }

        if (static.isNotEmpty()) {
            terms += "'" + static.toString() + "'"
        }
        return terms.takeIf { it.isNotEmpty() }?.joinToString(" + ")
    }

    private fun PsiElement.hasSubstitution(): Boolean {
        var child = firstChild
        while (child != null) {
            if (child.text == "$" && child.nextSibling?.text == "{") return true
            child = child.nextSibling
        }
        return false
    }

    private fun PsiElement.isTagged(): Boolean {
        val parentName = parent?.javaClass?.simpleName ?: return false
        return parentName.contains("Tagged")
    }

    private data class Term(val text: String, val isString: Boolean)

    private fun PsiElement.plusChainRoot(): JSBinaryExpression? {
        var node: PsiElement? = this
        var outermost: JSBinaryExpression? = null
        while (node != null && node !is PsiFile) {
            if (node is JSBinaryExpression) outermost = node
            node = node.parent
        }
        return outermost?.takeIf { it.isPlusOperation() }
    }

    private fun JSBinaryExpression.flattenPlusChain(): List<Term>? {
        if (!isPlusOperation()) return null
        val terms = mutableListOf<Term>()
        if (!collectInto(terms)) return null
        return terms
    }

    private fun JSBinaryExpression.collectInto(terms: MutableList<Term>): Boolean {
        if (!isPlusOperation()) return false
        val left = lOperand ?: return false
        val right = rOperand ?: return false
        if (!collectTerm(left, terms)) return false
        if (!collectTerm(right, terms)) return false
        return true
    }

    private fun collectTerm(node: PsiElement, terms: MutableList<Term>): Boolean {
        if (node is JSBinaryExpression && node.isPlusOperation()) {
            return node.collectInto(terms)
        }
        val text = node.text.trim()
        val isString = node is JSLiteralExpression && (text.startsWith("'") || text.startsWith("\""))
        terms += Term(text, isString)
        return true
    }

    private fun JSBinaryExpression.isPlusOperation(): Boolean {
        var child = firstChild
        while (child != null) {
            if (child !is PsiWhiteSpace && child.text == "+") return true
            child = child.nextSibling
        }
        return false
    }

    private fun rebuildAsTemplate(terms: List<Term>): String {
        val sb = StringBuilder("`")
        for (term in terms) {
            if (term.isString) {
                sb.append(term.text.trim('\'').trim('"'))
            } else {
                sb.append("\${").append(term.text).append("}")
            }
        }
        sb.append("`")
        return sb.toString()
    }

    private fun PsiElement.templateAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node.javaClass.simpleName == "JSStringTemplateExpressionImpl") return node
            node = node.parent
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
