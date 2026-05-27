package com.splitjoin.ruby

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RListOfExpressions
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall

class RubyMethodCallArgsHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RListOfExpressions) return false
        if (element.parent !is RCall) return false
        if (argsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RListOfExpressions) return false
        if (element.parent !is RCall) return false
        if (argsOf(element).isEmpty()) return false
        if (element.containsComment()) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val list = element as RListOfExpressions
        val args = argsOf(list)
        val parenRange = parenRange(list)
        if (parenRange != null) {
            val call = list.parent as RCall
            val baseIndent = lineIndent(context.document, call.textRange.startOffset)
            val argIndent = baseIndent + "  "
            val rebuilt = buildString {
                append("(\n")
                args.forEach { arg ->
                    append(argIndent)
                    append(arg.text)
                    append(",\n")
                }
                append(baseIndent)
                append(")")
            }
            val marker = context.replace(parenRange, rebuilt)
            context.skipAutoIndent = true
            val firstArgOffset = marker.startOffset + 1 + 1 + argIndent.length
            context.editor.caretModel.moveToOffset(firstArgOffset)
        } else {
            val rebuilt = args.joinToString(",\n") { it.text }
            context.replace(list, rebuilt)
        }
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val list = element as RListOfExpressions
        val joined = argsOf(list).joinToString(", ") { it.text }
        val parenRange = parenRange(list)
        if (parenRange != null) {
            context.replace(parenRange, "($joined)")
        } else {
            context.replace(list, joined)
        }
    }

    private fun argsOf(list: RListOfExpressions): List<PsiElement> =
        list.children.toList()

    private fun parenRange(list: RListOfExpressions): TextRange? {
        val open = leafAround(list, forward = false) ?: return null
        if (open.text != "(") return null
        val close = leafAround(list, forward = true) ?: return null
        if (close.text != ")") return null
        return TextRange(open.textRange.startOffset, close.textRange.endOffset)
    }

    private fun leafAround(list: RListOfExpressions, forward: Boolean): PsiElement? {
        var node: PsiElement? = if (forward) list.nextSibling else list.prevSibling
        while (node != null && node.text.isBlank()) {
            node = if (forward) node.nextSibling else node.prevSibling
        }
        return node
    }

    private fun lineIndent(doc: Document, offset: Int): String {
        val lineNum = doc.getLineNumber(offset)
        val lineStart = doc.getLineStartOffset(lineNum)
        val prefix = doc.charsSequence.subSequence(lineStart, offset).toString()
        return prefix.takeWhile { it == ' ' || it == '\t' }
    }
}
