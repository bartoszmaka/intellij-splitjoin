package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsDestructuringHandlerTest : BasePlatformTestCase() {

    fun `test split object destructuring`() {
        myFixture.configureByText("a.ts", """const { a<caret>, b } = obj""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const a = obj.a;
            const b = obj.b;
            """.trimIndent()
        )
    }

    fun `test split array destructuring`() {
        myFixture.configureByText("a.ts", """const [a<caret>, b] = arr""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const a = arr[0];
            const b = arr[1];
            """.trimIndent()
        )
    }

    fun `test split with let`() {
        myFixture.configureByText("a.ts", """let { a<caret>, b } = obj""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            let a = obj.a;
            let b = obj.b;
            """.trimIndent()
        )
    }

    fun `test join object run`() {
        myFixture.configureByText(
            "a.ts",
            """
            const a = obj.a;
            const b = obj.b<caret>;
            const c = obj.c;
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const { a, b, c } = obj;""")
    }

    fun `test join array run with contiguous indices`() {
        myFixture.configureByText(
            "a.ts",
            """
            const a = arr[0]<caret>;
            const b = arr[1];
            const c = arr[2];
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const [a, b, c] = arr;""")
    }

    fun `test round trip object`() {
        val source = """const { a<caret>, b, c } = obj"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const { a, b, c } = obj;""")
    }

    fun `test no-op when rename present`() {
        val source = """const { a: aa<caret>, b } = obj"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test no-op when rest present`() {
        val source = """const { a<caret>, ...rest } = obj"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test no-op when source is expression`() {
        val source = """const { a<caret>, b } = getObj()"""
        myFixture.configureByText("a.ts", source)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(source)
    }

    fun `test J3 bails when sources differ - J7 merge fallback`() {
        // J3 bails (sources don't match), so J7 fires and merges declarators.
        myFixture.configureByText(
            "a.ts",
            """
            const a = obj1.a<caret>;
            const b = obj2.b;
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const a = obj1.a, b = obj2.b;""")
    }

    fun `test J3 bails when array indices skip - J7 merge fallback`() {
        // J3 bails (indices not contiguous), so J7 fires and merges declarators.
        myFixture.configureByText(
            "a.ts",
            """
            const a = arr[0]<caret>;
            const b = arr[2];
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const a = arr[0], b = arr[2];""")
    }
}
