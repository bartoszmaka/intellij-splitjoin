package com.splitjoin.html

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class XmlClassAttributeHandlerTest : BasePlatformTestCase() {

    fun `test split class attribute three values`() {
        myFixture.configureByText("a.html", "<div class=\"a b c<caret>\"/>")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("<div class=\"a\n     b\n     c\"/>")
    }

    fun `test join multi-line class`() {
        myFixture.configureByText(
            "a.html",
            "<div class=\"a\n     b<caret>\n     c\"/>"
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("<div class=\"a b c\"/>")
    }

    fun `test single class value is not split into multiple lines`() {
        // H3 must bail on a single-value class. XmlAttributeHandler may still split the attribute list,
        // but the class value itself must stay on one line.
        myFixture.configureByText("a.html", "<div class=\"only<caret>\"></div>")
        myFixture.performEditorAction("Splitjoin.Split")
        val result = myFixture.editor.document.text
        assertFalse(
            "H3 must not multi-line a single-value class but got:\n$result",
            result.contains("class=\"only\n"),
        )
    }
}
