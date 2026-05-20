package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine
import org.jetbrains.plugins.ruby.ruby.lang.psi.assoc.RAssoc
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RAssocList

class RubyHashHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RAssocList) return false
        if (assocsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RAssocList) return false
        if (assocsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val list = element as RAssocList
        val entries = assocsOf(list)
        val rebuilt = buildString {
            append("{\n")
            entries.forEach { entry ->
                append(entry.text)
                append(",\n")
            }
            append("}")
        }
        context.replace(list, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val list = element as RAssocList
        val joined = assocsOf(list).joinToString(", ") { it.text }
        context.replace(list, "{ $joined }")
    }

    private fun assocsOf(list: RAssocList): List<RAssoc> =
        list.children.filterIsInstance<RAssoc>()
}
