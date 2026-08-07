package com.asulcons.embedded.armv8.completion

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.armv8.lexer.A64Registers
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.spec.A64Docs
import com.asulcons.embedded.armv8.spec.A64Spec
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

/**
 * Offers what can legally appear where the caret is.
 *
 * The lexer has already decided whether the caret sits at the head of a statement or inside its
 * operands, so the token type under the caret is enough to pick the right vocabulary — no re-parsing
 * of the surrounding line is needed.
 */
class ArmAsmCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), ArmAsmCompletionProvider())
    }
}

private class ArmAsmCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val file = position.containingFile as? ArmAsmFile ?: return
        val results = result.withPrefixMatcher(prefixAt(parameters))

        when (position.node?.elementType) {
            ArmAsmTokens.MNEMONIC -> completeStatementHead(file, results)
            ArmAsmTokens.DIRECTIVE -> completeDirectives(results)
            ArmAsmTokens.IDENTIFIER, ArmAsmTokens.REGISTER -> completeOperand(file, results)
            else -> Unit
        }
    }

    /**
     * The platform's default prefix stops at the first non-alphanumeric character, which would throw
     * away the leading `.` of a directive and the `_` of a mangled symbol.
     */
    private fun prefixAt(parameters: CompletionParameters): String {
        val position = parameters.position
        val offsetInElement = parameters.offset - position.textRange.startOffset
        if (offsetInElement <= 0) return ""
        return position.text
            .take(offsetInElement)
            .removeSuffix(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
    }

    private fun completeStatementHead(file: ArmAsmFile, result: CompletionResultSet) {
        for (macro in file.macroNames) {
            result.addElement(
                LookupElementBuilder.create(macro)
                    .withIcon(AllIcons.Nodes.Class)
                    .withTypeText(EmbeddedBundle.message("armAsm.completion.macro"), true)
                    .withBoldness(true),
            )
        }
        for (mnemonic in A64Spec.ALL_MNEMONICS) {
            result.addElement(mnemonicElement(mnemonic))
        }
        for (condition in A64Spec.CONDITION_CODES) {
            result.addElement(mnemonicElement("b.$condition"))
        }
        completeDirectives(result)
    }

    private fun completeDirectives(result: CompletionResultSet) {
        for (directive in A64Spec.DIRECTIVES) {
            result.addElement(
                LookupElementBuilder.create(directive)
                    .withIcon(AllIcons.Nodes.Annotationtype)
                    .withTypeText(EmbeddedBundle.message("armAsm.completion.directive"), true)
                    .withTailText(A64Docs.DIRECTIVES[directive]?.let { "  $it" }, true),
            )
        }
    }

    private fun completeOperand(file: ArmAsmFile, result: CompletionResultSet) {
        for (register in generalPurposeRegisters()) {
            result.addElement(registerElement(register))
        }
        for (register in A64Registers.NAMED) {
            result.addElement(registerElement(register))
        }
        for (condition in A64Spec.CONDITION_CODES) {
            result.addElement(keywordElement(condition, EmbeddedBundle.message("armAsm.completion.conditionCode")))
        }
        for (operator in A64Spec.SHIFT_OPERATORS + A64Spec.EXTEND_OPERATORS) {
            result.addElement(keywordElement(operator, EmbeddedBundle.message("armAsm.completion.shiftOperator")))
        }
        for (keyword in A64Spec.OPERAND_KEYWORDS) {
            result.addElement(keywordElement(keyword, EmbeddedBundle.message("armAsm.completion.operandKeyword")))
        }
        for (register in A64Spec.SYSTEM_REGISTERS) {
            result.addElement(
                LookupElementBuilder.create(register)
                    .withIcon(AllIcons.Nodes.Static)
                    .withTypeText(EmbeddedBundle.message("armAsm.completion.systemRegister"), true),
            )
        }
        for (specifier in A64Spec.RELOCATION_SPECIFIERS) {
            result.addElement(
                LookupElementBuilder.create(":$specifier:")
                    .withIcon(AllIcons.Nodes.Static)
                    .withTypeText(EmbeddedBundle.message("armAsm.completion.relocation"), true),
            )
        }
        for ((name, definitions) in file.symbolDefinitions) {
            val first = definitions.firstOrNull() ?: continue
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Method)
                    .withTypeText(first.containingFile?.name, true),
            )
        }
    }

    private fun mnemonicElement(mnemonic: String): LookupElement =
        LookupElementBuilder.create(mnemonic)
            .withIcon(AllIcons.Nodes.Method)
            .withTypeText(EmbeddedBundle.message("armAsm.completion.instruction"), true)
            .withTailText(A64Docs.MNEMONICS[mnemonic]?.let { "  $it" }, true)

    private fun registerElement(register: String): LookupElement =
        LookupElementBuilder.create(register)
            .withIcon(AllIcons.Nodes.Field)
            .withTypeText(EmbeddedBundle.message("armAsm.completion.register"), true)

    private fun keywordElement(keyword: String, type: String): LookupElement =
        LookupElementBuilder.create(keyword)
            .withIcon(AllIcons.Nodes.Static)
            .withTypeText(type, true)

    private fun generalPurposeRegisters(): List<String> = buildList {
        for (index in 0..30) {
            add("x$index")
            add("w$index")
        }
        for (index in 0..31) {
            add("v$index")
            add("d$index")
            add("s$index")
            add("q$index")
        }
    }
}
