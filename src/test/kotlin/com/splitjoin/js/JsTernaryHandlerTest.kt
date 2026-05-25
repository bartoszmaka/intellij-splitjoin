package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsTernaryHandlerTest : BasePlatformTestCase() {

    fun `test split const declaration becomes let`() {
        myFixture.configureByText("a.ts", "const x = a<caret> ? b : c")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("let x;\nif (a) { x = b; } else { x = c; }")
    }

    fun `test split let declaration preserves let`() {
        myFixture.configureByText("a.ts", "let x = a<caret> ? b : c")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("let x;\nif (a) { x = b; } else { x = c; }")
    }

    fun `test split assignment`() {
        myFixture.configureByText("a.ts", "x = a<caret> ? b : c")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("if (a) { x = b; } else { x = c; }")
    }

    fun `test split return`() {
        myFixture.configureByText("a.ts", "function f() { return a<caret> ? b : c }")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("function f() { if (a) { return b; } else { return c; } }")
    }

    fun `test join declaration form`() {
        myFixture.configureByText(
            "a.ts",
            "let x;\nif (a<caret>) { x = b; } else { x = c; }"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("let x = a ? b : c;")
    }

    fun `test join assignment form`() {
        myFixture.configureByText(
            "a.ts",
            "if (a<caret>) { x = b; } else { x = c; }"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("x = a ? b : c;")
    }

    fun `test join return form`() {
        myFixture.configureByText(
            "a.ts",
            "function f() { if (a<caret>) { return b; } else { return c; } }"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("function f() { return a ? b : c; }")
    }

    fun `test round trip assignment`() {
        myFixture.configureByText("a.ts", "x = a<caret> ? b : c")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("x = a ? b : c;")
    }

    fun `test no-op on ternary in non-eligible position`() {
        val source = "const x = f(a<caret> ? b : c)"
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }
}
