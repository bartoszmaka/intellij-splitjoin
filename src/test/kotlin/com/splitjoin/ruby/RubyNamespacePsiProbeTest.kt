package com.splitjoin.ruby

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyNamespacePsiProbeTest : BasePlatformTestCase() {

    fun `test probe compact class`() {
        val file = myFixture.configureByText("a.rb", "class Foo::Bar::Baz\nend")
        println("=== COMPACT CLASS (no superclass) ===")
        printTree(file)
    }

    fun `test probe compact class with superclass`() {
        val file = myFixture.configureByText("a.rb", "class Foo::Bar::Baz < Base\nend")
        println("=== COMPACT CLASS WITH SUPERCLASS ===")
        printTree(file)
    }

    fun `test probe compact module`() {
        val file = myFixture.configureByText("a.rb", "module Foo::Bar\nend")
        println("=== COMPACT MODULE ===")
        printTree(file)
    }

    fun `test probe nested modules with inner class`() {
        val file = myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module Bar
                class Baz
                end
              end
            end
            """.trimIndent()
        )
        println("=== NESTED MODULES WITH INNER CLASS ===")
        printTree(file)
    }

    fun `test probe nested modules with inner class and superclass`() {
        val file = myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module Bar
                class Baz < Base
                end
              end
            end
            """.trimIndent()
        )
        println("=== NESTED MODULES WITH INNER CLASS + SUPERCLASS ===")
        printTree(file)
    }

    fun `test probe nested modules with sibling content`() {
        val file = myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module Bar
                class Baz
                end
                CONST = 1
              end
            end
            """.trimIndent()
        )
        println("=== NESTED MODULES WITH SIBLING CONTENT (must not join) ===")
        printTree(file)
    }

    fun `test probe nested modules with inner module`() {
        val file = myFixture.configureByText(
            "a.rb",
            """
            module Foo
              module Bar
                module Baz
                end
              end
            end
            """.trimIndent()
        )
        println("=== NESTED MODULES TERMINATING IN MODULE ===")
        printTree(file)
    }

    private fun printTree(file: PsiFile) {
        val tree = StringBuilder()
        printElement(file, 0, tree)
        println(tree)
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
