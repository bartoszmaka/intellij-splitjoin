package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment

class JsxFragmentFormHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        val tag = element.jsxAncestor() ?: return false
        if (tag.containsComment()) return false
        return tag.isShortFragment()
    }

    override fun canJoin(element: PsiElement): Boolean {
        val tag = element.jsxAncestor() ?: return false
        if (tag.containsComment()) return false
        return tag.isReactFragment()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tag = element.jsxAncestor() ?: return
        // <>body</> -> <React.Fragment>body</React.Fragment>
        // Body is everything between the leading `<>` and trailing `</>` leaves.
        val body = tag.shortFragmentBody()
        context.replace(tag, "<React.Fragment>$body</React.Fragment>")
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val tag = element.jsxAncestor() ?: return
        val body = tag.namedFragmentBody()
        context.replace(tag, "<>$body</>")
    }

    // ---------- helpers ----------

    private fun PsiElement.jsxAncestor(): PsiElement? {
        var node: PsiElement? = this
        while (node != null && node !is PsiFile) {
            if (node.javaClass.simpleName == "JSXXmlLiteralExpressionImpl") return node
            node = node.parent
        }
        return null
    }

    private fun PsiElement.isShortFragment(): Boolean {
        // First non-whitespace child is `<>` leaf.
        var c = firstChild
        while (c != null && (c is PsiWhiteSpace || c.text.isBlank())) c = c.nextSibling
        return c?.text == "<>"
    }

    private fun PsiElement.isReactFragment(): Boolean {
        // Tag name is exactly "React.Fragment" or "Fragment".
        var c = firstChild
        var seenOpen = false
        while (c != null) {
            if (c is PsiWhiteSpace) { c = c.nextSibling; continue }
            if (c.text == "<") { seenOpen = true; c = c.nextSibling; continue }
            if (seenOpen) {
                val name = c.text
                return name == "React.Fragment" || name == "Fragment"
            }
            c = c.nextSibling
        }
        return false
    }

    private fun PsiElement.shortFragmentBody(): String {
        // Body = text between `<>` and `</>` leaves.
        val sb = StringBuilder()
        var c = firstChild
        var inBody = false
        while (c != null) {
            if (!inBody) {
                if (c.text == "<>") inBody = true
            } else {
                if (c.text == "</>") break
                sb.append(c.text)
            }
            c = c.nextSibling
        }
        return sb.toString().trim()
    }

    private fun PsiElement.namedFragmentBody(): String {
        // Body = text between opening `>` and closing `</` for named Fragment.
        val sb = StringBuilder()
        var c = firstChild
        var inBody = false
        while (c != null) {
            if (!inBody) {
                if (c.text == ">") {
                    inBody = true
                }
            } else {
                if (c.text == "</") break
                sb.append(c.text)
            }
            c = c.nextSibling
        }
        return sb.toString().trim()
    }
}
