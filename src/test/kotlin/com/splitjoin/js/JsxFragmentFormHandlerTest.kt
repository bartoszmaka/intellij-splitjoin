package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsxFragmentFormHandlerTest : BasePlatformTestCase() {

    fun `test split empty fragment to React Fragment`() {
        myFixture.configureByText("a.tsx", "const x = <<caret>></>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const x = <React.Fragment></React.Fragment>")
    }

    fun `test split fragment with text`() {
        myFixture.configureByText("a.tsx", "const x = <<caret>>hi</>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const x = <React.Fragment>hi</React.Fragment>")
    }

    fun `test join React Fragment to short form`() {
        myFixture.configureByText("a.tsx", "const x = <React<caret>.Fragment>hi</React.Fragment>")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = <>hi</>")
    }

    fun `test round trip`() {
        myFixture.configureByText("a.tsx", "const x = <<caret>></>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = <></>")
    }
}
