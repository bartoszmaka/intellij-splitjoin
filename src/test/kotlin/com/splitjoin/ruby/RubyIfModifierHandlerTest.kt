package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyIfModifierHandlerTest : BasePlatformTestCase() {

    fun `test split if modifier`() {
        myFixture.configureByText("a.rb", "return x if<caret> y")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            if y
              return x
            end
            """.trimIndent()
        )
    }

    fun `test split unless modifier`() {
        myFixture.configureByText("a.rb", "return x unless<caret> y")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            unless y
              return x
            end
            """.trimIndent()
        )
    }

    fun `test join simple if statement`() {
        myFixture.configureByText(
            "a.rb",
            """
            if y<caret>
              return x
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("return x if y")
    }

    fun `test join simple unless statement`() {
        myFixture.configureByText(
            "a.rb",
            """
            unless y<caret>
              return x
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("return x unless y")
    }

    fun `test round-trip if`() {
        myFixture.configureByText("a.rb", "return x if<caret> y")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("return x if y")
    }

    fun `test join does not fire on if with multiple body statements`() {
        myFixture.configureByText(
            "a.rb",
            """
            if y<caret>
              a
              b
            end
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }
}
