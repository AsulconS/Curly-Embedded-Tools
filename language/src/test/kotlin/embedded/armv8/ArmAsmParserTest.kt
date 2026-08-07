package com.asulcons.embedded.armv8

import com.asulcons.embedded.EmbeddedParsingTestCase
import com.asulcons.embedded.armv8.parser.ArmAsmParserDefinition
import com.asulcons.embedded.armv8.psi.ArmAsmDirectiveArgument
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.psi.ArmAsmInstruction
import com.asulcons.embedded.armv8.psi.ArmAsmLabelDefinition
import com.asulcons.embedded.armv8.psi.ArmAsmShiftOperand
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class ArmAsmParserTest : EmbeddedParsingTestCase("", "s", ArmAsmParserDefinition()) {

    fun testRealBootstrapParsesCleanly() {
        val text = """
            /* Reset vector. */
            #include <asm/config.h>

                    .arch armv8-a
                    .section .text.boot, "ax", %progbits
                    .global _start
                    .type   _start, %function

            .macro  SAVE_PAIR r1, r2
                    stp     \r1, \r2, [sp, #-16]!
            .endm

            _start:
                    mrs     x0, mpidr_el1
                    and     x0, x0, #0xff
                    cbnz    x0, 1f
                    ldr     x1, =__stack_top
                    mov     sp, x1
                    adrp    x2, __bss_start
                    add     x2, x2, :lo12:__bss_start
                    SAVE_PAIR x29, x30
                    movz    w3, #0x1234, lsl #16
                    add     x4, x5, x6, lsl #3
                    ld1     {v0.16b, v1.16b}, [x2], #32
                    csel    x7, x8, x9, ne
                    bl      kernel_main
            1:      wfe
                    b       1b
        """.trimIndent()

        assertNoErrors(text)
    }

    fun testLabelAndInstructionOnTheSameLineAreSeparateElements() {
        val file = parse("done: ret\n")
        val labels = PsiTreeUtil.findChildrenOfType(file, ArmAsmLabelDefinition::class.java)
        val instructions = PsiTreeUtil.findChildrenOfType(file, ArmAsmInstruction::class.java)
        assertEquals(1, labels.size)
        assertEquals("done", labels.first().name)
        assertEquals(1, instructions.size)
        assertEquals("ret", instructions.first().mnemonic)
    }

    fun testAStatementNeverRunsPastItsLineBreak() {
        // Without the line-break boundary the parser would read `x1` as a fourth operand of `mov`.
        val file = parse("mov x0, x1\nret\n")
        val instructions = PsiTreeUtil.findChildrenOfType(file, ArmAsmInstruction::class.java).toList()
        assertEquals(2, instructions.size)
        assertEquals(2, instructions[0].operands.size)
        assertEquals(0, instructions[1].operands.size)
    }

    fun testMissingClosingBracketIsReported() {
        val errors = errorsIn("ldr x0, [x1\n")
        assertFalse("expected a syntax error for the unclosed bracket", errors.isEmpty())
    }

    fun testTrailingCommaIsReported() {
        val errors = errorsIn("add x0, x1,\n")
        assertFalse("expected a syntax error for the dangling comma", errors.isEmpty())
    }

    fun testDirectiveArgumentsAreNeverReportedAsSyntaxErrors() {
        // GNU directives have no shared grammar, so the parser must stay quiet about their arguments.
        assertNoErrors(
            """
                .section .text.startup,"ax",%progbits
                .cfi_escape 0x0f, 0x04, 0x8f, 0x00
                .irp reg, x0, x1, x2
                .word \reg
                .endr
                .p2align 3, , 7
            """.trimIndent(),
        )
    }

    fun testSemicolonSeparatedStatementsAreParsedIndependently() {
        val file = parse("nop ; nop ; ret\n")
        val instructions = PsiTreeUtil.findChildrenOfType(file, ArmAsmInstruction::class.java)
        assertEquals(3, instructions.size)
    }

    fun testNumericLocalLabelReferencesResolveInBothDirections() {
        val file = parse("1:\n  b 1b\n  b 2f\n2:\n  ret\n") as ArmAsmFile
        assertEquals(2, file.numericLabels.size)
    }

    fun testShiftOperandExposesItsAmount() {
        // Regression: the amount used to be folded from the whole operand, whose leading `lsl`
        // identifier marks the expression non-constant — so every shift reported a null amount and
        // the range inspections silently passed.
        val file = parse("movz x0, #0x1234, lsl #8\nadd x1, x2, x3, lsl #64\n")
        val shifts = PsiTreeUtil.findChildrenOfType(file, ArmAsmShiftOperand::class.java).toList()
        assertEquals(2, shifts.size)
        assertEquals("lsl", shifts[0].operatorName)
        assertEquals(8L, shifts[0].amount)
        assertEquals(64L, shifts[1].amount)
    }

    fun testShiftAmountIsNullWhenItIsNotAConstant() {
        val file = parse("add x0, x1, x2, lsl #SHIFT_BITS\n")
        val shift = PsiTreeUtil.findChildOfType(file, ArmAsmShiftOperand::class.java)
        assertNotNull(shift)
        assertNull(shift!!.amount)
    }

    fun testAMacroInvocationResolvesToItsDefinition() {
        val file = parse(".macro SAVE_PAIR r1, r2\n  stp \\r1, \\r2, [sp, #-16]!\n.endm\n_start:\n  SAVE_PAIR x29, x30\n")
        val call = PsiTreeUtil.findChildrenOfType(file, ArmAsmInstruction::class.java)
            .first { it.mnemonic == "SAVE_PAIR" }

        val target = call.reference?.resolve()
        assertNotNull("a macro invocation should resolve to its .macro definition", target)
        assertEquals("SAVE_PAIR", (target as ArmAsmDirectiveArgument).name)
    }

    fun testAPreprocessorMacroInvocationResolvesToItsDefine() {
        val file = parse("#define PUSH_PAIR(a, b) stp a, b, [sp, #-16]!\n_start:\n  PUSH_PAIR x29, x30\n")
        val call = PsiTreeUtil.findChildrenOfType(file, ArmAsmInstruction::class.java)
            .first { it.mnemonic == "PUSH_PAIR" }
        assertNotNull(call.reference?.resolve())
    }

    fun testOrdinaryInstructionsCarryNoReference() {
        // Every statement head would otherwise look navigable, which makes Ctrl+Click hesitate.
        val file = parse("_start:\n  mov x0, x1\n  ret\n")
        val instructions = PsiTreeUtil.findChildrenOfType(file, ArmAsmInstruction::class.java)
        assertTrue(instructions.all { it.reference == null })
    }

    fun testTheMacroReferenceCoversOnlyTheMnemonic() {
        val file = parse(".macro M\n  nop\n.endm\n_start:\n  M x0, x1\n")
        val call = PsiTreeUtil.findChildrenOfType(file, ArmAsmInstruction::class.java)
            .first { it.mnemonic == "M" }
        val reference = call.reference
        assertNotNull(reference)
        assertEquals("M", reference!!.rangeInElement.substring(call.text))
    }

    private fun parse(text: String): PsiFile {
        val file = createPsiFile("test", text)
        ensureParsed(file)
        return file
    }

    private fun errorsIn(text: String): List<PsiErrorElement> =
        PsiTreeUtil.findChildrenOfType(parse(text), PsiErrorElement::class.java).toList()

    private fun assertNoErrors(text: String) {
        val errors = errorsIn(text)
        assertTrue(
            "unexpected syntax errors: " + errors.joinToString { "'${it.text}' — ${it.errorDescription}" },
            errors.isEmpty(),
        )
    }
}
