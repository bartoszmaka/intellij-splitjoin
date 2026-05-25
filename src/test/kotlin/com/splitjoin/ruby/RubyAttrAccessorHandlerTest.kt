package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyAttrAccessorHandlerTest : BasePlatformTestCase() {

    fun `test split attr_reader`() {
        myFixture.configureByText("a.rb", "attr_reader :a<caret>, :b, :c")
        myFixture.performEditorAction("Splitjoin.Split")
        println("=== ACTUAL ===\n" + myFixture.editor.document.text + "\n===")
        myFixture.checkResult(
            """
            attr_reader :a
            attr_reader :b
            attr_reader :c
            """.trimIndent()
        )
    }

    fun `test split attr_accessor two`() {
        myFixture.configureByText("a.rb", "attr_accessor :foo<caret>, :bar")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            attr_accessor :foo
            attr_accessor :bar
            """.trimIndent()
        )
    }

    fun `test join attr_reader run`() {
        myFixture.configureByText(
            "a.rb",
            """
            attr_reader :a<caret>
            attr_reader :b
            attr_reader :c
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("attr_reader :a, :b, :c")
    }

    fun `test join from middle of run`() {
        myFixture.configureByText(
            "a.rb",
            """
            attr_writer :x
            attr_writer :y<caret>
            attr_writer :z
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("attr_writer :x, :y, :z")
    }

    fun `test round trip`() {
        val source = "attr_accessor :a, :b, :c"
        myFixture.configureByText("a.rb", "$source<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op when args are non-symbol`() {
        val source = "attr_reader \"a\", \"b\"<caret>"
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test join stops at non-matching name`() {
        myFixture.configureByText(
            "a.rb",
            """
            attr_reader :a<caret>
            attr_writer :b
            attr_reader :c
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(
            """
            attr_reader :a
            attr_writer :b
            attr_reader :c
            """.trimIndent()
        )
    }

    fun `test comment in run bails`() {
        val source = """
            attr_reader :a<caret>
            # comment between
            attr_reader :b
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
