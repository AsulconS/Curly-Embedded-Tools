package com.asulcons.embedded.linker.psi

import com.asulcons.embedded.linker.LinkerScriptFileType
import com.asulcons.embedded.linker.LinkerScriptLanguage
import com.asulcons.embedded.linker.lexer.LinkerTokens
import com.asulcons.embedded.linker.spec.LinkerKeywords
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

class LinkerScriptFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, LinkerScriptLanguage) {

    override fun getFileType(): FileType = LinkerScriptFileType

    override fun toString(): String = "GNU Linker Script File"

    /** Cached against the project-wide PSI modification count; see `ArmAsmFile.cached`. */
    val memoryRegions: List<LinkerMemoryRegion>
        get() = CachedValuesManager.getCachedValue(this, REGIONS) {
            CachedValueProvider.Result.create(
                PsiTreeUtil.findChildrenOfType(this, LinkerMemoryRegion::class.java).toList(),
                PsiModificationTracker.MODIFICATION_COUNT,
            )
        }

    val outputSections: List<LinkerOutputSection>
        get() = PsiTreeUtil.findChildrenOfType(this, LinkerOutputSection::class.java).toList()

    fun findMemoryRegion(name: String): LinkerMemoryRegion? =
        memoryRegions.firstOrNull { it.name == name }

    private companion object {
        val REGIONS = Key.create<CachedValue<List<LinkerMemoryRegion>>>("linker.memoryRegions")
    }
}

open class LinkerElement(node: ASTNode) : ASTWrapperPsiElement(node)

class LinkerCommand(node: ASTNode) : LinkerElement(node) {
    val keyword: String?
        get() = node.findChildByType(LinkerTokens.KEYWORD)?.text
}

class LinkerMemoryBlock(node: ASTNode) : LinkerElement(node) {
    val regions: List<LinkerMemoryRegion>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, LinkerMemoryRegion::class.java)
}

class LinkerMemoryRegion(node: ASTNode) : LinkerElement(node), PsiNameIdentifierOwner {

    override fun getNameIdentifier(): PsiElement? = node.findChildByType(LinkerTokens.IDENTIFIER)?.psi

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        val identifier = nameIdentifier ?: return this
        identifier.replace(LinkerElementFactory.createRegionIdentifier(project, name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    val properties: List<LinkerRegionProperty>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, LinkerRegionProperty::class.java)

    val origin: LinkerRegionProperty?
        get() = properties.firstOrNull { it.propertyName in LinkerKeywords.ORIGIN_SPELLINGS }

    val length: LinkerRegionProperty?
        get() = properties.firstOrNull { it.propertyName in LinkerKeywords.LENGTH_SPELLINGS }

    /** The `(rwx)` letters, if the region declares any. */
    val attributes: String?
        get() = PsiTreeUtil.getChildOfType(this, LinkerRegionAttributes::class.java)
            ?.text
            ?.trim('(', ')')
}

class LinkerRegionAttributes(node: ASTNode) : LinkerElement(node)

class LinkerRegionProperty(node: ASTNode) : LinkerElement(node) {
    val propertyName: String?
        get() = firstChild?.text
}

class LinkerSectionsBlock(node: ASTNode) : LinkerElement(node) {
    val sections: List<LinkerOutputSection>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, LinkerOutputSection::class.java)
}

class LinkerOutputSection(node: ASTNode) : LinkerElement(node) {

    private val header: PsiElement?
        get() = PsiTreeUtil.getChildOfType(this, LinkerSectionHeader::class.java)

    val sectionNameElement: PsiElement?
        get() {
            var child = header?.firstChild
            while (child != null) {
                val type = child.node.elementType
                if (type === LinkerTokens.IDENTIFIER || type === LinkerTokens.WILDCARD ||
                    type === LinkerTokens.KEYWORD
                ) {
                    return child
                }
                child = child.nextSibling
            }
            return null
        }

    val sectionName: String?
        get() = sectionNameElement?.text

    val body: LinkerSectionBody?
        get() = PsiTreeUtil.getChildOfType(this, LinkerSectionBody::class.java)

    /** The `> REGION` reference, which is what says where the section is actually placed. */
    val regionReferences: List<LinkerRegionReference>
        get() = PsiTreeUtil.findChildrenOfType(
            PsiTreeUtil.getChildOfType(this, LinkerSectionTrailer::class.java),
            LinkerRegionReference::class.java,
        ).toList()
}

class LinkerSectionHeader(node: ASTNode) : LinkerElement(node)

class LinkerSectionBody(node: ASTNode) : LinkerElement(node)

class LinkerSectionTrailer(node: ASTNode) : LinkerElement(node)

class LinkerInputSection(node: ASTNode) : LinkerElement(node)

class LinkerPhdrsBlock(node: ASTNode) : LinkerElement(node)

class LinkerVersionBlock(node: ASTNode) : LinkerElement(node)

class LinkerAssignment(node: ASTNode) : LinkerElement(node), PsiNameIdentifierOwner {

    override fun getNameIdentifier(): PsiElement? =
        firstChild?.takeIf { it.node.elementType === LinkerTokens.IDENTIFIER }

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        val identifier = nameIdentifier ?: return this
        identifier.replace(LinkerElementFactory.createRegionIdentifier(project, name))
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    /** `. = ALIGN(8);` moves the location counter rather than defining a symbol. */
    val movesLocationCounter: Boolean
        get() = firstChild?.node?.elementType === LinkerTokens.DOT
}

class LinkerRegionReference(node: ASTNode) : LinkerElement(node) {

    val regionName: String
        get() = text

    override fun getReference(): PsiReference = LinkerRegionPsiReference(this)

    override fun getReferences(): Array<PsiReference> = arrayOf(reference)
}

class LinkerRegionPsiReference(element: LinkerRegionReference) :
    PsiReferenceBase<LinkerRegionReference>(element, TextRange(0, element.textLength), false) {

    override fun resolve(): PsiElement? =
        (element.containingFile as? LinkerScriptFile)?.findMemoryRegion(element.regionName)

    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = element.firstChild ?: return element
        identifier.replace(LinkerElementFactory.createRegionIdentifier(element.project, newElementName))
        return element
    }
}

class LinkerSymbol(node: ASTNode) : LinkerElement(node)

class LinkerLiteral(node: ASTNode) : LinkerElement(node)

class LinkerFunctionCall(node: ASTNode) : LinkerElement(node) {
    val functionName: String?
        get() = node.findChildByType(LinkerTokens.KEYWORD)?.text
}

class LinkerArgumentList(node: ASTNode) : LinkerElement(node)

class LinkerBinaryExpression(node: ASTNode) : LinkerElement(node)

class LinkerUnaryExpression(node: ASTNode) : LinkerElement(node)

class LinkerTernaryExpression(node: ASTNode) : LinkerElement(node)

class LinkerParenthesizedExpression(node: ASTNode) : LinkerElement(node)

object LinkerElementFactory {

    fun createFile(project: com.intellij.openapi.project.Project, text: String): LinkerScriptFile =
        PsiFileFactory.getInstance(project)
            .createFileFromText("_dummy_.ld", LinkerScriptFileType, text) as LinkerScriptFile

    fun createRegionIdentifier(project: com.intellij.openapi.project.Project, name: String): PsiElement {
        val file = createFile(project, "MEMORY { $name (rwx) : ORIGIN = 0, LENGTH = 0 }\n")
        return PsiTreeUtil.findChildOfType(file, LinkerMemoryRegion::class.java)?.nameIdentifier
            ?: throw IncorrectOperationException("'$name' is not a valid region name")
    }
}
