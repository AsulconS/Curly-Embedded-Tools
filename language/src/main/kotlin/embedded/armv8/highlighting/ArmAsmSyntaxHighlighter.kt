package com.asulcons.embedded.armv8.highlighting

import com.asulcons.embedded.armv8.lexer.ArmAsmLexer
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Lexer-driven colouring. Everything whose category the lexer already settled — mnemonics, labels,
 * registers, directives — is coloured here; anything that needs the surrounding statement to classify
 * (condition codes, system registers, macro calls) is left to [ArmAsmAnnotator].
 */
class ArmAsmSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = ArmAsmLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        pack(KEYS[tokenType])

    private companion object {
        val KEYS: Map<IElementType, TextAttributesKey> = mapOf(
            ArmAsmTokens.LINE_COMMENT to ArmAsmColors.LINE_COMMENT,
            ArmAsmTokens.BLOCK_COMMENT to ArmAsmColors.BLOCK_COMMENT,
            ArmAsmTokens.MNEMONIC to ArmAsmColors.MNEMONIC,
            ArmAsmTokens.DIRECTIVE to ArmAsmColors.DIRECTIVE,
            ArmAsmTokens.PREPROCESSOR to ArmAsmColors.PREPROCESSOR,
            ArmAsmTokens.LABEL to ArmAsmColors.LABEL,
            ArmAsmTokens.REGISTER to ArmAsmColors.REGISTER,
            ArmAsmTokens.NUMBER to ArmAsmColors.NUMBER,
            ArmAsmTokens.STRING to ArmAsmColors.STRING,
            ArmAsmTokens.CHAR to ArmAsmColors.STRING,
            ArmAsmTokens.IDENTIFIER to ArmAsmColors.IDENTIFIER,
            ArmAsmTokens.LOCAL_LABEL_REF to ArmAsmColors.LOCAL_LABEL_REFERENCE,
            ArmAsmTokens.MACRO_PARAM to ArmAsmColors.MACRO_PARAMETER,
            ArmAsmTokens.RELOCATION to ArmAsmColors.RELOCATION,
            ArmAsmTokens.COMMA to ArmAsmColors.COMMA,
            ArmAsmTokens.LBRACKET to ArmAsmColors.BRACKETS,
            ArmAsmTokens.RBRACKET to ArmAsmColors.BRACKETS,
            ArmAsmTokens.LBRACE to ArmAsmColors.BRACES,
            ArmAsmTokens.RBRACE to ArmAsmColors.BRACES,
            ArmAsmTokens.LPAREN to ArmAsmColors.PARENTHESES,
            ArmAsmTokens.RPAREN to ArmAsmColors.PARENTHESES,
            TokenType.BAD_CHARACTER to ArmAsmColors.BAD_CHARACTER,
        ) + ArmAsmTokens.OPERATORS.types.associateWith { ArmAsmColors.OPERATOR }
    }
}

class ArmAsmSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        ArmAsmSyntaxHighlighter()
}
