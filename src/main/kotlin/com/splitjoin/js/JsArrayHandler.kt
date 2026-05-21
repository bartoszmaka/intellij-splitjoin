package com.splitjoin.js

import com.intellij.lang.javascript.formatter.JSCodeStyleSettings
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings.TrailingCommaOption
import com.intellij.lang.javascript.psi.JSArrayLiteralExpression
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine

class JsArrayHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is JSArrayLiteralExpression) return false
        if (arrayChildren(element).isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is JSArrayLiteralExpression) return false
        if (arrayChildren(element).isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val arr = element as JSArrayLiteralExpression
        val items = arrayChildren(arr)
        val trailing = trailingCommaEnabled(element)
        val rebuilt = buildString {
            append("[\n")
            items.forEachIndexed { index, item ->
                append(item.text)
                if (index < items.size - 1 || trailing) append(",")
                append("\n")
            }
            append("]")
        }
        context.replace(arr, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val arr = element as JSArrayLiteralExpression
        val joined = arrayChildren(arr).joinToString(", ") { it.text }
        context.replace(arr, "[$joined]")
    }

    private fun arrayChildren(arr: JSArrayLiteralExpression): List<JSExpression> =
        arr.expressions.toList()

    private fun trailingCommaEnabled(element: PsiElement): Boolean {
        val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
        val js = settings.getCustomSettings(JSCodeStyleSettings::class.java)
        return js.ENFORCE_TRAILING_COMMA != TrailingCommaOption.Remove
    }
}
