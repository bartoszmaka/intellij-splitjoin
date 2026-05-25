package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.xml.XmlAttribute
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsxSelfCloseToggleHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val tag = element.jsxAncestor() ?: return false
        if (tag.containsComment()) return false
        // Only handle tags with no attributes (JsxAttributeHandler handles those)
        if (tag.hasAttributes()) return false
        return tag.isSelfClosing()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val tag = element.jsxAncestor() ?: return false
        if (tag.containsComment()) return false
        // Only handle tags with no attributes (JsxAttributeHandler handles those)
        if (tag.hasAttributes()) return false
        return tag.isOpenForm() && tag.isBodyEmpty()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tag = element.jsxAncestor() ?: return
        val tagName = tag.tagName() ?: return
        context.replace(tag, "<$tagName></$tagName>")
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val tag = element.jsxAncestor() ?: return
        val tagName = tag.tagName() ?: return
        context.replace(tag, "<$tagName/>")
    }

    // ---------- helpers ----------

    private fun PsiElement.jsxAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node.javaClass.simpleName == "JSXXmlLiteralExpressionImpl") return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.hasAttributes(): Boolean {
        // Check if this JSXXmlLiteralExpressionImpl has any XmlAttribute children
        var c = firstChild
        while (c != null) {
            if (c is XmlAttribute) return true
            c = c.nextSibling
        }
        return false
    }

    private fun PsiElement.tagName(): String? {
        // Tokens after `<` and before `>` or `/>`. For a simple tag, the second token.
        var c = firstChild
        var seenOpen = false
        while (c != null) {
            if (c is PsiWhiteSpace) { c = c.nextSibling; continue }
            if (c.text == "<") { seenOpen = true; c = c.nextSibling; continue }
            if (seenOpen) return c.text
            c = c.nextSibling
        }
        return null
    }

    private fun PsiElement.isSelfClosing(): Boolean {
        var c = lastChild
        while (c != null) {
            if (c is PsiWhiteSpace) { c = c.prevSibling; continue }
            return c.text == "/>"
        }
        return false
    }

    private fun PsiElement.isOpenForm(): Boolean = !isSelfClosing()

    /** True when there is no content between the opening `>` and the closing `</`. */
    private fun PsiElement.isBodyEmpty(): Boolean {
        // Find the first `>` (opening tag end), then check that everything until `</`
        // is whitespace.
        var c = firstChild
        var sawOpenEnd = false
        while (c != null) {
            if (!sawOpenEnd) {
                if (c.text == ">") sawOpenEnd = true
            } else {
                if (c.text == "</") return true
                if (c !is PsiWhiteSpace && c.text.isNotBlank()) return false
            }
            c = c.nextSibling
        }
        return false
    }
}
