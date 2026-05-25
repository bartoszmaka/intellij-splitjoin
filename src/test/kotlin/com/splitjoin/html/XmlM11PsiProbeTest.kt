package com.splitjoin.html

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class XmlM11PsiProbeTest : BasePlatformTestCase() {
    private fun dump(label: String, source: String, ext: String) {
        myFixture.configureByText("probe.$ext", source)
        println("=== $label ==="); println(source); println("---")
        printTree(myFixture.file, 0); println()
    }
    private fun printTree(e: PsiElement, d: Int) {
        val ind = "  ".repeat(d)
        val t = e.text.lineSequence().joinToString(" ").take(80)
        println("$ind${e.javaClass.simpleName}  |  \"$t\"")
        var c = e.firstChild
        while (c != null) { printTree(c, d + 1); c = c.nextSibling }
    }
    fun `test self-close`() = dump("H2 self-close", "<br/>", "html")
    fun `test open-empty`() = dump("H2 open-empty", "<br></br>", "html")
    fun `test text child`() = dump("H1 text", "<p>hello</p>", "html")
    fun `test element children`() = dump("H1 elements", "<ul><li/><li/></ul>", "html")
    fun `test class attr`() = dump("H3 class", "<div class=\"a b c\"/>", "html")
}
