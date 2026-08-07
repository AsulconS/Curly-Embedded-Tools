package com.asulcons.embedded.armv8.formatting

import com.asulcons.embedded.armv8.ArmAsmLanguage
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.psi.codeStyle.CodeStyleSettings

class ArmAsmFormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val custom = customSettings(settings)
        val file = formattingContext.containingFile

        val root = ArmAsmBlock(
            node = file.node,
            wrap = null,
            alignment = null,
            indent = Indent.getNoneIndent(),
            spacingBuilder = ArmAsmSpacing.createBuilder(settings, custom),
            settings = custom,
            keepBlankLines = settings.getCommonSettings(ArmAsmLanguage).KEEP_BLANK_LINES_IN_CODE,
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(file, root, settings)
    }

    /**
     * Reads this language's custom settings, tolerating a settings object that never had them attached.
     *
     * `getCustomSettings` throws when the requested class was not registered on that particular
     * [CodeStyleSettings] instance, and not every caller hands us one that went through the normal
     * project pipeline — the indent auto-detector formats against a throwaway settings object, which
     * blew up on a background thread with "Unable to get or create settings of
     * #ArmAsmCodeStyleSettings" while ordinary reformatting worked fine.
     *
     * The fallback instance carries the same field defaults, so the only thing lost in that case is
     * the user's overrides, on a code path that is measuring indentation rather than applying it.
     */
    private fun customSettings(settings: CodeStyleSettings): ArmAsmCodeStyleSettings =
        settings.getCustomSettingsIfCreated(ArmAsmCodeStyleSettings::class.java)
            ?: ArmAsmCodeStyleSettings(settings)
}
