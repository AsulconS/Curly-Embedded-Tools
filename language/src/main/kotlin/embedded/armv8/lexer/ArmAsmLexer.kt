package com.asulcons.embedded.armv8.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * Hand-written lexer for GNU-syntax AArch64 assembly.
 *
 * Assembly is line oriented and the meaning of a word depends on where it sits in its statement: `b1`
 * is a label definition in `b1:`, a mnemonic in `b1 x0`, and the SIMD register B1 in `fmov b1, b2`.
 * The lexer therefore carries a three-valued position state, which is small enough to survive the
 * incremental-relex contract ([getState] must let the platform restart at any token boundary).
 */
class ArmAsmLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var bufferEndOffset = 0
    private var tokenStartOffset = 0
    private var tokenEndOffset = 0
    private var currentToken: IElementType? = null
    private var currentState = STATEMENT_START
    private var followingState = STATEMENT_START

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
        val c = buffer[tokenStartOffset]
        followingState = OPERANDS

        if (c.isWhitespace()) return scanWhitespace()

        if (c == '/' && peek(1) == '/') return scanLineComment()
        if (c == '/' && peek(1) == '*') return scanBlockComment()

        if (currentState == AFTER_LABEL && c == ':') return single(ArmAsmTokens.COLON, STATEMENT_START)

        if (currentState == STATEMENT_START) {
            if (c == '#') return scanPreprocessorLine()
            if (isIdentifierStart(c) || c == '.' || c.isDigit()) return scanStatementHead()
        }

        return when {
            c.isDigit() -> scanNumber()
            isIdentifierStart(c) -> scanOperandWord()
            c == '.' -> scanDottedSymbol()
            c == '"' -> scanString()
            c == '\'' -> scanCharLiteral()
            c == '\\' -> scanMacroParam()
            c == ':' -> scanRelocation()
            c == ';' -> single(ArmAsmTokens.SEMICOLON, STATEMENT_START)
            c == ',' -> single(ArmAsmTokens.COMMA)
            c == '[' -> single(ArmAsmTokens.LBRACKET)
            c == ']' -> single(ArmAsmTokens.RBRACKET)
            c == '{' -> single(ArmAsmTokens.LBRACE)
            c == '}' -> single(ArmAsmTokens.RBRACE)
            c == '(' -> single(ArmAsmTokens.LPAREN)
            c == ')' -> single(ArmAsmTokens.RPAREN)
            c == '#' -> single(ArmAsmTokens.HASH)
            c == '@' -> single(ArmAsmTokens.AT)
            c == '+' -> single(ArmAsmTokens.PLUS)
            c == '-' -> single(ArmAsmTokens.MINUS)
            c == '*' -> single(ArmAsmTokens.STAR)
            c == '/' -> single(ArmAsmTokens.SLASH)
            c == '%' -> single(ArmAsmTokens.PERCENT)
            c == '^' -> single(ArmAsmTokens.CARET)
            c == '~' -> single(ArmAsmTokens.TILDE)
            c == '=' -> if (peek(1) == '=') pair(ArmAsmTokens.EQ_EQ) else single(ArmAsmTokens.EQ)
            c == '!' -> if (peek(1) == '=') pair(ArmAsmTokens.NE) else single(ArmAsmTokens.EXCL)
            c == '&' -> if (peek(1) == '&') pair(ArmAsmTokens.AND_AND) else single(ArmAsmTokens.AMP)
            c == '|' -> if (peek(1) == '|') pair(ArmAsmTokens.OR_OR) else single(ArmAsmTokens.PIPE)
            c == '<' -> when (peek(1)) {
                '<' -> pair(ArmAsmTokens.SHL)
                '=' -> pair(ArmAsmTokens.LE)
                '>' -> pair(ArmAsmTokens.NE)
                else -> single(ArmAsmTokens.LT)
            }
            c == '>' -> when (peek(1)) {
                '>' -> pair(ArmAsmTokens.SHR)
                '=' -> pair(ArmAsmTokens.GE)
                else -> single(ArmAsmTokens.GT)
            }
            else -> single(TokenType.BAD_CHARACTER)
        }
    }

    // Scanners ---------------------------------------------------------------------------------

    private fun scanWhitespace(): IElementType {
        var i = tokenStartOffset
        var sawLineBreak = false
        while (i < bufferEndOffset && buffer[i].isWhitespace()) {
            if (buffer[i] == '\n') sawLineBreak = true
            i++
        }
        tokenEndOffset = i
        followingState = if (sawLineBreak) STATEMENT_START else currentState
        return TokenType.WHITE_SPACE
    }

    private fun scanLineComment(): IElementType {
        var i = tokenStartOffset + 2
        while (i < bufferEndOffset && buffer[i] != '\n') i++
        tokenEndOffset = i
        followingState = currentState
        return ArmAsmTokens.LINE_COMMENT
    }

    private fun scanBlockComment(): IElementType {
        var i = tokenStartOffset + 2
        var sawLineBreak = false
        var closed = false
        while (i < bufferEndOffset) {
            if (buffer[i] == '\n') sawLineBreak = true
            if (buffer[i] == '*' && i + 1 < bufferEndOffset && buffer[i + 1] == '/') {
                i += 2
                closed = true
                break
            }
            i++
        }
        tokenEndOffset = if (closed) i else bufferEndOffset
        followingState = if (sawLineBreak) STATEMENT_START else currentState
        return ArmAsmTokens.BLOCK_COMMENT
    }

    /**
     * A whole `#…` line, backslash continuations included, so that `#define`d multi-line macros in a
     * `.S` file stay one atom instead of exploding into nonsense assembly tokens.
     */
    private fun scanPreprocessorLine(): IElementType {
        var i = tokenStartOffset + 1
        while (i < bufferEndOffset) {
            val c = buffer[i]
            if (c == '\\') {
                var j = i + 1
                while (j < bufferEndOffset && (buffer[j] == ' ' || buffer[j] == '\t' || buffer[j] == '\r')) j++
                if (j < bufferEndOffset && buffer[j] == '\n') {
                    i = j + 1
                    continue
                }
                i++
                continue
            }
            if (c == '\n') break
            i++
        }
        tokenEndOffset = i
        followingState = STATEMENT_START
        return ArmAsmTokens.PREPROCESSOR
    }

    /**
     * The first word of a statement: a label definition, an assembler directive, or a mnemonic
     * (possibly a macro invocation — telling those apart needs the PSI, so both come out as MNEMONIC).
     */
    private fun scanStatementHead(): IElementType {
        val first = buffer[tokenStartOffset]

        if (first.isDigit()) {
            var i = tokenStartOffset
            while (i < bufferEndOffset && buffer[i].isDigit()) i++
            if (i < bufferEndOffset && buffer[i] == ':') {
                tokenEndOffset = i
                followingState = AFTER_LABEL
                return ArmAsmTokens.LABEL
            }
            return scanNumber()
        }

        var i = tokenStartOffset
        if (buffer[i] == '.') i++
        while (i < bufferEndOffset && isSymbolPart(buffer[i])) i++
        tokenEndOffset = i

        if (first == '.' && i == tokenStartOffset + 1) {
            // A bare `.` is the location counter, as in `. = . + 4`.
            return ArmAsmTokens.DOT
        }

        // GNU as tolerates whitespace between a label and its colon.
        var j = i
        while (j < bufferEndOffset && (buffer[j] == ' ' || buffer[j] == '\t')) j++
        if (j < bufferEndOffset && buffer[j] == ':') {
            followingState = AFTER_LABEL
            return ArmAsmTokens.LABEL
        }

        return if (first == '.') ArmAsmTokens.DIRECTIVE else ArmAsmTokens.MNEMONIC
    }

    private fun scanOperandWord(): IElementType {
        var i = tokenStartOffset
        while (i < bufferEndOffset && isIdentifierPart(buffer[i])) i++

        val registerEnd = A64Registers.match(buffer, tokenStartOffset, i, bufferEndOffset)
        if (registerEnd > tokenStartOffset) {
            tokenEndOffset = registerEnd
            return ArmAsmTokens.REGISTER
        }

        // Not a register, so let dots back in: `foo.bar` and `.text.startup` are single symbols.
        tokenEndOffset = scanSymbolTail(i)
        return ArmAsmTokens.IDENTIFIER
    }

    private fun scanDottedSymbol(): IElementType {
        val next = peek(1)
        if (next == null || !isIdentifierPart(next)) return single(ArmAsmTokens.DOT)
        tokenEndOffset = scanSymbolTail(tokenStartOffset + 1)
        return ArmAsmTokens.IDENTIFIER
    }

    private fun scanSymbolTail(from: Int): Int {
        var i = from
        while (i < bufferEndOffset) {
            val c = buffer[i]
            if (isIdentifierPart(c)) {
                i++
            } else if (c == '.' && i + 1 < bufferEndOffset && isIdentifierPart(buffer[i + 1])) {
                i++
            } else {
                break
            }
        }
        return i
    }

    private fun scanNumber(): IElementType {
        var i = tokenStartOffset
        if (buffer[i] == '0' && i + 1 < bufferEndOffset) {
            when (buffer[i + 1].lowercaseChar()) {
                'x' -> {
                    i += 2
                    while (i < bufferEndOffset && (isHexDigit(buffer[i]) || buffer[i] == '_')) i++
                    tokenEndOffset = i
                    return ArmAsmTokens.NUMBER
                }
                'b' -> {
                    // `0b` on its own is a backwards reference to local label `0`, not an empty literal.
                    if (i + 2 < bufferEndOffset && (buffer[i + 2] == '0' || buffer[i + 2] == '1')) {
                        i += 2
                        while (i < bufferEndOffset && (buffer[i] == '0' || buffer[i] == '1' || buffer[i] == '_')) i++
                        tokenEndOffset = i
                        return ArmAsmTokens.NUMBER
                    }
                }
            }
        }

        var digitsEnd = tokenStartOffset
        while (digitsEnd < bufferEndOffset && buffer[digitsEnd].isDigit()) digitsEnd++

        val suffix = if (digitsEnd < bufferEndOffset) buffer[digitsEnd] else null
        if ((suffix == 'b' || suffix == 'f') &&
            (digitsEnd + 1 >= bufferEndOffset || !isIdentifierPart(buffer[digitsEnd + 1]))
        ) {
            tokenEndOffset = digitsEnd + 1
            return ArmAsmTokens.LOCAL_LABEL_REF
        }

        var end = digitsEnd
        if (end < bufferEndOffset && buffer[end] == '.' && end + 1 < bufferEndOffset && buffer[end + 1].isDigit()) {
            end++
            while (end < bufferEndOffset && buffer[end].isDigit()) end++
        }
        if (end < bufferEndOffset && buffer[end].lowercaseChar() == 'e') {
            var exponent = end + 1
            if (exponent < bufferEndOffset && (buffer[exponent] == '+' || buffer[exponent] == '-')) exponent++
            if (exponent < bufferEndOffset && buffer[exponent].isDigit()) {
                while (exponent < bufferEndOffset && buffer[exponent].isDigit()) exponent++
                end = exponent
            }
        }
        tokenEndOffset = end
        return ArmAsmTokens.NUMBER
    }

    private fun scanString(): IElementType {
        var i = tokenStartOffset + 1
        while (i < bufferEndOffset) {
            val c = buffer[i]
            if (c == '\\' && i + 1 < bufferEndOffset && buffer[i + 1] != '\n') {
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
        return ArmAsmTokens.STRING
    }

    private fun scanCharLiteral(): IElementType {
        var i = tokenStartOffset + 1
        if (i < bufferEndOffset && buffer[i] == '\\') {
            i++
            if (i < bufferEndOffset) i++
            while (i < bufferEndOffset && buffer[i].isDigit()) i++
        } else if (i < bufferEndOffset && buffer[i] != '\n') {
            i++
        }
        if (i < bufferEndOffset && buffer[i] == '\'') i++
        tokenEndOffset = i
        return ArmAsmTokens.CHAR
    }

    private fun scanMacroParam(): IElementType {
        var i = tokenStartOffset + 1
        if (i < bufferEndOffset) {
            when (buffer[i]) {
                '(' -> {
                    i++
                    if (i < bufferEndOffset && buffer[i] == ')') i++
                }
                '@', '\\' -> i++
                else -> while (i < bufferEndOffset && isIdentifierPart(buffer[i])) i++
            }
        }
        tokenEndOffset = i
        return ArmAsmTokens.MACRO_PARAM
    }

    /** `:lo12:` and friends wrap the symbol that follows them; the colons belong to the operator. */
    private fun scanRelocation(): IElementType {
        var i = tokenStartOffset + 1
        while (i < bufferEndOffset && isIdentifierPart(buffer[i])) i++
        if (i > tokenStartOffset + 1 && i < bufferEndOffset && buffer[i] == ':') {
            tokenEndOffset = i + 1
            return ArmAsmTokens.RELOCATION
        }
        return single(ArmAsmTokens.COLON)
    }

    // Helpers ----------------------------------------------------------------------------------

    private fun peek(offset: Int): Char? =
        (tokenStartOffset + offset).let { if (it < bufferEndOffset) buffer[it] else null }

    private fun single(type: IElementType, nextState: Int = OPERANDS): IElementType {
        tokenEndOffset = tokenStartOffset + 1
        followingState = nextState
        return type
    }

    private fun pair(type: IElementType): IElementType {
        tokenEndOffset = tokenStartOffset + 2
        return type
    }

    private fun isSymbolPart(c: Char): Boolean = isIdentifierPart(c) || c == '.'

    companion object {
        /** At the head of a statement: the next word is a label, a directive, or a mnemonic. */
        const val STATEMENT_START = 0

        /** Past the head: words are registers, symbols, or numbers. */
        const val OPERANDS = 1

        /** Between a label and its `:`, where a colon can never start a relocation specifier. */
        const val AFTER_LABEL = 2
    }
}
