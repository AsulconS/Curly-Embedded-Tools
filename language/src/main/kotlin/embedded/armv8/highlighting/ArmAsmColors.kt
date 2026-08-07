package com.asulcons.embedded.armv8.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

/**
 * Every key falls back to a default that the stock Light and Darcula schemes actually paint.
 *
 * This matters more than it looks: several obvious-sounding defaults — `FUNCTION_CALL`,
 * `PREDEFINED_SYMBOL`, `LABEL`, `PARAMETER`, `CLASS_NAME` — are left undefined by one or both stock
 * schemes and silently render as plain body text, so a category picked on name alone can end up
 * invisible. `ColorFallbackTest` pins the safe set.
 */
object ArmAsmColors {
    val LINE_COMMENT: TextAttributesKey =
        createTextAttributesKey("ARMV8_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val BLOCK_COMMENT: TextAttributesKey =
        createTextAttributesKey("ARMV8_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
    val MNEMONIC: TextAttributesKey =
        createTextAttributesKey("ARMV8_MNEMONIC", DefaultLanguageHighlighterColors.KEYWORD)
    val MACRO_CALL: TextAttributesKey =
        createTextAttributesKey("ARMV8_MACRO_CALL", DefaultLanguageHighlighterColors.CONSTANT)
    val DIRECTIVE: TextAttributesKey =
        createTextAttributesKey("ARMV8_DIRECTIVE", DefaultLanguageHighlighterColors.METADATA)
    val PREPROCESSOR: TextAttributesKey =
        createTextAttributesKey("ARMV8_PREPROCESSOR", DefaultLanguageHighlighterColors.METADATA)
    // A jump target reads like a function name, which is also what makes it stand out when scanning.
    val LABEL: TextAttributesKey =
        createTextAttributesKey("ARMV8_LABEL", DefaultLanguageHighlighterColors.STATIC_METHOD)
    val REGISTER: TextAttributesKey =
        createTextAttributesKey("ARMV8_REGISTER", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    val SYSTEM_REGISTER: TextAttributesKey =
        createTextAttributesKey("ARMV8_SYSTEM_REGISTER", DefaultLanguageHighlighterColors.STATIC_FIELD)
    val CONDITION_CODE: TextAttributesKey =
        createTextAttributesKey("ARMV8_CONDITION_CODE", DefaultLanguageHighlighterColors.KEYWORD)
    val SHIFT_OPERATOR: TextAttributesKey =
        createTextAttributesKey("ARMV8_SHIFT_OPERATOR", DefaultLanguageHighlighterColors.KEYWORD)
    val NUMBER: TextAttributesKey =
        createTextAttributesKey("ARMV8_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val STRING: TextAttributesKey =
        createTextAttributesKey("ARMV8_STRING", DefaultLanguageHighlighterColors.STRING)
    val IDENTIFIER: TextAttributesKey =
        createTextAttributesKey("ARMV8_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
    val LOCAL_LABEL_REFERENCE: TextAttributesKey =
        createTextAttributesKey("ARMV8_LOCAL_LABEL_REFERENCE", DefaultLanguageHighlighterColors.STATIC_METHOD)

    // `\reg` is a substitution marker inside macro text, which is what an escape sequence is too.
    val MACRO_PARAMETER: TextAttributesKey =
        createTextAttributesKey("ARMV8_MACRO_PARAMETER", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)

    // `:lo12:` annotates the assembler rather than the machine, like a directive does.
    val RELOCATION: TextAttributesKey =
        createTextAttributesKey("ARMV8_RELOCATION", DefaultLanguageHighlighterColors.METADATA)
    val OPERATOR: TextAttributesKey =
        createTextAttributesKey("ARMV8_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val COMMA: TextAttributesKey =
        createTextAttributesKey("ARMV8_COMMA", DefaultLanguageHighlighterColors.COMMA)
    val BRACKETS: TextAttributesKey =
        createTextAttributesKey("ARMV8_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val BRACES: TextAttributesKey =
        createTextAttributesKey("ARMV8_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val PARENTHESES: TextAttributesKey =
        createTextAttributesKey("ARMV8_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
    val BAD_CHARACTER: TextAttributesKey =
        createTextAttributesKey("ARMV8_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
}
