package com.splitjoin.html

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler

/**
 * Splits / joins the start-tag of an `XmlTag`. The closing `</tag>` (when present) and any
 * inner content remain untouched.
 *
 * Direction is determined by whether the start-tag spans multiple lines:
 *  - canSplit: tag has >=1 attribute AND the start-tag is on a single line.
 *  - canJoin:  tag has >=1 attribute AND the start-tag spans multiple lines.
 *
 * Bails on:
 *  - XmlComment inside the start-tag range.
 *  - Tag with no attributes (no-op).
 */
class XmlAttributeHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (element !is XmlTag) return false
        val range = startTagRange(element) ?: return false
        if (element.attributes.isEmpty()) return false
        if (startTagContainsComment(element, range)) return false
        return isSingleLine(element, range)
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (element !is XmlTag) return false
        val range = startTagRange(element) ?: return false
        if (element.attributes.isEmpty()) return false
        if (startTagContainsComment(element, range)) return false
        return !isSingleLine(element, range)
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val tag = element as XmlTag
        val range = startTagRange(tag) ?: return
        val name = tag.name
        val attrs = tag.attributes
        val selfClosing = isSelfClosing(tag, range)
        val rebuilt = buildString {
            append("<")
            append(name)
            append("\n")
            attrs.forEachIndexed { _, attr ->
                append(attr.text)
                append("\n")
            }
            append(if (selfClosing) "/>" else ">")
        }
        context.replace(range, rebuilt)
    }

    override fun join(element: PsiElement, context: SplitJoinContext) {
        val tag = element as XmlTag
        val range = startTagRange(tag) ?: return
        val name = tag.name
        val attrs = tag.attributes
        val selfClosing = isSelfClosing(tag, range)
        val joinedAttrs = attrs.joinToString(" ") { it.text }
        val rebuilt = "<$name $joinedAttrs${if (selfClosing) "/>" else ">"}"
        context.replace(range, rebuilt)
    }

    /**
     * Computes the start-tag's TextRange (from the leading `<` through the closing `>` or `/>`).
     * Returns null if the tag has no recognizable opening/closing tokens (shouldn't happen for
     * a well-formed `XmlTag`).
     */
    private fun startTagRange(tag: XmlTag): TextRange? {
        val start = tag.textRange.startOffset
        // Walk children looking for the closing `>` or `/>` token — that's the end of the start-tag.
        val closing = tag.children.firstOrNull { isStartTagEndToken(it) }
            ?: tag.node.findChildByType(XmlTokenType.XML_TAG_END)?.psi
            ?: tag.node.findChildByType(XmlTokenType.XML_EMPTY_ELEMENT_END)?.psi
            ?: return null
        val end = closing.textRange.endOffset
        return TextRange(start, end)
    }

    private fun isStartTagEndToken(child: PsiElement): Boolean {
        if (child !is XmlToken) return false
        val t = child.tokenType
        return t == XmlTokenType.XML_TAG_END || t == XmlTokenType.XML_EMPTY_ELEMENT_END
    }

    private fun isSelfClosing(tag: XmlTag, range: TextRange): Boolean {
        // The XML PSI exposes `isEmpty` / no closing-tag distinctions but the simplest signal
        // is whether the start-tag text ends with `/>`.
        val text = tag.containingFile.text.substring(range.startOffset, range.endOffset)
        return text.trim().endsWith("/>")
    }

    private fun isSingleLine(tag: XmlTag, range: TextRange): Boolean {
        val doc = tag.containingFile.viewProvider.document ?: return false
        return doc.getLineNumber(range.startOffset) == doc.getLineNumber(range.endOffset)
    }

    private fun startTagContainsComment(tag: XmlTag, range: TextRange): Boolean {
        // PsiComment covers XmlComment for HTML/XML AND PsiCommentImpl for JSX's JS-style
        // `/* */` / `//` comments (since JSXmlLiteralExpression is-a XmlTag and reaches this
        // handler as a fallback when JsxAttributeHandler is absent or bails).
        for (comment in PsiTreeUtil.findChildrenOfType(tag, PsiComment::class.java)) {
            if (range.contains(comment.textRange)) return true
        }
        // Text scan for `<!--` between attributes (some parser modes split a comment across
        // token boundaries instead of producing an XmlComment node).
        for (child in tag.children) {
            if (child is PsiWhiteSpace) continue
            if (range.contains(child.textRange) && child.text.contains("<!--")) return true
        }
        return false
    }
}
