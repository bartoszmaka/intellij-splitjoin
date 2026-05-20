package com.splitjoin.json

import com.intellij.json.psi.JsonObject
import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine

class JsonObjectHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is JsonObject) return false
        if (element.propertyList.isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is JsonObject) return false
        if (element.propertyList.isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val obj = element as JsonObject
        val properties = obj.propertyList
        val rebuilt = buildString {
            append("{\n")
            properties.forEachIndexed { index, property ->
                append(property.text)
                if (index < properties.size - 1) append(",")
                append("\n")
            }
            append("}")
        }
        context.replace(obj, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val obj = element as JsonObject
        val joined = obj.propertyList.joinToString(", ") { it.text }
        context.replace(obj, "{$joined}")
    }
}
