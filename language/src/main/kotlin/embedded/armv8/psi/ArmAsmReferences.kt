package com.asulcons.embedded.armv8.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

/**
 * Resolves a symbol operand to the label or directive that declares it, within the same file.
 *
 * The reference is deliberately *soft*: assembly routinely names symbols that live in C sources, in
 * other objects, or in the linker script, so a failure to resolve says nothing about correctness and
 * must not raise the platform's "cannot resolve" error.
 */
class ArmAsmSymbolReference(element: ArmAsmSymbol) :
    PsiReferenceBase<ArmAsmSymbol>(element, TextRange(0, element.textLength), true) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile as? ArmAsmFile ?: return null
        return file.symbolDefinitions[element.symbolName]?.firstOrNull()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = element.firstChild ?: return element
        identifier.replace(ArmAsmElementFactory.createSymbolIdentifier(element.project, newElementName))
        return element
    }
}

/**
 * Resolves a macro invocation to the `.macro` (or `#define`) that declares it.
 *
 * Only built when the name really is a macro in this file — every other statement head is a hardware
 * instruction with nothing to navigate to, and handing the platform a reference that can never
 * resolve just makes Ctrl+Click hesitate on all of them.
 */
class ArmAsmMacroReference private constructor(
    instruction: ArmAsmInstruction,
    rangeInInstruction: TextRange,
) : PsiReferenceBase<ArmAsmInstruction>(instruction, rangeInInstruction, true) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile as? ArmAsmFile ?: return null
        val name = element.mnemonic ?: return null
        return file.macroDefinition(name) ?: file.preprocessorDefinition(name)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val mnemonic = element.mnemonicElement ?: return element
        mnemonic.replace(ArmAsmElementFactory.createMnemonic(element.project, newElementName))
        return element
    }

    companion object {
        fun of(instruction: ArmAsmInstruction): ArmAsmMacroReference? {
            val mnemonicElement = instruction.mnemonicElement ?: return null
            val name = mnemonicElement.text
            val file = instruction.containingFile as? ArmAsmFile ?: return null
            if (name !in file.macroNames && name !in file.preprocessorMacroNames) return null

            val start = mnemonicElement.startOffsetInParent
            return ArmAsmMacroReference(instruction, TextRange(start, start + mnemonicElement.textLength))
        }
    }
}

/**
 * Resolves `1b` / `2f` to the nearest numeric label in the requested direction, which is exactly how
 * GNU `as` scopes them.
 */
class ArmAsmLocalLabelPsiReference(element: ArmAsmLocalLabelReference) :
    PsiReferenceBase<ArmAsmLocalLabelReference>(element, TextRange(0, element.textLength), true) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile as? ArmAsmFile ?: return null
        val number = element.labelNumber
        val origin = element.textRange.startOffset
        val candidates = file.numericLabels.filter { it.name == number }
        return if (element.searchesBackwards) {
            candidates.lastOrNull { it.textRange.endOffset <= origin }
        } else {
            candidates.firstOrNull { it.textRange.startOffset >= origin }
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement = element
}
