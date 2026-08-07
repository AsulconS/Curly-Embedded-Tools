package com.asulcons.embedded.armv8.psi

import com.asulcons.embedded.armv8.ArmAsmFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException

object ArmAsmElementFactory {

    fun createFile(project: Project, text: String): ArmAsmFile =
        PsiFileFactory.getInstance(project)
            .createFileFromText("_dummy_.s", ArmAsmFileType, text) as ArmAsmFile

    /** The `foo` leaf out of a synthetic `foo:` label definition, for renames. */
    fun createLabelIdentifier(project: Project, name: String): PsiElement {
        val file = createFile(project, "$name:\n")
        return PsiTreeUtil.findChildOfType(file, ArmAsmLabelDefinition::class.java)?.nameIdentifier
            ?: throw IncorrectOperationException("'$name' is not a valid label name")
    }

    /** The `foo` leaf out of a synthetic `b foo` instruction, for renaming symbol usages. */
    fun createSymbolIdentifier(project: Project, name: String): PsiElement {
        val file = createFile(project, "\tb $name\n")
        return PsiTreeUtil.findChildOfType(file, ArmAsmSymbol::class.java)?.firstChild
            ?: throw IncorrectOperationException("'$name' is not a valid symbol name")
    }

    /** The mnemonic leaf out of a synthetic instruction, for renaming a macro at its call sites. */
    fun createMnemonic(project: Project, name: String): PsiElement {
        val file = createFile(project, "\t$name\n")
        return PsiTreeUtil.findChildOfType(file, ArmAsmInstruction::class.java)?.mnemonicElement
            ?: throw IncorrectOperationException("'$name' is not a valid macro name")
    }

    fun createStatement(project: Project, text: String): ArmAsmStatement {
        val file = createFile(project, text.trimEnd() + "\n")
        return PsiTreeUtil.findChildOfType(file, ArmAsmStatement::class.java)
            ?: throw IncorrectOperationException("cannot parse '$text' as a statement")
    }
}
