package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RArray
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RListOfExpressions

class RubyArrayHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RArray) return false
        if (elementsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RArray) return false
        if (elementsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val array = element as RArray
        val elements = elementsOf(array)
        val rebuilt = buildString {
            append("[\n")
            elements.forEach { e ->
                append(e.text)
                append(",\n")
            }
            append("]")
        }
        context.replace(array, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val array = element as RArray
        val joined = elementsOf(array).joinToString(", ") { it.text }
        context.replace(array, "[$joined]")
    }

    private fun elementsOf(array: RArray): List<PsiElement> {
        val list = array.children.filterIsInstance<RListOfExpressions>().firstOrNull()
            ?: return emptyList()
        return list.children.toList()
    }
}
