package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TsTupleTypeHandlerTest : BasePlatformTestCase() {

    fun `test split tuple`() {
        myFixture.configureByText("a.ts", "type T = [A<caret>, B, C]")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            "type T = [\n    A,\n    B,\n    C,\n]"
        )
    }

    fun `test split tuple with labels`() {
        myFixture.configureByText("a.ts", "type T = [a<caret>: A, b: B]")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            "type T = [\n    a: A,\n    b: B,\n]"
        )
    }

    fun `test split tuple with rest`() {
        myFixture.configureByText("a.ts", "type T = [A<caret>, ...B[]]")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            "type T = [\n    A,\n    ...B[],\n]"
        )
    }

    fun `test join tuple`() {
        myFixture.configureByText(
            "a.ts",
            "type T = [\n    A<caret>,\n    B,\n    C,\n]"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("type T = [A, B, C]")
    }

    fun `test round trip`() {
        myFixture.configureByText("a.ts", "type T = [A<caret>, B, C]")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("type T = [A, B, C]")
    }

    fun `test no-op single member`() {
        val source = "type T = [A<caret>]"
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }
}
