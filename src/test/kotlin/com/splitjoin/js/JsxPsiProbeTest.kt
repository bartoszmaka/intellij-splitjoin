package com.splitjoin.js

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsxPsiProbeTest : BasePlatformTestCase() {

    fun `test probe JSX opening element with attrs`() {
        val file = myFixture.configureByText(
            "a.jsx",
            "const e = <div className=\"x\" id=\"y\">hi</div>;"
        )
        println("=== JSX OPENING ELEMENT ===")
        printTree(file)
    }

    fun `test probe JSX self closing with attrs`() {
        val file = myFixture.configureByText(
            "a.jsx",
            "const e = <img src=\"x\" alt=\"y\"/>;"
        )
        println("=== JSX SELF CLOSING ===")
        printTree(file)
    }

    fun `test probe JSX with spread and expression attrs`() {
        val file = myFixture.configureByText(
            "a.jsx",
            "const e = <Comp a={1} b=\"x\" {...rest}/>;"
        )
        println("=== JSX SPREAD + EXPR ATTRS ===")
        printTree(file)
    }

    fun `test probe TSX with attrs`() {
        val file = myFixture.configureByText(
            "a.tsx",
            "const e = <Comp<string> a={1} b=\"x\"/>;"
        )
        println("=== TSX (with type arg if applicable) ===")
        printTree(file)
    }

    fun `test probe JSX multi line attrs`() {
        val file = myFixture.configureByText(
            "a.jsx",
            "const e = <div\n  className=\"x\"\n  id=\"y\"\n>hi</div>;"
        )
        println("=== JSX MULTILINE ===")
        printTree(file)
    }

    fun `test probe JSX empty tag`() {
        val file = myFixture.configureByText("a.jsx", "const e = <div>hi</div>;")
        println("=== JSX NO ATTRS ===")
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
