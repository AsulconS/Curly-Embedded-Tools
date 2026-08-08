package com.asulcons.embedded.gdb.lexer

import com.asulcons.embedded.gdb.GdbLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class GdbTokenType(debugName: String) : IElementType(debugName, GdbLanguage) {
    override fun toString(): String = "Gdb:" + super.toString()
}

object GdbTokens {
    @JvmField val COMMENT = GdbTokenType("COMMENT")

    /** First word of a line, format suffix included, so that `x/16xw` stays one atom. */
    @JvmField val COMMAND = GdbTokenType("COMMAND")

    @JvmField val KW_DEFINE = GdbTokenType("KW_DEFINE")
    @JvmField val KW_DOCUMENT = GdbTokenType("KW_DOCUMENT")
    @JvmField val KW_PYTHON = GdbTokenType("KW_PYTHON")
    @JvmField val KW_COMMANDS = GdbTokenType("KW_COMMANDS")
    @JvmField val KW_IF = GdbTokenType("KW_IF")
    @JvmField val KW_ELSE = GdbTokenType("KW_ELSE")
    @JvmField val KW_WHILE = GdbTokenType("KW_WHILE")
    @JvmField val KW_END = GdbTokenType("KW_END")

    /** Verbatim body of a `python` or `document` block — not GDB command syntax. */
    @JvmField val RAW_BODY = GdbTokenType("RAW_BODY")

    @JvmField val IDENTIFIER = GdbTokenType("IDENTIFIER")
    @JvmField val NUMBER = GdbTokenType("NUMBER")
    @JvmField val STRING = GdbTokenType("STRING")
    @JvmField val CHAR = GdbTokenType("CHAR")

    /** A backslash escape in argument text: the `\n` in `echo \n--- state ---\n`. */
    @JvmField val ESCAPE_SEQUENCE = GdbTokenType("ESCAPE_SEQUENCE")

    /** A trailing backslash together with the line break it swallows, joining the next line on. */
    @JvmField val LINE_CONTINUATION = GdbTokenType("LINE_CONTINUATION")

    /** Convenience variable, register or value-history reference: `$pc`, `$x0`, `$1`. */
    @JvmField val DOLLAR_VARIABLE = GdbTokenType("DOLLAR_VARIABLE")

    @JvmField val COMMA = GdbTokenType("COMMA")
    @JvmField val SEMICOLON = GdbTokenType("SEMICOLON")
    @JvmField val COLON = GdbTokenType("COLON")
    @JvmField val LPAREN = GdbTokenType("LPAREN")
    @JvmField val RPAREN = GdbTokenType("RPAREN")
    @JvmField val LBRACKET = GdbTokenType("LBRACKET")
    @JvmField val RBRACKET = GdbTokenType("RBRACKET")
    @JvmField val LBRACE = GdbTokenType("LBRACE")
    @JvmField val RBRACE = GdbTokenType("RBRACE")

    @JvmField val OPERATOR = GdbTokenType("OPERATOR")

    @JvmField val COMMENTS: TokenSet = TokenSet.create(COMMENT)

    @JvmField val STRING_LITERALS: TokenSet = TokenSet.create(STRING, CHAR)

    @JvmField val BLOCK_OPENERS: TokenSet = TokenSet.create(
        KW_DEFINE, KW_DOCUMENT, KW_PYTHON, KW_COMMANDS, KW_IF, KW_WHILE,
    )

    @JvmField val KEYWORDS: TokenSet = TokenSet.orSet(
        BLOCK_OPENERS,
        TokenSet.create(KW_ELSE, KW_END),
    )

    /** Keyword spellings recognised at the head of a line. */
    val KEYWORD_BY_NAME: Map<String, IElementType> = mapOf(
        "define" to KW_DEFINE,
        "document" to KW_DOCUMENT,
        "python" to KW_PYTHON,
        "py" to KW_PYTHON,
        "commands" to KW_COMMANDS,
        "if" to KW_IF,
        "else" to KW_ELSE,
        "while" to KW_WHILE,
        "end" to KW_END,
    )
}
