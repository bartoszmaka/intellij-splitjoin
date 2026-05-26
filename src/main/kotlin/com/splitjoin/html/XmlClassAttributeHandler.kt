package com.splitjoin.html

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlAttribute
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class XmlClassAttributeHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (!element.isHtmlOrXmlFile()) return false
        val attr = element.classAttrAncestor() ?: return false
        if (attr.containsComment()) return false
        val value = attr.value ?: return false
        if (value.contains('\n')) return false
        return value.trim().split(Regex("\\s+")).size >= 2
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (!element.isHtmlOrXmlFile()) return false
        val attr = element.classAttrAncestor() ?: return false
        if (attr.containsComment()) return false
        val value = attr.value ?: return false
        if (!value.contains('\n')) return false
        return value.trim().split(Regex("\\s+")).size >= 2
    }

    private fun PsiElement.isHtmlOrXmlFile(): Boolean {
        val ext = containingFile?.virtualFile?.extension?.lowercase() ?: return false
        return ext == "html" || ext == "htm" || ext == "xml" || ext == "xhtml"
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val attr = element.classAttrAncestor() ?: return
        val classes = attr.value?.trim()?.split(Regex("\\s+")) ?: return
        val joined = classes.joinToString("\n     ")
        context.replace(attr, "class=\"$joined\"")
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val attr = element.classAttrAncestor() ?: return
        val classes = attr.value?.trim()?.split(Regex("\\s+")) ?: return
        context.replace(attr, "class=\"${classes.joinToString(" ")}\"")
    }

    private fun PsiElement.classAttrAncestor(): XmlAttribute? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is XmlAttribute && node.name == "class") return node
            node = node.parent
        }
        return null
    }
}
