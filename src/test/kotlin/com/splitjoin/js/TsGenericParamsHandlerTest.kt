package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TsGenericParamsHandlerTest : BasePlatformTestCase() {

    fun `test split call-site generics`() {
        myFixture.configureByText("a.ts", "type X = Foo<A<caret>, B, C>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            "type X = Foo<\n    A,\n    B,\n    C,\n>"
        )
    }

    fun `test split decl-site generics`() {
        myFixture.configureByText("a.ts", "function f<A<caret>, B, C>(x: A): void {}")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            "function f<\n    A,\n    B,\n    C,\n>(x: A): void {}"
        )
    }

    fun `test join call-site`() {
        myFixture.configureByText(
            "a.ts",
            "type X = Foo<\n    A<caret>,\n    B,\n    C,\n>"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("type X = Foo<A, B, C>")
    }

    fun `test round trip`() {
        myFixture.configureByText("a.ts", "type X = Foo<A<caret>, B, C>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("type X = Foo<A, B, C>")
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
