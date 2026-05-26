package com.splitjoin.html

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class XmlChildrenHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val tag = element.xmlTagAncestor() ?: return false
        if (tag.containsComment()) return false
        if (tag.isSelfClosing()) return false
        val children = tag.bodyChildren()
        return tag.isEligible(children) && !tag.text.contains('\n')
    }

    override fun canJoin(element: PsiElement): Boolean {
        val tag = element.xmlTagAncestor() ?: return false
        if (tag.containsComment()) return false
        if (tag.isSelfClosing()) return false
        val children = tag.bodyChildren()
        return tag.isEligible(children) && tag.text.contains('\n')
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tag = element.xmlTagAncestor() ?: return
        val name = tag.name
        val children = tag.bodyChildren()
        val isElementOnly = children.all { it is XmlTag }
        val rebuilt = if (isElementOnly) {
            "<$name>\n" + children.joinToString("\n") { "    ${it.text.trim()}" } + "\n</$name>"
        } else {
            val text = children.first().text.trim()
            "<$name>\n    $text\n</$name>"
        }
        context.replace(tag, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val tag = element.xmlTagAncestor() ?: return
        val name = tag.name
        val children = tag.bodyChildren()
        val joined = children.joinToString("") { it.text.trim() }
        context.replace(tag, "<$name>$joined</$name>")
    }

    private fun PsiElement.xmlTagAncestor(): XmlTag? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is XmlTag) return node
            node = node.parent
        }
        return null
    }

    private fun XmlTag.isSelfClosing(): Boolean {
        var c = lastChild
        while (c != null) {
            if (c is PsiWhiteSpace) { c = c.prevSibling; continue }
            return c.text == "/>"
        }
        return false
    }

    private fun XmlTag.bodyChildren(): List<PsiElement> {
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

    private fun XmlTag.isEligible(children: List<PsiElement>): Boolean {
        if (children.isEmpty()) return false
        val hasText = children.any { it is XmlText }
        val hasElement = children.any { it is XmlTag }
        if (hasText && hasElement) return false
        if (hasText) return children.size == 1
        return children.all { it is XmlTag }
    }
}
