package com.asulcons.embedded.armv8.editor

import com.asulcons.embedded.armv8.lexer.ArmAsmLexer
import com.asulcons.embedded.armv8.lexer.ArmAsmTokens
import com.asulcons.embedded.armv8.psi.ArmAsmDirective
import com.asulcons.embedded.armv8.psi.ArmAsmDirectiveArgument
import com.asulcons.embedded.armv8.psi.ArmAsmLabelDefinition
import com.asulcons.embedded.armv8.psi.ArmAsmNamedElement
import com.asulcons.embedded.armv8.spec.A64Spec
import com.intellij.lang.BracePair
import com.intellij.lang.Commenter
import com.intellij.lang.PairedBraceMatcher
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

class ArmAsmBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        val PAIRS: Array<BracePair> = arrayOf(
            BracePair(ArmAsmTokens.LBRACKET, ArmAsmTokens.RBRACKET, false),
            BracePair(ArmAsmTokens.LBRACE, ArmAsmTokens.RBRACE, false),
            BracePair(ArmAsmTokens.LPAREN, ArmAsmTokens.RPAREN, false),
        )
    }
}

/**
 * GNU `as` uses `//` for line comments on AArch64 — `#` starts an immediate and `;` separates
 * statements, so neither can be borrowed for commenting out code.
 */
class ArmAsmCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "//"

    override fun getBlockCommentPrefix(): String = "/*"

    override fun getBlockCommentSuffix(): String = "*/"

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}

class ArmAsmNamesValidator : NamesValidator {

    override fun isKeyword(name: String, project: Project?): Boolean =
        A64Spec.isKnownMnemonic(name) || A64Spec.isKnownDirective(name)

    override fun isIdentifier(name: String, project: Project?): Boolean =
        name.isNotEmpty() && IDENTIFIER.matches(name)

    private companion object {
        // `.L`-prefixed locals and `$`-containing mangled names both have to pass.
        val IDENTIFIER = Regex("[A-Za-z_.$][A-Za-z0-9_.$]*")
    }
}

class ArmAsmFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        ArmAsmLexer(),
        TokenSet.create(ArmAsmTokens.IDENTIFIER, ArmAsmTokens.LABEL, ArmAsmTokens.MNEMONIC),
        ArmAsmTokens.COMMENTS,
        ArmAsmTokens.STRING_LITERALS,
    )

    override fun canFindUsagesFor(element: PsiElement): Boolean = element is ArmAsmNamedElement

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when (element) {
        is ArmAsmLabelDefinition -> "label"
        is ArmAsmDirectiveArgument -> {
            when ((element.parent as? ArmAsmDirective)?.directiveName) {
                ".macro" -> "macro"
                ".comm", ".lcomm" -> "common symbol"
                else -> "symbol"
            }
        }
        else -> "symbol"
    }

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? ArmAsmNamedElement)?.symbolName ?: element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String = getDescriptiveName(element)
}
