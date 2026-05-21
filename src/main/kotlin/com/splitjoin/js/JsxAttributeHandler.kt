package com.splitjoin.js

import com.intellij.lang.javascript.psi.JSEmbeddedContent
import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlToken
import com.intellij.psi.xml.XmlTokenType
import com.intellij.psi.util.PsiTreeUtil
import com.splitjoin.core.SplitJoinContext
import com.splitjoin.core.SplitJoinHandler
import com.splitjoin.core.containsComment
import com.splitjoin.core.isOnSingleLine

/**
 * Splits / joins the attribute list of a JSX element. Direction is determined by whether
 * the opening element spans multiple lines.
 *
 * RubyMine 2024.2.4 uses `JSXXmlLiteralExpressionImpl` for both opening and self-closing elements.
 * We match by class name since the interface is not public. Attributes are matched by class name
 * (`JSXmlAttributeImpl`) and spread attributes (`{...rest}`) are `JSEmbeddedContent` children with
 * `JSSpreadExpression` inside.
 */
class JsxAttributeHandler : SplitJoinHandler {

    override fun canSplit(element: PsiElement): Boolean {
        if (!isJsxElement(element)) return false
        if (attributesOf(element).isEmpty()) return false
        if (hasComment(element)) return false
        return element.isOnSingleLine()
    }

    override fun canJoin(element: PsiElement): Boolean {
        if (!isJsxElement(element)) return false
        if (attributesOf(element).isEmpty()) return false
        if (hasComment(element)) return false
        return !element.isOnSingleLine()
    }

    override fun split(element: PsiElement, context: SplitJoinContext) {
        val range = openingTagRange(element) ?: return
        val tagName = tagNameOf(element) ?: return
        val attrs = attributesOf(element)
        val selfClosing = isSelfClosing(element)
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
        val range = openingTagRange(element) ?: return
        val tagName = tagNameOf(element) ?: return
        val attrs = attributesOf(element)
        val selfClosing = isSelfClosing(element)
        val joinedAttrs = attrs.joinToString(" ") { it.text }
        val rebuilt = "<$tagName $joinedAttrs${if (selfClosing) "/>" else ">"}"
        context.replace(range, rebuilt)
    }

    private fun isJsxElement(element: PsiElement): Boolean {
        // Match on class name since JSXXmlLiteralExpression is not publicly exposed.
        val className = element.javaClass.simpleName
        return className == "JSXXmlLiteralExpressionImpl"
    }

    private fun hasComment(element: PsiElement): Boolean {
        // Use the standard PsiComment search (for /* */ and // comments that are parsed as PSI)
        if (element.containsComment()) return true
        // Also do a text-based scan for comments that might not be parsed into the PSI tree,
        // especially within embedded content / spread attributes.
        if (element.text.contains("/*") || element.text.contains("//")) return true
        return false
    }

    /**
     * Computes the opening-tag range (from `<` through `>` or `/>`), not including
     * any content or closing tag.
     */
    private fun openingTagRange(element: PsiElement): TextRange? {
        val start = element.textRange.startOffset
        // Find the closing `>` or `/>` token.
        for (child in element.children) {
            if (child is XmlToken) {
                val t = child.tokenType
                if (t == XmlTokenType.XML_TAG_END || t == XmlTokenType.XML_EMPTY_ELEMENT_END) {
                    // Found it.
                    val end = child.textRange.endOffset
                    return TextRange(start, end)
                }
            }
        }
        return null
    }

    private fun isSelfClosing(element: PsiElement): Boolean {
        // Check if the last meaningful token is `/>` or `>`.
        val children = element.children
        for (i in children.size - 1 downTo 0) {
            val child = children[i]
            if (child is XmlToken) {
                val t = child.tokenType
                if (t == XmlTokenType.XML_TAG_END || t == XmlTokenType.XML_EMPTY_ELEMENT_END) {
                    // Found the closing token. Check if it's `/>`.
                    return child.text == "/>"
                }
            }
        }
        return false
    }

    /**
     * Returns the JSX tag name. The tag name is typically the first XmlToken
     * child (e.g., "div", "Comp"). We skip `<` and return the first non-bracket token.
     */
    private fun tagNameOf(element: PsiElement): String? {
        for (child in element.children) {
            if (child is XmlToken) {
                val text = child.text
                if (text != "<" && text != ">" && text != "/>" && text != "/" && !text.isBlank()) {
                    // This is likely the tag name. Stop before we hit an attribute.
                    if (!isJsxAttribute(child)) {
                        return text
                    }
                }
            } else if (isJsxAttribute(child)) {
                // Hit an attribute; stop before it.
                break
            }
        }
        return null
    }

    /**
     * Returns all JSX attribute-like children: elements with class name `JSXmlAttributeImpl`
     * and `JSEmbeddedContent` (which may contain spread attributes). We preserve them in order.
     */
    private fun attributesOf(element: PsiElement): List<PsiElement> {
        return element.children.filter { isJsxAttribute(it) || isSpreadAttribute(it) }
    }

    private fun isJsxAttribute(element: PsiElement): Boolean {
        val className = element.javaClass.simpleName
        return className == "JSXmlAttributeImpl"
    }

    private fun isSpreadAttribute(element: PsiElement): Boolean {
        if (element !is JSEmbeddedContent) return false
        // Check if this embedded content wraps a spread expression.
        for (child in element.children) {
            if (child is JSSpreadExpression) return true
        }
        return false
    }
}
