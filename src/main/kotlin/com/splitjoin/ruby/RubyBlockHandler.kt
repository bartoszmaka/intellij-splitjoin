package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RCompoundStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RBodyStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RArgument
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RBraceBlockCall
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RDoBlockCall
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RBraceCodeBlock
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RDoCodeBlock
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RBlockArgumentList

/**
 * Per spec: brace form is the joined form, do/end form is the split form.
 * canSplit iff the block is brace form; canJoin iff do/end form.
 * Direction is determined by the current block form, not by line span.
 */
class RubyBlockHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RBraceBlockCall) return false
        if (element.containsComment()) return false
        return true
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RDoBlockCall) return false
        if (element.containsComment()) return false
        return true
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val call = element as RBraceBlockCall
        val brace = call.children.filterIsInstance<RBraceCodeBlock>().firstOrNull() ?: return
        val args = blockArgsText(brace.children.filterIsInstance<RBlockArgumentList>().firstOrNull())
        val body = compoundBodyText(brace)
        val replacement = buildString {
            append("do")
            if (args.isNotEmpty()) {
                append(" |")
                append(args)
                append("|")
            }
            append("\n")
            append(body)
            append("\nend")
        }
        context.replace(brace, replacement)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val call = element as RDoBlockCall
        val doBlock = call.children.filterIsInstance<RDoCodeBlock>().firstOrNull() ?: return
        val args = blockArgsText(doBlock.children.filterIsInstance<RBlockArgumentList>().firstOrNull())
        val body = compoundBodyText(doBlock).trim().replace(Regex("\\s*\\n\\s*"), "; ")
        val replacement = buildString {
            append("{")
            if (args.isNotEmpty()) {
                append(" |")
                append(args)
                append("| ")
            } else {
                append(" ")
            }
            append(body)
            append(" }")
        }
        context.replace(doBlock, replacement)
    }

    private fun blockArgsText(argList: RBlockArgumentList?): String {
        if (argList == null) return ""
        return argList.children.filterIsInstance<RArgument>().joinToString(", ") { it.text }
    }

    private fun compoundBodyText(block: PsiElement): String {
        // RDoCodeBlock wraps body in RBodyStatement(RCompoundStatement); RBraceCodeBlock has RCompoundStatement directly.
        val body = block.children.filterIsInstance<RBodyStatement>().firstOrNull()
            ?: block.children.filterIsInstance<RCompoundStatement>().firstOrNull()
            ?: return ""
        val compound = body as? RCompoundStatement
            ?: body.children.filterIsInstance<RCompoundStatement>().firstOrNull()
            ?: return body.text.trim()
        return compound.text.trim()
    }
}
