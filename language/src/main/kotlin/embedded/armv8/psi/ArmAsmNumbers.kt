package com.asulcons.embedded.armv8.psi

import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

/** Numeric-literal handling shared by the range inspections and the "convert number base" intention. */
object ArmAsmNumbers {

    enum class Base(val displayName: String) {
        DECIMAL("decimal"),
        HEXADECIMAL("hexadecimal"),
        BINARY("binary"),
        OCTAL("octal"),
    }

    /** Parses a single literal as written; returns `null` for floats and for anything malformed. */
    fun parse(raw: String): Long? {
        val text = raw.replace("_", "")
        if (text.isEmpty()) return null
        return when {
            text.startsWith("0x", ignoreCase = true) -> parseRadix(text.substring(2), 16)
            text.startsWith("0b", ignoreCase = true) -> parseRadix(text.substring(2), 2)
            text.contains('.') || text.contains('e', ignoreCase = true) -> null
            text.length > 1 && text[0] == '0' -> parseRadix(text.substring(1), 8)
            else -> parseRadix(text, 10)
        }
    }

    fun baseOf(raw: String): Base = when {
        raw.startsWith("0x", ignoreCase = true) -> Base.HEXADECIMAL
        raw.startsWith("0b", ignoreCase = true) -> Base.BINARY
        raw.length > 1 && raw[0] == '0' && raw.drop(1).all { it.isDigit() } -> Base.OCTAL
        else -> Base.DECIMAL
    }

    fun format(value: Long, base: Base): String = when (base) {
        Base.DECIMAL -> value.toString()
        Base.HEXADECIMAL -> if (value < 0) "-0x" + java.lang.Long.toHexString(-value) else "0x" + java.lang.Long.toHexString(value)
        Base.BINARY -> if (value < 0) "-0b" + java.lang.Long.toBinaryString(-value) else "0b" + java.lang.Long.toBinaryString(value)
        Base.OCTAL -> if (value < 0) "-0" + java.lang.Long.toOctalString(-value) else "0" + java.lang.Long.toOctalString(value)
    }

    /**
     * Folds the constant an operand denotes, or `null` when it involves a symbol.
     *
     * Only leading signs and a single literal are folded — full constant folding would have to model
     * the assembler's own expression evaluator, and the inspections that use this only ever ask about
     * plain immediates such as `#4096`.
     */
    fun evaluate(element: PsiElement): Long? {
        var sign = 1L
        for (leaf in leavesOf(element)) {
            when (leaf.node.elementType) {
                TokenType.WHITE_SPACE, ArmAsmTokens.LINE_COMMENT, ArmAsmTokens.BLOCK_COMMENT,
                ArmAsmTokens.HASH, ArmAsmTokens.PLUS,
                -> Unit
                ArmAsmTokens.MINUS -> sign = -sign
                ArmAsmTokens.NUMBER -> return parse(leaf.text)?.let { it * sign }
                else -> return null
            }
        }
        return null
    }

    /** The literal token an operand is built from, if it is a single plain number. */
    fun literalLeaf(element: PsiElement): PsiElement? =
        leavesOf(element).firstOrNull { it.node.elementType === ArmAsmTokens.NUMBER }

    private fun leavesOf(element: PsiElement): List<PsiElement> {
        val leaves = ArrayList<PsiElement>()
        collectLeaves(element, leaves)
        return leaves
    }

    private fun collectLeaves(element: PsiElement, out: MutableList<PsiElement>) {
        val first = element.firstChild
        if (first == null) {
            out += element
            return
        }
        var child: PsiElement? = first
        while (child != null) {
            collectLeaves(child, out)
            child = child.nextSibling
        }
    }

    private fun parseRadix(digits: String, radix: Int): Long? {
        if (digits.isEmpty()) return null
        return try {
            java.lang.Long.parseLong(digits, radix)
        } catch (ignored: NumberFormatException) {
            // Masks such as `0xffffffffffffffff` overflow a signed long but are perfectly legal here.
            try {
                java.lang.Long.parseUnsignedLong(digits, radix)
            } catch (ignoredToo: NumberFormatException) {
                null
            }
        }
    }
}
