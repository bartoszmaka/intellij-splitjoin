package com.splitjoin.js

import com.intellij.lang.javascript.formatter.JSCodeStyleSettings
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings.TrailingCommaOption
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsArrayHandlerTest : BasePlatformTestCase() {

    private fun setTrailingComma(enabled: Boolean) {
        val settings = CodeStyleSettingsManager.getInstance(project).currentSettings
        val js = settings.getCustomSettings(JSCodeStyleSettings::class.java)
        js.ENFORCE_TRAILING_COMMA = if (enabled) TrailingCommaOption.WhenMultiline else TrailingCommaOption.Remove
    }

    fun `test split inline array`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = [1<caret>, 2, 3];")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = [
                1,
                2,
                3,
            ];
            """.trimIndent()
        )
    }

    fun `test split inline array trailing comma off`() {
        setTrailingComma(false)
        myFixture.configureByText("a.js", "const x = [1<caret>, 2, 3];")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = [
                1,
                2,
                3
            ];
            """.trimIndent()
        )
    }

    fun `test join multi-line array`() {
        myFixture.configureByText(
            "a.js",
            """
            const x = [
                1<caret>,
                2,
                3,
            ];
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = [1, 2, 3];")
    }

    fun `test round-trip`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = [1<caret>, 2, 3];")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("const x = [1, 2, 3];")
    }

    fun `test single element splits`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "const x = [1<caret>];")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            const x = [
                1,
            ];
            """.trimIndent()
        )
    }

    fun `test empty array is a no-op`() {
        myFixture.configureByText("a.js", "const x = <caret>[];")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test comment in array bails`() {
        myFixture.configureByText(
            "a.js",
            "const x = [1<caret>, /* keep */ 2];"
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }
}
