package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * J4 is join-only: `{ a: a, b: b }` -> `{ a, b }`. Split direction is delegated to
 * JsObjectHandler (single-line -> multi-line). Users wanting to expand shorthand to
 * explicit must rewrite manually.
 */
class JsObjectShorthandHandlerTest : BasePlatformTestCase() {

    fun `test join flips explicit to shorthand`() {
        myFixture.configureByText("a.ts", """const x = { a: a<caret>, b: b }""")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const x = { a, b }""")
    }

    fun `test join with single property`() {
        myFixture.configureByText("a.ts", """const x = { a: a<caret> }""")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const x = { a }""")
    }

    fun `test no-op join on mixed object`() {
        val source = """const x = { a<caret>, b: b }"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op join on non-matching key value`() {
        val source = """const x = { a: b<caret>, c: d }"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op join on already-shorthand`() {
        val source = """const x = { a<caret>, b }"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test split is delegated to JsObjectHandler`() {
        // J4 surrenders Split to JsObjectHandler. For a single-line key:key object,
        // Split triggers JsObjectHandler's multi-line behaviour, not a J4 flip.
        myFixture.configureByText("a.ts", """const x = { a: a<caret>, b: b }""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = {
                a: a,
                b: b,
            }
            """.trimIndent()
        )
    }
}
