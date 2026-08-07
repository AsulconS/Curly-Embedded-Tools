package com.asulcons.embedded.linker.parser

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.hasLineBreakBefore
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.asulcons.embedded.linker.psi.LinkerElementTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * Recursive-descent parser for GNU `ld` scripts.
 *
 * `MEMORY`, `SECTIONS` and output sections have real, checkable structure, so those are parsed
 * properly and missing braces, colons and `ORIGIN`/`LENGTH` entries are reported. Input-section
 * descriptions do not — `*(EXCLUDE_FILE(*crt*) .text .text.*)` follows rules that vary by construct —
 * so they are collected as balanced token runs bounded by a line break, which is how they are written.
 */
class LinkerParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val file = builder.mark()
        while (!builder.eof()) {
            val before = builder.currentOffset
            parseTopLevel(builder)
            if (builder.currentOffset == before) builder.advanceLexer()
        }
        file.done(root)
        return builder.treeBuilt
    }

    private fun parseTopLevel(b: PsiBuilder) {
        when {
            b.tokenType === LinkerTokens.SEMICOLON -> b.advanceLexer()
            isKeyword(b, "MEMORY") -> parseMemoryBlock(b)
            isKeyword(b, "SECTIONS") -> parseSectionsBlock(b)
            isKeyword(b, "PHDRS") -> parseBracedBlock(b, LinkerElementTypes.PHDRS_BLOCK)
            isKeyword(b, "VERSION") -> parseBracedBlock(b, LinkerElementTypes.VERSION_BLOCK)
            isAssignmentStart(b) -> parseAssignment(b)
            else -> parseCommand(b)
        }
    }

    // MEMORY ------------------------------------------------------------------------------------

    private fun parseMemoryBlock(b: PsiBuilder) {
        val block = b.mark()
        b.advanceLexer()
        if (!expect(b, LinkerTokens.LBRACE, "linker.parser.openBraceExpected")) {
            block.done(LinkerElementTypes.MEMORY_BLOCK)
            return
        }
        while (!b.eof() && b.tokenType !== LinkerTokens.RBRACE) {
            val before = b.currentOffset
            parseMemoryRegion(b)
            if (b.currentOffset == before) b.advanceLexer()
        }
        expect(b, LinkerTokens.RBRACE, "linker.parser.closeBraceExpected")
        block.done(LinkerElementTypes.MEMORY_BLOCK)
    }

    private fun parseMemoryRegion(b: PsiBuilder) {
        val region = b.mark()
        if (b.tokenType !== LinkerTokens.IDENTIFIER) {
            b.error(EmbeddedBundle.message("linker.parser.regionNameExpected"))
            b.advanceLexer()
            region.done(LinkerElementTypes.MEMORY_REGION)
            return
        }
        b.advanceLexer()

        if (b.tokenType === LinkerTokens.LPAREN) {
            val attributes = b.mark()
            consumeBalancedParens(b)
            attributes.done(LinkerElementTypes.REGION_ATTRIBUTES)
        }

        if (!expect(b, LinkerTokens.COLON, "linker.parser.regionColonExpected")) {
            region.done(LinkerElementTypes.MEMORY_REGION)
            return
        }

        while (!b.eof() && b.tokenType !== LinkerTokens.RBRACE) {
            if (b.tokenType === LinkerTokens.COMMA) {
                b.advanceLexer()
                continue
            }
            if (startsNewRegion(b)) break
            val before = b.currentOffset
            parseRegionProperty(b)
            if (b.currentOffset == before) break
        }
        region.done(LinkerElementTypes.MEMORY_REGION)
    }

    /** `ORIGIN = 0x40000000` / `len = 128K`, in any of the spellings `ld` accepts. */
    private fun parseRegionProperty(b: PsiBuilder) {
        val property = b.mark()
        if (b.tokenType === LinkerTokens.KEYWORD || b.tokenType === LinkerTokens.IDENTIFIER) {
            b.advanceLexer()
        } else {
            property.drop()
            return
        }
        if (!expect(b, LinkerTokens.ASSIGN, "linker.parser.equalsExpected")) {
            property.done(LinkerElementTypes.REGION_PROPERTY)
            return
        }
        requireExpression(b)
        property.done(LinkerElementTypes.REGION_PROPERTY)
    }

    /**
     * A region declaration ends where the next one begins; `ld` uses no separator, so the next
     * `NAME (attrs) :` or `NAME :` on a fresh line is the boundary.
     */
    private fun startsNewRegion(b: PsiBuilder): Boolean {
        if (b.tokenType !== LinkerTokens.IDENTIFIER) return false
        if (!b.hasLineBreakBefore(LinkerTokens.COMMENTS)) return false
        val next = b.lookAhead(1)
        return next === LinkerTokens.COLON || next === LinkerTokens.LPAREN
    }

    // SECTIONS ----------------------------------------------------------------------------------

    private fun parseSectionsBlock(b: PsiBuilder) {
        val block = b.mark()
        b.advanceLexer()
        if (!expect(b, LinkerTokens.LBRACE, "linker.parser.openBraceExpected")) {
            block.done(LinkerElementTypes.SECTIONS_BLOCK)
            return
        }
        while (!b.eof() && b.tokenType !== LinkerTokens.RBRACE) {
            val before = b.currentOffset
            when {
                b.tokenType === LinkerTokens.SEMICOLON -> b.advanceLexer()
                isAssignmentStart(b) -> parseAssignment(b)
                isKeyword(b, "PROVIDE") || isKeyword(b, "PROVIDE_HIDDEN") ||
                    isKeyword(b, "HIDDEN") || isKeyword(b, "ASSERT") || isKeyword(b, "INCLUDE") ->
                    parseCommand(b)
                else -> parseOutputSection(b)
            }
            if (b.currentOffset == before) b.advanceLexer()
        }
        expect(b, LinkerTokens.RBRACE, "linker.parser.closeBraceExpected")
        block.done(LinkerElementTypes.SECTIONS_BLOCK)
    }

    private fun parseOutputSection(b: PsiBuilder) {
        val section = b.mark()

        val header = b.mark()
        // Name, optional address expression and optional `(TYPE)`, all the way to the mandatory colon.
        var depth = 0
        while (!b.eof()) {
            val token = b.tokenType
            if (depth == 0 && (token === LinkerTokens.COLON || token === LinkerTokens.LBRACE ||
                    token === LinkerTokens.RBRACE)
            ) {
                break
            }
            when (token) {
                LinkerTokens.LPAREN -> depth++
                LinkerTokens.RPAREN -> if (depth > 0) depth--
            }
            b.advanceLexer()
        }
        val sawColon = b.tokenType === LinkerTokens.COLON
        if (sawColon) {
            b.advanceLexer()
            // `AT(lma)`, `ALIGN(n)`, `SUBALIGN(n)`, `ONLY_IF_RO` … up to the body.
            while (!b.eof() && b.tokenType !== LinkerTokens.LBRACE && b.tokenType !== LinkerTokens.RBRACE) {
                if (b.tokenType === LinkerTokens.LPAREN) consumeBalancedParens(b) else b.advanceLexer()
            }
        } else {
            b.error(EmbeddedBundle.message("linker.parser.sectionColonExpected"))
        }
        header.done(LinkerElementTypes.SECTION_HEADER)

        if (b.tokenType === LinkerTokens.LBRACE) {
            val body = b.mark()
            b.advanceLexer()
            while (!b.eof() && b.tokenType !== LinkerTokens.RBRACE) {
                val before = b.currentOffset
                parseSectionStatement(b)
                if (b.currentOffset == before) b.advanceLexer()
            }
            expect(b, LinkerTokens.RBRACE, "linker.parser.closeBraceExpected")
            body.done(LinkerElementTypes.SECTION_BODY)
        } else if (sawColon) {
            b.error(EmbeddedBundle.message("linker.parser.openBraceExpected"))
        }

        parseSectionTrailer(b)
        section.done(LinkerElementTypes.OUTPUT_SECTION)
    }

    /** `> RAM`, `AT> FLASH`, `:phdr`, `= 0xff` — whatever follows the closing brace. */
    private fun parseSectionTrailer(b: PsiBuilder) {
        val trailer = b.mark()
        var consumed = false
        loop@ while (!b.eof()) {
            when {
                b.tokenType === LinkerTokens.GT -> {
                    b.advanceLexer()
                    parseRegionReference(b)
                    consumed = true
                }
                isKeyword(b, "AT") && b.lookAhead(1) === LinkerTokens.GT -> {
                    b.advanceLexer()
                    b.advanceLexer()
                    parseRegionReference(b)
                    consumed = true
                }
                b.tokenType === LinkerTokens.COLON -> {
                    b.advanceLexer()
                    if (b.tokenType === LinkerTokens.IDENTIFIER) b.advanceLexer()
                    consumed = true
                }
                b.tokenType === LinkerTokens.ASSIGN -> {
                    b.advanceLexer()
                    requireExpression(b)
                    consumed = true
                }
                else -> break@loop
            }
        }
        if (consumed) trailer.done(LinkerElementTypes.SECTION_TRAILER) else trailer.drop()
    }

    private fun parseRegionReference(b: PsiBuilder) {
        if (b.tokenType !== LinkerTokens.IDENTIFIER) {
            b.error(EmbeddedBundle.message("linker.parser.regionNameExpected"))
            return
        }
        val reference = b.mark()
        b.advanceLexer()
        reference.done(LinkerElementTypes.REGION_REFERENCE)
    }

    private fun parseSectionStatement(b: PsiBuilder) {
        if (isAssignmentStart(b)) {
            parseAssignment(b)
            return
        }
        val statement = b.mark()
        var depth = 0
        var closedGroup = false
        var consumed = false
        while (!b.eof()) {
            val token = b.tokenType
            if (depth == 0) {
                if (token === LinkerTokens.RBRACE) break
                if (token === LinkerTokens.SEMICOLON) {
                    b.advanceLexer()
                    consumed = true
                    break
                }
                // A finished `name(...)` group ends the statement unless another group follows,
                // as in `SORT_BY_NAME(*)(.text)`.
                if (closedGroup && token !== LinkerTokens.LPAREN) break
                if (consumed && b.hasLineBreakBefore(LinkerTokens.COMMENTS)) break
            }
            when (token) {
                LinkerTokens.LPAREN -> depth++
                LinkerTokens.RPAREN -> if (depth > 0) depth--
            }
            b.advanceLexer()
            consumed = true
            closedGroup = closedGroup || (token === LinkerTokens.RPAREN && depth == 0)
        }
        if (consumed) statement.done(LinkerElementTypes.INPUT_SECTION) else statement.drop()
    }

    // Commands and assignments ------------------------------------------------------------------

    private fun parseCommand(b: PsiBuilder) {
        val command = b.mark()
        b.advanceLexer()
        if (b.tokenType === LinkerTokens.LPAREN) consumeBalancedParens(b)
        while (!b.eof() && b.tokenType !== LinkerTokens.SEMICOLON &&
            !b.hasLineBreakBefore(LinkerTokens.COMMENTS) &&
            b.tokenType !== LinkerTokens.RBRACE && b.tokenType !== LinkerTokens.LBRACE
        ) {
            if (b.tokenType === LinkerTokens.LPAREN) consumeBalancedParens(b) else b.advanceLexer()
        }
        if (b.tokenType === LinkerTokens.SEMICOLON) b.advanceLexer()
        command.done(LinkerElementTypes.COMMAND)
    }

    private fun isAssignmentStart(b: PsiBuilder): Boolean {
        val token = b.tokenType
        if (token !== LinkerTokens.IDENTIFIER && token !== LinkerTokens.DOT) return false
        return LinkerTokens.ASSIGNMENT_OPERATORS.contains(b.lookAhead(1))
    }

    private fun parseAssignment(b: PsiBuilder) {
        val assignment = b.mark()
        b.advanceLexer()
        b.advanceLexer()
        requireExpression(b)
        if (b.tokenType === LinkerTokens.SEMICOLON) {
            b.advanceLexer()
        } else {
            b.error(EmbeddedBundle.message("linker.parser.semicolonExpected"))
        }
        assignment.done(LinkerElementTypes.ASSIGNMENT)
    }

    private fun parseBracedBlock(b: PsiBuilder, type: IElementType) {
        val block = b.mark()
        b.advanceLexer()
        if (expect(b, LinkerTokens.LBRACE, "linker.parser.openBraceExpected")) {
            var depth = 1
            while (!b.eof() && depth > 0) {
                when (b.tokenType) {
                    LinkerTokens.LBRACE -> depth++
                    LinkerTokens.RBRACE -> depth--
                }
                if (depth > 0) b.advanceLexer()
            }
            expect(b, LinkerTokens.RBRACE, "linker.parser.closeBraceExpected")
        }
        block.done(type)
    }

    // Expressions -------------------------------------------------------------------------------

    private fun requireExpression(b: PsiBuilder) {
        if (!parseTernary(b)) b.error(EmbeddedBundle.message("linker.parser.expressionExpected"))
    }

    private fun parseTernary(b: PsiBuilder): Boolean {
        val condition = b.mark()
        if (!parseBinary(b, 0)) {
            condition.drop()
            return false
        }
        if (b.tokenType !== LinkerTokens.QUESTION) {
            condition.drop()
            return true
        }
        b.advanceLexer()
        requireExpression(b)
        if (b.tokenType === LinkerTokens.COLON) {
            b.advanceLexer()
            requireExpression(b)
        } else {
            b.error(EmbeddedBundle.message("linker.parser.colonExpected"))
        }
        condition.done(LinkerElementTypes.TERNARY_EXPRESSION)
        return true
    }

    private fun parseBinary(b: PsiBuilder, minPrecedence: Int): Boolean {
        var left = b.mark()
        if (!parseUnary(b)) {
            left.drop()
            return false
        }
        while (true) {
            val precedence = precedenceOf(b) ?: break
            if (precedence < minPrecedence) break
            b.advanceLexer()
            if (!parseBinary(b, precedence + 1)) {
                b.error(EmbeddedBundle.message("linker.parser.expressionExpected"))
            }
            val enclosing = left.precede()
            left.done(LinkerElementTypes.BINARY_EXPRESSION)
            left = enclosing
        }
        left.drop()
        return true
    }

    private fun precedenceOf(b: PsiBuilder): Int? {
        val token = b.tokenType
        if (token === LinkerTokens.GT || token === LinkerTokens.LT) return 6
        if (token === LinkerTokens.STAR) return 9
        if (token !== LinkerTokens.OPERATOR) return null
        return BINARY_PRECEDENCE[b.tokenText]
    }

    private fun parseUnary(b: PsiBuilder): Boolean {
        if (b.tokenType === LinkerTokens.OPERATOR && b.tokenText in UNARY_OPERATORS) {
            val unary = b.mark()
            b.advanceLexer()
            if (!parseUnary(b)) b.error(EmbeddedBundle.message("linker.parser.expressionExpected"))
            unary.done(LinkerElementTypes.UNARY_EXPRESSION)
            return true
        }
        return parsePrimary(b)
    }

    private fun parsePrimary(b: PsiBuilder): Boolean {
        when (b.tokenType) {
            LinkerTokens.LPAREN -> {
                val parenthesized = b.mark()
                b.advanceLexer()
                requireExpression(b)
                expect(b, LinkerTokens.RPAREN, "linker.parser.closeParenExpected")
                parenthesized.done(LinkerElementTypes.PARENTHESIZED_EXPRESSION)
            }
            LinkerTokens.KEYWORD -> {
                val call = b.mark()
                b.advanceLexer()
                if (b.tokenType === LinkerTokens.LPAREN) {
                    val arguments = b.mark()
                    consumeBalancedParens(b)
                    arguments.done(LinkerElementTypes.ARGUMENT_LIST)
                }
                call.done(LinkerElementTypes.FUNCTION_CALL)
            }
            LinkerTokens.IDENTIFIER -> {
                val symbol = b.mark()
                b.advanceLexer()
                symbol.done(LinkerElementTypes.SYMBOL)
            }
            LinkerTokens.NUMBER, LinkerTokens.STRING, LinkerTokens.DOT, LinkerTokens.WILDCARD -> {
                val literal = b.mark()
                b.advanceLexer()
                literal.done(LinkerElementTypes.LITERAL)
            }
            else -> return false
        }
        return true
    }

    // Utilities ---------------------------------------------------------------------------------

    private fun consumeBalancedParens(b: PsiBuilder) {
        if (b.tokenType !== LinkerTokens.LPAREN) return
        var depth = 0
        while (!b.eof()) {
            when (b.tokenType) {
                LinkerTokens.LPAREN -> depth++
                LinkerTokens.RPAREN -> depth--
            }
            b.advanceLexer()
            if (depth == 0) return
        }
        b.error(EmbeddedBundle.message("linker.parser.closeParenExpected"))
    }

    private fun expect(b: PsiBuilder, type: IElementType, messageKey: String): Boolean {
        if (b.tokenType === type) {
            b.advanceLexer()
            return true
        }
        b.error(EmbeddedBundle.message(messageKey))
        return false
    }

    private fun isKeyword(b: PsiBuilder, name: String): Boolean =
        b.tokenType === LinkerTokens.KEYWORD && b.tokenText == name

    private companion object {
        val BINARY_PRECEDENCE: Map<String, Int> = mapOf(
            "||" to 1, "&&" to 2, "|" to 3, "^" to 4, "&" to 5,
            "==" to 6, "!=" to 6, "<=" to 6, ">=" to 6,
            "<<" to 7, ">>" to 7,
            "+" to 8, "-" to 8,
            "*" to 9, "/" to 9, "%" to 9,
        )

        val UNARY_OPERATORS = setOf("-", "+", "~", "!")
    }
}
