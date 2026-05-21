package com.splitjoin.js

import com.intellij.json.JsonFileType
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings.TrailingCommaOption
import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine

class JsObjectHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is JSObjectLiteralExpression) return false
        if (isJsonFile(element)) return false
        if (objectChildren(element).isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is JSObjectLiteralExpression) return false
        if (isJsonFile(element)) return false
        if (objectChildren(element).isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val obj = element as JSObjectLiteralExpression
        val items = objectChildren(obj)
        val trailing = trailingCommaEnabled(element)
        val rebuilt = buildString {
            append("{\n")
            items.forEachIndexed { index, item ->
                append(item.text)
                if (index < items.size - 1 || trailing) append(",")
                append("\n")
            }
            append("}")
        }
        context.replace(obj, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val obj = element as JSObjectLiteralExpression
        val joined = objectChildren(obj).joinToString(", ") { it.text }
        context.replace(obj, "{ $joined }")
    }

    private fun isJsonFile(element: PsiElement): Boolean =
        element.containingFile?.fileType is JsonFileType

    private fun objectChildren(obj: JSObjectLiteralExpression): List<JSElement> =
        obj.propertiesIncludingSpreads.toList()

    private fun trailingCommaEnabled(element: PsiElement): Boolean {
        val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
        val js = settings.getCustomSettings(JSCodeStyleSettings::class.java)
        return js.ENFORCE_TRAILING_COMMA != TrailingCommaOption.Remove
    }
}
