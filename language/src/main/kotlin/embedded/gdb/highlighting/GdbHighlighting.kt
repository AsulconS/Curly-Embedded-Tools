package com.asulcons.embedded.gdb.highlighting

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.EmbeddedIcons
import com.asulcons.embedded.gdb.lexer.GdbLexer
import com.asulcons.embedded.gdb.lexer.GdbTokens
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

/** See [com.asulcons.embedded.armv8.highlighting.ArmAsmColors] on why the fallbacks are chosen this way. */
object GdbColors {
    val COMMENT: TextAttributesKey =
        createTextAttributesKey("GDB_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)

    /** A GDB command is the verb of its line — the language's vocabulary, so: keyword. */
    val COMMAND: TextAttributesKey =
        createTextAttributesKey("GDB_COMMAND", DefaultLanguageHighlighterColors.KEYWORD)
    val KEYWORD: TextAttributesKey =
        createTextAttributesKey("GDB_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    val SUBCOMMAND: TextAttributesKey =
        createTextAttributesKey("GDB_SUBCOMMAND", DefaultLanguageHighlighterColors.CONSTANT)
    val USER_COMMAND: TextAttributesKey =
        createTextAttributesKey("GDB_USER_COMMAND", DefaultLanguageHighlighterColors.STATIC_METHOD)

    /**
     * A word in command position that GDB would not recognise. Styled as a plain identifier so that a
     * typo stops *looking* like a command even before the inspection gets to it.
     */
    val UNKNOWN_COMMAND: TextAttributesKey =
        createTextAttributesKey("GDB_UNKNOWN_COMMAND", DefaultLanguageHighlighterColors.IDENTIFIER)

    /** The `/16xw` in `x/16xw`. */
    val FORMAT_SPECIFIER: TextAttributesKey =
        createTextAttributesKey("GDB_FORMAT_SPECIFIER", DefaultLanguageHighlighterColors.METADATA)
    val CONVENIENCE_VARIABLE: TextAttributesKey =
        createTextAttributesKey("GDB_CONVENIENCE_VARIABLE", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    val NUMBER: TextAttributesKey =
        createTextAttributesKey("GDB_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val STRING: TextAttributesKey =
        createTextAttributesKey("GDB_STRING", DefaultLanguageHighlighterColors.STRING)
    val IDENTIFIER: TextAttributesKey =
        createTextAttributesKey("GDB_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)

    /** The `\n` in `echo \n`, and the trailing `\` that joins the next line onto the command. */
    val ESCAPE_SEQUENCE: TextAttributesKey =
        createTextAttributesKey("GDB_ESCAPE_SEQUENCE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE)
    val OPERATOR: TextAttributesKey =
        createTextAttributesKey("GDB_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val BRACES: TextAttributesKey =
        createTextAttributesKey("GDB_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val RAW_BODY: TextAttributesKey =
        createTextAttributesKey("GDB_RAW_BODY", DefaultLanguageHighlighterColors.TEMPLATE_LANGUAGE_COLOR)
    val BAD_CHARACTER: TextAttributesKey =
        createTextAttributesKey("GDB_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
}

class GdbSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = GdbLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(KEYS[tokenType])

    private companion object {
        val KEYS: Map<IElementType, TextAttributesKey> = buildMap {
            put(GdbTokens.COMMENT, GdbColors.COMMENT)
            put(GdbTokens.COMMAND, GdbColors.COMMAND)
            put(GdbTokens.RAW_BODY, GdbColors.RAW_BODY)
            put(GdbTokens.DOLLAR_VARIABLE, GdbColors.CONVENIENCE_VARIABLE)
            put(GdbTokens.NUMBER, GdbColors.NUMBER)
            put(GdbTokens.STRING, GdbColors.STRING)
            put(GdbTokens.CHAR, GdbColors.STRING)
            put(GdbTokens.IDENTIFIER, GdbColors.IDENTIFIER)
            put(GdbTokens.ESCAPE_SEQUENCE, GdbColors.ESCAPE_SEQUENCE)
            put(GdbTokens.LINE_CONTINUATION, GdbColors.ESCAPE_SEQUENCE)
            put(GdbTokens.OPERATOR, GdbColors.OPERATOR)
            put(GdbTokens.LBRACE, GdbColors.BRACES)
            put(GdbTokens.RBRACE, GdbColors.BRACES)
            put(TokenType.BAD_CHARACTER, GdbColors.BAD_CHARACTER)
            for (keyword in GdbTokens.KEYWORDS.types) put(keyword, GdbColors.KEYWORD)
        }
    }
}

class GdbSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        GdbSyntaxHighlighter()
}

class GdbColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon = EmbeddedIcons.GdbInitFile

    override fun getHighlighter(): SyntaxHighlighter = GdbSyntaxHighlighter()

    override fun getDisplayName(): String = "GDB Script"

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = mapOf(
        "sub" to GdbColors.SUBCOMMAND,
        "user" to GdbColors.USER_COMMAND,
        "unknown" to GdbColors.UNKNOWN_COMMAND,
        "fmt" to GdbColors.FORMAT_SPECIFIER,
    )

    override fun getDemoText(): String = """
        # Attach to a QEMU instance started with -s -S
        set <sub>architecture</sub> aarch64
        set <sub>disassemble-next-line</sub> on
        target <sub>extended-remote</sub> :1234

        file build/kernel.elf
        load

        define <user>regs</user>
            info <sub>registers</sub> x0 x1 x2 x3
            x<fmt>/16xw</fmt> ${'$'}sp
        end

        <unknown>contineu</unknown>         # not a GDB command, and it no longer looks like one

        document <user>regs</user>
        Show the first argument registers and the next four instructions.
        end

        break _start
        if ${'$'}pc == 0x40000000
            printf "reset vector reached\n"
        else
            echo \n--- UNEXPECTED ENTRY POINT ---\n
        end

        continue
    """.trimIndent()

    private companion object {
        val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.command"), GdbColors.COMMAND),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.subcommand"), GdbColors.SUBCOMMAND),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.userCommand"), GdbColors.USER_COMMAND),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.unknownCommand"), GdbColors.UNKNOWN_COMMAND),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.formatSpecifier"), GdbColors.FORMAT_SPECIFIER),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.keyword"), GdbColors.KEYWORD),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.convenienceVariable"), GdbColors.CONVENIENCE_VARIABLE),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.identifier"), GdbColors.IDENTIFIER),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.number"), GdbColors.NUMBER),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.string"), GdbColors.STRING),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.escapeSequence"), GdbColors.ESCAPE_SEQUENCE),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.comment"), GdbColors.COMMENT),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.operator"), GdbColors.OPERATOR),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.braces"), GdbColors.BRACES),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.rawBody"), GdbColors.RAW_BODY),
            AttributesDescriptor(EmbeddedBundle.message("gdb.color.badCharacter"), GdbColors.BAD_CHARACTER),
        )
    }
}
