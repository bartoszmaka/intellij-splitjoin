package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TsTupleTypeHandlerTest : BasePlatformTestCase() {

    fun `test split tuple`() {
        myFixture.configureByText("a.ts", "type T = [A<caret>, B, C]")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains(',')) { "Split must work" }
    }

    fun `test split tuple with labels`() {
        myFixture.configureByText("a.ts", "type T = [a<caret>: A, b: B]")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains(':')) { "Split must work" }
    }

    fun `test split tuple with rest`() {
        myFixture.configureByText("a.ts", "type T = [A<caret>, ...B[]]")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains("...")) { "Split must work" }
    }

    fun `test join tuple`() {
        myFixture.configureByText("a.ts", "type T = [A<caret>, B, C]")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        val result = myFixture.editor.document.text
        assert(!result.contains('\n')) { "Round-trip must work" }
    }

    fun `test no-op single member`() {
        val source = "type T = [A<caret>]"
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }
}
