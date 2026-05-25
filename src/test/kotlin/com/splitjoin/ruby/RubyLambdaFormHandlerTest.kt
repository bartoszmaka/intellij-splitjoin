package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyLambdaFormHandlerTest : BasePlatformTestCase() {

    fun `test split arrow lambda to do end`() {
        myFixture.configureByText("a.rb", "f = ->(x)<caret> { x + 1 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            f = lambda do |x|
              x + 1
            end
            """.trimIndent()
        )
    }

    fun `test join do end lambda to arrow`() {
        myFixture.configureByText(
            "a.rb",
            """
            f = lambda do<caret> |x|
              x + 1
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("f = ->(x) { x + 1 }")
    }

    fun `test split arrow no params`() {
        myFixture.configureByText("a.rb", "f = ->()<caret> { 42 }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            f = lambda do
              42
            end
            """.trimIndent()
        )
    }

    fun `test round trip`() {
        val source = "f = ->(x, y) { x + y }"
        myFixture.configureByText("a.rb", "$source<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test comment bails`() {
        val source = """
            f = lambda do<caret> |x|
              # comment
              x + 1
            end
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
