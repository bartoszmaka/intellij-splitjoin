package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RArgument
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RFunctionArgumentList

class RubyMethodDefParamsHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RFunctionArgumentList) return false
        if (paramsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RFunctionArgumentList) return false
        if (paramsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val list = element as RFunctionArgumentList
        val params = paramsOf(list)
        val rebuilt = buildString {
            params.forEachIndexed { index, param ->
                append(param.text)
                append(",")
                if (index < params.size - 1) append("\n")
            }
        }
        context.replace(list, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val list = element as RFunctionArgumentList
        val joined = paramsOf(list).joinToString(", ") { it.text }
        context.replace(list, joined)
    }

    private fun paramsOf(list: RFunctionArgumentList): List<PsiElement> =
        list.children.filterIsInstance<RArgument>()
}
