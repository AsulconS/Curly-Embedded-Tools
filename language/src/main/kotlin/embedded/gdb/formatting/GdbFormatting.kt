package com.asulcons.embedded.gdb.formatting

import com.asulcons.embedded.gdb.GdbLanguage
import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.asulcons.embedded.gdb.psi.GdbElementTypes
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.psi.formatter.common.AbstractBlock

/**
 * Indents `define`/`if`/`while` bodies and normalises the line structure, and otherwise keeps its
 * hands off: GDB command arguments are free text, so re-spacing them would change what they mean.
 */
class GdbFormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val file = formattingContext.containingFile
        val root = GdbBlockFormatter(
            file.node,
            Indent.getNoneIndent(),
            settings.getCommonSettings(GdbLanguage).KEEP_BLANK_LINES_IN_CODE,
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(file, root, settings)
    }
}

private class GdbBlockFormatter(
    node: ASTNode,
    private val indent: Indent,
    private val keepBlankLines: Int,
) : AbstractBlock(node, null, null) {

    override fun getIndent(): Indent = indent

    override fun isLeaf(): Boolean = node.firstChildNode == null || isOpaque()

    /**
     * A `python` or `document` body is not GDB syntax; the formatter must not reindent a single line
     * of it, so the whole block is treated as one atom.
     */
    private fun isOpaque(): Boolean = when (node.elementType) {
        GdbElementTypes.COMMAND, GdbElementTypes.BLOCK_HEADER -> true
        GdbElementTypes.BLOCK -> node.findChildByType(GdbElementTypes.BLOCK_BODY)
            ?.findChildByType(GdbElementTypes.RAW_BODY) != null
        else -> false
    }

    override fun buildChildren(): List<Block> {
        if (isOpaque()) return emptyList()
        val blocks = ArrayList<Block>()
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType !== TokenType.WHITE_SPACE && child.textLength > 0) {
                blocks += GdbBlockFormatter(child, indentFor(child), keepBlankLines)
            }
            child = child.treeNext
        }
        return blocks
    }

    private fun indentFor(child: ASTNode): Indent = when (child.elementType) {
        GdbElementTypes.BLOCK_BODY -> Indent.getNormalIndent()
        else -> Indent.getNoneIndent()
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = when (node.elementType) {
        GdbElementTypes.FILE, GdbElementTypes.BLOCK_BODY, GdbElementTypes.BLOCK ->
            Spacing.createSpacing(0, 0, 1, true, keepBlankLines)
        else -> null
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes = when (node.elementType) {
        GdbElementTypes.BLOCK, GdbElementTypes.BLOCK_BODY -> ChildAttributes(Indent.getNormalIndent(), null)
        else -> ChildAttributes(Indent.getNoneIndent(), null)
    }
}

class GdbLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language = GdbLanguage

    override fun getConfigurableDisplayName(): String = "GDB Script"

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        indentOptions.INDENT_SIZE = 4
        indentOptions.CONTINUATION_INDENT_SIZE = 4
        commonSettings.KEEP_BLANK_LINES_IN_CODE = 2
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
            consumer.showStandardOptions("KEEP_BLANK_LINES_IN_CODE")
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String = CODE_SAMPLE

    private companion object {
        val CODE_SAMPLE = """
            set architecture aarch64
            target extended-remote :1234

            define reset
            monitor system_reset
            load
            break _start
            end

            if ${'$'}pc == 0
            printf "no entry point\n"
            end
        """.trimIndent()
    }
}
