package com.asulcons.embedded.gdb.completion

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.asulcons.embedded.gdb.psi.GdbCommand
import com.asulcons.embedded.gdb.psi.GdbFile
import com.asulcons.embedded.gdb.spec.GdbCommands
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class GdbCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), GdbCompletionProvider())
    }
}

private class GdbCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val file = position.containingFile as? GdbFile ?: return
        val results = result.withPrefixMatcher(prefixAt(parameters))

        when (position.node?.elementType) {
            GdbTokens.COMMAND -> completeCommands(file, results)
            GdbTokens.DOLLAR_VARIABLE -> completeConvenienceVariables(results)
            GdbTokens.IDENTIFIER -> completeSubcommands(position.let {
                PsiTreeUtil.getParentOfType(it, GdbCommand::class.java)
            }, results)
            else -> Unit
        }
    }

    private fun prefixAt(parameters: CompletionParameters): String {
        val position = parameters.position
        val offsetInElement = parameters.offset - position.textRange.startOffset
        if (offsetInElement <= 0) return ""
        return position.text
            .take(offsetInElement)
            .removeSuffix(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
    }

    private fun completeCommands(file: GdbFile, result: CompletionResultSet) {
        for (name in file.definedCommands) {
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Method)
                    .withTypeText(EmbeddedBundle.message("gdb.completion.userCommand"), true)
                    .withBoldness(true),
            )
        }
        for ((name, description) in GdbCommands.COMMANDS) {
            if (name == "!") continue
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Function)
                    .withTypeText(EmbeddedBundle.message("gdb.completion.command"), true)
                    .withTailText("  $description", true),
            )
        }
    }

    private fun completeSubcommands(command: GdbCommand?, result: CompletionResultSet) {
        val head = command?.commandName ?: return
        for (subcommand in GdbCommands.subcommandsOf(head)) {
            result.addElement(
                LookupElementBuilder.create(subcommand)
                    .withIcon(AllIcons.Nodes.Static)
                    .withTypeText(head, true),
            )
        }
    }

    private fun completeConvenienceVariables(result: CompletionResultSet) {
        for (variable in GdbCommands.CONVENIENCE_VARIABLES) {
            result.addElement(
                LookupElementBuilder.create(variable)
                    .withIcon(AllIcons.Nodes.Field)
                    .withTypeText(EmbeddedBundle.message("gdb.completion.convenienceVariable"), true),
            )
        }
    }
}
