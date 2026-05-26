package com.splitjoin.html

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class XmlChildrenHandlerTest : BasePlatformTestCase() {

    fun `test split text child`() {
        myFixture.configureByText("a.html", "<p<caret>>hello</p>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("<p>\n    hello\n</p>")
    }

    fun `test split element children`() {
        myFixture.configureByText("a.html", "<ul<caret>><li/><li/></ul>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("<ul>\n    <li/>\n    <li/>\n</ul>")
    }

    fun `test join element children`() {
        myFixture.configureByText("a.html", "<ul<caret>>\n    <li/>\n    <li/>\n</ul>")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("<ul><li/><li/></ul>")
    }

    fun `test no-op on mixed text and element`() {
        val source = "<p<caret>>hi <b>x</b>!</p>"
        myFixture.configureByText("a.html", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }
}
