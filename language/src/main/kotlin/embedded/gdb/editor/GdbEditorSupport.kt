package com.asulcons.embedded.gdb.editor

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.asulcons.embedded.gdb.psi.GdbBlock
import com.asulcons.embedded.gdb.psi.GdbCommand
import com.asulcons.embedded.gdb.psi.GdbFile
import com.asulcons.embedded.gdb.spec.GdbCommands
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
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

class GdbCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "#"

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}

class GdbBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS: Array<BracePair> = arrayOf(
            BracePair(GdbTokens.LPAREN, GdbTokens.RPAREN, false),
            BracePair(GdbTokens.LBRACKET, GdbTokens.RBRACKET, false),
            BracePair(GdbTokens.LBRACE, GdbTokens.RBRACE, false),
        )
    }
}

class GdbFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        if (root !is GdbFile) return FoldingDescriptor.EMPTY_ARRAY
        return PsiTreeUtil.findChildrenOfType(root, GdbBlock::class.java)
            .filter { it.textContains('\n') }
            .map { FoldingDescriptor(it.node, it.textRange, null, placeholderFor(it)) }
            .toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun placeholderFor(block: GdbBlock): String =
        block.text.lineSequence().first().trim() + " ... end"
}

class GdbStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is GdbFile) return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                GdbStructureViewModel(psiFile)

            override fun isRootNodeShown(): Boolean = false
        }
    }
}

class GdbStructureViewModel(file: GdbFile) :
    StructureViewModelBase(file, GdbStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    init {
        withSuitableClasses(GdbBlock::class.java)
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = element.value !is GdbFile
}

class GdbStructureViewElement(private val element: PsiElement) : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element

    override fun navigate(requestFocus: Boolean) {
        (element as? Navigatable)?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = (element as? Navigatable)?.canNavigate() ?: false

    override fun canNavigateToSource(): Boolean = (element as? Navigatable)?.canNavigateToSource() ?: false

    override fun getAlphaSortKey(): String = presentation.presentableText ?: ""

    override fun getPresentation(): ItemPresentation = when (element) {
        is GdbBlock -> PresentationData(
            element.declaredName ?: element.keywordElement?.text.orEmpty(),
            element.keywordElement?.text,
            AllIcons.Nodes.Method,
            null,
        )
        else -> PresentationData((element as? PsiFile)?.name.orEmpty(), null, AllIcons.Nodes.Package, null)
    }

    /** Only user-defined commands and their documentation are worth an outline entry. */
    override fun getChildren(): Array<TreeElement> {
        if (element !is GdbFile) return TreeElement.EMPTY_ARRAY
        return PsiTreeUtil.getChildrenOfTypeAsList(element, GdbBlock::class.java)
            .filter { it.declaredName != null }
            .map { GdbStructureViewElement(it) }
            .toTypedArray()
    }
}

class GdbDocumentationProvider : AbstractDocumentationProvider() {

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? {
        if (file !is GdbFile || contextElement == null) return null
        return contextElement.takeIf { it.node?.elementType === GdbTokens.COMMAND }
    }

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null || element.node?.elementType !== GdbTokens.COMMAND) return null
        val name = element.text.substringBefore('/')

        val file = element.containingFile as? GdbFile
        val userDefined = file?.let { script ->
            PsiTreeUtil.findChildrenOfType(script, GdbBlock::class.java)
                .firstOrNull { it.keywordType === GdbTokens.KW_DOCUMENT && it.declaredName == name }
        }
        if (userDefined != null) {
            val body = userDefined.body?.text?.trim().orEmpty()
            return section(name, EmbeddedBundle.message("gdb.doc.userCommand"), body)
        }
        if (file?.definedCommands?.contains(name) == true) {
            return section(name, EmbeddedBundle.message("gdb.doc.userCommand"), "")
        }

        val description = GdbCommands.describe(name) ?: return null
        val parent = PsiTreeUtil.getParentOfType(element, GdbCommand::class.java)
        val subcommand = parent?.firstArgumentWord
            ?.takeIf { it in GdbCommands.subcommandsOf(name) }
            ?.let { "<p>Subcommand: <code>$name $it</code></p>" }
            .orEmpty()
        return section(name, EmbeddedBundle.message("gdb.doc.command"), description + subcommand)
    }

    private fun section(name: String, kind: String, body: String): String = buildString {
        append(DocumentationMarkup.DEFINITION_START)
        append("<b>").append(name).append("</b> — ").append(kind)
        append(DocumentationMarkup.DEFINITION_END)
        if (body.isNotEmpty()) {
            append(DocumentationMarkup.CONTENT_START)
            append(body.replace("\n", "<br>"))
            append(DocumentationMarkup.CONTENT_END)
        }
    }
}
