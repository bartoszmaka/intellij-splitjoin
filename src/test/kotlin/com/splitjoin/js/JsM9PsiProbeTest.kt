package com.splitjoin.js

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsM9PsiProbeTest : BasePlatformTestCase() {

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

    fun `test probe union`() = dump("T1 union", "type T = A | B | C")
    fun `test probe intersection`() = dump("T1 intersection", "type T = A & B & C")
    fun `test probe generic call`() = dump("T2 call", "type X = Foo<A, B, C>")
    fun `test probe generic decl`() = dump("T2 decl", "function f<A, B, C>(x: A): void {}")
    fun `test probe type literal`() = dump("T3 alias", "type T = { a: number; b: string }")
    fun `test probe interface body`() = dump("T3 iface", "interface I { a: number; b: string }")
    fun `test probe tuple`() = dump("T4 tuple", "type T = [A, B, C]")
    fun `test probe tuple labels`() = dump("T4 labels", "type T = [a: A, b: B]")
    fun `test probe tuple rest`() = dump("T4 rest", "type T = [A, ...B[]]")
}
