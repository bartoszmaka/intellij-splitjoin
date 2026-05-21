package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSBlockStatement
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSFunctionExpression
import com.intellij.lang.javascript.psi.JSReturnStatement
import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsArrowBodyHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is JSFunctionExpression) return false
        if (!element.isArrowFunction) return false
        if (element.containsComment()) return false
        val body = expressionBodyOf(element) ?: return false
        return body !is JSBlockStatement
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is JSFunctionExpression) return false
        if (!element.isArrowFunction) return false
        if (element.containsComment()) return false
        val block = element.block ?: return false
        val ret = singleReturnOf(block) ?: return false
        return ret.expression != null
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val arrow = element as JSFunctionExpression
        val body = expressionBodyOf(arrow) ?: return
        val replacement = "{\nreturn ${body.text};\n}"
        context.replace(body, replacement)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val arrow = element as JSFunctionExpression
        val block = arrow.block ?: return
        val ret = singleReturnOf(block) ?: return
        val expr = ret.expression ?: return
        context.replace(block, expr.text)
    }

    private fun expressionBodyOf(arrow: JSFunctionExpression): PsiElement? {
        if (arrow.block != null) return arrow.block
        return arrow.children.filterIsInstance<JSExpression>().lastOrNull()
    }

    private fun singleReturnOf(block: JSBlockStatement): JSReturnStatement? {
        val statements = block.statements
        if (statements.size != 1) return null
        return statements.first() as? JSReturnStatement
    }
}
