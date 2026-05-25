package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsM8PsiProbeTest : BasePlatformTestCase() {

    private fun dump(label: String, source: String, ext: String = "ts") {
        myFixture.configureByText("probe.$ext", source)
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

    fun `test probe named import`() {
        dump("J2 import", """import { a, b, c } from 'x'""")
    }

    fun `test probe object destructuring`() {
        dump("J3 obj destructure", """const { a, b } = obj""")
    }

    fun `test probe array destructuring`() {
        dump("J3 arr destructure", """const [a, b] = arr""")
    }

    fun `test probe destructuring run`() {
        dump(
            "J3 run",
            """
            const a = obj.a;
            const b = obj.b;
            """.trimIndent()
        )
    }

    fun `test probe shorthand object`() {
        dump("J4 shorthand", """const x = { a, b }""")
    }

    fun `test probe explicit redundant`() {
        dump("J4 explicit", """const x = { a: a, b: b }""")
    }

    fun `test probe declarator list`() {
        dump("J7 declarators", """const a = 1, b = 2""")
    }

    fun `test probe declarator run`() {
        dump(
            "J7 run",
            """
            const a = 1;
            const b = 2;
            """.trimIndent()
        )
    }
}
