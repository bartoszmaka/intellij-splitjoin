package com.splitjoin.ruby

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyM6PsiProbeTest : BasePlatformTestCase() {

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
        for (child in element.children) printTree(child, depth + 1)
    }

    fun `test probe case when single line`() {
        dump("R1 single-line case", "case x; when 1 then a; when 2, 3 then b; else c; end")
    }

    fun `test probe case when multi line`() {
        dump(
            "R1 multi-line case",
            """
            case x
            when 1
              a
            when 2, 3
              b
            else
              c
            end
            """.trimIndent()
        )
    }

    fun `test probe attr_reader single line`() {
        dump("R2 single-line attr_reader", "attr_reader :a, :b, :c")
    }

    fun `test probe attr_reader run of statements`() {
        dump(
            "R2 attr_reader run",
            """
            attr_reader :a
            attr_reader :b
            attr_reader :c
            """.trimIndent()
        )
    }

    fun `test probe rescue modifier compact`() {
        dump("R3 rescue modifier", "foo rescue nil")
    }

    fun `test probe rescue modifier expanded`() {
        dump(
            "R3 rescue expanded",
            """
            begin
              foo
            rescue
              nil
            end
            """.trimIndent()
        )
    }

    fun `test probe lambda arrow`() {
        dump("R4 arrow lambda", "->(x) { x + 1 }")
    }

    fun `test probe lambda do end`() {
        dump(
            "R4 do-end lambda",
            """
            lambda do |x|
              x + 1
            end
            """.trimIndent()
        )
    }

    fun `test probe block pass amp sym`() {
        dump("R5 &:upcase arg", "arr.map(&:upcase)")
    }

    fun `test probe explicit block`() {
        dump("R5 explicit { |x| x.upcase }", "arr.map { |x| x.upcase }")
    }
}
