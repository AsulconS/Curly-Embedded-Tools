package com.asulcons.embedded.linker.formatting

import com.asulcons.embedded.linker.LinkerScriptLanguage
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.asulcons.embedded.linker.psi.LinkerElementTypes
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
import com.intellij.formatting.SpacingBuilder
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.TokenSet

class LinkerFormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val file = formattingContext.containingFile
        val root = LinkerFormattingBlock(
            file.node,
            Indent.getNoneIndent(),
            createSpacingBuilder(settings),
            settings.getCommonSettings(LinkerScriptLanguage).KEEP_BLANK_LINES_IN_CODE,
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(file, root, settings)
    }

    private fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder =
        SpacingBuilder(settings, LinkerScriptLanguage)
            .before(LinkerTokens.SEMICOLON).none()
            .before(LinkerTokens.COMMA).none()
            .after(LinkerTokens.COMMA).spaces(1)
            .after(LinkerTokens.LPAREN).none()
            .before(LinkerTokens.RPAREN).none()
            .around(LinkerTokens.ASSIGN).spaces(1)
            .around(LinkerTokens.COMPOUND_ASSIGN).spaces(1)
            .aroundInside(BINARY_OPERATORS, LinkerElementTypes.BINARY_EXPRESSION).spaces(1)
}

private val BINARY_OPERATORS: TokenSet =
    TokenSet.create(LinkerTokens.OPERATOR, LinkerTokens.GT, LinkerTokens.LT, LinkerTokens.STAR)

/**
 * Indents the braced blocks and puts one statement per line; input-section descriptions are left as a
 * single unit so that a hand-tuned `*(EXCLUDE_FILE(...) .text .text.*)` keeps its layout.
 */
private class LinkerFormattingBlock(
    node: ASTNode,
    private val indent: Indent,
    private val spacingBuilder: SpacingBuilder,
    private val keepBlankLines: Int,
) : AbstractBlock(node, null, null) {

    override fun getIndent(): Indent = indent

    override fun isLeaf(): Boolean = node.firstChildNode == null || isOpaque()

    private fun isOpaque(): Boolean = node.elementType === LinkerElementTypes.INPUT_SECTION ||
        node.elementType === LinkerElementTypes.SECTION_HEADER ||
        node.elementType === LinkerElementTypes.REGION_ATTRIBUTES

    override fun buildChildren(): List<Block> {
        if (isOpaque()) return emptyList()
        val blocks = ArrayList<Block>()
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType !== TokenType.WHITE_SPACE && child.textLength > 0) {
                blocks += LinkerFormattingBlock(child, indentFor(child), spacingBuilder, keepBlankLines)
            }
            child = child.treeNext
        }
        return blocks
    }

    /** Everything between the braces of a block is one level in; the braces themselves are not. */
    private fun indentFor(child: ASTNode): Indent {
        val insideBraces = node.elementType === LinkerElementTypes.MEMORY_BLOCK ||
            node.elementType === LinkerElementTypes.SECTIONS_BLOCK ||
            node.elementType === LinkerElementTypes.SECTION_BODY ||
            node.elementType === LinkerElementTypes.PHDRS_BLOCK ||
            node.elementType === LinkerElementTypes.VERSION_BLOCK
        if (!insideBraces) return Indent.getNoneIndent()
        return when (child.elementType) {
            LinkerTokens.LBRACE, LinkerTokens.RBRACE, LinkerTokens.KEYWORD -> Indent.getNoneIndent()
            else -> Indent.getNormalIndent()
        }
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        val parentType = node.elementType
        val rightType = (child2 as? LinkerFormattingBlock)?.node?.elementType
        val leftType = (child1 as? LinkerFormattingBlock)?.node?.elementType

        if (parentType === LinkerElementTypes.FILE) {
            return Spacing.createSpacing(0, 0, 1, true, keepBlankLines)
        }
        if (parentType === LinkerElementTypes.MEMORY_BLOCK || parentType === LinkerElementTypes.SECTIONS_BLOCK ||
            parentType === LinkerElementTypes.SECTION_BODY || parentType === LinkerElementTypes.PHDRS_BLOCK
        ) {
            if (leftType === LinkerTokens.LBRACE || rightType === LinkerTokens.RBRACE) {
                return Spacing.createSpacing(0, 0, 1, true, keepBlankLines)
            }
            return Spacing.createSpacing(0, 1, 1, true, keepBlankLines)
        }
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes = when (node.elementType) {
        LinkerElementTypes.MEMORY_BLOCK, LinkerElementTypes.SECTIONS_BLOCK,
        LinkerElementTypes.SECTION_BODY, LinkerElementTypes.PHDRS_BLOCK,
        -> ChildAttributes(Indent.getNormalIndent(), null)
        else -> ChildAttributes(Indent.getNoneIndent(), null)
    }
}

class LinkerLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language = LinkerScriptLanguage

    override fun getConfigurableDisplayName(): String = "GNU Linker Script"

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
            ENTRY(_start)

            MEMORY
            {
            FLASH (rx) : ORIGIN = 0x00000000, LENGTH = 2M
            RAM (rwx) : ORIGIN = 0x40000000, LENGTH = 128M
            }

            SECTIONS
            {
            .text : ALIGN(8)
            {
            KEEP(*(.text.boot))
            *(.text .text.*)
            } > FLASH

            __stack_top = ORIGIN(RAM) + LENGTH(RAM);
            }
        """.trimIndent()
    }
}
