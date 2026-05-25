package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyBlockPassHandlerTest : BasePlatformTestCase() {

    fun `test split amp sym to explicit block`() {
        myFixture.configureByText("a.rb", "arr.map(&:upcase<caret>)")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("arr.map { |x| x.upcase }")
    }

    fun `test join explicit block to amp sym`() {
        myFixture.configureByText("a.rb", "arr.map { |x|<caret> x.upcase }")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("arr.map(&:upcase)")
    }

    fun `test round trip`() {
        myFixture.configureByText("a.rb", "arr.map(&:upcase)<caret>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("arr.map(&:upcase)")
    }

    fun `test no-op when block has args to inner method`() {
        val source = "arr.map { |x|<caret> x.foo(1) }"
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op when block has multiple statements`() {
        val source = """
            arr.map { |x|<caret>
              y = x + 1
              y.to_s
            }
        """.trimIndent()
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test no-op when receiver mismatches parameter`() {
        val source = "arr.map { |x|<caret> other.upcase }"
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
