package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsVariableDeclaratorListHandlerTest : BasePlatformTestCase() {

    fun `test split const list`() {
        myFixture.configureByText("a.ts", """const a = 1, b<caret> = 2, c = 3""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const a = 1;
            const b = 2;
            const c = 3;
            """.trimIndent()
        )
    }

    fun `test split let list`() {
        myFixture.configureByText("a.ts", """let a<caret> = 1, b = 2""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            let a = 1;
            let b = 2;
            """.trimIndent()
        )
    }

    fun `test join const run`() {
        myFixture.configureByText(
            "a.ts",
            """
            const a = 1;
            const b<caret> = 2;
            const c = 3;
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const a = 1, b = 2, c = 3;""")
    }

    fun `test round trip`() {
        val source = """const a = 1, b = 2, c = 3"""
        myFixture.configureByText("a.ts", "$source<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const a = 1, b = 2, c = 3;""")
    }

    fun `test no-op on destructuring`() {
        val source = """const { a, b<caret> } = obj, c = 1"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        // Destructuring path bails here for J7 (mixed declarator with destructuring).
        // J3 also bails because it requires single declarator. So overall no-op expected.
        myFixture.checkResult(source)
    }

    fun `test no-op join mixed kinds`() {
        val source = """
            const a = 1<caret>;
            let b = 2;
        """.trimIndent()
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
