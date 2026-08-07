package com.asulcons.embedded.linker.parser

import com.asulcons.embedded.linker.lexer.LinkerLexer
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.asulcons.embedded.linker.psi.LinkerArgumentList
import com.asulcons.embedded.linker.psi.LinkerAssignment
import com.asulcons.embedded.linker.psi.LinkerBinaryExpression
import com.asulcons.embedded.linker.psi.LinkerCommand
import com.asulcons.embedded.linker.psi.LinkerElementTypes
import com.asulcons.embedded.linker.psi.LinkerFunctionCall
import com.asulcons.embedded.linker.psi.LinkerInputSection
import com.asulcons.embedded.linker.psi.LinkerLiteral
import com.asulcons.embedded.linker.psi.LinkerMemoryBlock
import com.asulcons.embedded.linker.psi.LinkerMemoryRegion
import com.asulcons.embedded.linker.psi.LinkerOutputSection
import com.asulcons.embedded.linker.psi.LinkerParenthesizedExpression
import com.asulcons.embedded.linker.psi.LinkerPhdrsBlock
import com.asulcons.embedded.linker.psi.LinkerRegionAttributes
import com.asulcons.embedded.linker.psi.LinkerRegionProperty
import com.asulcons.embedded.linker.psi.LinkerRegionReference
import com.asulcons.embedded.linker.psi.LinkerScriptFile
import com.asulcons.embedded.linker.psi.LinkerSectionBody
import com.asulcons.embedded.linker.psi.LinkerSectionHeader
import com.asulcons.embedded.linker.psi.LinkerSectionTrailer
import com.asulcons.embedded.linker.psi.LinkerSectionsBlock
import com.asulcons.embedded.linker.psi.LinkerSymbol
import com.asulcons.embedded.linker.psi.LinkerTernaryExpression
import com.asulcons.embedded.linker.psi.LinkerUnaryExpression
import com.asulcons.embedded.linker.psi.LinkerVersionBlock
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class LinkerParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = LinkerLexer()

    override fun createParser(project: Project?): PsiParser = LinkerParser()

    override fun getFileNodeType(): IFileElementType = LinkerElementTypes.FILE

    override fun getCommentTokens(): TokenSet = LinkerTokens.COMMENTS

    override fun getStringLiteralElements(): TokenSet = LinkerTokens.STRING_LITERALS

    override fun createFile(viewProvider: FileViewProvider): PsiFile = LinkerScriptFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = when (node.elementType) {
        LinkerElementTypes.COMMAND -> LinkerCommand(node)
        LinkerElementTypes.MEMORY_BLOCK -> LinkerMemoryBlock(node)
        LinkerElementTypes.MEMORY_REGION -> LinkerMemoryRegion(node)
        LinkerElementTypes.REGION_ATTRIBUTES -> LinkerRegionAttributes(node)
        LinkerElementTypes.REGION_PROPERTY -> LinkerRegionProperty(node)
        LinkerElementTypes.SECTIONS_BLOCK -> LinkerSectionsBlock(node)
        LinkerElementTypes.OUTPUT_SECTION -> LinkerOutputSection(node)
        LinkerElementTypes.SECTION_HEADER -> LinkerSectionHeader(node)
        LinkerElementTypes.SECTION_BODY -> LinkerSectionBody(node)
        LinkerElementTypes.SECTION_TRAILER -> LinkerSectionTrailer(node)
        LinkerElementTypes.INPUT_SECTION -> LinkerInputSection(node)
        LinkerElementTypes.PHDRS_BLOCK -> LinkerPhdrsBlock(node)
        LinkerElementTypes.VERSION_BLOCK -> LinkerVersionBlock(node)
        LinkerElementTypes.ASSIGNMENT -> LinkerAssignment(node)
        LinkerElementTypes.REGION_REFERENCE -> LinkerRegionReference(node)
        LinkerElementTypes.BINARY_EXPRESSION -> LinkerBinaryExpression(node)
        LinkerElementTypes.UNARY_EXPRESSION -> LinkerUnaryExpression(node)
        LinkerElementTypes.TERNARY_EXPRESSION -> LinkerTernaryExpression(node)
        LinkerElementTypes.PARENTHESIZED_EXPRESSION -> LinkerParenthesizedExpression(node)
        LinkerElementTypes.FUNCTION_CALL -> LinkerFunctionCall(node)
        LinkerElementTypes.ARGUMENT_LIST -> LinkerArgumentList(node)
        LinkerElementTypes.SYMBOL -> LinkerSymbol(node)
        LinkerElementTypes.LITERAL -> LinkerLiteral(node)
        else -> throw IllegalArgumentException("unexpected linker script element: ${node.elementType}")
    }
}
