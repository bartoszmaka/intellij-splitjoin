package com.splitjoin.json

import com.intellij.json.psi.JsonArray
import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine

class JsonArrayHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is JsonArray) return false
        if (element.valueList.isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is JsonArray) return false
        if (element.valueList.isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val arr = element as JsonArray
        val values = arr.valueList
        val rebuilt = buildString {
            append("[\n")
            values.forEachIndexed { index, value ->
                append(value.text)
                if (index < values.size - 1) append(",")
                append("\n")
            }
            append("]")
        }
        context.replace(arr, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val arr = element as JsonArray
        val joined = arr.valueList.joinToString(", ") { it.text }
        context.replace(arr, "[$joined]")
    }
}
