package com.splitjoin.json

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsonArrayHandlerTest : BasePlatformTestCase() {

    fun `test split inline array`() {
        myFixture.configureByText("a.json", """[1<caret>, 2, 3]""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            [
              1,
              2,
              3
            ]
            """.trimIndent()
        )
    }

    fun `test join multi-line array`() {
        myFixture.configureByText(
            "a.json",
            """
            [
              1<caret>,
              2,
              3
            ]
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""[1, 2, 3]""")
    }

    fun `test round-trip split then join returns original`() {
        myFixture.configureByText("a.json", """[1<caret>, 2, 3]""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""[1, 2, 3]""")
    }

    fun `test empty array does not crash`() {
        myFixture.configureByText("a.json", """<caret>[]""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("""[]""")
    }

    fun `test outer array splits when caret is on outer bracket`() {
        myFixture.configureByText("a.json", """<caret>[{"a": 1}, {"b": 2}]""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            [
              {"a": 1},
              {"b": 2}
            ]
            """.trimIndent()
        )
    }

    fun `test inner array splits when caret is inside inner array`() {
        myFixture.configureByText("a.json", """[[1<caret>, 2], [3, 4]]""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            [[
              1,
              2
            ], [3, 4]]
            """.trimIndent()
        )
    }
}
