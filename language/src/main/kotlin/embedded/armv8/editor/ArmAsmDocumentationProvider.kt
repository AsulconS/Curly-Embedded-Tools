package com.asulcons.embedded.armv8.editor

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.psi.ArmAsmNamedElement
import com.asulcons.embedded.armv8.spec.A64Docs
import com.asulcons.embedded.armv8.spec.A64Spec
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Quick Documentation for the things that have no declaration to jump to — mnemonics, registers and
 * assembler directives — plus a short summary for labels and `.equ` symbols that do.
 */
class ArmAsmDocumentationProvider : AbstractDocumentationProvider() {

    /**
     * Instructions and registers are plain tokens, so nothing resolves to them. Pointing the platform
     * at the token under the caret is what lets Ctrl+Q work on `adrp` or `x29` at all.
     */
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? {
        if (file !is ArmAsmFile || contextElement == null) return null
        return when (contextElement.node?.elementType) {
            ArmAsmTokens.MNEMONIC, ArmAsmTokens.DIRECTIVE, ArmAsmTokens.REGISTER, ArmAsmTokens.IDENTIFIER,
            -> contextElement
            else -> null
        }
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null
        (element as? ArmAsmNamedElement)?.let { return describeDeclaration(it) }
        return when (element.node?.elementType) {
            ArmAsmTokens.MNEMONIC -> describeMnemonic(element)
            ArmAsmTokens.DIRECTIVE -> describeDirective(element.text)
            ArmAsmTokens.REGISTER -> describeRegister(element.text)
            ArmAsmTokens.IDENTIFIER -> describeOperandKeyword(element.text)
            else -> null
        }
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? =
        (element as? ArmAsmNamedElement)?.let { "${it.symbolName} — ${kindOf(it)}" }

    private fun describeMnemonic(element: PsiElement): String? {
        val mnemonic = element.text
        val file = element.containingFile as? ArmAsmFile
        if (file != null && mnemonic in file.macroNames) {
            val definition = file.directives(".macro").firstOrNull { it.arguments.firstOrNull()?.name == mnemonic }
            val signature = definition?.text?.lineSequence()?.first()?.trim() ?: ".macro $mnemonic"
            return section(mnemonic, EmbeddedBundle.message("armAsm.doc.macro"), "<code>$signature</code>")
        }

        val normalized = mnemonic.lowercase()
        val description = A64Docs.MNEMONICS[normalized]
            ?: A64Docs.MNEMONICS[normalized.substringBefore('.')]
            ?: A64Docs.MNEMONICS[normalized.removeSuffix("2")]
        val suffix = normalized.substringAfter('.', "")
        val conditionNote = if (suffix in A64Spec.CONDITION_CODES) {
            "<p>Executed only when the condition <code>$suffix</code> (${conditionMeaning(suffix)}) holds.</p>"
        } else {
            ""
        }
        if (description == null && conditionNote.isEmpty()) {
            return if (A64Spec.isKnownMnemonic(mnemonic)) {
                section(mnemonic, EmbeddedBundle.message("armAsm.doc.instruction"), "")
            } else {
                null
            }
        }
        return section(mnemonic, EmbeddedBundle.message("armAsm.doc.instruction"), (description ?: "") + conditionNote)
    }

    private fun describeDirective(name: String): String? {
        val description = A64Docs.DIRECTIVES[name.lowercase()] ?: return null
        return section(name, EmbeddedBundle.message("armAsm.doc.directive"), description)
    }

    private fun describeRegister(name: String): String? {
        val description = A64Docs.forRegister(name) ?: return null
        return section(name, EmbeddedBundle.message("armAsm.doc.register"), description)
    }

    private fun describeOperandKeyword(name: String): String? {
        val lower = name.lowercase()
        return when {
            lower in A64Spec.CONDITION_CODES ->
                section(name, EmbeddedBundle.message("armAsm.doc.conditionCode"), conditionMeaning(lower))
            lower in A64Spec.SHIFT_OPERATORS ->
                section(name, EmbeddedBundle.message("armAsm.doc.shiftOperator"), shiftMeaning(lower))
            lower in A64Spec.EXTEND_OPERATORS ->
                section(name, EmbeddedBundle.message("armAsm.doc.extendOperator"), extendMeaning(lower))
            A64Spec.isSystemRegister(lower) ->
                section(name, EmbeddedBundle.message("armAsm.doc.systemRegister"), "")
            else -> null
        }
    }

    private fun describeDeclaration(element: ArmAsmNamedElement): String =
        section(element.symbolName ?: "?", kindOf(element), "<code>${element.text.trim()}</code>")

    private fun kindOf(element: ArmAsmNamedElement): String =
        if (element is com.asulcons.embedded.armv8.psi.ArmAsmLabelDefinition) {
            EmbeddedBundle.message("armAsm.doc.label")
        } else {
            EmbeddedBundle.message("armAsm.doc.symbol")
        }

    private fun section(name: String, kind: String, body: String): String = buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append("<b>").append(name).append("</b> — ").append(kind)
        append(DocumentationMarkup.DEFINITION_END)
        if (body.isNotEmpty()) {
            append(DocumentationMarkup.CONTENT_START)
            append(body)
            append(DocumentationMarkup.CONTENT_END)
        }
    }

    private fun conditionMeaning(code: String): String = when (code) {
        "eq" -> "equal (Z == 1)"
        "ne" -> "not equal (Z == 0)"
        "cs", "hs" -> "carry set / unsigned higher or same (C == 1)"
        "cc", "lo" -> "carry clear / unsigned lower (C == 0)"
        "mi" -> "negative (N == 1)"
        "pl" -> "positive or zero (N == 0)"
        "vs" -> "signed overflow (V == 1)"
        "vc" -> "no signed overflow (V == 0)"
        "hi" -> "unsigned higher (C == 1 and Z == 0)"
        "ls" -> "unsigned lower or same"
        "ge" -> "signed greater than or equal (N == V)"
        "lt" -> "signed less than (N != V)"
        "gt" -> "signed greater than"
        "le" -> "signed less than or equal"
        "al" -> "always"
        "nv" -> "always (the `nv` encoding behaves like `al`)"
        else -> code
    }

    private fun shiftMeaning(operator: String): String = when (operator) {
        "lsl" -> "Logical shift left."
        "lsr" -> "Logical shift right, shifting in zeros."
        "asr" -> "Arithmetic shift right, shifting in the sign bit."
        "ror" -> "Rotate right."
        "msl" -> "Masked shift left: shifts in ones, used by some `movi` forms."
        else -> operator
    }

    private fun extendMeaning(operator: String): String {
        val signed = operator.startsWith("s")
        val width = when (operator.last()) {
            'b' -> "byte"
            'h' -> "halfword"
            'w' -> "word"
            else -> "doubleword"
        }
        val kind = if (signed) "Sign-extend" else "Zero-extend"
        return "$kind the low $width of the operand, then optionally shift it left."
    }
}
