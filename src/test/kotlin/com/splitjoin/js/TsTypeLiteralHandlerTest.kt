package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TsTypeLiteralHandlerTest : BasePlatformTestCase() {

    fun `test split type alias`() {
        myFixture.configureByText("a.ts", "type T = { a<caret>: number; b: string }")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains(';')) { "Split must work" }
    }

    fun `test split interface body`() {
        myFixture.configureByText("a.ts", "interface I { a<caret>: number; b: string }")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains(';')) { "Split must work" }
    }

    fun `test join type alias`() {
        myFixture.configureByText("a.ts", "type T = { a<caret>: number; b: string }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        val result = myFixture.editor.document.text
        assert(!result.contains('\n')) { "Round-trip must work" }
    }

    fun `test no-op single member`() {
        val source = "type T = { a<caret>: number }"
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test no-op on JS file`() {
        myFixture.configureByText("a.js", "const x = { a<caret>: 1, b: 2 }")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(!result.contains(";")) { "TsTypeLiteralHandler must not fire on JS files" }
    }
}
