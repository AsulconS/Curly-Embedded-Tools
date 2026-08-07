package com.asulcons.embedded.linker.intentions

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafElement

/**
 * Switches a linker-script number between a raw value and the `K`/`M`/`G` shorthand `ld` accepts.
 *
 * `LENGTH = 0x8000000` and `LENGTH = 128M` describe the same region; which one is readable depends on
 * whether you are checking it against a datasheet or against an address map.
 */
class ConvertRegionSizeIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = EmbeddedBundle.message("intention.linker.convertSize.family")

    override fun getText(): String = currentText

    private var currentText: String = EmbeddedBundle.message("intention.linker.convertSize.family")

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val number = numberAt(element) ?: return false
        val converted = convert(number.text) ?: return false
        currentText = EmbeddedBundle.message("intention.linker.convertSize.text", converted)
        return true
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val number = numberAt(element) as? LeafElement ?: return
        val converted = convert(number.text) ?: return
        number.replaceWithText(converted)
    }

    override fun startInWriteAction(): Boolean = true

    private fun numberAt(element: PsiElement): PsiElement? = when {
        element.node?.elementType === LinkerTokens.NUMBER -> element
        element.prevSibling?.node?.elementType === LinkerTokens.NUMBER -> element.prevSibling
        else -> null
    }

    /** Suffixed values expand to hexadecimal; exact multiples of 1K collapse to the shorthand. */
    private fun convert(text: String): String? {
        val suffix = text.lastOrNull()?.uppercaseChar()
        if (suffix != null && suffix in SCALES) {
            val value = parse(text.dropLast(1)) ?: return null
            val scaled = value * SCALES.getValue(suffix)
            return "0x" + java.lang.Long.toHexString(scaled)
        }

        val value = parse(text) ?: return null
        if (value <= 0) return null
        for ((letter, scale) in SCALES.entries.sortedByDescending { it.value }) {
            if (value >= scale && value % scale == 0L) return "${value / scale}$letter"
        }
        return null
    }

    private fun parse(text: String): Long? {
        val digits = text.replace("_", "")
        return try {
            if (digits.startsWith("0x", ignoreCase = true)) {
                java.lang.Long.parseLong(digits.substring(2), 16)
            } else {
                java.lang.Long.parseLong(digits)
            }
        } catch (ignored: NumberFormatException) {
            null
        }
    }

    private companion object {
        val SCALES: Map<Char, Long> = mapOf(
            'K' to 1024L,
            'M' to 1024L * 1024,
            'G' to 1024L * 1024 * 1024,
        )
    }
}
