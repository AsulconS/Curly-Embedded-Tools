package com.asulcons.embedded.gdb.parser

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.asulcons.embedded.gdb.psi.GdbElementTypes
import com.asulcons.embedded.hasLineBreakBefore
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * Parser for GDB command scripts.
 *
 * The only real grammar GDB has is block nesting — `define`/`if`/`while`/`commands`/`document`/`python`
 * each run until a matching `end`. Getting that right is what makes the two mistakes people actually
 * make in a `.gdbinit` visible: a block left open, and a stray `end`. Everything else on a line is
 * arguments, and arguments are whatever the command wants them to be.
 */
class GdbParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val file = builder.mark()
        while (!builder.eof()) {
            parseElement(builder, insideIf = false)
        }
        file.done(root)
        return builder.treeBuilt
    }

    private fun parseElement(b: PsiBuilder, insideIf: Boolean) {
        val token = b.tokenType
        when {
            token == null -> return
            GdbTokens.BLOCK_OPENERS.contains(token) -> parseBlock(b)
            token === GdbTokens.KW_END -> {
                val stray = b.mark()
                b.advanceLexer()
                stray.error(EmbeddedBundle.message("gdb.parser.unexpectedEnd"))
            }
            token === GdbTokens.KW_ELSE -> {
                val stray = b.mark()
                b.advanceLexer()
                consumeToEndOfLine(b)
                if (insideIf) stray.drop() else stray.error(EmbeddedBundle.message("gdb.parser.unexpectedElse"))
            }
            else -> parseCommand(b)
        }
    }

    private fun parseBlock(b: PsiBuilder) {
        val block = b.mark()
        val opener = b.tokenType

        val header = b.mark()
        b.advanceLexer()
        consumeToEndOfLine(b)
        header.done(GdbElementTypes.BLOCK_HEADER)

        val body = b.mark()
        var closed = false
        var bodyIsEmpty = true
        while (!b.eof()) {
            if (b.tokenType === GdbTokens.KW_END) {
                closed = true
                break
            }
            if (b.tokenType === GdbTokens.RAW_BODY) {
                val raw = b.mark()
                b.advanceLexer()
                raw.done(GdbElementTypes.RAW_BODY)
                bodyIsEmpty = false
                continue
            }
            val before = b.currentOffset
            parseElement(b, insideIf = opener === GdbTokens.KW_IF)
            bodyIsEmpty = false
            if (b.currentOffset == before) b.advanceLexer()
        }
        if (bodyIsEmpty) body.drop() else body.done(GdbElementTypes.BLOCK_BODY)

        if (closed) {
            b.advanceLexer()
            consumeToEndOfLine(b)
        } else {
            b.error(EmbeddedBundle.message("gdb.parser.missingEnd", keywordText(opener)))
        }
        block.done(GdbElementTypes.BLOCK)
    }

    private fun parseCommand(b: PsiBuilder) {
        val command = b.mark()
        b.advanceLexer()
        if (!atLineEnd(b)) {
            val arguments = b.mark()
            consumeToEndOfLine(b)
            arguments.done(GdbElementTypes.ARGUMENTS)
        }
        command.done(GdbElementTypes.COMMAND)
    }

    private fun consumeToEndOfLine(b: PsiBuilder) {
        while (!atLineEnd(b)) b.advanceLexer()
    }

    private fun atLineEnd(b: PsiBuilder): Boolean = b.eof() || b.hasLineBreakBefore(GdbTokens.COMMENTS)

    private fun keywordText(opener: IElementType?): String = when (opener) {
        GdbTokens.KW_DEFINE -> "define"
        GdbTokens.KW_DOCUMENT -> "document"
        GdbTokens.KW_PYTHON -> "python"
        GdbTokens.KW_COMMANDS -> "commands"
        GdbTokens.KW_IF -> "if"
        GdbTokens.KW_WHILE -> "while"
        else -> "block"
    }
}
