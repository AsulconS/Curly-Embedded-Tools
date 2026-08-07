package com.asulcons.embedded.linker.inspections

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.Suggestions
import com.asulcons.embedded.linker.psi.LinkerMemoryRegion
import com.asulcons.embedded.linker.psi.LinkerRegionReference
import com.asulcons.embedded.linker.psi.LinkerScriptFile
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafElement

/**
 * A `> REGION` naming a region that `MEMORY` never declared.
 *
 * `ld` rejects this outright, and it is an easy typo to make when a script grows a new region, so it
 * is reported as an error with the closest declared name offered as a fix.
 */
class LinkerUnknownRegionInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val reference = element as? LinkerRegionReference ?: return
                val file = reference.containingFile as? LinkerScriptFile ?: return
                // A script with no MEMORY block places sections by address; nothing to check.
                if (file.memoryRegions.isEmpty()) return
                if (file.findMemoryRegion(reference.regionName) != null) return

                val declared = file.memoryRegions.mapNotNull { it.name }
                val suggestions = Suggestions.closestMatches(reference.regionName, declared)
                holder.registerProblem(
                    reference,
                    EmbeddedBundle.message("inspection.linker.unknownRegion.message", reference.regionName),
                    ProblemHighlightType.GENERIC_ERROR,
                    *suggestions.map { RenameRegionFix(it) }.toTypedArray(),
                )
            }
        }
}

/** `ld` requires both `ORIGIN` and `LENGTH`; omitting one is a hard error, not a default. */
class LinkerIncompleteRegionInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val region = element as? LinkerMemoryRegion ?: return
                val target = region.nameIdentifier ?: return
                val missing = buildList {
                    if (region.origin == null) add("ORIGIN")
                    if (region.length == null) add("LENGTH")
                }
                if (missing.isEmpty()) return
                holder.registerProblem(
                    target,
                    EmbeddedBundle.message(
                        "inspection.linker.incompleteRegion.message",
                        region.name.orEmpty(),
                        missing.joinToString(" and "),
                    ),
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
}

class LinkerDuplicateRegionInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val region = element as? LinkerMemoryRegion ?: return
                val name = region.name ?: return
                val file = region.containingFile as? LinkerScriptFile ?: return

                val sameName = file.memoryRegions.filter { it.name == name }
                if (sameName.size < 2) return
                if (sameName.firstOrNull() === region) return

                holder.registerProblem(
                    region.nameIdentifier ?: region,
                    EmbeddedBundle.message("inspection.linker.duplicateRegion.message", name),
                    ProblemHighlightType.GENERIC_ERROR,
                )
            }
        }
}

private class RenameRegionFix(private val replacement: String) : LocalQuickFix {

    override fun getName(): String = EmbeddedBundle.message("quickfix.changeTo", replacement)

    override fun getFamilyName(): String = EmbeddedBundle.message("quickfix.family.region")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val leaf = descriptor.psiElement.firstChild as? LeafElement
            ?: descriptor.psiElement as? LeafElement
            ?: return
        leaf.replaceWithText(replacement)
    }
}
