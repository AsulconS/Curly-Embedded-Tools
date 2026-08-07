package com.asulcons.embedded.gdb.inspections

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.Suggestions
import com.asulcons.embedded.gdb.psi.GdbCommand
import com.asulcons.embedded.gdb.psi.GdbFile
import com.asulcons.embedded.gdb.spec.GdbCommands
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafElement

/**
 * Flags command words GDB would reject.
 *
 * A `.gdbinit` fails silently in the middle when a command is misspelled — the session just carries on
 * without the setting — which is exactly the kind of mistake worth catching before the debug run.
 */
class GdbUnknownCommandInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val command = element as? GdbCommand ?: return
                val nameElement = command.commandElement ?: return
                val name = command.commandName ?: return
                if (GdbCommands.isKnownCommand(name)) return

                val file = command.containingFile as? GdbFile
                if (file != null && name in file.definedCommands) return

                val suggestions = Suggestions.closestMatches(name, GdbCommands.COMMANDS.keys)
                holder.registerProblem(
                    nameElement,
                    EmbeddedBundle.message("inspection.gdb.unknownCommand.message", name),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    *suggestions.map { ReplaceCommandFix(it) }.toTypedArray(),
                )
            }
        }
}

private class ReplaceCommandFix(private val replacement: String) : LocalQuickFix {

    override fun getName(): String = EmbeddedBundle.message("quickfix.changeTo", replacement)

    override fun getFamilyName(): String = EmbeddedBundle.message("quickfix.family.gdbCommand")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val leaf = descriptor.psiElement as? LeafElement ?: return
        // Keep any `/format` suffix the user typed: only the command word was wrong.
        val format = leaf.text.substringAfter('/', "")
        leaf.replaceWithText(if (format.isEmpty()) replacement else "$replacement/$format")
    }
}
