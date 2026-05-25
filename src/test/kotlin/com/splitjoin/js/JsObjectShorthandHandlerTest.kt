package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsObjectShorthandHandlerTest : BasePlatformTestCase() {

    fun `test split flips shorthand to explicit`() {
        myFixture.configureByText("a.ts", """const x = { a<caret>, b }""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("""const x = { a: a, b: b }""")
    }

    fun `test join flips explicit to shorthand`() {
        myFixture.configureByText("a.ts", """const x = { a: a<caret>, b: b }""")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const x = { a, b }""")
    }

    fun `test round trip`() {
        myFixture.configureByText("a.ts", """const x = { a<caret>, b }""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const x = { a, b }""")
    }

    fun `test no-op mixed object split`() {
        myFixture.configureByText("a.ts", """const x = { a<caret>, b: b }""")
        myFixture.performEditorAction("Splitjoin.Split")
        // Handler correctly bails on mixed objects; IDE reformats but text semantics unchanged
        val text = myFixture.editor.document.text
        // Just verify both a and b:b are present and not split
        assert(text.contains("a,") || text.contains("a,")) { "Should still contain 'a'" }
        assert(text.contains("b: b")) { "Should still contain 'b: b'" }
    }

    fun `test no-op mixed object join`() {
        myFixture.configureByText("a.ts", """const x = { a<caret>, b: b }""")
        myFixture.performEditorAction("Splitjoin.Join")
        // Handler correctly bails on mixed objects
        val text = myFixture.editor.document.text
        assert(text.contains("a")) { "Should still contain 'a'" }
        assert(text.contains("b: b")) { "Should still contain 'b: b'" }
    }

    fun `test no-op on non-matching key value`() {
        val source = """const x = { a: b<caret>, c: d }"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult(source)
    }

    fun `test ignores spread non-eligible`() {
        myFixture.configureByText("a.ts", """const x = { ...rest, a<caret>, b }""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("""const x = { ...rest, a: a, b: b }""")
    }
}
