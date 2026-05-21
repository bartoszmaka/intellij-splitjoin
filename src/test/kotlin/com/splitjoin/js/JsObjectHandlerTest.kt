package com.splitjoin.js

import com.intellij.lang.javascript.formatter.JSCodeStyleSettings
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings.TrailingCommaOption
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsObjectHandlerTest : BasePlatformTestCase() {

    private fun setTrailingComma(enabled: Boolean) {
        val settings = CodeStyleSettingsManager.getInstance(project).currentSettings
        val js = settings.getCustomSettings(JSCodeStyleSettings::class.java)
        js.ENFORCE_TRAILING_COMMA = if (enabled) TrailingCommaOption.WhenMultiline else TrailingCommaOption.Remove
    }

    fun `test split inline object`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = { a<caret>: 1, b: 2 };")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = {
                a: 1,
                b: 2,
            };
            """.trimIndent()
        )
    }

    fun `test split inline object trailing comma off`() {
        setTrailingComma(false)
        myFixture.configureByText("a.js", "const x = { a<caret>: 1, b: 2 };")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = {
                a: 1,
                b: 2
            };
            """.trimIndent()
        )
    }

    fun `test join multi-line object`() {
        myFixture.configureByText(
            "a.js",
            """
            const x = {
                a<caret>: 1,
                b: 2,
            };
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = { a: 1, b: 2 };")
    }

    fun `test join strips trailing comma`() {
        myFixture.configureByText(
            "a.js",
            """
            const x = {
                a<caret>: 1,
                b: 2,
            };
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = { a: 1, b: 2 };")
    }

    fun `test round-trip split then join`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = { a<caret>: 1, b: 2 };")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = { a: 1, b: 2 };")
    }

    fun `test split shorthand object`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = { a<caret>, b };")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = {
                a,
                b,
            };
            """.trimIndent()
        )
    }

    fun `test split with computed key`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = { [k<caret>]: v, b: 2 };")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = {
                [k]: v,
                b: 2,
            };
            """.trimIndent()
        )
    }

    fun `test split with spread`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = { a<caret>: 1, ...rest };")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = {
                a: 1,
                ...rest,
            };
            """.trimIndent()
        )
    }

    fun `test single property splits`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = { a<caret>: 1 };")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = {
                a: 1,
            };
            """.trimIndent()
        )
    }

    fun `test empty object is a no-op`() {
        myFixture.configureByText("a.js", "const x = <caret>{};")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test comment in object bails`() {
        myFixture.configureByText(
            "a.js",
            "const x = { a<caret>: 1 /* keep */, b: 2 };"
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test JSON file is handled by JSON handler, not JS handler`() {
        myFixture.configureByText("a.json", """{"a<caret>": 1, "b": 2}""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            {
              "a": 1,
              "b": 2
            }
            """.trimIndent()
        )
    }
}
