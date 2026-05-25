package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyRescueModifierHandlerTest : BasePlatformTestCase() {

    fun `test split rescue modifier`() {
        myFixture.configureByText("a.rb", "foo rescue<caret> nil")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            begin
              foo
            rescue
              nil
            end
            """.trimIndent()
        )
    }

    fun `test join simple begin rescue`() {
        myFixture.configureByText(
            "a.rb",
            """
            begin<caret>
              foo
            rescue
              nil
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("foo rescue nil")
    }

    fun `test round trip`() {
        myFixture.configureByText("a.rb", "foo rescue nil<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("foo rescue nil")
    }

    fun `test no-op when exception binding present`() {
        val source = """
            begin<caret>
              foo
            rescue StandardError => e
              nil
            end
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op when ensure present`() {
        val source = """
            begin<caret>
              foo
            rescue
              nil
            ensure
              cleanup
            end
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op when multiple body statements`() {
        val source = """
            begin<caret>
              foo
              bar
            rescue
              nil
            end
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test comment bails`() {
        val source = """
            begin<caret>
              # before
              foo
            rescue
              nil
            end
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
