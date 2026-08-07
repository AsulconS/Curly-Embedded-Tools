package com.asulcons.embedded.armv8

import com.asulcons.embedded.armv8.lexer.ArmAsmLexer
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.intellij.lexer.Lexer
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The lexer decides what a word *means* from where it sits in its statement, so these cases pin down
 * the position-sensitive parts: label vs. mnemonic, register vs. symbol, and the states that make
 * `#` a preprocessor line in one place and an immediate prefix in another.
 */
class ArmAsmLexerTest {

    @Test
    fun `label definition is distinguished from a mnemonic by the colon that follows`() {
        assertTokens(
            "_start:",
            ArmAsmTokens.LABEL to "_start",
            ArmAsmTokens.COLON to ":",
        )
        assertTokens(
            "_start",
            ArmAsmTokens.MNEMONIC to "_start",
        )
    }

    @Test
    fun `a label and an instruction can share a line`() {
        assertTokens(
            "loop: b loop",
            ArmAsmTokens.LABEL to "loop",
            ArmAsmTokens.COLON to ":",
            ArmAsmTokens.MNEMONIC to "b",
            ArmAsmTokens.IDENTIFIER to "loop",
        )
    }

    @Test
    fun `assembler-local and numeric labels are labels too`() {
        assertTokens(
            ".Lloop:",
            ArmAsmTokens.LABEL to ".Lloop",
            ArmAsmTokens.COLON to ":",
        )
        assertTokens(
            "1:",
            ArmAsmTokens.LABEL to "1",
            ArmAsmTokens.COLON to ":",
        )
    }

    @Test
    fun `registers keep their arrangement and predication suffixes`() {
        assertTokens(
            "ld1 {v0.16b}, [x1]",
            ArmAsmTokens.MNEMONIC to "ld1",
            ArmAsmTokens.LBRACE to "{",
            ArmAsmTokens.REGISTER to "v0.16b",
            ArmAsmTokens.RBRACE to "}",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.LBRACKET to "[",
            ArmAsmTokens.REGISTER to "x1",
            ArmAsmTokens.RBRACKET to "]",
        )
        assertTokens(
            "mov z0.d, p0/z, z1.d",
            ArmAsmTokens.MNEMONIC to "mov",
            ArmAsmTokens.REGISTER to "z0.d",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.REGISTER to "p0/z",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.REGISTER to "z1.d",
        )
    }

    @Test
    fun `hash starts a preprocessor line at a statement head and an immediate elsewhere`() {
        assertTokens(
            "#include <config.h>",
            ArmAsmTokens.PREPROCESSOR to "#include <config.h>",
        )
        assertTokens(
            "add x0, x1, #4095",
            ArmAsmTokens.MNEMONIC to "add",
            ArmAsmTokens.REGISTER to "x0",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.REGISTER to "x1",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.HASH to "#",
            ArmAsmTokens.NUMBER to "4095",
        )
    }

    @Test
    fun `0b is a backwards local label reference unless binary digits follow`() {
        assertTokens(
            "b 0b",
            ArmAsmTokens.MNEMONIC to "b",
            ArmAsmTokens.LOCAL_LABEL_REF to "0b",
        )
        assertTokens(
            "mov x0, #0b1010",
            ArmAsmTokens.MNEMONIC to "mov",
            ArmAsmTokens.REGISTER to "x0",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.HASH to "#",
            ArmAsmTokens.NUMBER to "0b1010",
        )
    }

    @Test
    fun `a colon inside operands opens a relocation specifier`() {
        assertTokens(
            "add x0, x0, :lo12:sym",
            ArmAsmTokens.MNEMONIC to "add",
            ArmAsmTokens.REGISTER to "x0",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.REGISTER to "x0",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.RELOCATION to ":lo12:",
            ArmAsmTokens.IDENTIFIER to "sym",
        )
    }

    @Test
    fun `directives and macro parameters are recognised`() {
        assertTokens(
            ".macro push, \\reg",
            ArmAsmTokens.DIRECTIVE to ".macro",
            ArmAsmTokens.IDENTIFIER to "push",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.MACRO_PARAM to "\\reg",
        )
    }

    @Test
    fun `a semicolon starts a new statement on the same line`() {
        assertTokens(
            "nop ; nop",
            ArmAsmTokens.MNEMONIC to "nop",
            ArmAsmTokens.SEMICOLON to ";",
            ArmAsmTokens.MNEMONIC to "nop",
        )
    }

    @Test
    fun `a trailing comment does not disturb the next statement`() {
        assertTokens(
            "mov x0, x1 // set up\nret",
            ArmAsmTokens.MNEMONIC to "mov",
            ArmAsmTokens.REGISTER to "x0",
            ArmAsmTokens.COMMA to ",",
            ArmAsmTokens.REGISTER to "x1",
            ArmAsmTokens.LINE_COMMENT to "// set up",
            ArmAsmTokens.MNEMONIC to "ret",
        )
    }

    @Test
    fun `restarting at any token boundary reproduces the same token`() {
        // Incremental relexing restarts at a token boundary using the state recorded there, so
        // `getState` has to carry the statement position or highlighting drifts after an edit.
        val text = "_start:\n\tmov x0, #1\n\tb 1f\n1:\tret\n// done\n.macro m\n\\a\n.endm\n"
        val lexer = ArmAsmLexer()
        lexer.start(text, 0, text.length, 0)
        while (lexer.tokenType != null) {
            val offset = lexer.tokenStart
            val resumed = ArmAsmLexer()
            resumed.start(text, offset, text.length, lexer.state)
            assertEquals("token type after restart at $offset", lexer.tokenType, resumed.tokenType)
            assertEquals("token end after restart at $offset", lexer.tokenEnd, resumed.tokenEnd)
            lexer.advance()
        }
    }

    private fun assertTokens(text: String, vararg expected: Pair<IElementType, String>) {
        val actual = tokenize(text)
        assertEquals(expected.map { "${it.first}='${it.second}'" }, actual)
    }

    private fun tokenize(text: String): List<String> {
        val lexer = ArmAsmLexer()
        lexer.start(text, 0, text.length, 0)
        return collect(lexer)
    }

    private fun collect(lexer: Lexer): List<String> {
        val tokens = ArrayList<String>()
        while (true) {
            val type = lexer.tokenType ?: break
            if (type !== TokenType.WHITE_SPACE) {
                tokens += "$type='${lexer.tokenText}'"
            }
            lexer.advance()
        }
        return tokens
    }
}
