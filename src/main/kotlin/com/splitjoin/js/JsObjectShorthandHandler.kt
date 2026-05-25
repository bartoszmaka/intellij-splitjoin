package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsObjectShorthandHandler : SplitJoinHandler {

    private enum class Form { SHORTHAND, KEY_KEY, INELIGIBLE }

    override fun canSplit(element: PsiElement): Boolean {
        val obj = element.objectLiteralAncestor() ?: return false
        if (obj.containsComment()) return false
        val eligible = obj.eligibilityProfile()
        return eligible.shorthand.isNotEmpty() && eligible.keyKey.isEmpty()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val obj = element.objectLiteralAncestor() ?: return false
        if (obj.containsComment()) return false
        val eligible = obj.eligibilityProfile()
        return eligible.keyKey.isNotEmpty() && eligible.shorthand.isEmpty()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val obj = element.objectLiteralAncestor() ?: return
        // Collect all replacements first, then apply in reverse order to preserve indices
        val replacements = mutableListOf<Pair<JSProperty, String>>()
        for (prop in obj.children) {
            if (prop is JSProperty && prop.classify() == Form.SHORTHAND) {
                val name = prop.name ?: continue
                replacements.add(prop to "$name: $name")
            }
        }
        // Apply in reverse order
        for ((prop, replacement) in replacements.reversed()) {
            context.replace(prop, replacement)
        }
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val obj = element.objectLiteralAncestor() ?: return
        // Collect all replacements first, then apply in reverse order to preserve indices
        val replacements = mutableListOf<Pair<JSProperty, String>>()
        for (prop in obj.children) {
            if (prop is JSProperty && prop.classify() == Form.KEY_KEY) {
                val name = prop.name ?: continue
                replacements.add(prop to name)
            }
        }
        // Apply in reverse order
        for ((prop, replacement) in replacements.reversed()) {
            context.replace(prop, replacement)
        }
    }

    // ---------- helpers ----------

    private data class Eligibility(val shorthand: List<JSProperty>, val keyKey: List<JSProperty>)

    private fun JSObjectLiteralExpression.eligibilityProfile(): Eligibility {
        val sh = mutableListOf<JSProperty>()
        val kk = mutableListOf<JSProperty>()
        for (child in children) {
            if (child !is JSProperty) continue
            when (child.classify()) {
                Form.SHORTHAND -> sh += child
                Form.KEY_KEY -> kk += child
                Form.INELIGIBLE -> {}
            }
        }
        return Eligibility(sh, kk)
    }

    private fun JSProperty.classify(): Form {
        val name = name ?: return Form.INELIGIBLE
        // ES6PropertyImpl is the shorthand variant; check by absence of explicit value text.
        // More robustly: a shorthand property has no `:` in its text and value is a JSReferenceExpression matching name.
        if (!text.contains(':')) return Form.SHORTHAND
        val value = value ?: return Form.INELIGIBLE
        if (value !is JSReferenceExpression) return Form.INELIGIBLE
        // Reject computed keys (text starts with `[`)
        if (text.trimStart().startsWith("[")) return Form.INELIGIBLE
        return if (value.text.trim() == name) Form.KEY_KEY else Form.INELIGIBLE
    }

    private fun PsiElement.objectLiteralAncestor(): JSObjectLiteralExpression? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is JSObjectLiteralExpression) return node
            node = node.parent
        }
        return null
    }
}
