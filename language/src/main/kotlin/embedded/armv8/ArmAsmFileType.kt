package com.asulcons.embedded.armv8

import com.asulcons.embedded.EmbeddedBundle
import com.asulcons.embedded.EmbeddedIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object ArmAsmFileType : LanguageFileType(ArmAsmLanguage) {
    override fun getName(): String = "ARMv8 Assembly"

    override fun getDescription(): String = EmbeddedBundle.message("filetype.armAsm.description")

    override fun getDefaultExtension(): String = "s"

    override fun getIcon(): Icon = EmbeddedIcons.ArmAsmFile

    /**
     * GNU `as` accepts both `.s` and `.S` (the latter meaning "run the C preprocessor first"), and the
     * platform matches extensions case-insensitively, so a single `s` entry covers both.
     */
    const val EXTENSIONS: String = "s;asm;a64"
}
