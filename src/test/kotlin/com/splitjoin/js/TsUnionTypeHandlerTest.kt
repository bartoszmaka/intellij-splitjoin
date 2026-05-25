package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TsUnionTypeHandlerTest : BasePlatformTestCase() {

    fun `test split union`() {
        myFixture.configureByText("a.ts", "type T = A<caret> | B | C")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains('|')) { "Split must work" }
    }

    fun `test split intersection`() {
        myFixture.configureByText("a.ts", "type T = A<caret> & B & C")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains('&')) { "Split must work" }
    }

    fun `test join union`() {
        myFixture.configureByText(
            "a.ts",
            "type T =\n    | A\n    | B\n    | C"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        val result = myFixture.editor.document.text
        assert(result.contains('|') && !result.endsWith('\n')) { "Join must work" }
    }

    fun `test join intersection`() {
        myFixture.configureByText(
            "a.ts",
            "type T =\n    & A\n    & B\n    & C"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        val result = myFixture.editor.document.text
        assert(result.contains('&') && !result.endsWith('\n')) { "Join must work" }
    }

    fun `test no-op on single member union`() {
        val source = "type T = A<caret>"
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test no-op on JS file`() {
        val source = "const x = a<caret> | b"
        myFixture.configureByText("a.js", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }
}
