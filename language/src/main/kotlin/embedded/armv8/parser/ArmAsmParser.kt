package com.asulcons.embedded.armv8.parser

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.psi.ArmAsmElementTypes
import com.asulcons.embedded.hasLineBreakBefore
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * Recursive-descent parser for GNU-syntax AArch64 assembly.
 *
 * Two things shape it. First, statements end at a line break rather than at a token, so every loop is
 * bounded by [atStatementEnd] instead of by a terminator. Second, GNU directives have no common
 * grammar at all (`.section .text,"ax",%progbits` next to `.macro push, reg=x0`), so directive
 * arguments are collected as balanced token runs — reporting syntax errors there would mean inventing
 * rules the assembler does not have.
 */
class ArmAsmParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val file = builder.mark()
        while (!builder.eof()) {
            if (builder.tokenType === ArmAsmTokens.SEMICOLON) {
                builder.advanceLexer()
                continue
            }
            parseStatement(builder)
        }
        file.done(root)
        return builder.treeBuilt
    }

    private fun parseStatement(b: PsiBuilder) {
        val statement = b.mark()
        val startOffset = b.currentOffset

        var sawLabel = false
        while (b.tokenType === ArmAsmTokens.LABEL) {
            parseLabelDefinition(b)
            sawLabel = true
        }

        if (!sawLabel || !atStatementEnd(b)) {
            when (b.tokenType) {
                ArmAsmTokens.DIRECTIVE -> parseDirective(b)
                ArmAsmTokens.MNEMONIC -> parseInstruction(b)
                ArmAsmTokens.PREPROCESSOR -> {
                    val preprocessor = b.mark()
                    b.advanceLexer()
                    preprocessor.done(ArmAsmElementTypes.PREPROCESSOR_STATEMENT)
                }
                null, ArmAsmTokens.SEMICOLON -> Unit
                else -> parseUnexpectedStatement(b)
            }
        }

        if (b.currentOffset == startOffset) {
            if (b.eof()) {
                statement.drop()
                return
            }
            b.advanceLexer()
        }
        statement.done(ArmAsmElementTypes.STATEMENT)
    }

    private fun parseLabelDefinition(b: PsiBuilder) {
        val label = b.mark()
        b.advanceLexer()
        if (b.tokenType === ArmAsmTokens.COLON) {
            b.advanceLexer()
        } else {
            b.error(EmbeddedBundle.message("armAsm.parser.colonExpected"))
        }
        label.done(ArmAsmElementTypes.LABEL_DEFINITION)
    }

    private fun parseInstruction(b: PsiBuilder) {
        val instruction = b.mark()
        b.advanceLexer()
        if (!atStatementEnd(b)) parseOperandList(b)
        instruction.done(ArmAsmElementTypes.INSTRUCTION)
    }

    private fun parseOperandList(b: PsiBuilder) {
        val list = b.mark()
        parseOperand(b)
        while (!atStatementEnd(b) && b.tokenType === ArmAsmTokens.COMMA) {
            b.advanceLexer()
            if (atStatementEnd(b)) {
                b.error(EmbeddedBundle.message("armAsm.parser.operandExpected"))
                break
            }
            parseOperand(b)
        }
        if (!atStatementEnd(b)) {
            val leftovers = b.mark()
            while (!atStatementEnd(b)) b.advanceLexer()
            leftovers.error(EmbeddedBundle.message("armAsm.parser.commaExpected"))
        }
        list.done(ArmAsmElementTypes.OPERAND_LIST)
    }

    private fun parseOperand(b: PsiBuilder) {
        val operand = b.mark()
        val startOffset = b.currentOffset

        val type = when (b.tokenType) {
            ArmAsmTokens.LBRACKET -> {
                parseMemoryBody(b)
                ArmAsmElementTypes.MEMORY_OPERAND
            }
            ArmAsmTokens.LBRACE -> {
                parseRegisterListBody(b)
                ArmAsmElementTypes.REGISTER_LIST
            }
            ArmAsmTokens.HASH -> {
                b.advanceLexer()
                requireExpression(b)
                ArmAsmElementTypes.IMMEDIATE_OPERAND
            }
            ArmAsmTokens.EQ -> {
                // `ldr x0, =symbol` asks the assembler for a literal-pool entry.
                b.advanceLexer()
                requireExpression(b)
                ArmAsmElementTypes.LITERAL_OPERAND
            }
            ArmAsmTokens.REGISTER -> {
                b.advanceLexer()
                parseOptionalElementIndex(b)
                ArmAsmElementTypes.REGISTER_OPERAND
            }
            ArmAsmTokens.IDENTIFIER -> {
                if (b.lookAhead(1) === ArmAsmTokens.HASH) {
                    // A shift or extend applied to the previous operand: `lsl #12`, `uxtw #2`.
                    b.advanceLexer()
                    b.advanceLexer()
                    requireExpression(b)
                    ArmAsmElementTypes.SHIFT_OPERAND
                } else {
                    parseExpression(b)
                    ArmAsmElementTypes.EXPRESSION_OPERAND
                }
            }
            else -> {
                if (!parseExpression(b)) b.error(EmbeddedBundle.message("armAsm.parser.operandExpected"))
                ArmAsmElementTypes.EXPRESSION_OPERAND
            }
        }

        if (b.currentOffset == startOffset && !b.eof()) b.advanceLexer()
        operand.done(type)
    }

    private fun parseMemoryBody(b: PsiBuilder) {
        b.advanceLexer()
        while (!atStatementEnd(b) && b.tokenType !== ArmAsmTokens.RBRACKET) {
            if (b.tokenType === ArmAsmTokens.COMMA) {
                b.advanceLexer()
                continue
            }
            val before = b.currentOffset
            parseOperand(b)
            if (b.currentOffset == before) break
        }
        if (b.tokenType === ArmAsmTokens.RBRACKET) {
            b.advanceLexer()
        } else {
            b.error(EmbeddedBundle.message("armAsm.parser.closingBracketExpected"))
        }
        // Pre-indexed addressing writes the computed address back: `[sp, #-16]!`
        if (b.tokenType === ArmAsmTokens.EXCL) b.advanceLexer()
    }

    private fun parseRegisterListBody(b: PsiBuilder) {
        b.advanceLexer()
        while (!atStatementEnd(b) && b.tokenType !== ArmAsmTokens.RBRACE) {
            if (b.tokenType === ArmAsmTokens.COMMA || b.tokenType === ArmAsmTokens.MINUS) {
                b.advanceLexer()
                continue
            }
            val before = b.currentOffset
            if (b.tokenType === ArmAsmTokens.REGISTER) {
                val register = b.mark()
                b.advanceLexer()
                parseOptionalElementIndex(b)
                register.done(ArmAsmElementTypes.REGISTER_OPERAND)
            } else if (!parseExpression(b)) {
                b.error(EmbeddedBundle.message("armAsm.parser.registerExpected"))
            }
            if (b.currentOffset == before) break
        }
        if (b.tokenType === ArmAsmTokens.RBRACE) {
            b.advanceLexer()
        } else {
            b.error(EmbeddedBundle.message("armAsm.parser.closingBraceExpected"))
        }
        parseOptionalElementIndex(b)
    }

    /** The `[3]` in `v0.s[3]` or `{v0.b}[7]`. */
    private fun parseOptionalElementIndex(b: PsiBuilder) {
        if (b.tokenType !== ArmAsmTokens.LBRACKET) return
        b.advanceLexer()
        requireExpression(b)
        if (b.tokenType === ArmAsmTokens.RBRACKET) {
            b.advanceLexer()
        } else {
            b.error(EmbeddedBundle.message("armAsm.parser.closingBracketExpected"))
        }
    }

    private fun parseDirective(b: PsiBuilder) {
        val directive = b.mark()
        b.advanceLexer()
        while (!atStatementEnd(b)) {
            val argument = b.mark()
            val startOffset = b.currentOffset
            var depth = 0
            while (!atStatementEnd(b)) {
                val token = b.tokenType
                if (depth == 0 && token === ArmAsmTokens.COMMA) break
                when (token) {
                    ArmAsmTokens.LPAREN, ArmAsmTokens.LBRACKET, ArmAsmTokens.LBRACE -> depth++
                    ArmAsmTokens.RPAREN, ArmAsmTokens.RBRACKET, ArmAsmTokens.RBRACE -> if (depth > 0) depth--
                }
                b.advanceLexer()
            }
            if (b.currentOffset == startOffset) argument.drop() else argument.done(ArmAsmElementTypes.DIRECTIVE_ARGUMENT)
            if (!atStatementEnd(b) && b.tokenType === ArmAsmTokens.COMMA) b.advanceLexer() else break
        }
        directive.done(ArmAsmElementTypes.DIRECTIVE_STATEMENT)
    }

    private fun parseUnexpectedStatement(b: PsiBuilder) {
        val junk = b.mark()
        b.advanceLexer()
        while (!atStatementEnd(b)) b.advanceLexer()
        junk.error(EmbeddedBundle.message("armAsm.parser.unexpectedToken"))
    }

    // Expressions ------------------------------------------------------------------------------

    private fun requireExpression(b: PsiBuilder) {
        if (!parseExpression(b)) b.error(EmbeddedBundle.message("armAsm.parser.expressionExpected"))
    }

    private fun parseExpression(b: PsiBuilder): Boolean = parseBinary(b, 0)

    private fun parseBinary(b: PsiBuilder, minPrecedence: Int): Boolean {
        var left = b.mark()
        if (!parseUnary(b)) {
            left.drop()
            return false
        }
        while (!atStatementEnd(b)) {
            val precedence = PRECEDENCE[b.tokenType] ?: break
            if (precedence < minPrecedence) break
            b.advanceLexer()
            if (!parseBinary(b, precedence + 1)) {
                b.error(EmbeddedBundle.message("armAsm.parser.expressionExpected"))
            }
            val enclosing = left.precede()
            left.done(ArmAsmElementTypes.BINARY_EXPRESSION)
            left = enclosing
        }
        left.drop()
        return true
    }

    private fun parseUnary(b: PsiBuilder): Boolean {
        val token = b.tokenType ?: return false
        if (token === ArmAsmTokens.MINUS || token === ArmAsmTokens.PLUS ||
            token === ArmAsmTokens.TILDE || token === ArmAsmTokens.EXCL
        ) {
            val unary = b.mark()
            b.advanceLexer()
            if (!parseUnary(b)) b.error(EmbeddedBundle.message("armAsm.parser.expressionExpected"))
            unary.done(ArmAsmElementTypes.UNARY_EXPRESSION)
            return true
        }
        return parsePrimary(b)
    }

    private fun parsePrimary(b: PsiBuilder): Boolean {
        when (b.tokenType) {
            ArmAsmTokens.LPAREN -> {
                val parenthesized = b.mark()
                b.advanceLexer()
                requireExpression(b)
                if (b.tokenType === ArmAsmTokens.RPAREN) {
                    b.advanceLexer()
                } else {
                    b.error(EmbeddedBundle.message("armAsm.parser.closingParenExpected"))
                }
                parenthesized.done(ArmAsmElementTypes.PARENTHESIZED_EXPRESSION)
            }
            ArmAsmTokens.RELOCATION -> {
                val relocated = b.mark()
                b.advanceLexer()
                if (!parseUnary(b)) b.error(EmbeddedBundle.message("armAsm.parser.symbolExpected"))
                relocated.done(ArmAsmElementTypes.RELOCATED_EXPRESSION)
            }
            ArmAsmTokens.HASH -> {
                val immediate = b.mark()
                b.advanceLexer()
                if (!parseUnary(b)) b.error(EmbeddedBundle.message("armAsm.parser.expressionExpected"))
                immediate.done(ArmAsmElementTypes.IMMEDIATE_OPERAND)
            }
            ArmAsmTokens.IDENTIFIER -> single(b, ArmAsmElementTypes.SYMBOL)
            ArmAsmTokens.LOCAL_LABEL_REF -> single(b, ArmAsmElementTypes.LOCAL_LABEL_REFERENCE)
            ArmAsmTokens.REGISTER -> single(b, ArmAsmElementTypes.REGISTER_OPERAND)
            ArmAsmTokens.NUMBER, ArmAsmTokens.STRING, ArmAsmTokens.CHAR,
            ArmAsmTokens.MACRO_PARAM, ArmAsmTokens.DOT,
            -> single(b, ArmAsmElementTypes.LITERAL)
            else -> return false
        }
        return true
    }

    private fun single(b: PsiBuilder, type: IElementType) {
        val marker = b.mark()
        b.advanceLexer()
        marker.done(type)
    }

    private fun atStatementEnd(b: PsiBuilder): Boolean =
        b.eof() || b.tokenType === ArmAsmTokens.SEMICOLON || b.hasLineBreakBefore(ArmAsmTokens.COMMENTS)

    private companion object {
        val PRECEDENCE: Map<IElementType, Int> = mapOf(
            ArmAsmTokens.OR_OR to 1,
            ArmAsmTokens.AND_AND to 1,
            ArmAsmTokens.PIPE to 2,
            ArmAsmTokens.CARET to 3,
            ArmAsmTokens.AMP to 4,
            ArmAsmTokens.EQ_EQ to 5,
            ArmAsmTokens.NE to 5,
            ArmAsmTokens.LT to 5,
            ArmAsmTokens.GT to 5,
            ArmAsmTokens.LE to 5,
            ArmAsmTokens.GE to 5,
            ArmAsmTokens.SHL to 6,
            ArmAsmTokens.SHR to 6,
            ArmAsmTokens.PLUS to 7,
            ArmAsmTokens.MINUS to 7,
            ArmAsmTokens.STAR to 8,
            ArmAsmTokens.SLASH to 8,
            ArmAsmTokens.PERCENT to 8,
        )
    }
}
