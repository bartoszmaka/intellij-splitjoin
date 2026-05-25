package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RBeginEndBlockStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RBodyStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RCompoundStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.REnsureBlockImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RRescueBlockImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.modifierStatements.RRescueModStatementImpl

class RubyRescueModifierHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val rescue = element as? RRescueModStatementImpl ?: return false
        return !rescue.containsComment() && rescue.parts() != null
    }

    override fun canJoin(element: PsiElement): Boolean {
        val begin = element as? RBeginEndBlockStatementImpl ?: return false
        if (begin.containsComment()) return false
        val parts = begin.expandedParts() ?: return false
        return parts.body.children.size == 1 && parts.rescueBody.children.size == 1
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val rescue = element as? RRescueModStatementImpl ?: return
        val (body, fallback) = rescue.parts() ?: return
        context.replace(rescue, "begin\n  ${body.text.trim()}\nrescue\n  ${fallback.text.trim()}\nend")
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val begin = element as? RBeginEndBlockStatementImpl ?: return
        val parts = begin.expandedParts() ?: return
        val body = parts.body.children.singleOrNull()?.text?.trim() ?: return
        val fallback = parts.rescueBody.children.singleOrNull()?.text?.trim() ?: return
        context.replace(begin, "$body rescue $fallback")
    }

    private data class ExpandedParts(
        val body: RCompoundStatementImpl,
        val rescueBody: RCompoundStatementImpl,
    )

    private fun RRescueModStatementImpl.parts(): Pair<PsiElement, PsiElement>? {
        val direct = children.toList()
        return if (direct.size == 2) direct[0] to direct[1] else null
    }

    private fun RBeginEndBlockStatementImpl.expandedParts(): ExpandedParts? {
        val bodyStatement = children.filterIsInstance<RBodyStatementImpl>().firstOrNull() ?: return null
        if (bodyStatement.children.filterIsInstance<REnsureBlockImpl>().isNotEmpty()) return null

        val body = bodyStatement.children.filterIsInstance<RCompoundStatementImpl>().firstOrNull() ?: return null
        val rescueBlocks = bodyStatement.children.filterIsInstance<RRescueBlockImpl>() +
            children.filterIsInstance<RRescueBlockImpl>()
        if (rescueBlocks.size != 1) return null

        val rescueBlock = rescueBlocks.single()
        if (rescueBlock.receiver != null) return null
        val rescueBody = rescueBlock.children.filterIsInstance<RCompoundStatementImpl>().firstOrNull() ?: return null
        return ExpandedParts(body, rescueBody)
    }
}
