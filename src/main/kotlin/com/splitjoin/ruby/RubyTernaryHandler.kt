package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.RCondition
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.RIfStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RCompoundStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RElseBlock
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RTernaryExpression

/**
 * Bidirectional: `a ? b : c` <-> `if a\n  b\nelse\n  c\nend`.
 * Split fires on ternary; Join fires on simple if/else (single body in each branch,
 * no elsif).
 */
class RubyTernaryHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RTernaryExpression) return false
        if (element.containsComment()) return false
        return true
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RIfStatement) return false
        if (element.containsComment()) return false
        return isJoinable(element)
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tern = element as RTernaryExpression
        val children = tern.children
        val cond = children.filterIsInstance<RCondition>().firstOrNull()?.text?.trim() ?: return
        // After the condition there are two more children: then-branch, else-branch
        val nonCondition = children.filter { it !is RCondition }
        if (nonCondition.size < 2) return
        val thenText = nonCondition[0].text.trim()
        val elseText = nonCondition[1].text.trim()
        val replacement = "if $cond\n  $thenText\nelse\n  $elseText\nend"
        context.replace(tern, replacement)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val ifStmt = element as RIfStatement
        val children = ifStmt.children
        val cond = children.filterIsInstance<RCondition>().firstOrNull()?.text?.trim() ?: return
        val thenBody = children.filterIsInstance<RCompoundStatement>().firstOrNull()?.text?.trim() ?: return
        val elseBlock = children.filterIsInstance<RElseBlock>().firstOrNull() ?: return
        val elseBody = elseBlock.children.filterIsInstance<RCompoundStatement>().firstOrNull()?.text?.trim() ?: return
        context.replace(ifStmt, "$cond ? $thenBody : $elseBody")
    }

    private fun isJoinable(element: RIfStatement): Boolean {
        val children = element.children
        val bodies = children.filterIsInstance<RCompoundStatement>()
        if (bodies.size != 1) return false
        if (bodies[0].children.size != 1) return false
        val elseBlock = children.filterIsInstance<RElseBlock>().firstOrNull() ?: return false
        val elseBodies = elseBlock.children.filterIsInstance<RCompoundStatement>()
        if (elseBodies.size != 1) return false
        if (elseBodies[0].children.size != 1) return false
        return true
    }
}
