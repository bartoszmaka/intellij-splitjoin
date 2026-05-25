package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsxM11PsiProbeTest : BasePlatformTestCase() {
    private fun dump(label: String, source: String) {
        myFixture.configureByText("probe.tsx", source)
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
    fun `test self-close`() = dump("X1 self-close", "const x = <div/>")
    fun `test open-empty`() = dump("X1 open-empty", "const x = <div></div>")
    fun `test text child`() = dump("X2 text", "const x = <p>hello</p>")
    fun `test element children`() = dump("X2 elements", "const x = <div><a/><b/></div>")
    fun `test fragment empty`() = dump("X3 fragment empty", "const x = <></>")
    fun `test fragment named`() = dump("X3 fragment named", "const x = <React.Fragment>a</React.Fragment>")
}
