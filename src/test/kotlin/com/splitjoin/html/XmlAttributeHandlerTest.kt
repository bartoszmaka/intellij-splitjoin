package com.splitjoin.html

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class XmlAttributeHandlerTest : BasePlatformTestCase() {

    fun `test split inline tag with multiple attrs`() {
        myFixture.configureByText("a.html", """<a href<caret>="x" class="y">link</a>""")
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """<a
        href="x"
        class="y"
>link</a>"""
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun `test split self closing tag`() {
        myFixture.configureByText("a.html", """<img src<caret>="x" alt="y"/>""")
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """<img
        src="x"
        alt="y"
/>"""
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun `test join multi-line tag`() {
        myFixture.configureByText(
            "a.html",
            """<a
        href<caret>="x"
        class="y"
>link</a>"""
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""<a href="x" class="y">link</a>""")
    }

    fun `test join multi-line self closing tag`() {
        myFixture.configureByText(
            "a.html",
            """<img
        src<caret>="x"
        alt="y"
/>"""
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""<img src="x" alt="y"/>""")
    }

    fun `test round-trip`() {
        myFixture.configureByText("a.html", """<a href<caret>="x" class="y">link</a>""")
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""<a href="x" class="y">link</a>""")
    }

    fun `test single attribute splits`() {
        myFixture.configureByText("a.html", """<a href<caret>="x">link</a>""")
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """<a
        href="x"
>link</a>"""
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun `test tag with no attributes is a no-op`() {
        // Empty body so M11/H1 (children) doesn't preempt the assertion.
        myFixture.configureByText("a.html", """<a<caret>></a>""")
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test XML file`() {
        myFixture.configureByText("a.xml", """<root attr<caret>="v" id="r">x</root>""")
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """<root
        attr="v"
        id="r"
>x</root>"""
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun `test comment in start tag bails`() {
        myFixture.configureByText(
            "a.html",
            """<a href<caret>="x" <!--note--> class="y">link</a>"""
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }
}
