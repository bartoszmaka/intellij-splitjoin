package com.splitjoin.js

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JsTemplateLiteralHandlerTest : BasePlatformTestCase() {

    fun `test split template with single interpolation`() {
        val src = "const x = " + "`" + "hi \${name<caret>}!" + "`"
        myFixture.configureByText("a.ts", src)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const x = 'hi ' + name + '!'")
    }

    fun `test split template with multiple interpolations`() {
        val src = "const x = " + "`" + "\${a<caret>}-\${b}-end" + "`"
        myFixture.configureByText("a.ts", src)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult("const x = a + '-' + b + '-end'")
    }

    fun `test join concat to template`() {
        myFixture.configureByText("a.ts", "const x = 'hi ' + name<caret> + '!'")
        myFixture.performEditorAction("Splitjoin.Join")
        val expected = "const x = " + "`" + "hi \${name}!" + "`"
        myFixture.checkResult(expected)
    }

    fun `test round trip`() {
        val src = "const x = " + "`" + "hi \${name<caret>}!" + "`"
        myFixture.configureByText("a.ts", src)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.performEditorAction("Splitjoin.Join")
        val expected = "const x = " + "`" + "hi \${name}!" + "`"
        myFixture.checkResult(expected)
    }

    fun `test no-op on plain template`() {
        val src = "const x = " + "`" + "plain text<caret>" + "`"
        myFixture.configureByText("a.ts", src)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(src)
    }

    fun `test no-op on tagged template`() {
        val src = "const x = tag" + "`" + "hi \${name<caret>}" + "`"
        myFixture.configureByText("a.ts", src)
        myFixture.performEditorAction("Splitjoin.Split")
        myFixture.checkResult(src)
    }
}
