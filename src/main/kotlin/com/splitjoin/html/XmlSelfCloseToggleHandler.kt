package com.splitjoin.html

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.xml.XmlTag
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class XmlSelfCloseToggleHandler : SplitJoinHandler {

    companion object {
        private val VOID_ELEMENTS = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
        )
    }

    override fun canSplit(element: PsiElement): Boolean {
        val tag = element.xmlTagAncestor() ?: return false
        if (tag.containsComment()) return false
        if (!tag.isSelfClosing()) return false
        if (tag.isHtmlVoidElement()) return false
        return true
    }

    override fun canJoin(element: PsiElement): Boolean {
        val tag = element.xmlTagAncestor() ?: return false
        if (tag.containsComment()) return false
        if (tag.isSelfClosing()) return false
        return tag.isBodyEmpty()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tag = element.xmlTagAncestor() ?: return
        context.replace(tag, "<${tag.name}></${tag.name}>")
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val tag = element.xmlTagAncestor() ?: return
        context.replace(tag, "<${tag.name}/>")
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

    private fun XmlTag.isBodyEmpty(): Boolean {
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

    private fun XmlTag.isHtmlVoidElement(): Boolean {
        val ext = containingFile?.virtualFile?.extension ?: return false
        if (ext != "html" && ext != "htm") return false
        return name.lowercase() in VOID_ELEMENTS
    }
}
