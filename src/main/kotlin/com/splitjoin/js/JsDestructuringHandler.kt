package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSDestructuringArray
import com.intellij.lang.javascript.psi.JSDestructuringElement
import com.intellij.lang.javascript.psi.JSDestructuringObject
import com.intellij.lang.javascript.psi.JSDestructuringShorthandedProperty
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVarStatement
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsDestructuringHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val pattern = element.destructuringAncestor() ?: return false
        val varStmt = pattern.varStatement() ?: return false
        if (varStmt.containsComment()) return false
        // Top-level declarators (direct children that are either JSDestructuringElement
        // or JSVariable) must be exactly one — bail when destructuring is mixed with
        // plain declarators (e.g. `const { a } = obj, c = 1`).
        val topLevel = varStmt.children.filter { it is JSDestructuringElement || it is JSVariable }
        if (topLevel.size != 1) return false
        if (topLevel[0] !is JSDestructuringElement) return false
        return pattern.canSplitDestructuring()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val anchor = element.varStatementAncestor() ?: return false
        if (anchor.containsComment()) return false
        // Skip if this is a destructuring split itself (handled by split direction).
        if (anchor.hasDestructuring()) return false
        val run = anchor.gatherDestructuringRun() ?: return false
        return run.statements.size >= 2
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val pattern = element.destructuringAncestor() ?: return
        val varStmt = pattern.varStatement() ?: return
        val kind = varStmt.declarationKind() ?: return
        val source = pattern.sourceText() ?: return
        val names = pattern.targetNames() ?: return
        val isArray = pattern is JSDestructuringArray
        val lines = names.mapIndexed { i, name ->
            val rhs = if (isArray) "$source[$i]" else "$source.$name"
            "$kind $name = $rhs;"
        }
        context.replace(varStmt, lines.joinToString("\n"))
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val anchor = element.varStatementAncestor() ?: return
        val run = anchor.gatherDestructuringRun() ?: return
        if (run.statements.size < 2) return
        val kind = run.kind
        val source = run.source
        val names = run.names
        val pattern = if (run.isArray) "[${names.joinToString(", ")}]" else "{ ${names.joinToString(", ")} }"
        val replacement = "$kind $pattern = $source;"
        val first = run.statements.first()
        val last = run.statements.last()
        context.replace(first.textRange.union(last.textRange), replacement)
    }

    // ---------- ancestor lookups ----------

    private fun PsiElement.destructuringAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is JSDestructuringObject || node is JSDestructuringArray) return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.varStatementAncestor(): JSVarStatement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is JSVarStatement) return node
            node = node.parent
        }
        // Sibling fallback when caret sits between statements
        val prev = prevSignificantSibling()
        if (prev is JSVarStatement) return prev
        val next = nextSignificantSibling()
        if (next is JSVarStatement) return next
        return null
    }

    // ---------- split helpers ----------

    private fun PsiElement.varStatement(): JSVarStatement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node is JSVarStatement) return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.canSplitDestructuring(): Boolean {
        val varStmt = varStatement() ?: return false
        val element = varStmt.children.filterIsInstance<JSDestructuringElement>().singleOrNull() ?: return false
        // Source must be plain identifier
        val source = element.children.find { 
            it is JSReferenceExpression 
        } as? JSReferenceExpression ?: return false
        if (source.text.contains('.') || source.text.contains('(')) return false
        return targetNames() != null
    }

    private fun PsiElement.targetNames(): List<String>? = when (this) {
        is JSDestructuringObject -> {
            val props = children.filterIsInstance<JSDestructuringShorthandedProperty>()
            // Must equal the count of property-like children; reject anything else (rename, rest, defaults, computed)
            val totalProps = children.count {
                it !is PsiWhiteSpace &&
                    it.text != "{" && it.text != "}" && it.text != ","
            }
            if (props.size != totalProps || props.size < 2) null
            else props.map { it.text.trim() }
        }
        is JSDestructuringArray -> {
            val vars = children.filterIsInstance<JSVariable>()
            val totalEntries = children.count {
                it !is PsiWhiteSpace &&
                    it.text != "[" && it.text != "]" && it.text != ","
            }
            if (vars.size != totalEntries || vars.size < 2) null
            else vars.map { it.text.trim() }
        }
        else -> null
    }

    private fun PsiElement.sourceText(): String? {
        val varStmt = varStatement() ?: return null
        val element = varStmt.children.filterIsInstance<JSDestructuringElement>().singleOrNull() ?: return null
        val refExpr = element.children.find { it is JSReferenceExpression } as? JSReferenceExpression ?: return null
        return refExpr.text?.trim()
    }

    // ---------- join helpers ----------

    private fun JSVarStatement.declarationKind(): String? {
        var child = firstChild
        while (child != null) {
            val t = child.text
            if (t == "const" || t == "let" || t == "var") return t
            child = child.nextSibling
        }
        return null
    }

    private fun JSVarStatement.hasDestructuring(): Boolean =
        children.any { it is JSDestructuringElement }

    private data class DestructuringRun(
        val statements: List<JSVarStatement>,
        val kind: String,
        val source: String,
        val names: List<String>,
        val isArray: Boolean,
    )

    private fun JSVarStatement.gatherDestructuringRun(): DestructuringRun? {
        val kind = declarationKind() ?: return null
        // Walk forward + backward to collect matching statements
        val gathered = mutableListOf(this)
        var n = nextSignificantSibling()
        while (n is JSVarStatement) {
            if (!matchesDestructuringRun(n, kind)) break
            gathered += n
            n = n.nextSignificantSibling()
        }
        var p = prevSignificantSibling()
        while (p is JSVarStatement) {
            if (!matchesDestructuringRun(p, kind)) break
            gathered.add(0, p)
            p = p.prevSignificantSibling()
        }
        if (gathered.size < 2) return null

        // Extract source + names; determine object vs array form
        val parsed = gathered.map { it.parseDestructuringDeclarator() ?: return null }
        val firstSource = parsed[0].source
        if (parsed.any { it.source != firstSource }) return null

        val isArray = parsed[0].arrayIndex != null
        if (isArray) {
            // Indices must be contiguous 0..N-1
            for ((i, p) in parsed.withIndex()) {
                if (p.arrayIndex != i) return null
            }
            return DestructuringRun(gathered, kind, firstSource, parsed.map { it.name }, isArray = true)
        } else {
            // Object form: name and accessed key must match
            for (p in parsed) {
                if (p.arrayIndex != null) return null
                if (p.objectKey == null) return null
                if (p.objectKey != p.name) return null
            }
            return DestructuringRun(gathered, kind, firstSource, parsed.map { it.name }, isArray = false)
        }
    }

    private fun matchesDestructuringRun(stmt: JSVarStatement, kind: String): Boolean {
        if (stmt.containsComment()) return false
        if (stmt.declarationKind() != kind) return false
        if (stmt.variables.size != 1) return false
        val v = stmt.variables[0]
        if (v is JSDestructuringElement) return false  // nested destructuring on LHS — not our shape
        return stmt.parseDestructuringDeclarator() != null
    }

    private data class DeclaratorParts(
        val name: String,
        val source: String,
        val objectKey: String?,
        val arrayIndex: Int?,
    )

    /** Parse `const <name> = <source>.<key>` or `const <name> = <source>[<i>]`. Returns null if shape differs. */
    private fun JSVarStatement.parseDestructuringDeclarator(): DeclaratorParts? {
        val v = variables.singleOrNull() ?: return null
        if (v is JSDestructuringElement) return null
        val name = v.name ?: return null
        val initializer = v.initializerOrStub ?: return null
        val text = initializer.text.trim()

        // Try object form: <source>.<identifier>
        val dotIdx = text.lastIndexOf('.')
        if (dotIdx > 0 && dotIdx < text.length - 1) {
            val source = text.substring(0, dotIdx).trim()
            val key = text.substring(dotIdx + 1).trim()
            if (source.isValidJsIdent() && key.isValidJsIdent()) {
                return DeclaratorParts(name, source, objectKey = key, arrayIndex = null)
            }
        }
        // Try array form: <source>[<integer>]
        val openBracket = text.indexOf('[')
        val closeBracket = text.lastIndexOf(']')
        if (openBracket > 0 && closeBracket == text.length - 1) {
            val source = text.substring(0, openBracket).trim()
            val idxStr = text.substring(openBracket + 1, closeBracket).trim()
            val idx = idxStr.toIntOrNull()
            if (source.isValidJsIdent() && idx != null && idx >= 0) {
                return DeclaratorParts(name, source, objectKey = null, arrayIndex = idx)
            }
        }
        return null
    }

    private fun String.isValidJsIdent(): Boolean =
        isNotEmpty() && first().let { it.isLetter() || it == '_' || it == '$' } &&
        all { it.isLetterOrDigit() || it == '_' || it == '$' }

    // ---------- sibling walker (skip both PsiWhiteSpace and blank-text leaves) ----------

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
