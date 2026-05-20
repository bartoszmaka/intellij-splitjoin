package com.splitjoin.core

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class SplitJoinContext(
    val editor: Editor,
    val document: Document,
    val project: Project,
    val file: PsiFile,
) {
    private var startOffset: Int = Int.MAX_VALUE
    private var endOffset: Int = Int.MIN_VALUE

    fun replace(element: PsiElement, text: String): RangeMarker =
        replace(element.textRange, text)

    fun replace(range: TextRange, text: String): RangeMarker {
        val start = range.startOffset
        document.replaceString(range.startOffset, range.endOffset, text)
        val end = start + text.length
        if (start < startOffset) startOffset = start
        if (end > endOffset) endOffset = end
        return document.createRangeMarker(start, end)
    }

    fun affectedRange(): TextRange? =
        if (startOffset <= endOffset) TextRange(startOffset, endOffset) else null
}
