package com.splitjoin.json

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsonObjectHandlerTest : BasePlatformTestCase() {

    fun `test split inline object`() {
        myFixture.configureByText("a.json", """{"a<caret>": 1, "b": 2}""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            {
              "a": 1,
              "b": 2
            }
            """.trimIndent()
        )
    }

    fun `test join multi-line object`() {
        myFixture.configureByText(
            "a.json",
            """
            {
              "a<caret>": 1,
              "b": 2
            }
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""{"a": 1, "b": 2}""")
    }

    fun `test round-trip split then join returns original`() {
        myFixture.configureByText("a.json", """{"a<caret>": 1, "b": 2}""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""{"a": 1, "b": 2}""")
    }

    fun `test caret outside any object is a no-op`() {
        myFixture.configureByText("a.json", """<caret>{"a": 1}""")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test empty object does not crash`() {
        myFixture.configureByText("a.json", """<caret>{}""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("""{}""")
    }

    fun `test object with single property splits`() {
        myFixture.configureByText("a.json", """{"a<caret>": 1}""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            {
              "a": 1
            }
            """.trimIndent()
        )
    }
}
