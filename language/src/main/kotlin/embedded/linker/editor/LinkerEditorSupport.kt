package com.asulcons.embedded.linker.editor

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.linker.lexer.LinkerLexer
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.asulcons.embedded.linker.psi.LinkerAssignment
import com.asulcons.embedded.linker.psi.LinkerMemoryBlock
import com.asulcons.embedded.linker.psi.LinkerMemoryRegion
import com.asulcons.embedded.linker.psi.LinkerOutputSection
import com.asulcons.embedded.linker.psi.LinkerScriptFile
import com.asulcons.embedded.linker.psi.LinkerSectionsBlock
import com.asulcons.embedded.linker.spec.LinkerKeywords
import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.ASTNode
import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

/** `ld` scripts only have C-style block comments; there is no line-comment form. */
class LinkerCommenter : Commenter {
    override fun getLineCommentPrefix(): String? = null

    override fun getBlockCommentPrefix(): String = "/*"

    override fun getBlockCommentSuffix(): String = "*/"

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}

class LinkerBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS: Array<BracePair> = arrayOf(
            BracePair(LinkerTokens.LBRACE, LinkerTokens.RBRACE, true),
            BracePair(LinkerTokens.LPAREN, LinkerTokens.RPAREN, false),
            BracePair(LinkerTokens.LBRACKET, LinkerTokens.RBRACKET, false),
        )
    }
}

class LinkerNamesValidator : NamesValidator {

    override fun isKeyword(name: String, project: Project?): Boolean = name in LinkerKeywords.KEYWORD_NAMES

    override fun isIdentifier(name: String, project: Project?): Boolean =
        name.isNotEmpty() && IDENTIFIER.matches(name)

    private companion object {
        val IDENTIFIER = Regex("[A-Za-z_.$][A-Za-z0-9_.$-]*")
    }
}

class LinkerFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        LinkerLexer(),
        TokenSet.create(LinkerTokens.IDENTIFIER, LinkerTokens.KEYWORD),
        LinkerTokens.COMMENTS,
        LinkerTokens.STRING_LITERALS,
    )

    override fun canFindUsagesFor(element: PsiElement): Boolean =
        element is LinkerMemoryRegion || element is LinkerAssignment

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when (element) {
        is LinkerMemoryRegion -> "memory region"
        else -> "linker symbol"
    }

    override fun getDescriptiveName(element: PsiElement): String = when (element) {
        is LinkerMemoryRegion -> element.name.orEmpty()
        is LinkerAssignment -> element.name.orEmpty()
        else -> element.text
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = getDescriptiveName(element)
}

class LinkerFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (root !is LinkerScriptFile) return FoldingDescriptor.EMPTY_ARRAY
        val descriptors = ArrayList<FoldingDescriptor>()

        for (block in PsiTreeUtil.findChildrenOfType(root, LinkerMemoryBlock::class.java)) {
            addIfMultiLine(descriptors, block, "MEMORY { ... }")
        }
        for (block in PsiTreeUtil.findChildrenOfType(root, LinkerSectionsBlock::class.java)) {
            addIfMultiLine(descriptors, block, "SECTIONS { ... }")
        }
        for (section in PsiTreeUtil.findChildrenOfType(root, LinkerOutputSection::class.java)) {
            val body = section.body ?: continue
            addIfMultiLine(descriptors, body, "{ ... }")
        }
        for (comment in PsiTreeUtil.findChildrenOfType(root, PsiElement::class.java)) {
            if (comment.node.elementType !== LinkerTokens.BLOCK_COMMENT) continue
            addIfMultiLine(descriptors, comment, "/*...*/")
        }
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun addIfMultiLine(out: MutableList<FoldingDescriptor>, element: PsiElement, placeholder: String) {
        if (!element.textContains('\n')) return
        out += FoldingDescriptor(element.node, element.textRange, null, placeholder)
    }
}

class LinkerStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is LinkerScriptFile) return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                LinkerStructureViewModel(psiFile)

            override fun isRootNodeShown(): Boolean = false
        }
    }
}

class LinkerStructureViewModel(file: LinkerScriptFile) :
    StructureViewModelBase(file, LinkerStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    init {
        withSuitableClasses(LinkerMemoryRegion::class.java, LinkerOutputSection::class.java)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = element.value !is LinkerScriptFile
}

class LinkerStructureViewElement(private val element: PsiElement) : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) {
        (element as? Navigatable)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = (element as? Navigatable)?.canNavigate() ?: false

    override fun canNavigateToSource(): Boolean = (element as? Navigatable)?.canNavigateToSource() ?: false

    override fun getAlphaSortKey(): String = presentation.presentableText.orEmpty()

    override fun getPresentation(): ItemPresentation = when (element) {
        is LinkerMemoryRegion -> PresentationData(
            element.name.orEmpty(),
            describeRegion(element),
            AllIcons.Nodes.Package,
            null,
        )
        is LinkerOutputSection -> PresentationData(
            element.sectionName.orEmpty(),
            element.regionReferences.firstOrNull()?.regionName?.let { "> $it" },
            AllIcons.Nodes.Field,
            null,
        )
        else -> PresentationData((element as? PsiFile)?.name.orEmpty(), null, AllIcons.Nodes.Package, null)
    }

    override fun getChildren(): Array<TreeElement> {
        if (element !is LinkerScriptFile) return TreeElement.EMPTY_ARRAY
        val children = ArrayList<TreeElement>()
        element.memoryRegions.mapTo(children) { LinkerStructureViewElement(it) }
        element.outputSections.mapTo(children) { LinkerStructureViewElement(it) }
        return children.toTypedArray()
    }

    private fun describeRegion(region: LinkerMemoryRegion): String {
        val origin = region.origin?.text?.substringAfter('=')?.trim()
        val length = region.length?.text?.substringAfter('=')?.trim()
        return listOfNotNull(origin?.let { "origin $it" }, length?.let { "length $it" }).joinToString(", ")
    }
}

class LinkerDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? {
        if (file !is LinkerScriptFile || contextElement == null) return null
        return contextElement.takeIf { it.node?.elementType === LinkerTokens.KEYWORD }
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null
        if (element is LinkerMemoryRegion) {
            return section(
                element.name.orEmpty(),
                EmbeddedBundle.message("linker.doc.region"),
                "<code>${element.text.lineSequence().first().trim()}</code>",
            )
        }
        if (element.node?.elementType !== LinkerTokens.KEYWORD) return null
        val description = LinkerKeywords.describe(element.text) ?: return null
        return section(element.text, EmbeddedBundle.message("linker.doc.keyword"), description)
    }

    private fun section(name: String, kind: String, body: String): String = buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append("<b>").append(name).append("</b> — ").append(kind)
        append(DocumentationMarkup.DEFINITION_END)
        if (body.isNotEmpty()) {
            append(DocumentationMarkup.CONTENT_START)
            append(body)
            append(DocumentationMarkup.CONTENT_END)
        }
    }
}
