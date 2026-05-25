package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class TsGenericParamsHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (!isTsFile(element)) return false
        val list = element.genericListAncestor() ?: return false
        if (list.containsComment()) return false
        return list.params().size >= 2 && !list.text.contains('\n')
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (!isTsFile(element)) return false
        val list = element.genericListAncestor() ?: return false
        if (list.containsComment()) return false
        return list.params().size >= 2 && list.text.contains('\n')
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val list = element.genericListAncestor() ?: return
        val params = list.params()
        val rebuilt = "<\n  " + params.joinToString(",\n  ") + ",\n>"
        context.replace(list, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val list = element.genericListAncestor() ?: return
        val params = list.params()
        context.replace(list, "<" + params.joinToString(", ") + ">")
    }

    // ---------- helpers ----------

    private fun isTsFile(element: PsiElement): Boolean {
        val ext = element.containingFile?.virtualFile?.extension ?: return false
        return ext in setOf("ts", "tsx", "mts", "cts")
    }

    private fun PsiElement.genericListAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            val name = node.javaClass.simpleName
            if (name == "TypeScriptTypeArgumentListImpl" || name == "TypeScriptTypeParameterListImpl") return node
            node = node.parent
        }
        return null
    }

    /** Returns param text entries (each preserving constraint/default clauses verbatim). */
    private fun PsiElement.params(): List<String> {
        val result = mutableListOf<String>()
        var child = firstChild
        while (child != null) {
            if (child !is PsiWhiteSpace &&
                child.text != "<" && child.text != ">" && child.text != "," &&
                !child.text.isBlank()
            ) {
                result += child.text.trim()
            }
            child = child.nextSibling
        }
        return result
    }
}
