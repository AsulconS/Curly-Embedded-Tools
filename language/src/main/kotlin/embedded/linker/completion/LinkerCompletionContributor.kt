package com.asulcons.embedded.linker.completion

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.asulcons.embedded.linker.psi.LinkerScriptFile
import com.asulcons.embedded.linker.psi.LinkerSectionBody
import com.asulcons.embedded.linker.psi.LinkerSectionsBlock
import com.asulcons.embedded.linker.spec.LinkerKeywords
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionUtilCore
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

class LinkerCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), LinkerCompletionProvider())
    }
}

private class LinkerCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val file = position.containingFile as? LinkerScriptFile ?: return
        val results = result.withPrefixMatcher(prefixAt(parameters))

        if (afterRegionArrow(position)) {
            completeRegions(file, results)
            return
        }

        when {
            PsiTreeUtil.getParentOfType(position, LinkerSectionBody::class.java) != null ->
                completeKeywords(results, LinkerKeywords.SECTION_KEYWORDS + LinkerKeywords.FUNCTIONS)
            PsiTreeUtil.getParentOfType(position, LinkerSectionsBlock::class.java) != null ->
                completeKeywords(results, LinkerKeywords.SECTION_KEYWORDS + LinkerKeywords.FUNCTIONS)
            else ->
                completeKeywords(results, LinkerKeywords.TOP_LEVEL_COMMANDS + LinkerKeywords.FUNCTIONS)
        }
        completeRegions(file, results)
    }

    /** After `>` or `AT>` the only sensible completions are the regions declared in `MEMORY`. */
    private fun afterRegionArrow(position: PsiElement): Boolean {
        var previous = PsiTreeUtil.prevVisibleLeaf(position)
        if (previous?.node?.elementType === LinkerTokens.GT) return true
        // `AT>` lexes as two tokens; allow one hop back over the `AT`.
        previous = previous?.let { PsiTreeUtil.prevVisibleLeaf(it) }
        return previous?.node?.elementType === LinkerTokens.GT
    }

    private fun prefixAt(parameters: CompletionParameters): String {
        val position = parameters.position
        val offsetInElement = parameters.offset - position.textRange.startOffset
        if (offsetInElement <= 0) return ""
        return position.text
            .take(offsetInElement)
            .removeSuffix(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED)
    }

    private fun completeKeywords(result: CompletionResultSet, keywords: Map<String, String>) {
        for ((name, description) in keywords) {
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Static)
                    .withTypeText(EmbeddedBundle.message("linker.completion.keyword"), true)
                    .withTailText("  $description", true)
                    .withBoldness(true),
            )
        }
    }

    private fun completeRegions(file: LinkerScriptFile, result: CompletionResultSet) {
        for (region in file.memoryRegions) {
            val name = region.name ?: continue
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Package)
                    .withTypeText(EmbeddedBundle.message("linker.completion.region"), true),
            )
        }
    }
}
