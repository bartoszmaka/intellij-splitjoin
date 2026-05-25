package com.splitjoin.js

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class TsUnionTypeHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (!isTsFile(element)) return false
        val union = element.unionAncestor() ?: return false
        if (union.containsComment()) return false
        return union.terms().size >= 2 && !union.text.contains('\n')
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (!isTsFile(element)) return false
        val union = element.unionAncestor() ?: return false
        if (union.containsComment()) return false
        return union.terms().size >= 2 && union.text.contains('\n')
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val union = element.unionAncestor() ?: return
        val terms = union.terms()
        val op = union.operator() ?: return
        // Replacement consumes preceding whitespace so the result doesn't leave a
        // dangling space after `=`. The handler emits a leading newline + content;
        // adjustLineIndent fixes the indent according to the file's code style.
        val range = union.rangeWithLeadingWhitespace()
        val rebuilt = terms.joinToString("\n") { "$op $it" }
        context.replace(range, "\n$rebuilt")
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val union = element.unionAncestor() ?: return
        val terms = union.terms()
        val op = union.operator() ?: return
        // Join also consumes preceding whitespace so we don't end up with `=\n  A | B | C`.
        val range = union.rangeWithLeadingWhitespace()
        context.replace(range, " " + terms.joinToString(" $op "))
    }

    // ---------- helpers ----------

    private fun isTsFile(element: PsiElement): Boolean {
        val ext = element.containingFile?.virtualFile?.extension ?: return false
        return ext in setOf("ts", "tsx", "mts", "cts")
    }

    private fun PsiElement.unionAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node.javaClass.simpleName == "TypeScriptUnionOrIntersectionTypeImpl") return node
            node = node.parent
        }
        return null
    }

    /** Returns the type terms (skipping operator leaves and whitespace). */
    private fun PsiElement.terms(): List<String> {
        val result = mutableListOf<String>()
        var child = firstChild
        while (child != null) {
            if (child !is PsiWhiteSpace && child.text != "|" && child.text != "&" && !child.text.isBlank()) {
                result += child.text.trim()
            }
            child = child.nextSibling
        }
        return result
    }

    /** Detects whether operator is `|` or `&`. */
    private fun PsiElement.operator(): String? {
        var child = firstChild
        while (child != null) {
            if (child !is PsiWhiteSpace && (child.text == "|" || child.text == "&")) return child.text
            child = child.nextSibling
        }
        return null
    }

    /**
     * Extends this element's text range backward to include immediately-preceding whitespace
     * AND operator leaves (`|`, `&`). TS parses leading-`&` intersections with the first `&`
     * outside the union PSI node, so we have to claim it here for the join replacement to
     * produce a clean result.
     */
    private fun PsiElement.rangeWithLeadingWhitespace(): TextRange {
        var start = textRange.startOffset
        var prev: PsiElement? = prevSibling
        while (prev != null) {
            val text = prev.text
            if (prev is PsiWhiteSpace || text.isBlank() || text == "|" || text == "&") {
                start = prev.textRange.startOffset
                prev = prev.prevSibling
            } else break
        }
        return TextRange(start, textRange.endOffset)
    }
}
