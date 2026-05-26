package com.splitjoin.html

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class XmlSelfCloseToggleHandlerTest : BasePlatformTestCase() {

    fun `test split xml self-close to open-empty`() {
        myFixture.configureByText("a.xml", "<foo<caret>/>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("<foo></foo>")
    }

    fun `test join xml open-empty to self-close`() {
        myFixture.configureByText("a.xml", "<foo<caret>></foo>")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("<foo/>")
    }

    fun `test no-op on HTML void element split`() {
        val source = "<br<caret>/>"
        myFixture.configureByText("a.html", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test HTML non-void self-close works`() {
        myFixture.configureByText("a.html", "<div<caret>/>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("<div></div>")
    }
}
