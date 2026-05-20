package com.splitjoin.core

import com.intellij.json.psi.JsonObject
import com.intellij.psi.PsiElement
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class SplitJoinDispatcherTest : BasePlatformTestCase() {

    private class StubJsonObjectHandler : SplitJoinHandler {
        var splitCalledOn: PsiElement? = null
        var joinCalledOn: PsiElement? = null
        override fun canSplit(element: PsiElement) = element is JsonObject
        override fun canJoin(element: PsiElement) = element is JsonObject
        override fun split(element: PsiElement, context: SplitJoinContext) { splitCalledOn = element }
        override fun join(element: PsiElement, context: SplitJoinContext) { joinCalledOn = element }
    }

    private fun installStub(): StubJsonObjectHandler {
        val stub = StubJsonObjectHandler()
        ExtensionTestUtil.maskExtensions(
            SplitJoinHandler.EP_NAME,
            listOf(stub),
            testRootDisposable
        )
        return stub
    }

    fun `test split finds JsonObject when caret is on a property`() {
        val stub = installStub()
        myFixture.configureByText("a.json", """{"a<caret>": 1}""")

        SplitJoinDispatcher.dispatch(project, myFixture.editor, Direction.SPLIT)

        assertNotNull("split should be invoked on the JsonObject", stub.splitCalledOn)
        assertTrue(stub.splitCalledOn is JsonObject)
    }

    fun `test caret just after closing brace still resolves to JsonObject`() {
        val stub = installStub()
        myFixture.configureByText("a.json", """{"a": 1}<caret>""")

        SplitJoinDispatcher.dispatch(project, myFixture.editor, Direction.SPLIT)

        assertNotNull("split should be invoked when caret is at end-of-token", stub.splitCalledOn)
    }

    fun `test no handler match leaves document untouched`() {
        ExtensionTestUtil.maskExtensions(SplitJoinHandler.EP_NAME, emptyList(), testRootDisposable)
        myFixture.configureByText("a.json", """{"a<caret>": 1}""")

        SplitJoinDispatcher.dispatch(project, myFixture.editor, Direction.SPLIT)

        assertEquals("""{"a": 1}""", myFixture.editor.document.text)
    }
}
