package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyTernaryHandlerTest : BasePlatformTestCase() {

    fun `test split ternary`() {
        myFixture.configureByText("a.rb", "x = a ?<caret> b : c")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            "x = if a\n      b\n    else\n      c\n    end"
        )
    }

    fun `test join simple if else`() {
        myFixture.configureByText(
            "a.rb",
            """
            x = if a<caret>
              b
            else
              c
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("x = a ? b : c")
    }

    fun `test round-trip ternary`() {
        myFixture.configureByText("a.rb", "x = a ?<caret> b : c")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("x = a ? b : c")
    }

    fun `test join uses if-mod handler for if without else`() {
        // Ternary handler bails (no else); RubyIfModifierHandler picks it up.
        // Wrapped in `x = (...)` so the modifier form is grammatically distinct.
        myFixture.configureByText(
            "a.rb",
            """
            if a<caret>
              b
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("b if a")
    }

    fun `test join does not fire on multi-statement bodies`() {
        myFixture.configureByText(
            "a.rb",
            """
            if a<caret>
              b
              c
            else
              d
            end
            """.trimIndent()
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Join")
        assertEquals(before, myFixture.editor.document.text)
    }
}
