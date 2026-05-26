package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsxAttributeHandlerTest : BasePlatformTestCase() {

    fun `test split inline JSX with string attrs`() {
        myFixture.configureByText(
            "a.jsx",
            """const e = <div className<caret>="x" id="y">hi</div>;"""
        )
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """const e = <div
    className="x"
    id="y"
>hi</div>;"""
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun `test split self closing JSX`() {
        myFixture.configureByText(
            "a.jsx",
            """const e = <img src<caret>="x" alt="y"/>;"""
        )
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """const e = <img
    src="x"
    alt="y"
/>;"""
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun `test split with expression and spread attrs`() {
        myFixture.configureByText(
            "a.jsx",
            """const e = <Comp a<caret>={1} b="x" {...rest}/>;"""
        )
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """const e = <Comp
    a={1}
    b="x"
    {...rest}
/>;"""
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun `test join multi-line JSX`() {
        myFixture.configureByText(
            "a.jsx",
            """const e = <div
    className<caret>="x"
    id="y"
>hi</div>;"""
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const e = <div className="x" id="y">hi</div>;""")
    }

    fun `test join multi-line self closing JSX`() {
        myFixture.configureByText(
            "a.jsx",
            """const e = <img
    src<caret>="x"
    alt="y"
/>;"""
        )
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const e = <img src="x" alt="y"/>;""")
    }

    fun `test round-trip JSX`() {
        myFixture.configureByText(
            "a.jsx",
            """const e = <div className<caret>="x" id="y">hi</div>;"""
        )
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        myFixture.checkResult("""const e = <div className="x" id="y">hi</div>;""")
    }

    fun `test JSX with no attributes is a no-op`() {
        // Empty body so M11/X2 (children) and M11/X1 (self-close toggle) don't preempt.
        myFixture.configureByText(
            "a.jsx",
            """const e = <div<caret>></div>;"""
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test TSX file`() {
        myFixture.configureByText(
            "a.tsx",
            """const e = <Comp a<caret>={1} b="x"/>;"""
        )
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """const e = <Comp
    a={1}
    b="x"
/>;"""
        assertEquals(expected, myFixture.editor.document.text)
    }

    fun `test JSX with block comment between attributes bails`() {
        // The IDE parses `/* */` between attributes as a PsiComment child of the JSX element.
        // The handler must bail so the comment isn't dropped during a split.
        myFixture.configureByText(
            "a.jsx",
            """const e = <div className<caret>="x" /*c*/ id="y">hi</div>;"""
        )
        val before = myFixture.editor.document.text
        myFixture.performEditorAction("Splitjoin.Split")
        assertEquals(before, myFixture.editor.document.text)
    }

    fun `test JSX with child expression comment splits normally`() {
        // Comments inside element children (e.g., `{/* */}` in a JSX child) don't intersect the
        // opening-tag region the handler rewrites, so a split is allowed and the comment is
        // preserved verbatim.
        myFixture.configureByText(
            "a.jsx",
            """const e = <div className<caret>="x" id="y">{/* hello */}</div>;"""
        )
        myFixture.performEditorAction("Splitjoin.Split")
        val expected = """const e = <div
    className="x"
    id="y"
>{/* hello */}</div>;"""
        assertEquals(expected, myFixture.editor.document.text)
    }
}
