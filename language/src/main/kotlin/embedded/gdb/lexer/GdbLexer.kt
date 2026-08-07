package com.asulcons.embedded.gdb.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Lexer for GDB command scripts (`.gdbinit`, `*.gdb`).
 *
 * GDB arguments are free-form text, so the lexer only commits to structure where GDB itself does: the
 * first word of a line names the command, and `python`/`document` hand the rest of the block to a
 * different language entirely — those bodies are kept as one opaque token instead of being shredded
 * into GDB tokens that would mean nothing.
 */
class GdbLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var bufferEndOffset = 0
    private var tokenStartOffset = 0
    private var tokenEndOffset = 0
    private var currentToken: IElementType? = null
    private var currentState = LINE_START
    private var followingState = LINE_START

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEndOffset = endOffset
        this.tokenStartOffset = startOffset
        this.tokenEndOffset = startOffset
        this.followingState = initialState
        advance()
    }

    override fun getState(): Int = currentState

    override fun getTokenType(): IElementType? = currentToken

    override fun getTokenStart(): Int = tokenStartOffset

    override fun getTokenEnd(): Int = tokenEndOffset

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEndOffset

    override fun advance() {
        currentState = followingState
        tokenStartOffset = tokenEndOffset
        if (tokenStartOffset >= bufferEndOffset) {
            currentToken = null
            return
        }
        currentToken = scan()
    }

    private fun scan(): IElementType {
        if (currentState == RAW_BODY) {
            scanRawBody()?.let { return it }
        }

        val c = buffer[tokenStartOffset]
        followingState = if (currentState == RAW_HEAD) RAW_HEAD else ARGUMENTS

        if (c.isWhitespace()) return scanWhitespace()
        if (c == '#') return scanComment()

        if (currentState == LINE_START && (c.isLetter() || c == '_' || c == '!')) return scanCommand()

        return when {
            c == '"' -> scanString('"', GdbTokens.STRING)
            c == '\'' -> scanString('\'', GdbTokens.CHAR)
            c == '$' -> scanDollarVariable()
            c.isDigit() -> scanNumber()
            c.isLetter() || c == '_' || c == '.' || c == '~' || c == '/' -> scanWord()
            c == ',' -> single(GdbTokens.COMMA)
            c == ';' -> single(GdbTokens.SEMICOLON)
            c == ':' -> single(GdbTokens.COLON)
            c == '(' -> single(GdbTokens.LPAREN)
            c == ')' -> single(GdbTokens.RPAREN)
            c == '[' -> single(GdbTokens.LBRACKET)
            c == ']' -> single(GdbTokens.RBRACKET)
            c == '{' -> single(GdbTokens.LBRACE)
            c == '}' -> single(GdbTokens.RBRACE)
            c in OPERATOR_CHARS -> scanOperator()
            else -> single(TokenType.BAD_CHARACTER)
        }
    }

    private fun scanWhitespace(): IElementType {
        var i = tokenStartOffset
        var sawLineBreak = false
        while (i < bufferEndOffset && buffer[i].isWhitespace()) {
            if (buffer[i] == '\n') sawLineBreak = true
            i++
        }
        tokenEndOffset = i
        followingState = when {
            !sawLineBreak -> currentState
            currentState == RAW_HEAD -> RAW_BODY
            currentState == RAW_BODY -> RAW_BODY
            else -> LINE_START
        }
        return TokenType.WHITE_SPACE
    }

    private fun scanComment(): IElementType {
        var i = tokenStartOffset + 1
        while (i < bufferEndOffset && buffer[i] != '\n') i++
        tokenEndOffset = i
        followingState = currentState
        return GdbTokens.COMMENT
    }

    /**
     * The command word plus any `/format` suffix, so `x/16xw` and `p/x` survive as single tokens and
     * `/` keeps its ordinary meaning everywhere else on the line.
     */
    private fun scanCommand(): IElementType {
        var i = tokenStartOffset
        if (buffer[i] == '!') {
            tokenEndOffset = i + 1
            return GdbTokens.COMMAND
        }
        while (i < bufferEndOffset && isCommandChar(buffer[i])) i++
        val wordEnd = i
        if (i < bufferEndOffset && buffer[i] == '/') {
            i++
            while (i < bufferEndOffset && (buffer[i].isLetterOrDigit())) i++
        }
        tokenEndOffset = i

        val word = buffer.subSequence(tokenStartOffset, wordEnd).toString()
        val keyword = GdbTokens.KEYWORD_BY_NAME[word]
        if (keyword != null && wordEnd == tokenEndOffset) {
            followingState = when (keyword) {
                GdbTokens.KW_PYTHON, GdbTokens.KW_DOCUMENT -> RAW_HEAD
                else -> ARGUMENTS
            }
            return keyword
        }
        return GdbTokens.COMMAND
    }

    /**
     * Everything up to the line that reads `end`. Returns `null` when the body is empty, letting the
     * caller fall through and lex that `end` normally.
     */
    private fun scanRawBody(): IElementType? {
        var lineStart = tokenStartOffset
        var foundEnd = false
        while (lineStart < bufferEndOffset) {
            val lineEnd = lineEndFrom(lineStart)
            if (buffer.subSequence(lineStart, lineEnd).toString().trim() == "end") {
                foundEnd = true
                break
            }
            lineStart = if (lineEnd < bufferEndOffset) lineEnd + 1 else bufferEndOffset
        }
        if (foundEnd && lineStart == tokenStartOffset) {
            // The block is empty; let `end` be lexed as the keyword it is.
            currentState = LINE_START
            followingState = LINE_START
            return null
        }
        // Stop just short of the newline so the `end` line is reached through ordinary whitespace.
        tokenEndOffset = if (foundEnd) lineStart - 1 else bufferEndOffset
        followingState = LINE_START
        return GdbTokens.RAW_BODY
    }

    private fun lineEndFrom(from: Int): Int {
        var i = from
        while (i < bufferEndOffset && buffer[i] != '\n') i++
        return i
    }

    private fun scanString(quote: Char, type: IElementType): IElementType {
        var i = tokenStartOffset + 1
        while (i < bufferEndOffset) {
            val c = buffer[i]
            if (c == '\\' && i + 1 < bufferEndOffset && buffer[i + 1] != '\n') {
                i += 2
                continue
            }
            if (c == quote) {
                i++
                break
            }
            if (c == '\n') break
            i++
        }
        tokenEndOffset = i
        return type
    }

    private fun scanDollarVariable(): IElementType {
        var i = tokenStartOffset + 1
        while (i < bufferEndOffset && (buffer[i].isLetterOrDigit() || buffer[i] == '_')) i++
        tokenEndOffset = i
        return GdbTokens.DOLLAR_VARIABLE
    }

    private fun scanNumber(): IElementType {
        var i = tokenStartOffset
        if (buffer[i] == '0' && i + 1 < bufferEndOffset && buffer[i + 1].lowercaseChar() == 'x') {
            i += 2
            while (i < bufferEndOffset && isHex(buffer[i])) i++
        } else {
            while (i < bufferEndOffset && buffer[i].isDigit()) i++
            if (i < bufferEndOffset && buffer[i] == '.' && i + 1 < bufferEndOffset && buffer[i + 1].isDigit()) {
                i++
                while (i < bufferEndOffset && buffer[i].isDigit()) i++
            }
        }
        tokenEndOffset = i
        return GdbTokens.NUMBER
    }

    /** Arguments are free text: file paths, symbol names and `+`-joined expressions all land here. */
    private fun scanWord(): IElementType {
        var i = tokenStartOffset
        while (i < bufferEndOffset && isWordChar(buffer[i])) i++
        if (i == tokenStartOffset) i++
        tokenEndOffset = i
        return GdbTokens.IDENTIFIER
    }

    private fun scanOperator(): IElementType {
        val pair = if (tokenStartOffset + 1 < bufferEndOffset) {
            buffer.subSequence(tokenStartOffset, tokenStartOffset + 2).toString()
        } else {
            ""
        }
        tokenEndOffset = if (pair in TWO_CHAR_OPERATORS) tokenStartOffset + 2 else tokenStartOffset + 1
        return GdbTokens.OPERATOR
    }

    private fun single(type: IElementType): IElementType {
        tokenEndOffset = tokenStartOffset + 1
        return type
    }

    private fun isCommandChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '-'

    private fun isWordChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c == '.' || c == '-' || c == '/' || c == '~' || c == '\\'

    private fun isHex(c: Char): Boolean =
        c.isDigit() || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')

    private companion object {
        const val LINE_START = 0
        const val ARGUMENTS = 1

        /** On the `python`/`document` line itself; the body starts after the next newline. */
        const val RAW_HEAD = 2

        /** Inside a verbatim block body, looking for the closing `end`. */
        const val RAW_BODY = 3

        const val OPERATOR_CHARS = "+-*%&|^~!<>=?@"

        val TWO_CHAR_OPERATORS = setOf("==", "!=", "<=", ">=", "&&", "||", "<<", ">>", "->")
    }
}
