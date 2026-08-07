package com.asulcons.embedded.armv8.formatting

import com.asulcons.embedded.armv8.ArmAsmLanguage
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.psi.ArmAsmElementTypes
import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

/**
 * Formatting block for one assembly node.
 *
 * Two things are unusual compared to a curly-brace language:
 *
 * * indentation is decided per statement rather than by nesting — a statement that opens with a label
 *   sits in column 0, everything else gets one indent level;
 * * consecutive instructions can share an [Alignment] so their operands line up in a column, which is
 *   the layout assembly is normally read in. The alignment is created per run of statements and reset
 *   at every blank line, so distant blocks never drag each other around.
 */
class ArmAsmBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val indent: Indent,
    private val spacingBuilder: SpacingBuilder,
    private val settings: ArmAsmCodeStyleSettings,
    private val keepBlankLines: Int,
    private val operandAlignment: Alignment? = null,
) : AbstractBlock(node, wrap, alignment) {

    override fun getIndent(): Indent = indent

    override fun isLeaf(): Boolean = node.firstChildNode == null

    override fun buildChildren(): List<Block> = when (node.elementType) {
        ArmAsmElementTypes.FILE -> buildFileChildren()
        else -> buildNestedChildren()
    }

    private fun buildFileChildren(): List<Block> {
        val blocks = ArrayList<Block>()
        var groupAlignment = newOperandAlignment()

        var child = node.firstChildNode
        while (child != null) {
            if (isIgnorable(child)) {
                if (child.elementType === TokenType.WHITE_SPACE && child.text.count { it == '\n' } > 1) {
                    groupAlignment = newOperandAlignment()
                }
                child = child.treeNext
                continue
            }

            val alignable = child.elementType === ArmAsmElementTypes.STATEMENT && hasInstruction(child)
            blocks += ArmAsmBlock(
                node = child,
                wrap = null,
                alignment = null,
                indent = indentForTopLevel(child),
                spacingBuilder = spacingBuilder,
                settings = settings,
                keepBlankLines = keepBlankLines,
                operandAlignment = if (alignable) groupAlignment else null,
            )
            child = child.treeNext
        }
        return blocks
    }

    private fun buildNestedChildren(): List<Block> {
        val blocks = ArrayList<Block>()
        var child = node.firstChildNode
        while (child != null) {
            if (isIgnorable(child)) {
                child = child.treeNext
                continue
            }
            blocks += ArmAsmBlock(
                node = child,
                wrap = null,
                alignment = alignmentForChild(child),
                indent = Indent.getNoneIndent(),
                spacingBuilder = spacingBuilder,
                settings = settings,
                keepBlankLines = keepBlankLines,
                // Only the statement -> instruction -> operand-list chain carries the column alignment.
                operandAlignment = operandAlignment.takeIf { child.elementType === ArmAsmElementTypes.INSTRUCTION },
            )
            child = child.treeNext
        }
        return blocks
    }

    private fun alignmentForChild(child: ASTNode): Alignment? =
        if (node.elementType === ArmAsmElementTypes.INSTRUCTION &&
            child.elementType === ArmAsmElementTypes.OPERAND_LIST
        ) {
            operandAlignment
        } else {
            null
        }

    private fun indentForTopLevel(child: ASTNode): Indent {
        if (child.elementType !== ArmAsmElementTypes.STATEMENT) return Indent.getNoneIndent()
        val first = firstMeaningfulChild(child) ?: return Indent.getNoneIndent()
        return when (first.elementType) {
            ArmAsmElementTypes.LABEL_DEFINITION -> Indent.getNoneIndent()
            ArmAsmElementTypes.PREPROCESSOR_STATEMENT -> Indent.getNoneIndent()
            ArmAsmElementTypes.DIRECTIVE_STATEMENT ->
                if (settings.INDENT_DIRECTIVES) Indent.getNormalIndent() else Indent.getNoneIndent()
            else -> Indent.getNormalIndent()
        }
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        if (node.elementType === ArmAsmElementTypes.FILE) return fileLevelSpacing(child1, child2)
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    /**
     * Statements are separated by line breaks rather than by a terminator token, so the file block has
     * to state that itself; the surrounding rules exist to keep trailing comments and `;`-separated
     * statements on the line they were written on.
     */
    private fun fileLevelSpacing(child1: Block?, child2: Block): Spacing? {
        if (child1 !is ArmAsmBlock) return null
        val left = child1.node.elementType
        val right = (child2 as? ArmAsmBlock)?.node?.elementType ?: return null

        if (left === ArmAsmTokens.SEMICOLON) return Spacing.createSpacing(1, 1, 0, true, keepBlankLines)
        if (right === ArmAsmTokens.SEMICOLON) return Spacing.createSpacing(0, 0, 0, true, keepBlankLines)

        // A comment written at the end of a statement stays there; one on its own line stays there too.
        if (ArmAsmTokens.COMMENTS.contains(right)) return Spacing.createSpacing(1, 1, 0, true, keepBlankLines)
        if (left === ArmAsmTokens.BLOCK_COMMENT) return Spacing.createSpacing(0, 1, 0, true, keepBlankLines)

        return Spacing.createSpacing(0, 0, 1, true, keepBlankLines)
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes = when (node.elementType) {
        ArmAsmElementTypes.FILE -> ChildAttributes(Indent.getNormalIndent(), null)
        else -> ChildAttributes(Indent.getNoneIndent(), null)
    }

    private fun newOperandAlignment(): Alignment? =
        if (settings.ALIGN_OPERANDS) Alignment.createAlignment(true) else null

    private companion object {
        val IGNORED: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

        fun isIgnorable(node: ASTNode): Boolean =
            IGNORED.contains(node.elementType) || node.textLength == 0

        fun hasInstruction(statement: ASTNode): Boolean =
            statement.findChildByType(ArmAsmElementTypes.INSTRUCTION) != null

        fun firstMeaningfulChild(statement: ASTNode): ASTNode? {
            var child = statement.firstChildNode
            while (child != null && isIgnorable(child)) child = child.treeNext
            return child
        }
    }
}

internal object ArmAsmSpacing {

    private val UNARY_OPERATORS: TokenSet = TokenSet.create(
        ArmAsmTokens.MINUS, ArmAsmTokens.PLUS, ArmAsmTokens.TILDE, ArmAsmTokens.EXCL,
    )

    fun createBuilder(settings: CodeStyleSettings, custom: ArmAsmCodeStyleSettings): SpacingBuilder =
        SpacingBuilder(settings, ArmAsmLanguage)
            .before(ArmAsmTokens.COLON).none()
            .before(ArmAsmTokens.COMMA).none()
            .after(ArmAsmTokens.COMMA).spaceIf(custom.SPACE_AFTER_COMMA)
            .after(ArmAsmTokens.LBRACKET).none()
            .before(ArmAsmTokens.RBRACKET).none()
            .after(ArmAsmTokens.LPAREN).none()
            .before(ArmAsmTokens.RPAREN).none()
            .after(ArmAsmTokens.LBRACE).none()
            .before(ArmAsmTokens.RBRACE).none()
            .before(ArmAsmTokens.EXCL).none()
            .after(ArmAsmTokens.HASH).none()
            .after(ArmAsmTokens.RELOCATION).none()
            .between(ArmAsmTokens.MNEMONIC, ArmAsmElementTypes.OPERAND_LIST).spaces(1)
            .between(ArmAsmTokens.DIRECTIVE, ArmAsmElementTypes.DIRECTIVE_ARGUMENT).spaces(1)
            .between(ArmAsmElementTypes.LABEL_DEFINITION, ArmAsmElementTypes.INSTRUCTION).spaces(1)
            .between(ArmAsmElementTypes.LABEL_DEFINITION, ArmAsmElementTypes.DIRECTIVE_STATEMENT).spaces(1)
            // A unary sign binds tightly to its operand; a binary one follows the user's preference.
            .aroundInside(UNARY_OPERATORS, ArmAsmElementTypes.UNARY_EXPRESSION).none()
            .aroundInside(ArmAsmTokens.OPERATORS, ArmAsmElementTypes.BINARY_EXPRESSION)
            .spaceIf(custom.SPACE_AROUND_OPERATORS)
}
