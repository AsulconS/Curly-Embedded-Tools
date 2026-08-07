package com.asulcons.embedded.armv8.lexer

import com.asulcons.embedded.armv8.ArmAsmLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class ArmAsmTokenType(debugName: String) : IElementType(debugName, ArmAsmLanguage) {
    override fun toString(): String = "ArmAsm:" + super.toString()
}

object ArmAsmTokens {
    // Trivia -------------------------------------------------------------------------------------
    @JvmField val LINE_COMMENT = ArmAsmTokenType("LINE_COMMENT")
    @JvmField val BLOCK_COMMENT = ArmAsmTokenType("BLOCK_COMMENT")

    // Statement heads ----------------------------------------------------------------------------
    /** Identifier (or digit run) that the lexer proved is followed by `:` on the same line. */
    @JvmField val LABEL = ArmAsmTokenType("LABEL")
    /** First word of a statement that is not a label, e.g. `ldr`, `b.eq`, or a macro invocation. */
    @JvmField val MNEMONIC = ArmAsmTokenType("MNEMONIC")
    /** A `.`-prefixed assembler directive such as `.section` or `.cfi_startproc`. */
    @JvmField val DIRECTIVE = ArmAsmTokenType("DIRECTIVE")
    /** A whole `#...` C-preprocessor line, continuations included. */
    @JvmField val PREPROCESSOR = ArmAsmTokenType("PREPROCESSOR")

    // Operand atoms ------------------------------------------------------------------------------
    @JvmField val REGISTER = ArmAsmTokenType("REGISTER")
    @JvmField val IDENTIFIER = ArmAsmTokenType("IDENTIFIER")
    @JvmField val NUMBER = ArmAsmTokenType("NUMBER")
    @JvmField val STRING = ArmAsmTokenType("STRING")
    @JvmField val CHAR = ArmAsmTokenType("CHAR")
    /** Backwards/forwards reference to a numeric local label, e.g. `1b` or `2f`. */
    @JvmField val LOCAL_LABEL_REF = ArmAsmTokenType("LOCAL_LABEL_REF")
    /** Macro parameter substitution inside a `.macro` body: `\count`, `\()`, `\@`. */
    @JvmField val MACRO_PARAM = ArmAsmTokenType("MACRO_PARAM")
    /** Relocation specifier wrapping a symbol, e.g. `:lo12:` in `add x0, x0, :lo12:sym`. */
    @JvmField val RELOCATION = ArmAsmTokenType("RELOCATION")

    // Punctuation --------------------------------------------------------------------------------
    @JvmField val COMMA = ArmAsmTokenType("COMMA")
    @JvmField val COLON = ArmAsmTokenType("COLON")
    @JvmField val SEMICOLON = ArmAsmTokenType("SEMICOLON")
    @JvmField val LBRACKET = ArmAsmTokenType("LBRACKET")
    @JvmField val RBRACKET = ArmAsmTokenType("RBRACKET")
    @JvmField val LBRACE = ArmAsmTokenType("LBRACE")
    @JvmField val RBRACE = ArmAsmTokenType("RBRACE")
    @JvmField val LPAREN = ArmAsmTokenType("LPAREN")
    @JvmField val RPAREN = ArmAsmTokenType("RPAREN")
    @JvmField val HASH = ArmAsmTokenType("HASH")
    @JvmField val EXCL = ArmAsmTokenType("EXCL")

    // Operators ----------------------------------------------------------------------------------
    @JvmField val EQ = ArmAsmTokenType("EQ")
    @JvmField val PLUS = ArmAsmTokenType("PLUS")
    @JvmField val MINUS = ArmAsmTokenType("MINUS")
    @JvmField val STAR = ArmAsmTokenType("STAR")
    @JvmField val SLASH = ArmAsmTokenType("SLASH")
    @JvmField val PERCENT = ArmAsmTokenType("PERCENT")
    @JvmField val AMP = ArmAsmTokenType("AMP")
    @JvmField val AND_AND = ArmAsmTokenType("AND_AND")
    @JvmField val PIPE = ArmAsmTokenType("PIPE")
    @JvmField val OR_OR = ArmAsmTokenType("OR_OR")
    @JvmField val CARET = ArmAsmTokenType("CARET")
    @JvmField val TILDE = ArmAsmTokenType("TILDE")
    @JvmField val SHL = ArmAsmTokenType("SHL")
    @JvmField val SHR = ArmAsmTokenType("SHR")
    @JvmField val LT = ArmAsmTokenType("LT")
    @JvmField val GT = ArmAsmTokenType("GT")
    @JvmField val LE = ArmAsmTokenType("LE")
    @JvmField val GE = ArmAsmTokenType("GE")
    @JvmField val EQ_EQ = ArmAsmTokenType("EQ_EQ")
    @JvmField val NE = ArmAsmTokenType("NE")
    /** The location counter `.` when it stands on its own. */
    @JvmField val DOT = ArmAsmTokenType("DOT")
    @JvmField val AT = ArmAsmTokenType("AT")

    // Token sets ---------------------------------------------------------------------------------
    @JvmField val COMMENTS: TokenSet = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)

    @JvmField val STRING_LITERALS: TokenSet = TokenSet.create(STRING, CHAR)

    @JvmField val OPERATORS: TokenSet = TokenSet.create(
        EQ, PLUS, MINUS, STAR, SLASH, PERCENT, AMP, AND_AND, PIPE, OR_OR,
        CARET, TILDE, SHL, SHR, LT, GT, LE, GE, EQ_EQ, NE,
    )

    @JvmField val BRACKETS: TokenSet = TokenSet.create(LBRACKET, RBRACKET)

    @JvmField val BRACES: TokenSet = TokenSet.create(LBRACE, RBRACE)

    @JvmField val PARENTHESES: TokenSet = TokenSet.create(LPAREN, RPAREN)

    /** Everything the expression parser accepts as a leading atom. */
    @JvmField val EXPRESSION_ATOMS: TokenSet = TokenSet.create(
        NUMBER, STRING, CHAR, IDENTIFIER, REGISTER, LOCAL_LABEL_REF, MACRO_PARAM, RELOCATION, DOT, LPAREN,
    )
}
