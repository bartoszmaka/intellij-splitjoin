package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsArrowBodyHandlerTest : BasePlatformTestCase() {

    fun `test split expression body to block with return`() {
        myFixture.configureByText("a.js", "const f = (x<caret>) => x + 1;")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const f = (x) => {
                return x + 1;
            };
            """.trimIndent()
        )
    }

    fun `test join block with single return to expression`() {
        myFixture.configureByText(
            "a.js",
            """
            const f = (x<caret>) => {
                return x + 1;
            };
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const f = (x) => x + 1;")
    }

    fun `test round-trip arrow`() {
        myFixture.configureByText("a.js", "const f = (x<caret>) => x + 1;")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const f = (x) => x + 1;")
    }

    fun `test join bails on multi-statement block`() {
        myFixture.configureByText(
            "a.js",
            """
            const f = (x<caret>) => {
                console.log(x);
                return x + 1;
            };
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test join bails on void return`() {
        myFixture.configureByText(
            "a.js",
            """
            const f = (x<caret>) => {
                return;
            };
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test split preserves TS return type`() {
        myFixture.configureByText("a.ts", "const f = (x<caret>: number): number => x + 1;")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const f = (x: number): number => {
                return x + 1;
            };
            """.trimIndent()
        )
    }

    fun `test comment in arrow body bails`() {
        myFixture.configureByText(
            "a.js",
            """
            const f = (x<caret>) => {
                // keep
                return x + 1;
            };
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }
}
