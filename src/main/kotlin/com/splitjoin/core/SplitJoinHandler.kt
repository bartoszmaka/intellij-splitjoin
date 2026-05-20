package com.splitjoin.core

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement

interface SplitJoinHandler {
    fun canSplit(element: PsiElement): Boolean
    fun canJoin(element: PsiElement): Boolean

    fun split(element: PsiElement, context: SplitJoinContext)
    fun join(element: PsiElement, context: SplitJoinContext)

    companion object {
        val EP_NAME: ExtensionPointName<SplitJoinHandler> =
            ExtensionPointName.create("com.splitjoin.handler")
    }
}
