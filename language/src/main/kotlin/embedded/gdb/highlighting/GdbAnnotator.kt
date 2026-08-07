package com.asulcons.embedded.gdb.highlighting

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.asulcons.embedded.gdb.psi.GdbCommand
import com.asulcons.embedded.gdb.psi.GdbFile
import com.asulcons.embedded.gdb.spec.GdbCommands
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Colours a command line by what its parts actually are.
 *
 * The lexer can only say "this is the first word of a line"; whether that word is a GDB built-in, a
 * command the script defined itself, or a typo needs the command table and the file. Splitting those
 * three apart is what makes a misspelling visible at a glance — it stops being rendered as a command.
 */
class GdbAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when {
            element is GdbCommand -> annotateCommand(element, holder)
            element.node.elementType === GdbTokens.STRING -> annotateString(element, holder)
        }
    }

    private fun annotateCommand(command: GdbCommand, holder: AnnotationHolder) {
        val nameElement = command.commandElement ?: return
        val name = command.commandName ?: return
        val file = command.containingFile as? GdbFile

        val nameStart = nameElement.textRange.startOffset
        colorize(holder, TextRange(nameStart, nameStart + name.length), categoryOf(name, file))

        // `x/16xw` is one token; the format suffix is not part of the command name.
        if (nameElement.textLength > name.length) {
            colorize(
                holder,
                TextRange(nameStart + name.length, nameElement.textRange.endOffset),
                GdbColors.FORMAT_SPECIFIER,
            )
        }

        annotateSubcommand(command, name, nameElement.textLength, holder)
    }

    private fun categoryOf(name: String, file: GdbFile?): TextAttributesKey = when {
        file != null && name in file.definedCommands -> GdbColors.USER_COMMAND
        GdbCommands.isKnownCommand(name) -> GdbColors.COMMAND
        else -> GdbColors.UNKNOWN_COMMAND
    }

    /** `set architecture`, `info registers`, `target remote`: the second word completes the command. */
    private fun annotateSubcommand(
        command: GdbCommand,
        name: String,
        searchFrom: Int,
        holder: AnnotationHolder,
    ) {
        val subcommands = GdbCommands.subcommandsOf(name)
        if (subcommands.isEmpty()) return
        val argument = command.firstArgumentWord ?: return
        if (argument !in subcommands) return

        val offset = command.text.indexOf(argument, startIndex = searchFrom)
        if (offset < 0) return
        val start = command.textRange.startOffset + offset
        colorize(holder, TextRange(start, start + argument.length), GdbColors.SUBCOMMAND)
    }

    private fun annotateString(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        if (text.length >= 2 && text.endsWith("\"") && !text.endsWith("\\\"")) return
        holder.newAnnotation(HighlightSeverity.ERROR, EmbeddedBundle.message("gdb.error.unterminatedString"))
            .range(element)
            .create()
    }

    private fun colorize(holder: AnnotationHolder, range: TextRange, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(key)
            .create()
    }
}
