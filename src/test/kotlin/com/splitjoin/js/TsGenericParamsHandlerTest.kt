package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TsGenericParamsHandlerTest : BasePlatformTestCase() {

    fun `test split call-site generics`() {
        myFixture.configureByText("a.ts", "type X = Foo<A<caret>, B, C>")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains(',')) { "Split must work" }
    }

    fun `test split decl-site generics`() {
        myFixture.configureByText("a.ts", "function f<A<caret>, B, C>(x: A): void {}")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assert(result.contains('\n') && result.contains(',')) { "Split must work" }
    }

    fun `test join call-site`() {
        myFixture.configureByText("a.ts", "type X = Foo<A<caret>, B, C>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        val result = myFixture.editor.document.text
        assert(!result.contains('\n') && result.contains(',')) { "Round-trip must work" }
    }

    fun `test no-op single param`() {
        val source = "type X = Foo<A<caret>>"
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test no-op on JS file`() {
        val source = "const x = a<caret>"
        myFixture.configureByText("a.js", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }
}
