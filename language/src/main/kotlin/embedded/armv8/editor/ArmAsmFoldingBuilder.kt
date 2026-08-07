package com.asulcons.embedded.armv8.editor

import com.asulcons.embedded.armv8.psi.ArmAsmDirective
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.spec.A64Spec
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class ArmAsmFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val file = root as? ArmAsmFile ?: return FoldingDescriptor.EMPTY_ARRAY
        val descriptors = ArrayList<FoldingDescriptor>()
        foldMultiLineComments(file, descriptors)
        foldDirectiveRegions(file, descriptors)
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun foldMultiLineComments(file: ArmAsmFile, out: MutableList<FoldingDescriptor>) {
        for (comment in PsiTreeUtil.findChildrenOfType(file, PsiComment::class.java)) {
            val text = comment.text
            if (!text.startsWith("/*") || !text.contains('\n')) continue
            out += FoldingDescriptor(comment.node, comment.textRange, null, "/*...*/")
        }
    }

    /**
     * `.macro`/`.endm`, `.if`/`.endif` and the repeat blocks nest, so a stack gives the right pairing
     * even for a `.macro` that contains a `.if`. Unbalanced regions are simply left unfolded — the
     * inspection is what complains about them.
     */
    private fun foldDirectiveRegions(file: ArmAsmFile, out: MutableList<FoldingDescriptor>) {
        val open = ArrayDeque<ArmAsmDirective>()
        for (directive in PsiTreeUtil.findChildrenOfType(file, ArmAsmDirective::class.java)) {
            val name = directive.directiveName ?: continue
            if (name in A64Spec.REGION_OPENERS) {
                open.addLast(directive)
                continue
            }
            if (name !in A64Spec.REGION_CLOSERS) continue
            val opener = open.removeLastOrNull() ?: continue
            if (A64Spec.REGION_OPENERS[opener.directiveName] != name) continue

            if (!containsLineBreak(opener, directive)) continue
            val range = TextRange(opener.textRange.startOffset, directive.textRange.endOffset)
            out += FoldingDescriptor(opener.node, range, null, placeholderFor(opener))
        }
    }

    private fun containsLineBreak(from: PsiElement, to: PsiElement): Boolean =
        from.containingFile.text
            .substring(from.textRange.startOffset, to.textRange.endOffset)
            .contains('\n')

    private fun placeholderFor(opener: ArmAsmDirective): String {
        val header = opener.text.lineSequence().first().trim()
        return "$header ..."
    }
}
