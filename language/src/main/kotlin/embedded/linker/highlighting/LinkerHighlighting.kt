package com.asulcons.embedded.linker.highlighting

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.EmbeddedIcons
import com.asulcons.embedded.linker.lexer.LinkerLexer
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import javax.swing.Icon

object LinkerColors {
    val COMMENT: TextAttributesKey =
        createTextAttributesKey("LD_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
    val PREPROCESSOR: TextAttributesKey =
        createTextAttributesKey("LD_PREPROCESSOR", DefaultLanguageHighlighterColors.METADATA)
    val KEYWORD: TextAttributesKey =
        createTextAttributesKey("LD_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    val IDENTIFIER: TextAttributesKey =
        createTextAttributesKey("LD_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
    val REGION: TextAttributesKey =
        createTextAttributesKey("LD_REGION", DefaultLanguageHighlighterColors.STATIC_METHOD)
    val SECTION_NAME: TextAttributesKey =
        createTextAttributesKey("LD_SECTION_NAME", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    val WILDCARD: TextAttributesKey =
        createTextAttributesKey("LD_WILDCARD", DefaultLanguageHighlighterColors.STRING)
    // `.` is a built-in of the language, not a symbol the script declared.
    val LOCATION_COUNTER: TextAttributesKey =
        createTextAttributesKey("LD_LOCATION_COUNTER", DefaultLanguageHighlighterColors.KEYWORD)
    val NUMBER: TextAttributesKey =
        createTextAttributesKey("LD_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val STRING: TextAttributesKey =
        createTextAttributesKey("LD_STRING", DefaultLanguageHighlighterColors.STRING)
    val OPERATOR: TextAttributesKey =
        createTextAttributesKey("LD_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val BRACES: TextAttributesKey =
        createTextAttributesKey("LD_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val PARENTHESES: TextAttributesKey =
        createTextAttributesKey("LD_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
    val SEMICOLON: TextAttributesKey =
        createTextAttributesKey("LD_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val BAD_CHARACTER: TextAttributesKey =
        createTextAttributesKey("LD_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
}

class LinkerSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = LinkerLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(KEYS[tokenType])

    private companion object {
        val KEYS: Map<IElementType, TextAttributesKey> = mapOf(
            LinkerTokens.BLOCK_COMMENT to LinkerColors.COMMENT,
            LinkerTokens.PREPROCESSOR to LinkerColors.PREPROCESSOR,
            LinkerTokens.KEYWORD to LinkerColors.KEYWORD,
            LinkerTokens.IDENTIFIER to LinkerColors.IDENTIFIER,
            LinkerTokens.WILDCARD to LinkerColors.WILDCARD,
            LinkerTokens.DOT to LinkerColors.LOCATION_COUNTER,
            LinkerTokens.NUMBER to LinkerColors.NUMBER,
            LinkerTokens.STRING to LinkerColors.STRING,
            LinkerTokens.OPERATOR to LinkerColors.OPERATOR,
            LinkerTokens.ASSIGN to LinkerColors.OPERATOR,
            LinkerTokens.COMPOUND_ASSIGN to LinkerColors.OPERATOR,
            LinkerTokens.GT to LinkerColors.OPERATOR,
            LinkerTokens.LT to LinkerColors.OPERATOR,
            LinkerTokens.STAR to LinkerColors.OPERATOR,
            LinkerTokens.QUESTION to LinkerColors.OPERATOR,
            LinkerTokens.LBRACE to LinkerColors.BRACES,
            LinkerTokens.RBRACE to LinkerColors.BRACES,
            LinkerTokens.LPAREN to LinkerColors.PARENTHESES,
            LinkerTokens.RPAREN to LinkerColors.PARENTHESES,
            LinkerTokens.SEMICOLON to LinkerColors.SEMICOLON,
            TokenType.BAD_CHARACTER to LinkerColors.BAD_CHARACTER,
        )
    }
}

class LinkerSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        LinkerSyntaxHighlighter()
}

class LinkerColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon = EmbeddedIcons.LinkerScriptFile

    override fun getHighlighter(): SyntaxHighlighter = LinkerSyntaxHighlighter()

    override fun getDisplayName(): String = "GNU Linker Script"

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "region" to LinkerColors.REGION,
        "section" to LinkerColors.SECTION_NAME,
    )

    override fun getDemoText(): String = """
        /* Bare-metal AArch64 image: text in flash, data copied to RAM at boot. */
        OUTPUT_FORMAT("elf64-littleaarch64")
        OUTPUT_ARCH(aarch64)
        ENTRY(_start)

        MEMORY
        {
            <region>FLASH</region> (rx)  : ORIGIN = 0x00000000, LENGTH = 2M
            <region>RAM</region>   (rwx) : ORIGIN = 0x40000000, LENGTH = 128M
        }

        SECTIONS
        {
            <section>.text</section> : ALIGN(8)
            {
                KEEP(*(.text.boot))
                *(.text .text.*)
                *(.rodata .rodata.*)
            } > <region>FLASH</region>

            <section>.data</section> : ALIGN(8)
            {
                __data_start = .;
                *(.data .data.*)
                __data_end = .;
            } > <region>RAM</region> AT> <region>FLASH</region>

            <section>.bss</section> (NOLOAD) : ALIGN(16)
            {
                __bss_start = .;
                *(.bss .bss.* COMMON)
                . = ALIGN(16);
                __bss_end = .;
            } > <region>RAM</region>

            . = ALIGN(16);
            __stack_top = ORIGIN(RAM) + LENGTH(RAM);

            /DISCARD/ : { *(.comment) *(.note.*) }
        }
    """.trimIndent()

    private companion object {
        val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor(EmbeddedBundle.message("linker.color.keyword"), LinkerColors.KEYWORD),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.region"), LinkerColors.REGION),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.sectionName"), LinkerColors.SECTION_NAME),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.identifier"), LinkerColors.IDENTIFIER),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.wildcard"), LinkerColors.WILDCARD),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.locationCounter"), LinkerColors.LOCATION_COUNTER),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.number"), LinkerColors.NUMBER),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.string"), LinkerColors.STRING),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.comment"), LinkerColors.COMMENT),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.preprocessor"), LinkerColors.PREPROCESSOR),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.operator"), LinkerColors.OPERATOR),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.braces"), LinkerColors.BRACES),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.parentheses"), LinkerColors.PARENTHESES),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.semicolon"), LinkerColors.SEMICOLON),
            AttributesDescriptor(EmbeddedBundle.message("linker.color.badCharacter"), LinkerColors.BAD_CHARACTER),
        )
    }
}
