package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.RCaseStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.RWhenCaseImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RCompoundStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RElseBlockImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.expressions.RListOfExpressionsImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.variables.RIdentifierImpl

class RubyCaseWhenHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val case = element as? RCaseStatementImpl ?: return false
        if (case.containsComment() || case.textContains('\n')) return false
        return case.subjectText() != null && case.whenCases().all { it.branchParts() != null }
    }

    override fun canJoin(element: PsiElement): Boolean {
        val case = element as? RCaseStatementImpl ?: return false
        if (case.containsComment() || !case.textContains('\n')) return false
        return case.subjectText() != null && case.allBranchesAreSingleStatements()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val case = element as? RCaseStatementImpl ?: return
        val subject = case.subjectText() ?: return
        val branches = case.whenCases().map { it.branchParts() ?: return }
        val elseBody = case.elseBodyText()

        val rebuilt = buildString {
            append("case ").append(subject)
            for ((condition, body) in branches) append("\nwhen $condition\n  $body")
            if (elseBody != null) append("\nelse\n  $elseBody")
            append("\nend")
        }
        context.replace(case, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val case = element as? RCaseStatementImpl ?: return
        val subject = case.subjectText() ?: return
        val branches = case.whenCases().map { it.branchParts() ?: return }
        val elseBody = case.elseBodyText()

        val rebuilt = buildString {
            append("case ").append(subject)
            for ((condition, body) in branches) append("; when $condition then $body")
            if (elseBody != null) append("; else $elseBody")
            append("; end")
        }
        context.replace(case, rebuilt)
    }

    private fun RCaseStatementImpl.subjectText(): String? =
        children.filterIsInstance<RIdentifierImpl>().firstOrNull()?.text?.trim()

    private fun RCaseStatementImpl.whenCases(): List<RWhenCaseImpl> =
        children.filterIsInstance<RWhenCaseImpl>()

    private fun RWhenCaseImpl.branchParts(): Pair<String, String>? {
        val condition = children.filterIsInstance<RListOfExpressionsImpl>().firstOrNull()?.text?.trim()
        val body = children.filterIsInstance<RCompoundStatementImpl>().firstOrNull()?.text?.cleanBody()
        return if (condition != null && body != null) condition to body else null
    }

    private fun RCaseStatementImpl.elseBodyText(): String? =
        children.filterIsInstance<RElseBlockImpl>()
            .firstOrNull()
            ?.children
            ?.filterIsInstance<RCompoundStatementImpl>()
            ?.firstOrNull()
            ?.text
            ?.cleanBody()

    private fun String.cleanBody(): String = trim().trimEnd(';').trimEnd()

    private fun RCaseStatementImpl.allBranchesAreSingleStatements(): Boolean {
        if (whenCases().isEmpty()) return false
        val whenBodies = whenCases().map {
            it.children.filterIsInstance<RCompoundStatementImpl>().firstOrNull() ?: return false
        }
        val elseBody = children.filterIsInstance<RElseBlockImpl>().firstOrNull()
            ?.children
            ?.filterIsInstance<RCompoundStatementImpl>()
            ?.firstOrNull()
        return whenBodies.all { it.children.size == 1 } && (elseBody == null || elseBody.children.size == 1)
    }
}
