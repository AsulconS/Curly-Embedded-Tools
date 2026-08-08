package com.asulcons.embedded.gdb

import com.asulcons.embedded.gdb.lexer.GdbLexer
import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.intellij.lexer.Lexer
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A GDB argument is free text, and the lexer's job is to hand it back unharmed. Anything it refuses to
 * classify becomes a `BAD_CHARACTER`, which the platform paints as an error with no message behind it
 * and no quick fix to offer — so the cases here are mostly about *not* producing one.
 */
class GdbLexerTest {

    @Test
    fun `escapes in argument text are escapes, not bad characters`() {
        assertTokens(
            """echo \n--- PROCESSOR STATE ---\n""",
            GdbTokens.COMMAND to "echo",
            GdbTokens.ESCAPE_SEQUENCE to "\\n",
            GdbTokens.OPERATOR to "-",
            GdbTokens.OPERATOR to "-",
            GdbTokens.OPERATOR to "-",
            GdbTokens.IDENTIFIER to "PROCESSOR",
            GdbTokens.IDENTIFIER to "STATE",
            GdbTokens.OPERATOR to "-",
            GdbTokens.OPERATOR to "-",
            GdbTokens.OPERATOR to "-",
            GdbTokens.ESCAPE_SEQUENCE to "\\n",
        )
    }

    @Test
    fun `an escape is split out of the word it sits in`() {
        assertTokens(
            """echo done\n""",
            GdbTokens.COMMAND to "echo",
            GdbTokens.IDENTIFIER to "done",
            GdbTokens.ESCAPE_SEQUENCE to "\\n",
        )
    }

    @Test
    fun `an octal escape takes up to three digits`() {
        assertTokens(
            """echo \033[0m""",
            GdbTokens.COMMAND to "echo",
            GdbTokens.ESCAPE_SEQUENCE to "\\033",
            GdbTokens.LBRACKET to "[",
            GdbTokens.NUMBER to "0",
            GdbTokens.IDENTIFIER to "m",
        )
    }

    @Test
    fun `a trailing backslash keeps the next line on the same command`() {
        // The line break is inside the continuation token: leaving it in whitespace would drop the
        // lexer back to LINE_START and read `--enable-foo` as a command of its own.
        assertTokens(
            "set args \\\n  --verbose",
            GdbTokens.COMMAND to "set",
            GdbTokens.IDENTIFIER to "args",
            GdbTokens.LINE_CONTINUATION to "\\\n",
            GdbTokens.OPERATOR to "-",
            GdbTokens.OPERATOR to "-",
            GdbTokens.IDENTIFIER to "verbose",
        )
    }

    @Test
    fun `escapes inside a string stay part of the string`() {
        assertTokens(
            """printf "%d\n", ${'$'}pc""",
            GdbTokens.COMMAND to "printf",
            GdbTokens.STRING to "\"%d\\n\"",
            GdbTokens.COMMA to ",",
            GdbTokens.DOLLAR_VARIABLE to "\$pc",
        )
    }

    @Test
    fun `a python body is opaque, backslashes included`() {
        assertTokens(
            "python\nprint('a\\tb')\nend\n",
            GdbTokens.KW_PYTHON to "python",
            GdbTokens.RAW_BODY to "print('a\\tb')",
            GdbTokens.KW_END to "end",
        )
    }

    @Test
    fun `no argument text produces a bad character`() {
        val text = """
            echo \n--- PROCESSOR STATE ---\n
            file C:\build\kernel.elf
            set args --target \
              aarch64
            printf "sp=%p\n", ${'$'}sp
        """.trimIndent() + "\n"
        val lexer = GdbLexer()
        lexer.start(text, 0, text.length, 0)
        while (true) {
            val type = lexer.tokenType ?: break
            assertTrue(
                "bad character '${lexer.tokenText}' at ${lexer.tokenStart}",
                type !== TokenType.BAD_CHARACTER,
            )
            lexer.advance()
        }
    }

    @Test
    fun `restarting at any token boundary reproduces the same token`() {
        // Incremental relexing resumes from a token boundary with the state recorded there, so
        // `getState` has to carry the line position — the continuation token included, since it is
        // what keeps the following line in ARGUMENTS instead of LINE_START.
        val text = "echo \\n--- state ---\\n\nset args \\\n  --verbose\npython\nimport gdb\nend\n"
        val lexer = GdbLexer()
        lexer.start(text, 0, text.length, 0)
        while (lexer.tokenType != null) {
            val offset = lexer.tokenStart
            val resumed = GdbLexer()
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
        val lexer = GdbLexer()
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
