package com.splitjoin.core

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

fun PsiElement.isOnSingleLine(): Boolean {
    val doc = containingFile?.viewProvider?.document ?: return false
    val range = textRange
    return doc.getLineNumber(range.startOffset) == doc.getLineNumber(range.endOffset)
}

fun PsiElement.containsComment(): Boolean =
    PsiTreeUtil.findChildOfType(this, PsiComment::class.java) != null
