package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyBlockHandlerTest : BasePlatformTestCase() {

    fun `test split brace block to do end`() {
        myFixture.configureByText("a.rb", "arr.map { |<caret>x| x + 1 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            arr.map do |x|
              x + 1
            end
            """.trimIndent()
        )
    }

    fun `test join do end to brace block`() {
        myFixture.configureByText(
            "a.rb",
            """
            arr.map do |<caret>x|
              x + 1
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("arr.map { |x| x + 1 }")
    }

    fun `test round-trip block`() {
        myFixture.configureByText("a.rb", "arr.map { |<caret>x| x + 1 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("arr.map { |x| x + 1 }")
    }

    fun `test split brace block with no params`() {
        myFixture.configureByText("a.rb", "arr.each { puts<caret> 'hi' }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            arr.each do
              puts 'hi'
            end
            """.trimIndent()
        )
    }

    fun `test split does not fire on do end block`() {
        myFixture.configureByText(
            "a.rb",
            """
            arr.map do |<caret>x|
              x + 1
            end
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test join does not fire on brace block`() {
        myFixture.configureByText("a.rb", "arr.map { |<caret>x| x + 1 }")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }
}
