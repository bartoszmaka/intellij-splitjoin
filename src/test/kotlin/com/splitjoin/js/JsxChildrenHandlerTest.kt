package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsxChildrenHandlerTest : BasePlatformTestCase() {

    fun `test split text child`() {
        myFixture.configureByText("a.tsx", "const x = <p<caret>>hello</p>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const x = <p>\n    hello\n</p>")
    }

    fun `test split element children`() {
        myFixture.configureByText("a.tsx", "const x = <div<caret>><a/><b/></div>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const x = <div>\n    <a/>\n    <b/>\n</div>")
    }

    fun `test join element children`() {
        myFixture.configureByText("a.tsx", "const x = <div<caret>>\n    <a/>\n    <b/>\n</div>")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = <div><a/><b/></div>")
    }

    fun `test no-op on mixed text and element`() {
        val source = "const x = <p<caret>>hi <em>x</em>!</p>"
        myFixture.configureByText("a.tsx", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }
}
