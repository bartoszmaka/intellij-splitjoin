package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class TsTypeLiteralHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (!isTsFile(element)) return false
        val obj = element.typeObjectAncestor() ?: return false
        if (obj.containsComment()) return false
        return obj.members().size >= 2 && !obj.text.contains('\n')
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (!isTsFile(element)) return false
        val obj = element.typeObjectAncestor() ?: return false
        if (obj.containsComment()) return false
        return obj.members().size >= 2 && obj.text.contains('\n')
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val obj = element.typeObjectAncestor() ?: return
        val members = obj.members()
        val rebuilt = "{\n  " + members.joinToString(";\n  ") + ";\n}"
        context.replace(obj, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val obj = element.typeObjectAncestor() ?: return
        val members = obj.members()
        context.replace(obj, "{ " + members.joinToString("; ") + " }")
    }

    // ---------- helpers ----------

    private fun isTsFile(element: PsiElement): Boolean {
        val ext = element.containingFile?.virtualFile?.extension ?: return false
        return ext in setOf("ts", "tsx", "mts", "cts")
    }

    private fun PsiElement.typeObjectAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node.javaClass.simpleName == "TypeScriptObjectTypeImpl") return node
            node = node.parent
        }
        return null
    }

    /** Returns member texts (property signatures, method signatures, index signatures, etc.). */
    private fun PsiElement.members(): List<String> {
        val result = mutableListOf<String>()
        var child = firstChild
        while (child != null) {
            if (child !is PsiWhiteSpace &&
                child.text != "{" && child.text != "}" && child.text != ";" && child.text != "," &&
                !child.text.isBlank()
            ) {
                result += child.text.trim()
            }
            child = child.nextSibling
        }
        return result
    }
}
