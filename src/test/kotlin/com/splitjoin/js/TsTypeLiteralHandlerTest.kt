package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TsTypeLiteralHandlerTest : BasePlatformTestCase() {

    fun `test split type alias`() {
        myFixture.configureByText("a.ts", "type T = { a<caret>: number; b: string }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            "type T = {\n    a: number;\n    b: string;\n}"
        )
    }

    fun `test split interface body`() {
        myFixture.configureByText("a.ts", "interface I { a<caret>: number; b: string }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            "interface I {\n    a: number;\n    b: string;\n}"
        )
    }

    fun `test join type alias`() {
        myFixture.configureByText(
            "a.ts",
            "type T = {\n    a<caret>: number;\n    b: string;\n}"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("type T = { a: number; b: string }")
    }

    fun `test round trip`() {
        myFixture.configureByText("a.ts", "type T = { a<caret>: number; b: string }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("type T = { a: number; b: string }")
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
        // TsTypeLiteralHandler is TS-only, but JsObjectHandler will split this object.
        // Verify the TS handler stayed out by checking no `;` appeared in the result.
        assert(!result.contains(";")) { "TsTypeLiteralHandler must not fire on JS files. Got: $result" }
    }
}
