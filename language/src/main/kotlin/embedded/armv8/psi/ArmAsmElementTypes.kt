package com.asulcons.embedded.armv8.psi

import com.asulcons.embedded.armv8.ArmAsmLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class ArmAsmElementType(debugName: String) : IElementType(debugName, ArmAsmLanguage) {
    override fun toString(): String = "ArmAsm:" + super.toString()
}

object ArmAsmElementTypes {
    @JvmField val FILE = IFileElementType(ArmAsmLanguage)

    @JvmField val STATEMENT = ArmAsmElementType("STATEMENT")
    @JvmField val LABEL_DEFINITION = ArmAsmElementType("LABEL_DEFINITION")
    @JvmField val INSTRUCTION = ArmAsmElementType("INSTRUCTION")
    @JvmField val DIRECTIVE_STATEMENT = ArmAsmElementType("DIRECTIVE_STATEMENT")
    @JvmField val DIRECTIVE_ARGUMENT = ArmAsmElementType("DIRECTIVE_ARGUMENT")
    @JvmField val PREPROCESSOR_STATEMENT = ArmAsmElementType("PREPROCESSOR_STATEMENT")

    @JvmField val OPERAND_LIST = ArmAsmElementType("OPERAND_LIST")
    @JvmField val REGISTER_OPERAND = ArmAsmElementType("REGISTER_OPERAND")
    @JvmField val IMMEDIATE_OPERAND = ArmAsmElementType("IMMEDIATE_OPERAND")
    @JvmField val MEMORY_OPERAND = ArmAsmElementType("MEMORY_OPERAND")
    @JvmField val REGISTER_LIST = ArmAsmElementType("REGISTER_LIST")
    @JvmField val SHIFT_OPERAND = ArmAsmElementType("SHIFT_OPERAND")
    @JvmField val LITERAL_OPERAND = ArmAsmElementType("LITERAL_OPERAND")
    @JvmField val EXPRESSION_OPERAND = ArmAsmElementType("EXPRESSION_OPERAND")

    @JvmField val BINARY_EXPRESSION = ArmAsmElementType("BINARY_EXPRESSION")
    @JvmField val UNARY_EXPRESSION = ArmAsmElementType("UNARY_EXPRESSION")
    @JvmField val PARENTHESIZED_EXPRESSION = ArmAsmElementType("PARENTHESIZED_EXPRESSION")
    @JvmField val RELOCATED_EXPRESSION = ArmAsmElementType("RELOCATED_EXPRESSION")
    @JvmField val SYMBOL = ArmAsmElementType("SYMBOL")
    @JvmField val LOCAL_LABEL_REFERENCE = ArmAsmElementType("LOCAL_LABEL_REFERENCE")
    @JvmField val LITERAL = ArmAsmElementType("LITERAL")

    @JvmField val OPERANDS: TokenSet = TokenSet.create(
        REGISTER_OPERAND, IMMEDIATE_OPERAND, MEMORY_OPERAND, REGISTER_LIST,
        SHIFT_OPERAND, LITERAL_OPERAND, EXPRESSION_OPERAND,
    )
}
