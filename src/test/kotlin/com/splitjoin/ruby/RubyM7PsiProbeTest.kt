package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyM7PsiProbeTest : BasePlatformTestCase() {

    private fun dump(label: String, source: String) {
        myFixture.configureByText("probe.rb", source)
        println("=== $label ===")
        println(source)
        println("---")
        printTree(myFixture.file, 0)
        println()
    }

    private fun printTree(element: PsiElement, depth: Int) {
        val indent = "  ".repeat(depth)
        val text = element.text.lineSequence().joinToString(" ").take(80)
        println("$indent${element.javaClass.simpleName}  |  \"$text\"")
        var child = element.firstChild
        while (child != null) {
            printTree(child, depth + 1)
            child = child.nextSibling
        }
    }

    fun `test probe interpolated string`() {
        dump("R6 interp", """x = "hi #{name}!"""")
    }

    fun `test probe concat chain`() {
        dump("R6 concat", """x = "hi " + name + "!"""")
    }

    fun `test probe simple chain`() {
        dump("R8 short chain", "a.b.c")
    }

    fun `test probe long chain`() {
        dump("R8 long chain", "a.b.c.d.e")
    }
}
