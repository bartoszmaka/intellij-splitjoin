package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.xml.XmlAttribute
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsxChildrenHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val tag = element.jsxAncestor() ?: return false
        if (tag.containsComment()) return false
        if (tag.isSelfClosing()) return false
        if (tag.hasAttributes()) return false
        val children = tag.bodyChildren()
        return tag.isEligible(children) && !tag.text.contains('\n')
    }

    override fun canJoin(element: PsiElement): Boolean {
        val tag = element.jsxAncestor() ?: return false
        if (tag.containsComment()) return false
        if (tag.isSelfClosing()) return false
        if (tag.hasAttributes()) return false
        val children = tag.bodyChildren()
        return tag.isEligible(children) && tag.text.contains('\n')
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tag = element.jsxAncestor() ?: return
        val tagName = tag.tagName() ?: return
        val children = tag.bodyChildren()
        val isElementOnly = children.all { it.javaClass.simpleName == "JSXXmlLiteralExpressionImpl" }
        val rebuilt = if (isElementOnly) {
            "<$tagName>\n" + children.joinToString("\n") { "    ${it.text.trim()}" } + "\n</$tagName>"
        } else {
            val text = children.first().text.trim()
            "<$tagName>\n    $text\n</$tagName>"
        }
        context.replace(tag, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val tag = element.jsxAncestor() ?: return
        val tagName = tag.tagName() ?: return
        val children = tag.bodyChildren()
        val joined = children.joinToString("") { it.text.trim() }
        context.replace(tag, "<$tagName>$joined</$tagName>")
    }

    private fun PsiElement.jsxAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node.javaClass.simpleName == "JSXXmlLiteralExpressionImpl") return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.hasAttributes(): Boolean {
        var c = firstChild
        while (c != null) {
            if (c is XmlAttribute) return true
            c = c.nextSibling
        }
        return false
    }

    private fun PsiElement.tagName(): String? {
        var c = firstChild; var seenOpen = false
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

    private fun PsiElement.bodyChildren(): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        var c = firstChild
        var inBody = false
        while (c != null) {
            if (!inBody) {
                if (c.text == ">") inBody = true
            } else {
                if (c.text == "</") break
                if (c !is PsiWhiteSpace && c.text.isNotBlank()) result += c
            }
            c = c.nextSibling
        }
        return result
    }

    private fun PsiElement.isEligible(children: List<PsiElement>): Boolean {
        if (children.isEmpty()) return false
        val isTextNode: (PsiElement) -> Boolean = { it.javaClass.simpleName == "JSXmlTextImpl" }
        val isElement: (PsiElement) -> Boolean = { it.javaClass.simpleName == "JSXXmlLiteralExpressionImpl" }

        val hasText = children.any(isTextNode)
        val hasElement = children.any(isElement)
        if (hasText && hasElement) return false

        if (hasText) return children.size == 1
        return children.all(isElement)
    }
}
