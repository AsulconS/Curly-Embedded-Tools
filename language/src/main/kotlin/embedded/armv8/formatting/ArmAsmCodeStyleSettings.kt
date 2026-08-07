package com.asulcons.embedded.armv8.formatting

import com.asulcons.embedded.armv8.ArmAsmLanguage
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class ArmAsmCodeStyleSettings(container: CodeStyleSettings) :
    CustomCodeStyleSettings("ArmAsmCodeStyleSettings", container) {

    /** Indent instructions and directives one level; labels always stay in column 0. */
    @JvmField
    var INDENT_DIRECTIVES: Boolean = true

    /** Pad consecutive instructions so their operands start in the same column. */
    @JvmField
    var ALIGN_OPERANDS: Boolean = true

    @JvmField
    var SPACE_AFTER_COMMA: Boolean = true

    @JvmField
    var SPACE_AROUND_OPERATORS: Boolean = true
}

class ArmAsmLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language = ArmAsmLanguage

    override fun getConfigurableDisplayName(): String = "ARMv8 Assembly"

    override fun createCustomSettings(settings: CodeStyleSettings): CustomCodeStyleSettings =
        ArmAsmCodeStyleSettings(settings)

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        // Toolchain assembly is overwhelmingly written with 8-column instruction indentation.
        indentOptions.INDENT_SIZE = 8
        indentOptions.CONTINUATION_INDENT_SIZE = 8
        indentOptions.TAB_SIZE = 8
        commonSettings.KEEP_BLANK_LINES_IN_CODE = 2
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        when (settingsType) {
            SettingsType.SPACING_SETTINGS -> {
                consumer.showCustomOption(
                    ArmAsmCodeStyleSettings::class.java,
                    "SPACE_AFTER_COMMA",
                    "After comma",
                    ASSEMBLY_GROUP,
                )
                consumer.showCustomOption(
                    ArmAsmCodeStyleSettings::class.java,
                    "SPACE_AROUND_OPERATORS",
                    "Around expression operators",
                    ASSEMBLY_GROUP,
                )
            }
            SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
                consumer.showCustomOption(
                    ArmAsmCodeStyleSettings::class.java,
                    "INDENT_DIRECTIVES",
                    "Indent directives with instructions",
                    ASSEMBLY_GROUP,
                )
                consumer.showCustomOption(
                    ArmAsmCodeStyleSettings::class.java,
                    "ALIGN_OPERANDS",
                    "Align operands in consecutive instructions",
                    ASSEMBLY_GROUP,
                )
            }
            SettingsType.BLANK_LINES_SETTINGS ->
                consumer.showStandardOptions("KEEP_BLANK_LINES_IN_CODE")
            else -> Unit
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String = CODE_SAMPLE

    private companion object {
        const val ASSEMBLY_GROUP = "Assembly"

        val CODE_SAMPLE = """
            .section .text.boot, "ax", %progbits
            .global _start
            .type _start, %function
            _start:
            mrs x0, mpidr_el1
            and x0, x0, #0xff
            cbnz x0, park
            ldr x1, =__stack_top
            mov sp, x1
            bl kernel_main
            park:
            wfe
            b park
        """.trimIndent()
    }
}
