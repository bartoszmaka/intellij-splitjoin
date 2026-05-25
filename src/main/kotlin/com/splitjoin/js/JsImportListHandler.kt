package com.splitjoin.js

import com.intellij.lang.javascript.formatter.JSCodeStyleSettings
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings.TrailingCommaOption
import com.intellij.lang.javascript.psi.JSElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine

class JsImportListHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val named = element.namedImportsAncestor() ?: return false
        if (named.containsComment()) return false
        val specs = named.children.filter { it::class.simpleName == "ES6ImportSpecifierImpl" }
        return specs.size >= 2 && named.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val named = element.namedImportsAncestor() ?: return false
        if (named.containsComment()) return false
        val specs = named.children.filter { it::class.simpleName == "ES6ImportSpecifierImpl" }
        return specs.size >= 2 && !named.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val named = element.namedImportsAncestor() ?: return
        val specs = named.children.filter { it::class.simpleName == "ES6ImportSpecifierImpl" }
        val trailing = trailingCommaEnabled(element)
        val rebuilt = buildString {
            append("{\n")
            specs.forEachIndexed { index, spec ->
                append("    ").append(spec.text)
                if (index < specs.size - 1 || trailing) append(",")
                append("\n")
            }
            append("}")
        }
        context.replace(named, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val named = element.namedImportsAncestor() ?: return
        val specs = named.children.filter { it::class.simpleName == "ES6ImportSpecifierImpl" }
        context.replace(named, "{ " + specs.joinToString(", ") { it.text } + " }")
    }

    private fun PsiElement.namedImportsAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node::class.simpleName == "ES6NamedImportsImpl") return node
            node = node.parent
        }
        return null
    }

    private fun trailingCommaEnabled(element: PsiElement): Boolean {
        val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
        val js = settings.getCustomSettings(JSCodeStyleSettings::class.java)
        return js.ENFORCE_TRAILING_COMMA != TrailingCommaOption.Remove
    }
}
