package com.asulcons.embedded.armv8.parser

import com.asulcons.embedded.armv8.lexer.ArmAsmLexer
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.psi.ArmAsmBinaryExpression
import com.asulcons.embedded.armv8.psi.ArmAsmDirective
import com.asulcons.embedded.armv8.psi.ArmAsmDirectiveArgument
import com.asulcons.embedded.armv8.psi.ArmAsmElementTypes
import com.asulcons.embedded.armv8.psi.ArmAsmExpressionOperand
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.psi.ArmAsmImmediateOperand
import com.asulcons.embedded.armv8.psi.ArmAsmInstruction
import com.asulcons.embedded.armv8.psi.ArmAsmLabelDefinition
import com.asulcons.embedded.armv8.psi.ArmAsmLiteral
import com.asulcons.embedded.armv8.psi.ArmAsmLiteralOperand
import com.asulcons.embedded.armv8.psi.ArmAsmLocalLabelReference
import com.asulcons.embedded.armv8.psi.ArmAsmMemoryOperand
import com.asulcons.embedded.armv8.psi.ArmAsmOperandList
import com.asulcons.embedded.armv8.psi.ArmAsmParenthesizedExpression
import com.asulcons.embedded.armv8.psi.ArmAsmPreprocessorStatement
import com.asulcons.embedded.armv8.psi.ArmAsmRegisterList
import com.asulcons.embedded.armv8.psi.ArmAsmRegisterOperand
import com.asulcons.embedded.armv8.psi.ArmAsmRelocatedExpression
import com.asulcons.embedded.armv8.psi.ArmAsmShiftOperand
import com.asulcons.embedded.armv8.psi.ArmAsmStatement
import com.asulcons.embedded.armv8.psi.ArmAsmSymbol
import com.asulcons.embedded.armv8.psi.ArmAsmUnaryExpression
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

class ArmAsmParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = ArmAsmLexer()

    override fun createParser(project: Project?): PsiParser = ArmAsmParser()

    override fun getFileNodeType(): IFileElementType = ArmAsmElementTypes.FILE

    override fun getCommentTokens(): TokenSet = ArmAsmTokens.COMMENTS

    override fun getStringLiteralElements(): TokenSet = ArmAsmTokens.STRING_LITERALS

    override fun createFile(viewProvider: FileViewProvider): PsiFile = ArmAsmFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = when (node.elementType) {
        ArmAsmElementTypes.STATEMENT -> ArmAsmStatement(node)
        ArmAsmElementTypes.LABEL_DEFINITION -> ArmAsmLabelDefinition(node)
        ArmAsmElementTypes.INSTRUCTION -> ArmAsmInstruction(node)
        ArmAsmElementTypes.DIRECTIVE_STATEMENT -> ArmAsmDirective(node)
        ArmAsmElementTypes.DIRECTIVE_ARGUMENT -> ArmAsmDirectiveArgument(node)
        ArmAsmElementTypes.PREPROCESSOR_STATEMENT -> ArmAsmPreprocessorStatement(node)
        ArmAsmElementTypes.OPERAND_LIST -> ArmAsmOperandList(node)
        ArmAsmElementTypes.REGISTER_OPERAND -> ArmAsmRegisterOperand(node)
        ArmAsmElementTypes.IMMEDIATE_OPERAND -> ArmAsmImmediateOperand(node)
        ArmAsmElementTypes.MEMORY_OPERAND -> ArmAsmMemoryOperand(node)
        ArmAsmElementTypes.REGISTER_LIST -> ArmAsmRegisterList(node)
        ArmAsmElementTypes.SHIFT_OPERAND -> ArmAsmShiftOperand(node)
        ArmAsmElementTypes.LITERAL_OPERAND -> ArmAsmLiteralOperand(node)
        ArmAsmElementTypes.EXPRESSION_OPERAND -> ArmAsmExpressionOperand(node)
        ArmAsmElementTypes.BINARY_EXPRESSION -> ArmAsmBinaryExpression(node)
        ArmAsmElementTypes.UNARY_EXPRESSION -> ArmAsmUnaryExpression(node)
        ArmAsmElementTypes.PARENTHESIZED_EXPRESSION -> ArmAsmParenthesizedExpression(node)
        ArmAsmElementTypes.RELOCATED_EXPRESSION -> ArmAsmRelocatedExpression(node)
        ArmAsmElementTypes.SYMBOL -> ArmAsmSymbol(node)
        ArmAsmElementTypes.LOCAL_LABEL_REFERENCE -> ArmAsmLocalLabelReference(node)
        ArmAsmElementTypes.LITERAL -> ArmAsmLiteral(node)
        else -> throw IllegalArgumentException("unexpected ARMv8 assembly element: ${node.elementType}")
    }
}
