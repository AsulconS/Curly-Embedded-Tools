package com.asulcons.embedded.linker.highlighting

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.asulcons.embedded.linker.psi.LinkerMemoryRegion
import com.asulcons.embedded.linker.psi.LinkerOutputSection
import com.asulcons.embedded.linker.psi.LinkerRegionReference
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement

/**
 * Colours what the lexer cannot classify on its own — a bare identifier is a region name in one place
 * and a symbol in another — and reports the one lexical error the lexer recovers from silently.
 */
class LinkerAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when {
            element is LinkerRegionReference -> colorize(holder, element, LinkerColors.REGION)
            element is LinkerMemoryRegion ->
                element.nameIdentifier?.let { colorize(holder, it, LinkerColors.REGION) }
            element is LinkerOutputSection ->
                element.sectionNameElement?.let { colorize(holder, it, LinkerColors.SECTION_NAME) }
            element.node.elementType === LinkerTokens.BLOCK_COMMENT -> annotateComment(element, holder)
        }
    }

    private fun annotateComment(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        if (text.length >= 4 && text.endsWith("*/")) return
        holder.newAnnotation(HighlightSeverity.ERROR, EmbeddedBundle.message("linker.error.unterminatedComment"))
            .range(element)
            .create()
    }

    private fun colorize(holder: AnnotationHolder, element: PsiElement, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(key)
            .create()
    }
}
