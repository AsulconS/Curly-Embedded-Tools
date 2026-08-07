package com.asulcons.embedded.armv8.highlighting

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.EmbeddedIcons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class ArmAsmColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon = EmbeddedIcons.ArmAsmFile

    override fun getHighlighter(): SyntaxHighlighter = ArmAsmSyntaxHighlighter()

    override fun getDisplayName(): String = "ARMv8 Assembly"

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "cond" to ArmAsmColors.CONDITION_CODE,
        "shift" to ArmAsmColors.SHIFT_OPERATOR,
        "sysreg" to ArmAsmColors.SYSTEM_REGISTER,
        "macro" to ArmAsmColors.MACRO_CALL,
    )

    override fun getDemoText(): String = """
        /*
         * Vector table entry for a bare-metal AArch64 target.
         */
        #include <asm/config.h>

                .arch armv8-a
                .section .text.boot, "ax", %progbits
                .global _start
                .type   _start, %function

        .macro  SAVE_PAIR, r1, r2
                stp     \r1, \r2, [sp, #-16]!
        .endm

        _start:
                mrs     x0, <sysreg>mpidr_el1</sysreg>
                and     x0, x0, #0xff           // affinity level 0
                cbnz    x0, 1f
                ldr     x1, =__stack_top
                mov     sp, x1
                adrp    x2, bss_start
                add     x2, x2, :lo12:bss_start
                <macro>SAVE_PAIR</macro> x29, x30
                movz    w3, #0x1234, lsl #16
                add     x4, x5, x6, <shift>lsl</shift> #3
                ld1     { v0.16b, v1.16b }, [x2], #32
                csel    x7, x8, x9, <cond>ne</cond>
                bl      kernel_main
        1:      wfe
                b       1b
    """.trimIndent()

    private companion object {
        val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.mnemonic"), ArmAsmColors.MNEMONIC),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.macroCall"), ArmAsmColors.MACRO_CALL),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.directive"), ArmAsmColors.DIRECTIVE),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.preprocessor"), ArmAsmColors.PREPROCESSOR),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.label"), ArmAsmColors.LABEL),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.localLabelReference"), ArmAsmColors.LOCAL_LABEL_REFERENCE),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.register"), ArmAsmColors.REGISTER),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.systemRegister"), ArmAsmColors.SYSTEM_REGISTER),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.conditionCode"), ArmAsmColors.CONDITION_CODE),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.shiftOperator"), ArmAsmColors.SHIFT_OPERATOR),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.relocation"), ArmAsmColors.RELOCATION),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.macroParameter"), ArmAsmColors.MACRO_PARAMETER),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.identifier"), ArmAsmColors.IDENTIFIER),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.number"), ArmAsmColors.NUMBER),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.string"), ArmAsmColors.STRING),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.lineComment"), ArmAsmColors.LINE_COMMENT),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.blockComment"), ArmAsmColors.BLOCK_COMMENT),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.operator"), ArmAsmColors.OPERATOR),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.comma"), ArmAsmColors.COMMA),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.brackets"), ArmAsmColors.BRACKETS),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.braces"), ArmAsmColors.BRACES),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.parentheses"), ArmAsmColors.PARENTHESES),
            AttributesDescriptor(EmbeddedBundle.message("armAsm.color.badCharacter"), ArmAsmColors.BAD_CHARACTER),
        )
    }
}
