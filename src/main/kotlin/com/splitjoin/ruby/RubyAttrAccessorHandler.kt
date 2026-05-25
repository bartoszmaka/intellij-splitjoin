package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.basicTypes.RSymbolImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.expressions.RListOfExpressionsImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.methodCall.RCallImpl
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.variables.RIdentifierImpl

class RubyAttrAccessorHandler : SplitJoinHandler {

    companion object {
        private val ACCESSOR_NAMES = setOf("attr_reader", "attr_writer", "attr_accessor", "private_constant")
    }

    override fun canSplit(element: PsiElement): Boolean {
        val call = element.accessorCall() ?: return false
        if (call.containsComment() || call.textContains('\n')) return false
        // Claim every accessor call so MethodCallArgsHandler does not fall through and
        // split string-arg variants. split() is a no-op unless every arg is a symbol.
        return call.argCount() >= 2
    }

    override fun canJoin(element: PsiElement): Boolean {
        val call = element.accessorCall() ?: return false
        val run = call.gatherRun()
        return run.size >= 2 && run.none { it.containsComment() }
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val call = element.accessorCall() ?: return
        val name = call.accessorName() ?: return
        // Claim-only: bail silently when args aren't all symbols. Prevents fall-through.
        val symbols = call.symbolArgs() ?: return
        if (symbols.size < 2) return
        context.replace(call, symbols.joinToString("\n") { "$name ${it.text.trim()}" })
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val call = element.accessorCall() ?: return
        val run = call.gatherRun()
        if (run.size < 2) return

        val name = run.first().accessorName() ?: return
        val symbols = run.map { it.symbolArgs()?.singleOrNull()?.text?.trim() ?: return }
        context.replace(run.first().textRange.union(run.last().textRange), "$name ${symbols.joinToString(", ")}")
    }

    private fun PsiElement.accessorCall(): RCallImpl? {
        var node: PsiElement? = this
        while (node != null && node !is com.intellij.psi.PsiFile) {
            if (node is RCallImpl && node.accessorName() in ACCESSOR_NAMES) return node
            node = node.parent
        }
        // Fall back to nearest non-whitespace sibling when caret sits between statements.
        val prev = prevSignificantSibling()
        if (prev is RCallImpl && prev.accessorName() in ACCESSOR_NAMES) return prev
        val next = nextSignificantSibling()
        if (next is RCallImpl && next.accessorName() in ACCESSOR_NAMES) return next
        return null
    }

    private fun RCallImpl.accessorName(): String? =
        children.filterIsInstance<RIdentifierImpl>().firstOrNull()?.text

    private fun RCallImpl.symbolArgs(): List<RSymbolImpl>? {
        val list = children.filterIsInstance<RListOfExpressionsImpl>().firstOrNull() ?: return null
        val symbols = list.children.filterIsInstance<RSymbolImpl>()
        return symbols.takeIf { it.isNotEmpty() && it.size == list.elements.size }
    }

    private fun RCallImpl.argCount(): Int =
        children.filterIsInstance<RListOfExpressionsImpl>().firstOrNull()?.elements?.size ?: 0

    private fun RCallImpl.gatherRun(): List<RCallImpl> {
        val name = accessorName() ?: return emptyList()
        val result = mutableListOf(this)

        var next = nextSignificantSibling()
        while (next is RCallImpl && next.accessorName() == name && next.symbolArgs()?.size == 1) {
            result += next
            next = next.nextSignificantSibling()
        }

        var prev = prevSignificantSibling()
        while (prev is RCallImpl && prev.accessorName() == name && prev.symbolArgs()?.size == 1) {
            result.add(0, prev)
            prev = prev.prevSignificantSibling()
        }

        return result
    }

    private fun PsiElement.nextSignificantSibling(): PsiElement? {
        var sibling = nextSibling
        while (sibling != null && (sibling is PsiWhiteSpace || sibling.text.isBlank())) sibling = sibling.nextSibling
        return sibling
    }

    private fun PsiElement.prevSignificantSibling(): PsiElement? {
        var sibling = prevSibling
        while (sibling != null && (sibling is PsiWhiteSpace || sibling.text.isBlank())) sibling = sibling.prevSibling
        return sibling
    }
}
