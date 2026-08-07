package com.asulcons.embedded.linker.lexer

import com.asulcons.embedded.linker.LinkerScriptLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class LinkerTokenType(debugName: String) : IElementType(debugName, LinkerScriptLanguage) {
    override fun toString(): String = "Ld:" + super.toString()
}

object LinkerTokens {
    @JvmField val BLOCK_COMMENT = LinkerTokenType("BLOCK_COMMENT")

    /** `#include`/`#define` lines, seen when a script is run through `cpp` before linking. */
    @JvmField val PREPROCESSOR = LinkerTokenType("PREPROCESSOR")

    @JvmField val KEYWORD = LinkerTokenType("KEYWORD")
    @JvmField val IDENTIFIER = LinkerTokenType("IDENTIFIER")
    @JvmField val NUMBER = LinkerTokenType("NUMBER")
    @JvmField val STRING = LinkerTokenType("STRING")

    /** The location counter. */
    @JvmField val DOT = LinkerTokenType("DOT")

    @JvmField val LBRACE = LinkerTokenType("LBRACE")
    @JvmField val RBRACE = LinkerTokenType("RBRACE")
    @JvmField val LPAREN = LinkerTokenType("LPAREN")
    @JvmField val RPAREN = LinkerTokenType("RPAREN")
    @JvmField val LBRACKET = LinkerTokenType("LBRACKET")
    @JvmField val RBRACKET = LinkerTokenType("RBRACKET")
    @JvmField val SEMICOLON = LinkerTokenType("SEMICOLON")
    @JvmField val COMMA = LinkerTokenType("COMMA")
    @JvmField val COLON = LinkerTokenType("COLON")
    @JvmField val QUESTION = LinkerTokenType("QUESTION")

    @JvmField val ASSIGN = LinkerTokenType("ASSIGN")

    /** `+=`, `-=`, `*=`, `/=`, `&=`, `|=`, `<<=`, `>>=`. */
    @JvmField val COMPOUND_ASSIGN = LinkerTokenType("COMPOUND_ASSIGN")

    @JvmField val GT = LinkerTokenType("GT")
    @JvmField val LT = LinkerTokenType("LT")
    @JvmField val OPERATOR = LinkerTokenType("OPERATOR")

    /** A bare `*` — multiplication, or "match every input file". */
    @JvmField val STAR = LinkerTokenType("STAR")

    /** A glob such as `*.o`, `*libc.a` or `.text*`. */
    @JvmField val WILDCARD = LinkerTokenType("WILDCARD")

    @JvmField val COMMENTS: TokenSet = TokenSet.create(BLOCK_COMMENT)

    @JvmField val STRING_LITERALS: TokenSet = TokenSet.create(STRING)

    @JvmField val ASSIGNMENT_OPERATORS: TokenSet = TokenSet.create(ASSIGN, COMPOUND_ASSIGN)

    @JvmField val NAMES: TokenSet = TokenSet.create(IDENTIFIER, WILDCARD, STRING)

    @JvmField val OPERATORS: TokenSet =
        TokenSet.create(OPERATOR, GT, LT, STAR, QUESTION, ASSIGN, COMPOUND_ASSIGN)
}
