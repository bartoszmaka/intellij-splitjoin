package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.references.RDotReferenceImpl

class RubyMethodChainHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val root = element.chainRoot() ?: return false
        if (root.containsComment()) return false
        if (root.text.contains('\n')) return false
        return root.chainLinks()?.let { it.size >= 3 } == true
    }

    override fun canJoin(element: PsiElement): Boolean {
        val root = element.chainRoot() ?: return false
        if (root.containsComment()) return false
        if (!root.text.contains('\n')) return false
        return root.chainLinks()?.let { it.size >= 3 } == true
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val root = element.chainRoot() ?: return
        val (receiver, links) = root.walkChain() ?: return
        val rebuilt = buildString {
            append(receiver)
            for (link in links) {
                append("\n  .")
                append(link)
            }
        }
        context.replace(root, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val root = element.chainRoot() ?: return
        val (receiver, links) = root.walkChain() ?: return
        val rebuilt = buildString {
            append(receiver)
            for (link in links) {
                append('.')
                append(link)
            }
        }
        context.replace(root, rebuilt)
    }

    // ---------- ancestor lookup ----------

    /** Returns the outermost `RDotReferenceImpl` containing this element. */
    private fun PsiElement.chainRoot(): RDotReferenceImpl? {
        var node: PsiElement? = this
        var outermost: RDotReferenceImpl? = null
        while (node != null && node !is PsiFile) {
            if (node is RDotReferenceImpl) outermost = node
            node = node.parent
        }
        return outermost
    }

    /** Returns the list of method names in the chain, or null if any link is non-pure. */
    private fun RDotReferenceImpl.chainLinks(): List<String>? = walkChain()?.second

    /**
     * Walks down receivers, returning (receiverText, [methodNames]) in forward order.
     * Returns null if any chain link isn't a pure RDotReferenceImpl (i.e., block call or arg list).
     */
    private fun RDotReferenceImpl.walkChain(): Pair<String, List<String>>? {
        val methods = mutableListOf<String>()
        var node: PsiElement = this

        while (node is RDotReferenceImpl) {
            val children = node.children
            if (children.size != 2) return null
            val receiver = children[0]
            val method = children[1]
            methods.add(0, method.text.trim())
            node = receiver
        }

        // Terminal receiver must be a pure identifier or constant — not a brace-block-call,
        // not a call with args. Reject anything that's a dot-call-related composite.
        if (node.text.contains('\n') && node.text.contains('.')) return null
        if (node.javaClass.simpleName.let { it.contains("BlockCall") || it.contains("Call") }) return null
        return node.text.trim() to methods
    }
}
