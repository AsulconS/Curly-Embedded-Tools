package com.asulcons.embedded.gdb.psi

import com.asulcons.embedded.gdb.GdbFileType
import com.asulcons.embedded.gdb.GdbLanguage
import com.asulcons.embedded.gdb.lexer.GdbTokens
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.FileViewProvider
import com.intellij.psi.LiteralTextEscaper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil

class GdbFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, GdbLanguage) {

    override fun getFileType(): FileType = GdbFileType

    override fun toString(): String = "GDB Script File"

    /** Commands the script itself defines, which are as valid as GDB's own from that point on. */
    val definedCommands: Set<String>
        get() = PsiTreeUtil.findChildrenOfType(this, GdbBlock::class.java)
            .filter { it.keywordType === GdbTokens.KW_DEFINE }
            .mapNotNullTo(HashSet()) { it.declaredName }
}

class GdbCommand(node: ASTNode) : ASTWrapperPsiElement(node) {

    val commandElement: PsiElement?
        get() = node.findChildByType(GdbTokens.COMMAND)?.psi

    /** The command word without its `/format` suffix: `x/16xw` reports as `x`. */
    val commandName: String?
        get() = commandElement?.text?.substringBefore('/')

    /**
     * `info registers`, `set architecture`, `target remote` — GDB's vocabulary is two words deep in
     * many places, and the inspection has to know the pair before it can call a command unknown.
     */
    val qualifiedName: String?
        get() {
            val head = commandName ?: return null
            val argument = firstArgumentWord ?: return null
            return "$head $argument"
        }

    val firstArgumentWord: String?
        get() {
            val arguments = PsiTreeUtil.getChildOfType(this, GdbArguments::class.java) ?: return null
            var child = arguments.firstChild
            while (child != null) {
                if (child.node.elementType === GdbTokens.IDENTIFIER) return child.text
                child = child.nextSibling
            }
            return null
        }
}

class GdbArguments(node: ASTNode) : ASTWrapperPsiElement(node)

class GdbBlock(node: ASTNode) : ASTWrapperPsiElement(node) {

    private val header: PsiElement?
        get() = PsiTreeUtil.getChildOfType(this, GdbBlockHeader::class.java)

    val keywordElement: PsiElement?
        get() {
            var child = header?.firstChild
            while (child != null) {
                if (GdbTokens.KEYWORDS.contains(child.node.elementType)) return child
                child = child.nextSibling
            }
            return null
        }

    val keywordType: IElementType?
        get() = keywordElement?.node?.elementType

    /** The name given to a `define`/`document` block. */
    val declaredName: String?
        get() {
            if (keywordType !== GdbTokens.KW_DEFINE && keywordType !== GdbTokens.KW_DOCUMENT) return null
            var child = keywordElement?.nextSibling
            while (child != null) {
                val type = child.node.elementType
                if (type === GdbTokens.IDENTIFIER || type === GdbTokens.COMMAND) return child.text
                child = child.nextSibling
            }
            return null
        }

    val body: GdbBlockBody?
        get() = PsiTreeUtil.getChildOfType(this, GdbBlockBody::class.java)

    val isClosed: Boolean
        get() = node.findChildByType(GdbTokens.KW_END) != null
}

class GdbBlockHeader(node: ASTNode) : ASTWrapperPsiElement(node)

class GdbBlockBody(node: ASTNode) : ASTWrapperPsiElement(node)

/**
 * The body of a `python` or `document` block, kept verbatim by the lexer.
 *
 * A `python` body is Python, so it acts as a language-injection host and the real Python plugin takes
 * over highlighting, completion and error reporting inside it. A `document` body is prose and stays a
 * plain opaque region.
 */
class GdbRawBody(node: ASTNode) : ASTWrapperPsiElement(node), PsiLanguageInjectionHost {

    private val block: GdbBlock?
        get() = PsiTreeUtil.getParentOfType(this, GdbBlock::class.java)

    val isPython: Boolean
        get() = block?.keywordType === GdbTokens.KW_PYTHON

    override fun isValidHost(): Boolean = isPython

    override fun updateText(text: String): PsiLanguageInjectionHost {
        val leaf = node.firstChildNode as? LeafElement ?: return this
        leaf.replaceWithText(text)
        return this
    }

    override fun createLiteralTextEscaper(): LiteralTextEscaper<out PsiLanguageInjectionHost> =
        LiteralTextEscaper.createSimple(this)
}

/**
 * Lets the platform rewrite a slice of an injected fragment, which is what makes editing inside the
 * injected Python actually write back into the `.gdbinit`.
 */
class GdbRawBodyManipulator : AbstractElementManipulator<GdbRawBody>() {
    override fun handleContentChange(element: GdbRawBody, range: TextRange, newContent: String): GdbRawBody {
        val text = element.text
        val updated = text.substring(0, range.startOffset) + newContent + text.substring(range.endOffset)
        element.updateText(updated)
        return element
    }
}
