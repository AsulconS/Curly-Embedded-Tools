package com.asulcons.embedded.gdb.parser

import com.asulcons.embedded.gdb.lexer.GdbLexer
import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.asulcons.embedded.gdb.psi.GdbArguments
import com.asulcons.embedded.gdb.psi.GdbBlock
import com.asulcons.embedded.gdb.psi.GdbBlockBody
import com.asulcons.embedded.gdb.psi.GdbBlockHeader
import com.asulcons.embedded.gdb.psi.GdbCommand
import com.asulcons.embedded.gdb.psi.GdbElementTypes
import com.asulcons.embedded.gdb.psi.GdbFile
import com.asulcons.embedded.gdb.psi.GdbRawBody
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

class GdbParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = GdbLexer()

    override fun createParser(project: Project?): PsiParser = GdbParser()

    override fun getFileNodeType(): IFileElementType = GdbElementTypes.FILE

    override fun getCommentTokens(): TokenSet = GdbTokens.COMMENTS

    override fun getStringLiteralElements(): TokenSet = GdbTokens.STRING_LITERALS

    override fun createFile(viewProvider: FileViewProvider): PsiFile = GdbFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = when (node.elementType) {
        GdbElementTypes.COMMAND -> GdbCommand(node)
        GdbElementTypes.ARGUMENTS -> GdbArguments(node)
        GdbElementTypes.BLOCK -> GdbBlock(node)
        GdbElementTypes.BLOCK_HEADER -> GdbBlockHeader(node)
        GdbElementTypes.BLOCK_BODY -> GdbBlockBody(node)
        GdbElementTypes.RAW_BODY -> GdbRawBody(node)
        else -> throw IllegalArgumentException("unexpected GDB script element: ${node.elementType}")
    }
}
