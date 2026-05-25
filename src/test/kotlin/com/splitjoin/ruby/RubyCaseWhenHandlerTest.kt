package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyCaseWhenHandlerTest : BasePlatformTestCase() {

    fun `test split case with then`() {
        myFixture.configureByText("a.rb", "case x; when 1 then a; when 2 then b; else c; end<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            case x
            when 1
              a
            when 2
              b
            else
              c
            end
            """.trimIndent()
        )
    }

    fun `test split case with comma list`() {
        myFixture.configureByText("a.rb", "case x; when 1 then a; when 2, 3 then b; end<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            case x
            when 1
              a
            when 2, 3
              b
            end
            """.trimIndent()
        )
    }

    fun `test split case no else`() {
        myFixture.configureByText("a.rb", "case x; when :a then 1; when :b then 2; end<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            case x
            when :a
              1
            when :b
              2
            end
            """.trimIndent()
        )
    }

    fun `test join multi-line case`() {
        myFixture.configureByText(
            "a.rb",
            """
            case x<caret>
            when 1
              a
            when 2
              b
            else
              c
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("case x; when 1 then a; when 2 then b; else c; end")
    }

    fun `test join with comma list preserved`() {
        myFixture.configureByText(
            "a.rb",
            """
            case x<caret>
            when 1
              a
            when 2, 3
              b
            end
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("case x; when 1 then a; when 2, 3 then b; end")
    }

    fun `test round trip`() {
        val source = "case x; when 1 then a; when 2 then b; end"
        myFixture.configureByText("a.rb", "$source<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op when body has multiple statements`() {
        val source = """
            case x
            when 1<caret>
              a
              b
            end
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test comment in case bails`() {
        val source = """
            case x<caret>
            when 1
              # leading
              a
            end
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
