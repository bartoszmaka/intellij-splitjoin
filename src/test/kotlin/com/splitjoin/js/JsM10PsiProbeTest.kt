package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsM10PsiProbeTest : BasePlatformTestCase() {

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

    fun `test probe template with interpolation`() {
        // Use String() concat to avoid Kotlin interpolating ${name}
        val src = "const x = " + "`" + "hi \${name}!" + "`"
        dump("J5 template", src)
    }

    fun `test probe template no interpolation`() {
        val src = "const x = " + "`" + "plain text" + "`"
        dump("J5 plain template", src)
    }

    fun `test probe ternary decl`() = dump("J1 decl", "const x = a ? b : c")
    fun `test probe ternary assign`() = dump("J1 assign", "x = a ? b : c")
    fun `test probe ternary return`() = dump("J1 return", "function f() { return a ? b : c }")
    fun `test probe arrow expr`() = dump("J6 arrow expr", "const f = (x) => x + 1")
    fun `test probe function expr`() = dump("J6 fn expr", "const f = function (x) { return x + 1 }")
    fun `test probe async`() = dump("J6 async", "const f = async (x) => x + 1")
    fun `test probe generator`() = dump("J6 gen", "const f = function*() { yield 1 }")
}
