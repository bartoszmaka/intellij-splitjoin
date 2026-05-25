package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsxSelfCloseToggleHandlerTest : BasePlatformTestCase() {

    fun `test split self-close to open-empty`() {
        myFixture.configureByText("a.tsx", "const x = <div<caret>/>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const x = <div></div>")
    }

    fun `test join open-empty to self-close`() {
        myFixture.configureByText("a.tsx", "const x = <div<caret>></div>")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = <div/>")
    }

    fun `test no-op on non-empty body`() {
        val source = "const x = <div<caret>>hello</div>"
        myFixture.configureByText("a.tsx", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }
}
