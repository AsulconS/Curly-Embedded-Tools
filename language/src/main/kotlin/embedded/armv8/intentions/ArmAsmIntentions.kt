package com.asulcons.embedded.armv8.intentions

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.psi.ArmAsmDirective
import com.asulcons.embedded.armv8.psi.ArmAsmElementFactory
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.psi.ArmAsmLabelDefinition
import com.asulcons.embedded.armv8.psi.ArmAsmNumbers
import com.asulcons.embedded.armv8.psi.ArmAsmStatement
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Rewrites the literal under the caret in the next base round the cycle
 * (decimal → hexadecimal → binary → decimal).
 *
 * Bare-metal code reads register masks in hex and field widths in decimal, and the conversion is
 * exactly the kind of arithmetic that is easy to get wrong by hand.
 */
class ConvertNumberBaseIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = EmbeddedBundle.message("intention.armAsm.convertNumberBase.family")

    override fun getText(): String = currentText

    private var currentText: String = EmbeddedBundle.message("intention.armAsm.convertNumberBase.family")

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val literal = numberAt(element) ?: return false
        val value = ArmAsmNumbers.parse(literal.text) ?: return false
        val target = nextBase(ArmAsmNumbers.baseOf(literal.text))
        currentText = EmbeddedBundle.message(
            "intention.armAsm.convertNumberBase.text",
            ArmAsmNumbers.format(value, target),
        )
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val literal = numberAt(element) as? LeafElement ?: return
        val value = ArmAsmNumbers.parse(literal.text) ?: return
        literal.replaceWithText(ArmAsmNumbers.format(value, nextBase(ArmAsmNumbers.baseOf(literal.text))))
    }

    override fun startInWriteAction(): Boolean = true

    private fun numberAt(element: PsiElement): PsiElement? = when {
        element.node?.elementType === ArmAsmTokens.NUMBER -> element
        element.prevSibling?.node?.elementType === ArmAsmTokens.NUMBER -> element.prevSibling
        else -> null
    }

    /** Octal is skipped in the cycle: writing an assembly constant in octal is almost never intended. */
    private fun nextBase(base: ArmAsmNumbers.Base): ArmAsmNumbers.Base = when (base) {
        ArmAsmNumbers.Base.DECIMAL -> ArmAsmNumbers.Base.HEXADECIMAL
        ArmAsmNumbers.Base.HEXADECIMAL -> ArmAsmNumbers.Base.BINARY
        else -> ArmAsmNumbers.Base.DECIMAL
    }
}

/**
 * Adds `.global <label>` immediately above a label definition that does not have one, which is what
 * turns a local routine into something the linker (and a `.ld` script's `ENTRY`) can name.
 */
class ExportLabelIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = EmbeddedBundle.message("intention.armAsm.exportLabel.family")

    override fun getText(): String = currentText

    private var currentText: String = EmbeddedBundle.message("intention.armAsm.exportLabel.family")

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val label = labelAt(element) ?: return false
        val name = label.name ?: return false
        if (label.isNumeric || label.isAssemblerLocal) return false
        if (isAlreadyExported(label, name)) return false
        currentText = EmbeddedBundle.message("intention.armAsm.exportLabel.text", name)
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val label = labelAt(element) ?: return
        val name = label.name ?: return
        val statement = PsiTreeUtil.getParentOfType(label, ArmAsmStatement::class.java) ?: return
        val parent = statement.parent ?: return

        val declaration = ArmAsmElementFactory.createStatement(project, "\t.global $name")
        val separator = ArmAsmElementFactory.createFile(project, "\n").firstChild
        parent.addBefore(declaration, statement)
        if (separator != null) parent.addBefore(separator, statement)
    }

    override fun startInWriteAction(): Boolean = true

    private fun labelAt(element: PsiElement): ArmAsmLabelDefinition? = when {
        element.node?.elementType === ArmAsmTokens.LABEL ->
            element.parent as? ArmAsmLabelDefinition
        else -> PsiTreeUtil.getParentOfType(element, ArmAsmLabelDefinition::class.java)
    }

    private fun isAlreadyExported(label: ArmAsmLabelDefinition, name: String): Boolean {
        val file = label.containingFile as? ArmAsmFile ?: return true
        return PsiTreeUtil.findChildrenOfType(file, ArmAsmDirective::class.java).any { directive ->
            directive.directiveName in EXPORT_DIRECTIVES &&
                directive.arguments.any { it.text.trim() == name }
        }
    }

    private companion object {
        val EXPORT_DIRECTIVES = setOf(".global", ".globl", ".weak")
    }
}
