package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsImportListHandlerTest : BasePlatformTestCase() {

    fun `test split named imports`() {
        myFixture.configureByText("a.ts", """import { a<caret>, b, c } from 'x'""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            import {
                a,
                b,
                c,
            } from 'x'
            """.trimIndent()
        )
    }

    fun `test split with default`() {
        myFixture.configureByText("a.ts", """import D, { a<caret>, b } from 'x'""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            import D, {
                a,
                b,
            } from 'x'
            """.trimIndent()
        )
    }

    fun `test join named imports`() {
        myFixture.configureByText(
            "a.ts",
            """
            import {
                a<caret>,
                b,
                c,
            } from 'x'
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""import { a, b, c } from 'x'""")
    }

    fun `test round trip`() {
        val source = """import { a, b, c } from 'x'"""
        myFixture.configureByText("a.ts", "$source<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op on single import`() {
        val source = """import { a<caret> } from 'x'"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test comment bails`() {
        val source = """
            import {
                a<caret>,
                // skip me
                b,
            } from 'x'
        """.trimIndent()
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
