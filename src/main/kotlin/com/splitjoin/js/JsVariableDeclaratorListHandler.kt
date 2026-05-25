package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSDestructuringElement
import com.intellij.lang.javascript.psi.JSVarStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsVariableDeclaratorListHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val stmt = element.varStatementAncestor() ?: return false
        if (stmt.containsComment()) return false
        if (stmt.variables.size < 2) return false
        // Reject if any variable is destructuring
        if (stmt.variables.any { it is JSDestructuringElement }) return false
        // Also reject if any child is destructuring (covers mixed cases)
        if (stmt.children.any { it is JSDestructuringElement }) return false
        return true
    }

    override fun canJoin(element: PsiElement): Boolean {
        val stmt = element.varStatementAncestor() ?: return false
        if (stmt.containsComment()) return false
        if (stmt.variables.size != 1) return false
        if (stmt.variables.any { it is JSDestructuringElement }) return false
        val run = stmt.gatherDeclaratorRun() ?: return false
        return run.size >= 2
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val stmt = element.varStatementAncestor() ?: return
        val kind = stmt.declarationKind() ?: return
        val parts = stmt.variables.map { v -> "$kind ${v.text.trim()};" }
        context.replace(stmt, parts.joinToString("\n"))
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val stmt = element.varStatementAncestor() ?: return
        val run = stmt.gatherDeclaratorRun() ?: return
        if (run.size < 2) return
        val kind = run.first().declarationKind() ?: return
        val decls = run.map { it.variables[0].text.trim() }
        val replacement = "$kind ${decls.joinToString(", ")};"
        context.replace(run.first().textRange.union(run.last().textRange), replacement)
    }

    // ---------- helpers ----------

    private fun PsiElement.varStatementAncestor(): JSVarStatement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is JSVarStatement) return node
            node = node.parent
        }
        // Sibling fallback (caret between statements)
        val prev = prevSignificantSibling()
        if (prev is JSVarStatement) return prev
        val next = nextSignificantSibling()
        if (next is JSVarStatement) return next
        return null
    }

    private fun JSVarStatement.declarationKind(): String? {
        var child = firstChild
        while (child != null) {
            val t = child.text
            if (t == "const" || t == "let" || t == "var") return t
            child = child.nextSibling
        }
        return null
    }

    private fun JSVarStatement.gatherDeclaratorRun(): List<JSVarStatement>? {
        val kind = declarationKind() ?: return null
        val result = mutableListOf(this)
        var n = nextSignificantSibling()
        while (n is JSVarStatement && matchesRun(n, kind)) {
            result += n
            n = n.nextSignificantSibling()
        }
        var p = prevSignificantSibling()
        while (p is JSVarStatement && matchesRun(p, kind)) {
            result.add(0, p)
            p = p.prevSignificantSibling()
        }
        return result
    }

    private fun matchesRun(stmt: JSVarStatement, kind: String): Boolean {
        if (stmt.containsComment()) return false
        if (stmt.declarationKind() != kind) return false
        if (stmt.variables.size != 1) return false
        if (stmt.variables.any { it is JSDestructuringElement }) return false
        return true
    }

    private fun PsiElement.nextSignificantSibling(): PsiElement? {
        var s = nextSibling
        while (s != null && (s is PsiWhiteSpace || s.text.isBlank())) s = s.nextSibling
        return s
    }

    private fun PsiElement.prevSignificantSibling(): PsiElement? {
        var s = prevSibling
        while (s != null && (s is PsiWhiteSpace || s.text.isBlank())) s = s.prevSibling
        return s
    }
}
