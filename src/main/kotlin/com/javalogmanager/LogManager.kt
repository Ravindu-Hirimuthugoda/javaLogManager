package com.javalogmanager

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

class LogManager: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        if (project == null) {
            println("Project is null")
            return
        }

        val editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)

        if (editor != null) {
            // Single file mode
            val document = editor.document
            val psiFile = PsiManager.getInstance(project).findFile(FileDocumentManager.getInstance().getFile(document)!!)
            if (psiFile != null) {
                updateLoggerInFile(project, psiFile)
            } else {
                println("PsiFile is null")
            }
        } else {
            val psiManager = PsiManager.getInstance(project)
            val fileType = FileTypeManager.getInstance().getFileTypeByExtension("java")

            val virtualFiles = FileTypeIndex.getFiles(fileType, GlobalSearchScope.projectScope(project))
            virtualFiles.forEach { virtualFile ->
                val psiFile = psiManager.findFile(virtualFile) ?: return@forEach
                updateLoggerInFile(project, psiFile)
            }
        }
    }

    private fun updateLoggerInFile(project: Project, psiFile: PsiFile) {
        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile) ?: return
        val content = document.text
        val lines = content.split("\n").toMutableList()

        val updatedLines = lines.mapIndexed { index, line ->
            val lineNumber = index + 1
            if (line.contains(Regex("\\blogger\\.(info|error|warn)\\("))) {
                line.replace(
                        Regex("\\[.*?\\.java:\\d+\\]"),
                        "[${getClassName(psiFile)}.java:$lineNumber]"
                ).ifEmpty {
                    line.replace(
                            Regex("\\blogger\\.(info|error|warn)\\((.*?)\\)"),
                            "logger.$1([${getClassName(psiFile)}.java:$lineNumber] $2)"
                    )
                }
            } else {
                line
            }
        }

        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(updatedLines.joinToString("\n"))
        }
    }

    private fun getClassName(psiFile: PsiFile): String {
        val fileName = psiFile.virtualFile.name
        return fileName.substringBefore(".java")
    }
}