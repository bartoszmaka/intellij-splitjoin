package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyPsiProbeTest : BasePlatformTestCase() {

    fun `test probehash literal`() {
        val file = myFixture.configureByText("a.rb", "x = { a: 1, b: 2 }")
        println("=== HASH LITERAL ===")
        printTree(file)
    }

    fun `test probearray literal`() {
        val file = myFixture.configureByText("a.rb", "x = [1, 2, 3]")
        println("=== ARRAY LITERAL ===")
        printTree(file)
    }

    fun `test probemethod call with parens`() {
        val file = myFixture.configureByText("a.rb", "foo(a, b, c)")
        println("=== CALL WITH PARENS ===")
        printTree(file)
    }

    fun `test probemethod call without parens`() {
        val file = myFixture.configureByText("a.rb", "foo a, b, c")
        println("=== CALL WITHOUT PARENS ===")
        printTree(file)
    }

    fun `test probemethod call with keyword args`() {
        val file = myFixture.configureByText("a.rb", "foo(a: 1, b: 2)")
        println("=== CALL WITH KWARGS ===")
        printTree(file)
    }

    fun `test probemethod call with trailing hash arg no braces`() {
        val file = myFixture.configureByText("a.rb", "foo a: 1, b: 2")
        println("=== CALL TRAILING HASH NO BRACES ===")
        printTree(file)
    }

    fun `test probemethod definition`() {
        val file = myFixture.configureByText("a.rb", "def foo(a, b, c); end")
        println("=== METHOD DEFINITION ===")
        printTree(file)
    }

    private fun printTree(file: com.intellij.psi.PsiFile) {
        val tree = StringBuilder()
        printElement(file, 0, tree)
        println(tree)
    }

    private fun printElement(element: com.intellij.psi.PsiElement, depth: Int, sb: StringBuilder) {
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
