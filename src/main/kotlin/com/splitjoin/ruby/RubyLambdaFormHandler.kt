package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RBodyStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RCompoundStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.methods.RBlockArgumentListImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.methods.RFunctionArgumentListImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.methods.arguments.RArgumentImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.iterators.RBraceCodeBlockImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.iterators.RDoBlockCallImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.iterators.RDoCodeBlockImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.methodCall.RCallImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.variables.RIdentifierImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.ruby19.impl.controlStructures.RLambdaImpl

class RubyLambdaFormHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val lambda = element as? RLambdaImpl ?: return false
        return !lambda.containsComment() && !lambda.textContains('\n') && lambda.bodyText() != null
    }

    override fun canJoin(element: PsiElement): Boolean {
        val call = element as? RDoBlockCallImpl ?: return false
        return !call.containsComment() && call.isLambdaCall() && call.doBlockBodyText() != null
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val lambda = element as? RLambdaImpl ?: return
        val params = lambda.functionParams()
        val body = lambda.bodyText() ?: return
        val opener = if (params.isEmpty()) "lambda do" else "lambda do |${params.joinToString(", ")}|"
        context.replace(lambda, "$opener\n  $body\nend")
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val call = element as? RDoBlockCallImpl ?: return
        val body = call.doBlockBodyText() ?: return
        val params = call.blockParams().joinToString(", ")
        context.replace(call, "->($params) { $body }")
    }

    private fun RLambdaImpl.functionParams(): List<String> =
        children.filterIsInstance<RFunctionArgumentListImpl>()
            .firstOrNull()
            ?.children
            ?.filterIsInstance<RArgumentImpl>()
            ?.map { it.text.trim() }
            .orEmpty()

    private fun RLambdaImpl.bodyText(): String? =
        children.filterIsInstance<RBraceCodeBlockImpl>()
            .firstOrNull()
            ?.children
            ?.filterIsInstance<RCompoundStatementImpl>()
            ?.firstOrNull()
            ?.text
            ?.trim()

    private fun RDoBlockCallImpl.isLambdaCall(): Boolean {
        val directName = children.filterIsInstance<RIdentifierImpl>().firstOrNull()?.text
        val callName = children.filterIsInstance<RCallImpl>().firstOrNull()
            ?.children
            ?.filterIsInstance<RIdentifierImpl>()
            ?.firstOrNull()
            ?.text
        return directName == "lambda" || callName == "lambda"
    }

    private fun RDoBlockCallImpl.blockParams(): List<String> =
        children.filterIsInstance<RDoCodeBlockImpl>()
            .firstOrNull()
            ?.children
            ?.filterIsInstance<RBlockArgumentListImpl>()
            ?.firstOrNull()
            ?.children
            ?.filterIsInstance<RArgumentImpl>()
            ?.map { it.text.trim() }
            .orEmpty()

    private fun RDoBlockCallImpl.doBlockBodyText(): String? {
        val codeBlock = children.filterIsInstance<RDoCodeBlockImpl>().firstOrNull() ?: return null
        val compound = codeBlock.children.filterIsInstance<RBodyStatementImpl>().firstOrNull()
            ?.children
            ?.filterIsInstance<RCompoundStatementImpl>()
            ?.firstOrNull() ?: return null
        if (compound.children.size != 1) return null
        return compound.text.trim()
    }
}
