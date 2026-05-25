package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsFunctionFormHandlerTest : BasePlatformTestCase() {

    fun `test split function expression to arrow`() {
        myFixture.configureByText("a.ts", "const f = function (x<caret>) { return x + 1 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const f = (x) => x + 1")
    }

    fun `test split function with multi-statement body keeps block`() {
        myFixture.configureByText(
            "a.ts",
            "const f = function (x<caret>) { const s = x + 1; return s; }"
        )
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const f = (x) => { const s = x + 1; return s; }")
    }

    fun `test split async function`() {
        myFixture.configureByText(
            "a.ts",
            "const f = async function (x<caret>) { return x }"
        )
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const f = async (x) => x")
    }

    fun `test join arrow expression to function`() {
        myFixture.configureByText("a.ts", "const f = (x<caret>) => x + 1")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const f = function (x) { return x + 1; }")
    }

    fun `test join arrow block to function`() {
        myFixture.configureByText(
            "a.ts",
            "const f = (x<caret>) => { const s = x + 1; return s; }"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const f = function (x) { const s = x + 1; return s; }")
    }

    fun `test join async arrow`() {
        myFixture.configureByText("a.ts", "const f = async (x<caret>) => x")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const f = async function (x) { return x; }")
    }

    fun `test round trip`() {
        myFixture.configureByText("a.ts", "const f = function (x<caret>) { return x + 1 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const f = function (x) { return x + 1; }")
    }

    fun `test no-op on generator`() {
        val source = "const f = function*(<caret>) { yield 1 }"
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test allows this in body`() {
        // 'this' binding semantics differ between function and arrow, but per spec
        // the handler does NOT guard against this - user owns the implications.
        myFixture.configureByText(
            "a.ts",
            "const f = function (x<caret>) { return this.y + x }"
        )
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const f = (x) => this.y + x")
    }
}
