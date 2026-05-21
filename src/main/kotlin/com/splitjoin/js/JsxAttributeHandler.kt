package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSEmbeddedContent
import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.lang.javascript.psi.JSXmlLiteralExpression
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler

/**
 * Splits / joins the attribute list of a JSX/TSX element. Direction is determined by whether
 * the opening tag (NOT the whole element) spans multiple lines.
 *
 * JSXmlLiteralExpression is a public interface in the JS plugin that extends HtmlTag/XmlTag;
 * both opening and self-closing JSX elements are represented this way. The interface also
 * covers the legacy E4X XML-literal syntax, but E4X is effectively extinct so we don't go
 * out of our way to exclude it.
 *
 * JSX-specific concern: spread attributes (`{...rest}`) are not XmlAttribute instances; they
 * surface as JSEmbeddedContent wrapping a JSSpreadExpression. We collect both shapes so a split
 * preserves them in source order.
 */
class JsxAttributeHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is JSXmlLiteralExpression) return false
        val range = openingTagRange(element) ?: return false
        if (attributesOf(element).isEmpty()) return false
        if (openingTagContainsComment(element, range)) return false
        return isSingleLine(element, range)
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is JSXmlLiteralExpression) return false
        val range = openingTagRange(element) ?: return false
        if (attributesOf(element).isEmpty()) return false
        if (openingTagContainsComment(element, range)) return false
        return !isSingleLine(element, range)
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tag = element as JSXmlLiteralExpression
        val range = openingTagRange(tag) ?: return
        val tagName = tag.name ?: return
        val attrs = attributesOf(tag)
        val selfClosing = isSelfClosing(tag)
        val rebuilt = buildString {
            append("<")
            append(tagName)
            append("\n")
            attrs.forEach { attr ->
                append(attr.text)
                append("\n")
            }
            append(if (selfClosing) "/>" else ">")
        }
        context.replace(range, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val tag = element as JSXmlLiteralExpression
        val range = openingTagRange(tag) ?: return
        val tagName = tag.name ?: return
        val attrs = attributesOf(tag)
        val selfClosing = isSelfClosing(tag)
        val joinedAttrs = attrs.joinToString(" ") { it.text }
        val rebuilt = "<$tagName $joinedAttrs${if (selfClosing) "/>" else ">"}"
        context.replace(range, rebuilt)
    }

    /**
     * Returns the opening tag's range: from the leading `<` through `>` or `/>`. The closing
     * tag (if present) and any inner content stay outside the range.
     */
    private fun openingTagRange(tag: JSXmlLiteralExpression): TextRange? {
        val start = tag.textRange.startOffset
        for (child in tag.children) {
            if (child is XmlToken) {
                val t = child.tokenType
                if (t == XmlTokenType.XML_TAG_END || t == XmlTokenType.XML_EMPTY_ELEMENT_END) {
                    return TextRange(start, child.textRange.endOffset)
                }
            }
        }
        return null
    }

    private fun isSelfClosing(tag: JSXmlLiteralExpression): Boolean {
        for (child in tag.children) {
            if (child is XmlToken && child.tokenType == XmlTokenType.XML_EMPTY_ELEMENT_END) {
                return true
            }
        }
        return false
    }

    private fun isSingleLine(tag: JSXmlLiteralExpression, range: TextRange): Boolean {
        val doc = tag.containingFile.viewProvider.document ?: return false
        return doc.getLineNumber(range.startOffset) == doc.getLineNumber(range.endOffset)
    }

    /**
     * Collects items between `<tagname` and `>`/`/>` that contribute to the attribute list:
     * regular JSX attributes (which the IDE exposes as XmlAttribute) and spread attributes
     * (JSEmbeddedContent wrapping JSSpreadExpression). PsiWhiteSpace and XmlToken boundaries
     * are skipped.
     */
    private fun attributesOf(tag: JSXmlLiteralExpression): List<PsiElement> =
        tag.children.filter { it is XmlAttribute || isSpreadAttribute(it) }

    private fun isSpreadAttribute(element: PsiElement): Boolean {
        if (element !is JSEmbeddedContent) return false
        return element.children.any { it is JSSpreadExpression }
    }

    /**
     * Bail only on comments inside the opening-tag range. Comments in element children (e.g.,
     * `<div>{/* x */}</div>`) are preserved verbatim by the split — they don't intersect the
     * region we rewrite — so they're not the handler's concern.
     */
    private fun openingTagContainsComment(tag: JSXmlLiteralExpression, range: TextRange): Boolean {
        for (comment in PsiTreeUtil.findChildrenOfType(tag, PsiComment::class.java)) {
            if (range.contains(comment.textRange)) return true
        }
        return false
    }
}
