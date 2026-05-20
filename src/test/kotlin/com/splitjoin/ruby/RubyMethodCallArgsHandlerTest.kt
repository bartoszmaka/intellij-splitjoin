package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyMethodCallArgsHandlerTest : BasePlatformTestCase() {

    fun `test split parenthesized call`() {
        myFixture.configureByText("a.rb", "foo(a<caret>, b, c)")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            foo(a,
                b,
                c,)
            """.trimIndent()
        )
    }

    fun `test join multi-line parenthesized call`() {
        myFixture.configureByText(
            "a.rb",
            """
            foo(a<caret>,
                b,
                c,)
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("foo(a, b, c)")
    }

    fun `test split no-parens call`() {
        myFixture.configureByText("a.rb", "foo a<caret>, b, c")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            foo a,
                b,
                c
            """.trimIndent()
        )
    }

    fun `test join multi-line no-parens call`() {
        myFixture.configureByText(
            "a.rb",
            """
            foo a<caret>,
                b,
                c
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("foo a, b, c")
    }

    fun `test split keyword args`() {
        myFixture.configureByText("a.rb", "foo(a<caret>: 1, b: 2)")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            foo(a: 1,
                b: 2,)
            """.trimIndent()
        )
    }

    fun `test split trailing hash no braces`() {
        myFixture.configureByText("a.rb", "foo a<caret>: 1, b: 2")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            foo a: 1,
                b: 2
            """.trimIndent()
        )
    }

    fun `test round-trip parenthesized call`() {
        myFixture.configureByText("a.rb", "foo(a<caret>, b, c)")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("foo(a, b, c)")
    }

    fun `test single arg call splits`() {
        myFixture.configureByText("a.rb", "foo(<caret>abc)")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("foo(abc,)")
    }

    fun `test no-arg call is a no-op`() {
        myFixture.configureByText("a.rb", "foo(<caret>)")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }
}
