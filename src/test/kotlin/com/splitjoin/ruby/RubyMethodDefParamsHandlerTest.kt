package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyMethodDefParamsHandlerTest : BasePlatformTestCase() {

    fun `test split inline method def`() {
        myFixture.configureByText("a.rb", "def foo(a<caret>, b, c); end")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            def foo(a,
                    b,
                    c,); end
            """.trimIndent()
        )
    }

    fun `test join multi-line method def`() {
        myFixture.configureByText(
            "a.rb",
            """
            def foo(a<caret>,
                    b,
                    c,)
              42
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            """
            def foo(a, b, c)
              42
            end
            """.trimIndent()
        )
    }

    fun `test split method def with default values`() {
        myFixture.configureByText("a.rb", "def foo(a<caret>, b = 1, c:); end")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            def foo(a,
                    b = 1,
                    c:,); end
            """.trimIndent()
        )
    }

    fun `test round-trip method def`() {
        myFixture.configureByText("a.rb", "def foo(a<caret>, b, c); end")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("def foo(a, b, c); end")
    }

    fun `test no-param def is a no-op`() {
        myFixture.configureByText("a.rb", "def foo(<caret>); end")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }
}
