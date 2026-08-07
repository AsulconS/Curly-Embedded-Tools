package com.asulcons.embedded.armv8.inspections

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.armv8.psi.ArmAsmDirective
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.psi.ArmAsmImmediateOperand
import com.asulcons.embedded.armv8.psi.ArmAsmInstruction
import com.asulcons.embedded.armv8.psi.ArmAsmRegisterOperand
import com.asulcons.embedded.armv8.psi.ArmAsmShiftOperand
import com.asulcons.embedded.armv8.spec.A64Spec
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Checks that `.macro`/`.endm`, `.if`/`.endif` and the repeat directives pair up.
 *
 * The whole file is walked at once because a region's two halves are arbitrarily far apart, and both
 * halves need reporting: an unmatched `.endm` and a `.macro` that never closes are different mistakes.
 */
class ArmAsmUnbalancedRegionInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                val assembly = file as? ArmAsmFile ?: return
                val open = ArrayDeque<ArmAsmDirective>()

                for (directive in PsiTreeUtil.findChildrenOfType(assembly, ArmAsmDirective::class.java)) {
                    val name = directive.directiveName ?: continue
                    val nameElement = directive.directiveElement ?: continue
                    when {
                        name in A64Spec.REGION_OPENERS -> open.addLast(directive)

                        name in A64Spec.REGION_CLOSERS -> {
                            val opener = open.removeLastOrNull()
                            if (opener == null) {
                                holder.registerProblem(
                                    nameElement,
                                    EmbeddedBundle.message("inspection.armAsm.unbalancedRegion.strayCloser", name),
                                    ProblemHighlightType.GENERIC_ERROR,
                                )
                                continue
                            }
                            val expected = A64Spec.REGION_OPENERS[opener.directiveName]
                            if (expected != null && expected != name) {
                                holder.registerProblem(
                                    nameElement,
                                    EmbeddedBundle.message(
                                        "inspection.armAsm.unbalancedRegion.mismatch",
                                        name,
                                        opener.directiveName.orEmpty(),
                                        expected,
                                    ),
                                    ProblemHighlightType.GENERIC_ERROR,
                                    RenameLeafFix(expected, "quickfix.family.directive"),
                                )
                            }
                        }

                        name in A64Spec.REGION_CONTINUATIONS -> {
                            val enclosing = open.lastOrNull()?.directiveName
                            if (enclosing == null || A64Spec.REGION_OPENERS[enclosing] != ".endif") {
                                holder.registerProblem(
                                    nameElement,
                                    EmbeddedBundle.message("inspection.armAsm.unbalancedRegion.strayElse", name),
                                    ProblemHighlightType.GENERIC_ERROR,
                                )
                            }
                        }
                    }
                }

                for (unclosed in open) {
                    val name = unclosed.directiveName ?: continue
                    holder.registerProblem(
                        unclosed.directiveElement ?: unclosed,
                        EmbeddedBundle.message(
                            "inspection.armAsm.unbalancedRegion.unclosed",
                            name,
                            A64Spec.REGION_OPENERS[name].orEmpty(),
                        ),
                        ProblemHighlightType.GENERIC_ERROR,
                    )
                }
            }
        }
}

/**
 * Range-checks the immediates whose encoding limits are fixed and easy to get wrong by hand:
 * the 12-bit `add`/`sub` field, the 16-bit `movz`/`movk` field and its `lsl` multiple-of-16 shift,
 * the bit index of `tbz`/`tbnz`, and shift amounts against the operand width.
 */
class ArmAsmImmediateRangeInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val instruction = element as? ArmAsmInstruction ?: return
                val mnemonic = instruction.mnemonic?.lowercase()?.substringBefore('.') ?: return
                val operands = instruction.operands

                when (mnemonic) {
                    in ADD_SUB_FAMILY -> checkAddSubImmediate(instruction)
                    in MOVE_WIDE_FAMILY -> checkMoveWideImmediate(instruction)
                    in TEST_BRANCH_FAMILY -> checkTestedBit(instruction)
                }
                if (operands.isNotEmpty()) checkShiftAmounts(instruction)
            }

            private fun checkAddSubImmediate(instruction: ArmAsmInstruction) {
                val operands = instruction.operands
                val shifted = operands.lastOrNull() is ArmAsmShiftOperand
                val index = if (shifted) operands.size - 2 else operands.size - 1
                val immediate = operands.getOrNull(index) as? ArmAsmImmediateOperand ?: return
                val value = immediate.constantValue ?: return
                // gas rewrites `add …, #-n` into `sub`, so only the magnitude is constrained.
                if (value in -4095..4095) return
                holder.registerProblem(
                    immediate,
                    EmbeddedBundle.message("inspection.armAsm.immediateRange.addSub", value),
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }

            private fun checkMoveWideImmediate(instruction: ArmAsmInstruction) {
                val operands = instruction.operands
                val immediate = operands.getOrNull(1) as? ArmAsmImmediateOperand ?: return
                val value = immediate.constantValue
                if (value != null && value !in 0..65535) {
                    holder.registerProblem(
                        immediate,
                        EmbeddedBundle.message("inspection.armAsm.immediateRange.moveWide", value),
                        ProblemHighlightType.GENERIC_ERROR,
                    )
                }

                val shift = operands.getOrNull(2) as? ArmAsmShiftOperand ?: return
                if (shift.operatorName != "lsl") return
                val amount = shift.amount ?: return
                if (amount % 16 == 0L && amount in 0..48) return
                holder.registerProblem(
                    shift,
                    EmbeddedBundle.message("inspection.armAsm.immediateRange.moveWideShift", amount),
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }

            private fun checkTestedBit(instruction: ArmAsmInstruction) {
                val operands = instruction.operands
                val immediate = operands.getOrNull(1) as? ArmAsmImmediateOperand ?: return
                val value = immediate.constantValue ?: return
                val maximum = operandWidth(instruction) - 1
                if (value in 0..maximum) return
                holder.registerProblem(
                    immediate,
                    EmbeddedBundle.message("inspection.armAsm.immediateRange.testedBit", value, maximum),
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }

            private fun checkShiftAmounts(instruction: ArmAsmInstruction) {
                val width = operandWidth(instruction)
                for (operand in instruction.operands) {
                    val shift = operand as? ArmAsmShiftOperand ?: continue
                    if (shift.operatorName !in SHIFT_OPERATORS_WITH_LIMIT) continue
                    val amount = shift.amount ?: continue
                    if (amount in 0 until width) continue
                    holder.registerProblem(
                        shift,
                        EmbeddedBundle.message(
                            "inspection.armAsm.immediateRange.shiftAmount",
                            amount,
                            width - 1,
                        ),
                        ProblemHighlightType.GENERIC_ERROR,
                    )
                }
            }

            /** 32 for a `w`-register destination, 64 otherwise — the default when nothing says. */
            private fun operandWidth(instruction: ArmAsmInstruction): Long {
                val register = instruction.operands
                    .filterIsInstance<ArmAsmRegisterOperand>()
                    .firstOrNull()
                    ?.registerName
                    ?.lowercase()
                    ?: return 64
                return if (register.startsWith("w") || register == "wsp" || register == "wzr") 32 else 64
            }
        }

    private companion object {
        val ADD_SUB_FAMILY = setOf("add", "adds", "sub", "subs", "cmp", "cmn")
        val MOVE_WIDE_FAMILY = setOf("movz", "movk", "movn")
        val TEST_BRANCH_FAMILY = setOf("tbz", "tbnz")
        val SHIFT_OPERATORS_WITH_LIMIT = setOf("lsl", "lsr", "asr", "ror")
    }
}
