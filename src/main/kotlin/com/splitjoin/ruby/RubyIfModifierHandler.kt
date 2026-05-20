package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.RIfStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.RUnlessStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.RCondition
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RCompoundStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RElseBlock
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.modifierStatements.RIfModStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.modifierStatements.RUnlessModStatement

/**
 * Bidirectional: modifier form (`return x if y`) <-> full form (`if y\n  return x\nend`).
 * Split fires on modifier; Join fires on simple full form (no else, single body statement).
 */
class RubyIfModifierHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RIfModStatement && element !is RUnlessModStatement) return false
        if (element.containsComment()) return false
        return true
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RIfStatement && element !is RUnlessStatement) return false
        if (element.containsComment()) return false
        return isJoinable(element)
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val keyword = if (element is RIfModStatement) "if" else "unless"
        val (statement, condition) = modifierParts(element)
        val replacement = "$keyword $condition\n  ${statement.trim()}\nend"
        context.replace(element, replacement)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val keyword = if (element is RIfStatement) "if" else "unless"
        val children = element.children
        val condition = children.filterIsInstance<RCondition>().firstOrNull()?.text?.trim() ?: return
        val body = children.filterIsInstance<RCompoundStatement>().firstOrNull()?.text?.trim() ?: return
        context.replace(element, "$body $keyword $condition")
    }

    private fun modifierParts(element: PsiElement): Pair<String, String> {
        val children = element.children
        val statement = children.firstOrNull { it !is RCondition }?.text ?: ""
        val condition = children.filterIsInstance<RCondition>().firstOrNull()?.text?.trim() ?: ""
        return statement to condition
    }

    private fun isJoinable(element: PsiElement): Boolean {
        val children = element.children
        // No elsif/else; exactly one body
        if (children.any { it is RElseBlock }) return false
        val bodies = children.filterIsInstance<RCompoundStatement>()
        if (bodies.size != 1) return false
        // Body must be a single statement (not multiple, not empty)
        val bodyChildren = bodies[0].children
        return bodyChildren.size == 1
    }
}
