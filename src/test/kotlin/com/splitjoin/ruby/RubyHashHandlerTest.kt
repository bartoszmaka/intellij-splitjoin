package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyHashHandlerTest : BasePlatformTestCase() {

    fun `test split inline hash`() {
        myFixture.configureByText("a.rb", "x = { a<caret>: 1, b: 2 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            x = {
              a: 1,
              b: 2,
            }
            """.trimIndent()
        )
    }

    fun `test join multi-line hash`() {
        myFixture.configureByText(
            "a.rb",
            """
            x = {
              a<caret>: 1,
              b: 2,
            }
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("x = { a: 1, b: 2 }")
    }

    fun `test round-trip split then join returns original`() {
        myFixture.configureByText("a.rb", "x = { a<caret>: 1, b: 2 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("x = { a: 1, b: 2 }")
    }

    fun `test single-entry hash splits`() {
        myFixture.configureByText("a.rb", "x = { a<caret>: 1 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            x = {
              a: 1,
            }
            """.trimIndent()
        )
    }

    fun `test empty hash is a no-op`() {
        myFixture.configureByText("a.rb", "x = {<caret>}")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test hash with comment inside is a no-op`() {
        myFixture.configureByText(
            "a.rb",
            """
            x = {
              a<caret>: 1, # comment
              b: 2,
            }
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }
}
