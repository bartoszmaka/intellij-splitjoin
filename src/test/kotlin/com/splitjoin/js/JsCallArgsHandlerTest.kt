package com.splitjoin.js

import com.intellij.lang.javascript.formatter.JSCodeStyleSettings
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings.TrailingCommaOption
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsCallArgsHandlerTest : BasePlatformTestCase() {

    private fun setTrailingComma(enabled: Boolean) {
        val settings = CodeStyleSettingsManager.getInstance(project).currentSettings
        val js = settings.getCustomSettings(JSCodeStyleSettings::class.java)
        js.ENFORCE_TRAILING_COMMA = if (enabled) TrailingCommaOption.WhenMultiline else TrailingCommaOption.Remove
    }

    fun `test split inline call`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "foo(a<caret>, b, c);")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            foo(
                a,
                b,
                c,
            );
            """.trimIndent()
        )
    }

    fun `test split inline call trailing comma off`() {
        setTrailingComma(false)
        myFixture.configureByText("a.js", "foo(a<caret>, b, c);")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            foo(
                a,
                b,
                c
            );
            """.trimIndent()
        )
    }

    fun `test join multi-line call`() {
        myFixture.configureByText(
            "a.js",
            """
            foo(
                a<caret>,
                b,
                c,
            );
            """.trimIndent()
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("foo(a, b, c);")
    }

    fun `test round-trip`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "foo(a<caret>, b, c);")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("foo(a, b, c);")
    }

    fun `test single arg splits`() {
        setTrailingComma(true)
        myFixture.configureByText("a.js", "foo(a<caret>);")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(
            """
            foo(
                a,
            );
            """.trimIndent()
        )
    }

    fun `test empty call is a no-op`() {
        myFixture.configureByText("a.js", "foo<caret>();")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test comment in args bails`() {
        myFixture.configureByText(
            "a.js",
            "foo(a<caret>, /* keep */ b);"
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }
}
