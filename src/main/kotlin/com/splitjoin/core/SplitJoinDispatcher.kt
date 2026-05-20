package com.splitjoin.core

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager

enum class Direction { SPLIT, JOIN }

object SplitJoinDispatcher {

    fun dispatch(project: Project, editor: Editor, direction: Direction) {
        val document = editor.document
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return

        val match = runReadAction { findTarget(psiFile, editor, direction) }
        if (match == null) {
            showNoOp(project, direction)
            return
        }

        val (element, handler) = match
        val context = SplitJoinContext(editor, document, project, psiFile)
        val title = if (direction == Direction.SPLIT) "SplitJoin: Split" else "SplitJoin: Join"

        WriteCommandAction.runWriteCommandAction(project, title, "splitjoin", Runnable {
            when (direction) {
                Direction.SPLIT -> handler.split(element, context)
                Direction.JOIN -> handler.join(element, context)
            }
            val range = context.affectedRange() ?: return@Runnable
            PsiDocumentManager.getInstance(project).commitDocument(document)
            if (direction == Direction.SPLIT) {
                CodeStyleManager.getInstance(project).reformatRange(
                    psiFile, range.startOffset, range.endOffset
                )
            }
        })
    }

    internal fun findTarget(
        psiFile: PsiFile,
        editor: Editor,
        direction: Direction
    ): Pair<PsiElement, SplitJoinHandler>? {
        val offset = editor.caretModel.offset
        val leaf = psiFile.findElementAt(offset)
            ?: (offset - 1).takeIf { it >= 0 }?.let { psiFile.findElementAt(it) }
            ?: return null

        val handlers = SplitJoinHandler.EP_NAME.extensionList
        var node: PsiElement? = leaf
        while (node != null && node !is PsiFile) {
            for (handler in handlers) {
                val ok = when (direction) {
                    Direction.SPLIT -> handler.canSplit(node)
                    Direction.JOIN -> handler.canJoin(node)
                }
                if (ok) return node to handler
            }
            node = node.parent
        }
        return null
    }

    private fun showNoOp(project: Project, direction: Direction) {
        val msg = if (direction == Direction.SPLIT) "Splitjoin: nothing to split here"
        else "Splitjoin: nothing to join here"
        WindowManager.getInstance().getStatusBar(project)?.info = msg
    }
}
