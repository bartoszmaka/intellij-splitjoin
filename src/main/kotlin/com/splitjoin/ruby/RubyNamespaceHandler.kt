package com.splitjoin.ruby

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RBodyStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RCompoundStatement
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.modules.RModule
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.names.RName
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.names.RSuperClass

/**
 * Bidirectional: `class Foo::Bar::Baz [< Base]` <-> nested `module Foo / module Bar / class Baz`.
 * canSplit fires on a compact-name class/module; canJoin fires on a wrapper module that contains
 * exactly one nested class/module, recursively, with no sibling content and no wrapper-level superclass.
 * Direction is determined by the current form, not by line span.
 */
class RubyNamespaceHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is RClass && element !is RModule) return false
        if (element.containsComment()) return false
        val name = nameTextOf(element) ?: return false
        return name.contains("::")
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is RClass && element !is RModule) return false
        if (element.containsComment()) return false
        val chain = collectJoinableChain(element) ?: return false
        if (chain.size < 2) return false
        // The outermost element of the chain must NOT have a parent class/module.
        // If it does, joining would produce a compact namespace inside an enclosing structure,
        // which would be semantically broken (the outer wrappers can't be joined away).
        val outer = chain.first()
        val outerParent = PsiTreeUtil.getParentOfType(outer, RClass::class.java, RModule::class.java)
        return outerParent == null
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val name = nameTextOf(element) ?: return
        val segments = name.split("::").map { it.trim() }.filter { it.isNotEmpty() }
        if (segments.size < 2) return

        val isClass = element is RClass
        val superclass = if (isClass) superclassTextOf(element as RClass) else null
        val bodyText = bodyTextOf(element) ?: ""

        val replacement = buildString {
            for (i in 0 until segments.size - 1) {
                append("module ")
                append(segments[i])
                append("\n")
            }
            append(if (isClass) "class " else "module ")
            append(segments.last())
            if (superclass != null) {
                append(" < ")
                append(superclass)
            }
            if (bodyText.isNotEmpty()) {
                append("\n")
                append(bodyText)
            }
            append("\n")
            for (i in segments.indices) {
                append("end")
                if (i < segments.size - 1) append("\n")
            }
        }
        context.replace(element, replacement)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val chain = collectJoinableChain(element) ?: return
        val outer = chain.first()
        val inner = chain.last()
        val segments = chain.map { simpleNameOf(it) ?: return }
        val joinedName = segments.joinToString("::")

        val isClass = inner is RClass
        val superclass = if (isClass) superclassTextOf(inner as RClass) else null
        val innerBody = bodyTextOf(inner) ?: ""

        val replacement = buildString {
            append(if (isClass) "class " else "module ")
            append(joinedName)
            if (superclass != null) {
                append(" < ")
                append(superclass)
            }
            if (innerBody.isNotEmpty()) {
                append("\n")
                append(indentBody(innerBody))
            }
            append("\nend")
        }
        context.replace(outer, replacement)
    }

    // --- helpers ---

    /**
     * Normalizes indentation of [body]: strips the common leading whitespace from all non-blank lines,
     * then re-indents every non-blank line by 2 spaces.
     */
    private fun indentBody(body: String): String {
        val lines = body.lines()
        val minIndent = lines
            .filter { it.isNotBlank() }
            .minOfOrNull { it.length - it.trimStart().length } ?: 0
        return lines.joinToString("\n") { line ->
            if (line.isBlank()) line
            else "  " + line.drop(minIndent)
        }
    }

    /** Returns the textual name of the RClass or RModule's RName child, e.g. "Foo", "Foo::Bar::Baz". */
    private fun nameTextOf(element: PsiElement): String? {
        if (element !is RClass && element !is RModule) return null
        val nameNode = PsiTreeUtil.findChildOfType(element, RName::class.java) ?: return null
        return nameNode.text?.trim()
    }

    /** Returns the last `::`-segment, or the full name if no `::`. */
    private fun simpleNameOf(element: PsiElement): String? {
        val name = nameTextOf(element) ?: return null
        return if (name.contains("::")) name.substringAfterLast("::") else name
    }

    /** Returns the superclass text (without leading `<`), or null when no superclass clause exists. */
    private fun superclassTextOf(rClass: RClass): String? {
        val sc = PsiTreeUtil.findChildOfType(rClass, RSuperClass::class.java) ?: return null
        val txt = sc.text?.trim() ?: return null
        return txt.takeIf { it.isNotEmpty() }
    }

    /**
     * Returns the body text of [element]'s RCompoundStatement, normalized to column-0 indentation.
     * The first line of PSI text has no leading spaces (it starts at the element's start offset),
     * but subsequent lines retain their absolute document indentation. We strip that absolute indent
     * from lines 2+, then the whole block is at column 0 (ready for re-indenting by [indentBody]).
     * Returns "" when empty.
     */
    private fun bodyTextOf(element: PsiElement): String? {
        val compound = bodyCompoundOf(element) ?: return null
        val raw = compound.text
        if (raw.isBlank()) return ""
        val lines = raw.lines()
        if (lines.size == 1) return lines[0].trim()
        // Compute the minimum indentation of non-blank lines starting from line index 1
        // (line 0 has no leading spaces in PSI text)
        val bodyIndent = lines.drop(1)
            .filter { it.isNotBlank() }
            .minOfOrNull { it.length - it.trimStart().length } ?: 0
        return buildString {
            append(lines[0].trim())
            for (line in lines.drop(1)) {
                append("\n")
                if (line.isBlank()) append(line)
                else append(line.drop(bodyIndent))
            }
        }.trimEnd()
    }

    private fun bodyCompoundOf(element: PsiElement): RCompoundStatement? {
        val body = PsiTreeUtil.findChildOfType(element, RBodyStatement::class.java) ?: return null
        return PsiTreeUtil.findChildOfType(body, RCompoundStatement::class.java)
    }

    /**
     * Walks up from `element` to the outermost wrapper RClass/RModule that's still part of the chain,
     * then walks back down collecting the chain. Returns null when the chain isn't joinable.
     *
     * Joinable means: every level except the innermost contains exactly one non-whitespace child
     * in its RCompoundStatement, and that child is an RClass/RModule with no superclass clause
     * (only the innermost may have one).
     */
    private fun collectJoinableChain(element: PsiElement): List<PsiElement>? {
        if (element !is RClass && element !is RModule) return null

        // Walk up: find the outermost RClass/RModule whose body contains exactly the current node.
        var outer: PsiElement = element
        while (true) {
            val parent = PsiTreeUtil.getParentOfType(outer, RClass::class.java, RModule::class.java) ?: break
            if (!isSingleChildWrapper(parent, outer)) break
            outer = parent
        }

        // Walk down from outer, collecting the chain.
        val chain = mutableListOf<PsiElement>()
        var node: PsiElement = outer
        while (true) {
            chain.add(node)
            // A wrapper level (i.e., not the innermost yet) must have its body contain exactly one
            // non-whitespace child of type RClass/RModule, AND must not have its own superclass.
            if (node is RClass && superclassTextOf(node) != null) {
                // This RClass has a superclass — it must be the innermost; chain ends here.
                break
            }
            if (node.containsComment()) break
            val body = bodyCompoundOf(node) ?: break
            val children = body.children.filter { it !is PsiWhiteSpace && it !is PsiComment }
            if (children.size != 1) break
            val onlyChild = children.first()
            if (onlyChild !is RClass && onlyChild !is RModule) break
            node = onlyChild
        }
        return chain
    }

    /**
     * True iff `wrapper`'s body contains exactly one non-whitespace, non-comment child equal to `child`,
     * and `wrapper` has no superclass clause.
     * Checks both the RBodyStatement and the RCompoundStatement for comments and extra siblings.
     */
    private fun isSingleChildWrapper(wrapper: PsiElement, child: PsiElement): Boolean {
        if (wrapper is RClass && superclassTextOf(wrapper) != null) return false
        if (wrapper.containsComment()) return false
        val body = bodyCompoundOf(wrapper) ?: return false
        val children = body.children.filter { it !is PsiWhiteSpace && it !is PsiComment }
        if (children.size != 1) return false
        return children.first() == child
    }
}
