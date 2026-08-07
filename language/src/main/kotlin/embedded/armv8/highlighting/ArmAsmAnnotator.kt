package com.asulcons.embedded.armv8.highlighting

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.psi.ArmAsmInstruction
import com.asulcons.embedded.armv8.psi.ArmAsmShiftOperand
import com.asulcons.embedded.armv8.psi.ArmAsmSymbol
import com.asulcons.embedded.armv8.spec.A64Spec
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement

/**
 * Adds the highlighting that needs more than a token to decide, plus the two lexical errors the lexer
 * deliberately recovers from instead of failing on (unterminated string / block comment).
 */
class ArmAsmAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is ArmAsmSymbol -> annotateSymbol(element, holder)
            is ArmAsmShiftOperand -> colorize(holder, element.operatorElement, ArmAsmColors.SHIFT_OPERATOR)
            is ArmAsmInstruction -> annotateInstruction(element, holder)
            else -> annotateUnterminatedLiterals(element, holder)
        }
    }

    private fun annotateSymbol(symbol: ArmAsmSymbol, holder: AnnotationHolder) {
        val name = symbol.symbolName.lowercase()
        val key = when {
            name in A64Spec.CONDITION_CODES -> ArmAsmColors.CONDITION_CODE
            name in A64Spec.SHIFT_OPERATORS || name in A64Spec.EXTEND_OPERATORS -> ArmAsmColors.SHIFT_OPERATOR
            name in A64Spec.OPERAND_KEYWORDS -> ArmAsmColors.CONDITION_CODE
            A64Spec.isSystemRegister(name) -> ArmAsmColors.SYSTEM_REGISTER
            else -> return
        }
        colorize(holder, symbol, key)
    }

    /** A mnemonic that names a `.macro` in this file reads as a call, not as an instruction. */
    private fun annotateInstruction(instruction: ArmAsmInstruction, holder: AnnotationHolder) {
        val mnemonic = instruction.mnemonic ?: return
        val file = instruction.containingFile as? ArmAsmFile ?: return
        if (mnemonic in file.macroNames) {
            colorize(holder, instruction.mnemonicElement, ArmAsmColors.MACRO_CALL)
        }
    }

    private fun annotateUnterminatedLiterals(element: PsiElement, holder: AnnotationHolder) {
        when (element.node.elementType) {
            ArmAsmTokens.STRING -> if (!isTerminatedString(element.text)) {
                error(holder, element, EmbeddedBundle.message("armAsm.error.unterminatedString"))
            }
            ArmAsmTokens.BLOCK_COMMENT -> {
                val text = element.text
                if (text.length < 4 || !text.endsWith("*/")) {
                    error(holder, element, EmbeddedBundle.message("armAsm.error.unterminatedComment"))
                }
            }
        }
    }

    private fun isTerminatedString(text: String): Boolean {
        if (text.length < 2) return false
        var i = 1
        while (i < text.length) {
            when (text[i]) {
                '\\' -> i += 2
                '"' -> return i == text.length - 1
                else -> i++
            }
        }
        return false
    }

    private fun colorize(holder: AnnotationHolder, element: PsiElement?, key: TextAttributesKey) {
        if (element == null) return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(key)
            .create()
    }

    private fun error(holder: AnnotationHolder, element: PsiElement, message: String) {
        holder.newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
    }
}
