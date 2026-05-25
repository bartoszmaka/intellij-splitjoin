package com.splitjoin.ruby

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RubyStringInterpolationHandlerTest : BasePlatformTestCase() {

    fun `test split single interpolation`() {
        myFixture.configureByText("a.rb", """x = "hi #{name<caret>}!"""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("""x = "hi " + name.to_s + "!"""")
    }

    fun `test split leading interpolation`() {
        myFixture.configureByText("a.rb", """x = "#{name<caret>}!"""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("""x = name.to_s + "!"""")
    }

    fun `test split trailing interpolation`() {
        myFixture.configureByText("a.rb", """x = "hi #{name<caret>}"""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("""x = "hi " + name.to_s""")
    }

    fun `test split multiple interpolations`() {
        myFixture.configureByText("a.rb", """x = "#{a<caret>}-#{b}-end"""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("""x = a.to_s + "-" + b.to_s + "-end"""")
    }

    fun `test no-op on plain string`() {
        val source = """x = "hello<caret>""""
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test no-op on single-quoted string`() {
        val source = """x = 'hello<caret>'"""
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test join concat to interpolation`() {
        myFixture.configureByText("a.rb", """x = "hi " + name.to_s<caret> + "!"""")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""x = "hi #{name}!"""")
    }

    fun `test join without to_s`() {
        myFixture.configureByText("a.rb", """x = "hi " + name<caret> + "!"""")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""x = "hi #{name}!"""")
    }

    fun `test join leading expression`() {
        myFixture.configureByText("a.rb", """x = name.to_s<caret> + "!"""")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""x = "#{name}!"""")
    }

    fun `test join trailing expression`() {
        myFixture.configureByText("a.rb", """x = "hi " + name<caret>""")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""x = "hi #{name}"""")
    }

    fun `test round trip split then join`() {
        myFixture.configureByText("a.rb", """x = "hi #{name<caret>}!"""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""x = "hi #{name}!"""")
    }

    fun `test no-op when concat has non-string non-identifier mix`() {
        val source = """x = a<caret> + b + c"""
        myFixture.configureByText("a.rb", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
