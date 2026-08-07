package com.asulcons.embedded.armv8.editor

import com.asulcons.embedded.armv8.psi.ArmAsmDirective
import com.asulcons.embedded.armv8.psi.ArmAsmFile
import com.asulcons.embedded.armv8.psi.ArmAsmLabelDefinition
import com.asulcons.embedded.armv8.psi.ArmAsmNamedElement
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

class ArmAsmStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is ArmAsmFile) return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                ArmAsmStructureViewModel(psiFile)

            override fun isRootNodeShown(): Boolean = false
        }
    }
}

class ArmAsmStructureViewModel(file: ArmAsmFile) :
    StructureViewModelBase(file, ArmAsmStructureViewElement(ArmAsmOutline.build(file))),
    StructureViewModel.ElementInfoProvider {

    init {
        withSuitableClasses(ArmAsmNamedElement::class.java, ArmAsmDirective::class.java)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        (element as? ArmAsmStructureViewElement)?.node?.children?.isEmpty() ?: true
}

/**
 * Groups the file's named things under the section directive that is in force where they appear, which
 * is how a bare-metal source is usually read: "what ends up in `.text.boot`, what ends up in `.data`".
 */
internal object ArmAsmOutline {

    private val SECTION_DIRECTIVES = setOf(".section", ".text", ".data", ".bss", ".rodata", ".pushsection")

    class Node(
        val element: PsiElement,
        val label: String,
        val detail: String?,
        val icon: Icon,
        val children: MutableList<Node> = ArrayList(),
    )

    fun build(file: ArmAsmFile): Node {
        val root = Node(file, file.name, null, AllIcons.Nodes.Package)
        var current = root

        for (statement in file.statements) {
            val directive = statement.directive
            val directiveName = directive?.directiveName

            if (directiveName != null && directiveName in SECTION_DIRECTIVES) {
                val name = directive.arguments.firstOrNull()?.text?.trim()?.takeIf { it.isNotEmpty() }
                    ?: directiveName
                current = Node(directive, name, directiveName, AllIcons.Nodes.Package)
                root.children += current
                continue
            }

            if (directive != null && directiveName != null) {
                val declaration = directive.arguments.firstOrNull()?.takeIf { it.isSymbolDeclaration }
                if (declaration?.name != null) {
                    val icon = if (directiveName == ".macro") AllIcons.Nodes.Class else AllIcons.Nodes.Field
                    current.children += Node(declaration, declaration.name!!, directiveName, icon)
                }
                continue
            }

            for (label in statement.labelDefinitions) {
                val name = label.name ?: continue
                // `1:`/`2:` locals are loop markers, not landmarks; they would drown out the outline.
                if (label.isNumeric) continue
                current.children += Node(label, name, null, iconFor(label))
            }
        }
        return root
    }

    private fun iconFor(label: ArmAsmLabelDefinition): Icon =
        if (label.isAssemblerLocal) AllIcons.Nodes.Variable else AllIcons.Nodes.Method
}

internal class ArmAsmStructureViewElement(val node: ArmAsmOutline.Node) :
    StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = node.element

    override fun navigate(requestFocus: Boolean) {
        (node.element as? Navigatable)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = (node.element as? Navigatable)?.canNavigate() ?: false

    override fun canNavigateToSource(): Boolean = (node.element as? Navigatable)?.canNavigateToSource() ?: false

    override fun getAlphaSortKey(): String = node.label

    override fun getPresentation(): ItemPresentation =
        PresentationData(node.label, node.detail, node.icon, null)

    override fun getChildren(): Array<TreeElement> =
        node.children.map { ArmAsmStructureViewElement(it) }.toTypedArray()
}
