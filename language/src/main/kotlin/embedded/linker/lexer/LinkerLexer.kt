package com.asulcons.embedded.linker.lexer

import com.asulcons.embedded.linker.spec.LinkerKeywords
import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Lexer for GNU `ld` scripts.
 *
 * The awkward part of `ld`'s syntax is that `*` is both multiplication and "match every input file",
 * and that section names are written with characters no other language would allow in an identifier
 * (`.text.*`, `*libc.a`, `/DISCARD/`). The rule used here mirrors how the scripts are actually
 * written: a `*` glued to name characters is part of a glob, a `*` standing alone is an operator.
 */
class LinkerLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var bufferEndOffset = 0
    private var tokenStartOffset = 0
    private var tokenEndOffset = 0
    private var currentToken: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEndOffset = endOffset
        this.tokenStartOffset = startOffset
        this.tokenEndOffset = startOffset
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = currentToken

    override fun getTokenStart(): Int = tokenStartOffset

    override fun getTokenEnd(): Int = tokenEndOffset

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEndOffset

    override fun advance() {
        tokenStartOffset = tokenEndOffset
        if (tokenStartOffset >= bufferEndOffset) {
            currentToken = null
            return
        }
        currentToken = scan()
    }

    private fun scan(): IElementType {
        val c = buffer[tokenStartOffset]

        if (c.isWhitespace()) return scanWhitespace()
        if (c == '/' && peek(1) == '*') return scanBlockComment()
        if (c == '#' && atLineStart()) return scanPreprocessorLine()
        if (c == '"') return scanString()
        if (c.isDigit()) return scanNumber()

        if (c == '*' || c == '?') {
            return if (startsGlob()) scanName() else single(if (c == '*') LinkerTokens.STAR else LinkerTokens.QUESTION)
        }
        if (c == '.') {
            val next = peek(1)
            return if (next != null && isNameChar(next)) scanName() else single(LinkerTokens.DOT)
        }
        if (c == '/' || c == '\\') {
            // `/DISCARD/` is a section name; a lone `/` is division.
            val next = peek(1)
            return if (next != null && isNameChar(next)) scanName() else scanOperator()
        }
        if (isNameStart(c)) return scanName()

        return when (c) {
            '{' -> single(LinkerTokens.LBRACE)
            '}' -> single(LinkerTokens.RBRACE)
            '(' -> single(LinkerTokens.LPAREN)
            ')' -> single(LinkerTokens.RPAREN)
            '[' -> single(LinkerTokens.LBRACKET)
            ']' -> single(LinkerTokens.RBRACKET)
            ';' -> single(LinkerTokens.SEMICOLON)
            ',' -> single(LinkerTokens.COMMA)
            ':' -> single(LinkerTokens.COLON)
            else -> scanOperator()
        }
    }

    private fun scanWhitespace(): IElementType {
        var i = tokenStartOffset
        while (i < bufferEndOffset && buffer[i].isWhitespace()) i++
        tokenEndOffset = i
        return TokenType.WHITE_SPACE
    }

    private fun scanBlockComment(): IElementType {
        var i = tokenStartOffset + 2
        var closed = false
        while (i < bufferEndOffset) {
            if (buffer[i] == '*' && i + 1 < bufferEndOffset && buffer[i + 1] == '/') {
                i += 2
                closed = true
                break
            }
            i++
        }
        tokenEndOffset = if (closed) i else bufferEndOffset
        return LinkerTokens.BLOCK_COMMENT
    }

    private fun scanPreprocessorLine(): IElementType {
        var i = tokenStartOffset + 1
        while (i < bufferEndOffset) {
            if (buffer[i] == '\\') {
                var j = i + 1
                while (j < bufferEndOffset && (buffer[j] == ' ' || buffer[j] == '\t' || buffer[j] == '\r')) j++
                if (j < bufferEndOffset && buffer[j] == '\n') {
                    i = j + 1
                    continue
                }
            }
            if (buffer[i] == '\n') break
            i++
        }
        tokenEndOffset = i
        return LinkerTokens.PREPROCESSOR
    }

    private fun scanString(): IElementType {
        var i = tokenStartOffset + 1
        while (i < bufferEndOffset) {
            val c = buffer[i]
            if (c == '\\' && i + 1 < bufferEndOffset) {
                i += 2
                continue
            }
            if (c == '"') {
                i++
                break
            }
            if (c == '\n') break
            i++
        }
        tokenEndOffset = i
        return LinkerTokens.STRING
    }

    /** `ld` numbers may carry a `K`/`M`/`G` scale suffix, as in `LENGTH = 128K`. */
    private fun scanNumber(): IElementType {
        var i = tokenStartOffset
        if (buffer[i] == '0' && i + 1 < bufferEndOffset && buffer[i + 1].lowercaseChar() == 'x') {
            i += 2
            while (i < bufferEndOffset && isHexDigit(buffer[i])) i++
        } else {
            while (i < bufferEndOffset && buffer[i].isDigit()) i++
        }
        if (i < bufferEndOffset && buffer[i].uppercaseChar() in "KMG") i++
        tokenEndOffset = i
        return LinkerTokens.NUMBER
    }

    private fun scanName(): IElementType {
        var i = tokenStartOffset
        var glob = buffer[i] == '*' || buffer[i] == '?'
        while (i < bufferEndOffset && isNameChar(buffer[i])) {
            if (buffer[i] == '*' || buffer[i] == '?' || buffer[i] == '[') glob = true
            i++
        }
        tokenEndOffset = i

        if (glob) return LinkerTokens.WILDCARD
        val text = buffer.subSequence(tokenStartOffset, i).toString()
        return if (text in LinkerKeywords.KEYWORD_NAMES) LinkerTokens.KEYWORD else LinkerTokens.IDENTIFIER
    }

    private fun scanOperator(): IElementType {
        val c = buffer[tokenStartOffset]
        val next = peek(1)
        val third = peek(2)

        // `<<=` and `>>=` are the only three-character operators.
        if ((c == '<' || c == '>') && next == c && third == '=') {
            tokenEndOffset = tokenStartOffset + 3
            return LinkerTokens.COMPOUND_ASSIGN
        }
        if (next == '=' && c in "+-*/%&|^") {
            tokenEndOffset = tokenStartOffset + 2
            return LinkerTokens.COMPOUND_ASSIGN
        }
        if (next != null) {
            val pair = "$c$next"
            if (pair in TWO_CHAR_OPERATORS) {
                tokenEndOffset = tokenStartOffset + 2
                return LinkerTokens.OPERATOR
            }
        }
        return when (c) {
            '=' -> single(LinkerTokens.ASSIGN)
            '>' -> single(LinkerTokens.GT)
            '<' -> single(LinkerTokens.LT)
            '?' -> single(LinkerTokens.QUESTION)
            in OPERATOR_CHARS -> single(LinkerTokens.OPERATOR)
            else -> single(TokenType.BAD_CHARACTER)
        }
    }

    /** True when the `*`/`?` at the cursor is glued to name characters and so belongs to a glob. */
    private fun startsGlob(): Boolean {
        val next = peek(1) ?: return false
        return isNameChar(next) && next != '*' && next != '?'
    }

    private fun atLineStart(): Boolean {
        var i = tokenStartOffset - 1
        while (i >= 0) {
            val c = buffer[i]
            if (c == '\n') return true
            if (c != ' ' && c != '\t' && c != '\r') return false
            i--
        }
        return true
    }

    private fun peek(offset: Int): Char? =
        (tokenStartOffset + offset).let { if (it < bufferEndOffset) buffer[it] else null }

    private fun single(type: IElementType): IElementType {
        tokenEndOffset = tokenStartOffset + 1
        return type
    }

    private fun isNameStart(c: Char): Boolean =
        c.isLetter() || c == '_' || c == '.' || c == '$' || c == '~'

    private fun isNameChar(c: Char): Boolean =
        c.isLetterOrDigit() || c in NAME_PUNCTUATION

    private fun isHexDigit(c: Char): Boolean =
        c.isDigit() || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')

    private companion object {
        /** Everything `ld` tolerates inside a section-name or file-name pattern. */
        const val NAME_PUNCTUATION = "_.$/\\~-*?[]"

        const val OPERATOR_CHARS = "+-*/%&|^~!<>=?"

        val TWO_CHAR_OPERATORS = setOf("==", "!=", "<=", ">=", "&&", "||", "<<", ">>")
    }
}
