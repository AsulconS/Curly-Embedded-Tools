package com.asulcons.embedded.armv8.inspections

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.Suggestions
import com.asulcons.embedded.armv8.lexer.A64Registers
import com.asulcons.embedded.armv8.psi.ArmAsmDirective
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.psi.ArmAsmInstruction
import com.asulcons.embedded.armv8.psi.ArmAsmLabelDefinition
import com.asulcons.embedded.armv8.psi.ArmAsmLocalLabelReference
import com.asulcons.embedded.armv8.psi.ArmAsmRegisterOperand
import com.asulcons.embedded.armv8.spec.A64Spec
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
 * Replaces the text of a single leaf. Assembly names are plain tokens with no structure of their own,
 * so swapping the leaf's characters is both sufficient and the least disruptive edit available.
 */
internal class RenameLeafFix(
    private val replacement: String,
    private val familyKey: String,
) : LocalQuickFix {

    override fun getName(): String = EmbeddedBundle.message("quickfix.changeTo", replacement)

    override fun getFamilyName(): String = EmbeddedBundle.message(familyKey)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val leaf = descriptor.psiElement as? LeafElement ?: return
        leaf.replaceWithText(replacement)
    }
}

class ArmAsmUnknownMnemonicInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val instruction = element as? ArmAsmInstruction ?: return
                val mnemonicElement = instruction.mnemonicElement ?: return
                val mnemonic = mnemonicElement.text
                if (A64Spec.isKnownMnemonic(mnemonic)) return

                val file = instruction.containingFile as? ArmAsmFile ?: return
                // `name .req x9` is an alias declaration, not an instruction.
                if (instruction.operands.firstOrNull()?.text?.trim()?.lowercase() in ALIAS_DIRECTIVES) return
                if (mnemonic in file.macroNames) return
                if (mnemonic in file.preprocessorMacroNames) return

                val suggestions = Suggestions.closestMatches(mnemonic.lowercase(), A64Spec.ALL_MNEMONICS)
                holder.registerProblem(
                    mnemonicElement,
                    EmbeddedBundle.message("inspection.armAsm.unknownMnemonic.message", mnemonic),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    *suggestions.map { RenameLeafFix(it, "quickfix.family.mnemonic") }.toTypedArray(),
                )
            }
        }

    private companion object {
        val ALIAS_DIRECTIVES = setOf(".req", ".unreq")
    }
}

class ArmAsmUnknownDirectiveInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val directive = element as? ArmAsmDirective ?: return
                val nameElement = directive.directiveElement ?: return
                val name = nameElement.text
                if (A64Spec.isKnownDirective(name)) return

                val suggestions = Suggestions.closestMatches(name.lowercase(), A64Spec.DIRECTIVES)
                holder.registerProblem(
                    nameElement,
                    EmbeddedBundle.message("inspection.armAsm.unknownDirective.message", name),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    *suggestions.map { RenameLeafFix(it, "quickfix.family.directive") }.toTypedArray(),
                )
            }
        }
}

class ArmAsmInvalidRegisterInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val operand = element as? ArmAsmRegisterOperand ?: return
                val registerElement = operand.registerElement ?: return
                val name = registerElement.text
                if (A64Registers.isValidRegisterName(name)) return

                val base = name.lowercase().substringBefore('.').substringBefore('/')
                val fixes = when (base) {
                    // X31/W31 is the encoding shared by the stack pointer and the zero register.
                    "x31" -> listOf("sp", "xzr")
                    "w31" -> listOf("wsp", "wzr")
                    else -> emptyList()
                }
                holder.registerProblem(
                    registerElement,
                    EmbeddedBundle.message("inspection.armAsm.invalidRegister.message", name),
                    ProblemHighlightType.GENERIC_ERROR,
                    *fixes.map { RenameLeafFix(it, "quickfix.family.register") }.toTypedArray(),
                )
            }
        }
}

class ArmAsmDuplicateLabelInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val label = element as? ArmAsmLabelDefinition ?: return
                // Numeric labels are meant to be reused; that is the whole point of `1b`/`1f`.
                if (label.isNumeric) return
                val name = label.name ?: return
                val file = label.containingFile as? ArmAsmFile ?: return

                val definitions = file.symbolDefinitions[name].orEmpty()
                if (definitions.size < 2) return
                // Report on every definition except the first, so the original stays clean.
                if (definitions.firstOrNull() === label) return

                holder.registerProblem(
                    label.nameIdentifier ?: label,
                    EmbeddedBundle.message("inspection.armAsm.duplicateLabel.message", name),
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
}

class ArmAsmUnresolvedLocalLabelInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val reference = element as? ArmAsmLocalLabelReference ?: return
                if (reference.reference.resolve() != null) return
                val direction = if (reference.searchesBackwards) {
                    EmbeddedBundle.message("inspection.armAsm.unresolvedLocalLabel.backwards")
                } else {
                    EmbeddedBundle.message("inspection.armAsm.unresolvedLocalLabel.forwards")
                }
                holder.registerProblem(
                    reference,
                    EmbeddedBundle.message(
                        "inspection.armAsm.unresolvedLocalLabel.message",
                        reference.labelNumber,
                        direction,
                    ),
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
}
