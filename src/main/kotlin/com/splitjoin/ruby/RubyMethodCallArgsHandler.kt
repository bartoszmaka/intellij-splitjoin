package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RListOfExpressions
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall

class RubyMethodCallArgsHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RListOfExpressions) return false
        if (element.parent !is RCall) return false
        if (argsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RListOfExpressions) return false
        if (element.parent !is RCall) return false
        if (argsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val list = element as RListOfExpressions
        val args = argsOf(list)
        val hasParens = callHasParens(list)
        val rebuilt = buildString {
            args.forEachIndexed { index, arg ->
                append(arg.text)
                val isLast = index == args.size - 1
                if (!isLast) {
                    append(",\n")
                } else if (hasParens) {
                    append(",")
                }
            }
        }
        context.replace(list, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val list = element as RListOfExpressions
        val joined = argsOf(list).joinToString(", ") { it.text }
        context.replace(list, joined)
    }

    private fun argsOf(list: RListOfExpressions): List<PsiElement> =
        list.children.toList()

    private fun callHasParens(list: RListOfExpressions): Boolean {
        val call = list.parent as RCall
        return call.text.contains("(")
    }
}
