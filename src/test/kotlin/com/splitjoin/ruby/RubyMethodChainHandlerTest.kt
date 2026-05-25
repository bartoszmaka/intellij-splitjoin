package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyMethodChainHandlerTest : BasePlatformTestCase() {

    fun `test split three-call chain`() {
        myFixture.configureByText("a.rb", "a.b<caret>.c.d")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            a
              .b
              .c
              .d
            """.trimIndent()
        )
    }

    fun `test split five-call chain`() {
        myFixture.configureByText("a.rb", "a.b<caret>.c.d.e.f")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            a
              .b
              .c
              .d
              .e
              .f
            """.trimIndent()
        )
    }

    fun `test join multi-line chain`() {
        myFixture.configureByText(
            "a.rb",
            """
            a
              .b<caret>
              .c
              .d
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("a.b.c.d")
    }

    fun `test round trip split then join`() {
        myFixture.configureByText("a.rb", "a.b<caret>.c.d")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("a.b.c.d")
    }

    fun `test no-op on two-call chain`() {
        val source = "a.b<caret>"
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test no-op when chain has block call`() {
        val source = "arr.map { |x| x + 1 }<caret>.filter.first"
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test no-op when chain has parenthesized call`() {
        val source = "a.b(1)<caret>.c.d"
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test comment in chain bails`() {
        val source = """
            a
              .b # comment<caret>
              .c
              .d
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
