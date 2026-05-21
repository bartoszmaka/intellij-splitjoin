package com.splitjoin.html

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class XmlPsiProbeTest : BasePlatformTestCase() {

    fun `test probe HTML tag with attributes inline`() {
        val file = myFixture.configureByText(
            "a.html",
            "<a href=\"x\" class=\"y\">link</a>"
        )
        println("=== HTML TAG INLINE ATTRS ===")
        printTree(file)
    }

    fun `test probe HTML tag self closing`() {
        val file = myFixture.configureByText(
            "a.html",
            "<img src=\"x\" alt=\"y\"/>"
        )
        println("=== HTML SELF CLOSING ===")
        printTree(file)
    }

    fun `test probe HTML tag multi line attrs`() {
        val file = myFixture.configureByText(
            "a.html",
            "<a\n  href=\"x\"\n  class=\"y\"\n>link</a>"
        )
        println("=== HTML MULTILINE ATTRS ===")
        printTree(file)
    }

    fun `test probe XML tag`() {
        val file = myFixture.configureByText(
            "a.xml",
            "<root attr=\"v\"><child id=\"c\"/></root>"
        )
        println("=== XML ===")
        printTree(file)
    }

    fun `test probe Vue template`() {
        val file = myFixture.configureByText(
            "a.html",
            "<template>\n  <div class=\"x\" id=\"y\">hi</div>\n</template>"
        )
        println("=== VUE-LIKE TEMPLATE (in .html) ===")
        printTree(file)
    }

    fun `test probe tag with embedded comment`() {
        val file = myFixture.configureByText(
            "a.html",
            "<a href=\"x\" <!--note--> class=\"y\">link</a>"
        )
        println("=== HTML WITH EMBEDDED COMMENT ===")
        printTree(file)
    }

    fun `test probe empty tag (no attrs)`() {
        val file = myFixture.configureByText("a.html", "<a>x</a>")
        println("=== HTML NO ATTRS ===")
        printTree(file)
    }

    private fun printTree(file: PsiFile) {
        val sb = StringBuilder()
        printElement(file, 0, sb)
        println(sb)
    }

    private fun printElement(element: PsiElement, depth: Int, sb: StringBuilder) {
        repeat(depth) { sb.append("  ") }
        sb.append(element.javaClass.simpleName)
        sb.append("  [")
        sb.append(element.text.replace("\n", "\\n"))
        sb.append("]\n")
        for (child in element.children) {
            printElement(child, depth + 1, sb)
        }
    }
}
