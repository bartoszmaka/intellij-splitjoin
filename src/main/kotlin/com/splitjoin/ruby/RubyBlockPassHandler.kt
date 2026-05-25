package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.basicTypes.RSymbolImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.blocks.RCompoundStatementImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.methods.RBlockArgumentListImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.controlStructures.methods.arguments.RArgumentImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.expressions.RListOfExpressionsImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.iterators.RBraceBlockCallImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.iterators.RBraceCodeBlockImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.methodCall.RArgumentToBlockImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.methodCall.RCallImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.references.RDotReferenceImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.variables.RIdentifierImpl

class RubyBlockPassHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val call = element.dotCallAncestor() ?: return false
        return !call.containsComment() && call.blockPassSymbol() != null
    }

    override fun canJoin(element: PsiElement): Boolean {
        val blockCall = element.braceBlockCallAncestor() ?: return false
        return !blockCall.containsComment() && blockCall.simpleProxyMethod() != null
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val call = element.dotCallAncestor() ?: return
        val (receiver, method) = call.receiverAndMethod() ?: return
        val symbol = call.blockPassSymbol() ?: return
        context.replace(call, "$receiver.$method { |x| x.$symbol }")
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val blockCall = element.braceBlockCallAncestor() ?: return
        val (receiver, method) = blockCall.receiverAndMethod() ?: return
        val innerMethod = blockCall.simpleProxyMethod() ?: return
        context.replace(blockCall, "$receiver.$method(&:$innerMethod)")
    }

    // ---------- ancestry helpers ----------

    private fun PsiElement.dotCallAncestor(): RCallImpl? {
        var node: PsiElement? = this
        while (node != null && node !is com.intellij.psi.PsiFile) {
            if (node is RCallImpl && node.receiverAndMethod() != null) return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.braceBlockCallAncestor(): RBraceBlockCallImpl? {
        var node: PsiElement? = this
        while (node != null && node !is com.intellij.psi.PsiFile) {
            if (node is RBraceBlockCallImpl) return node
            node = node.parent
        }
        return null
    }

    // ---------- PSI extractors ----------

    /** Returns (receiver, method) for a `receiver.method` call. Only dot-reference shape. */
    private fun PsiElement.receiverAndMethod(): Pair<String, String>? {
        val dot = children.filterIsInstance<RDotReferenceImpl>().firstOrNull() ?: return null
        val parts = dot.children.filterIsInstance<RIdentifierImpl>()
        if (parts.size != 2) return null
        return parts[0].text.trim() to parts[1].text.trim()
    }

    /** Returns the inner symbol of an `&:name` block-pass argument, when it's the sole arg. */
    private fun RCallImpl.blockPassSymbol(): String? {
        val argList = children.filterIsInstance<RListOfExpressionsImpl>().firstOrNull() ?: return null
        val blockArg = argList.children.filterIsInstance<RArgumentToBlockImpl>().singleOrNull() ?: return null
        if (argList.elements.size != 1) return null
        val symbol = blockArg.children.filterIsInstance<RSymbolImpl>().firstOrNull() ?: return null
        return symbol.text.trim().removePrefix(":")
    }

    /**
     * For `arr.map { |x| x.upcase }`, returns "upcase" if the block is exactly `param.method`
     * (single statement, parameterless inner call, receiver matches block param).
     */
    private fun RBraceBlockCallImpl.simpleProxyMethod(): String? {
        val block = children.filterIsInstance<RBraceCodeBlockImpl>().firstOrNull() ?: return null

        val paramList = block.children.filterIsInstance<RBlockArgumentListImpl>().firstOrNull() ?: return null
        val params = paramList.children.filterIsInstance<RArgumentImpl>()
        if (params.size != 1) return null
        val paramName = params[0].text.trim()

        val compound = block.children.filterIsInstance<RCompoundStatementImpl>().firstOrNull() ?: return null
        val statements = compound.children.filter { it !is com.intellij.psi.PsiWhiteSpace && it.text.isNotBlank() }
        if (statements.size != 1) return null

        val body = statements[0] as? RDotReferenceImpl ?: return null
        val bodyParts = body.children.filterIsInstance<RIdentifierImpl>()
        if (bodyParts.size != 2) return null
        if (bodyParts[0].text.trim() != paramName) return null

        return bodyParts[1].text.trim()
    }
}
