package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class TsTupleTypeHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (!isTsFile(element)) return false
        val tuple = element.tupleAncestor() ?: return false
        if (tuple.containsComment()) return false
        return tuple.members().size >= 2 && !tuple.text.contains('\n')
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (!isTsFile(element)) return false
        val tuple = element.tupleAncestor() ?: return false
        if (tuple.containsComment()) return false
        return tuple.members().size >= 2 && tuple.text.contains('\n')
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tuple = element.tupleAncestor() ?: return
        val members = tuple.members()
        val rebuilt = "[\n  " + members.joinToString(",\n  ") + ",\n]"
        context.replace(tuple, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val tuple = element.tupleAncestor() ?: return
        val members = tuple.members()
        context.replace(tuple, "[" + members.joinToString(", ") + "]")
    }

    // ---------- helpers ----------

    private fun isTsFile(element: PsiElement): Boolean {
        val ext = element.containingFile?.virtualFile?.extension ?: return false
        return ext in setOf("ts", "tsx", "mts", "cts")
    }

    private fun PsiElement.tupleAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node.javaClass.simpleName == "TypeScriptTupleTypeImpl") return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.members(): List<String> {
        val result = mutableListOf<String>()
        var child = firstChild
        while (child != null) {
            if (child !is PsiWhiteSpace &&
                child.text != "[" && child.text != "]" && child.text != "," &&
                !child.text.isBlank()
            ) {
                result += child.text.trim()
            }
            child = child.nextSibling
        }
        return result
    }
}
