package com.splitjoin.js

import com.intellij.lang.javascript.formatter.JSCodeStyleSettings
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings.TrailingCommaOption
import com.intellij.lang.javascript.psi.JSArgumentList
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine

class JsCallArgsHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is JSArgumentList) return false
        if (argumentChildren(element).isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is JSArgumentList) return false
        if (argumentChildren(element).isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val list = element as JSArgumentList
        val args = argumentChildren(list)
        val trailing = trailingCommaEnabled(element)
        val rebuilt = buildString {
            append("(\n")
            args.forEachIndexed { index, arg ->
                append(arg.text)
                if (index < args.size - 1 || trailing) append(",")
                append("\n")
            }
            append(")")
        }
        context.replace(list, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val list = element as JSArgumentList
        val joined = argumentChildren(list).joinToString(", ") { it.text }
        context.replace(list, "($joined)")
    }

    private fun argumentChildren(list: JSArgumentList): List<JSExpression> =
        list.arguments.toList()

    private fun trailingCommaEnabled(element: PsiElement): Boolean {
        val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
        val js = settings.getCustomSettings(JSCodeStyleSettings::class.java)
        return js.ENFORCE_TRAILING_COMMA != TrailingCommaOption.Remove
    }
}
