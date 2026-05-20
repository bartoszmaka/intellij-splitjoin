package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyArrayHandlerTest : BasePlatformTestCase() {

    fun `test split inline array`() {
        myFixture.configureByText("a.rb", "x = [1<caret>, 2, 3]")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            x = [
              1,
              2,
              3,
            ]
            """.trimIndent()
        )
    }

    fun `test join multi-line array`() {
        myFixture.configureByText(
            "a.rb",
            """
            x = [
              1<caret>,
              2,
              3,
            ]
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("x = [1, 2, 3]")
    }

    fun `test round-trip split then join returns original`() {
        myFixture.configureByText("a.rb", "x = [1<caret>, 2, 3]")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("x = [1, 2, 3]")
    }

    fun `test single-element array splits`() {
        myFixture.configureByText("a.rb", "x = [1<caret>]")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            x = [
              1,
            ]
            """.trimIndent()
        )
    }

    fun `test empty array is a no-op`() {
        myFixture.configureByText("a.rb", "x = [<caret>]")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test array with comment inside is a no-op`() {
        myFixture.configureByText(
            "a.rb",
            """
            x = [
              1<caret>, # comment
              2,
            ]
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }
}
