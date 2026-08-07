package com.asulcons.embedded

import com.asulcons.embedded.armv8.highlighting.ArmAsmColors
import com.asulcons.embedded.gdb.highlighting.GdbColors
import com.asulcons.embedded.linker.highlighting.LinkerColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Guards every highlighting key against falling back to a default that the stock schemes leave blank.
 *
 * The trap is that the plausible-sounding keys are the empty ones. `FUNCTION_CALL` is defined-but-empty
 * in Darcula and absent from the Light scheme; `PREDEFINED_SYMBOL` and `LABEL` are styled in Light but
 * not Darcula; `PARAMETER` and `CLASS_NAME` are blank in both. A category wired to any of those simply
 * renders as body text, which is not a crash and not a test failure — it just quietly looks wrong,
 * which is why it needs pinning here.
 *
 * The allow-list was read off `DefaultColorSchemesManager.xml` in the platform: these are the keys that
 * carry attributes in *both* bundled schemes.
 */
class ColorFallbackTest {

    private val styledInStockSchemes = setOf(
        "DEFAULT_KEYWORD",
        "DEFAULT_IDENTIFIER",
        "DEFAULT_NUMBER",
        "DEFAULT_STRING",
        "DEFAULT_LINE_COMMENT",
        "DEFAULT_BLOCK_COMMENT",
        "DEFAULT_DOC_COMMENT",
        "DEFAULT_METADATA",
        "DEFAULT_CONSTANT",
        "DEFAULT_INSTANCE_FIELD",
        "DEFAULT_STATIC_FIELD",
        "DEFAULT_STATIC_METHOD",
        "DEFAULT_VALID_STRING_ESCAPE",
        "DEFAULT_TEMPLATE_LANGUAGE_COLOR",
        "BAD_CHARACTER",
    )

    /**
     * Punctuation is plain text in every IntelliJ language, so these are meant to inherit nothing —
     * colouring commas and braces by default would look wrong next to Java or Rust.
     */
    private val deliberatelyUnstyled = setOf(
        "ARMV8_COMMA", "ARMV8_BRACES", "ARMV8_BRACKETS", "ARMV8_PARENTHESES", "ARMV8_OPERATOR",
        "GDB_BRACES", "GDB_OPERATOR",
        "LD_BRACES", "LD_PARENTHESES", "LD_SEMICOLON", "LD_OPERATOR",
    )

    @Test
    fun `every semantic colour key falls back to something the stock schemes paint`() {
        val offenders = mutableListOf<String>()
        for ((owner, keys) in allKeys()) {
            for (key in keys) {
                if (key.externalName in deliberatelyUnstyled) continue
                val fallback = key.fallbackAttributeKey
                if (fallback == null) {
                    offenders += "$owner.${key.externalName} has no fallback at all"
                } else if (fallback.externalName !in styledInStockSchemes) {
                    offenders += "$owner.${key.externalName} -> ${fallback.externalName}"
                }
            }
        }
        if (offenders.isNotEmpty()) {
            fail(
                "these keys render as plain text in a stock scheme:\n  " +
                    offenders.joinToString("\n  ") +
                    "\npick a fallback from: " + styledInStockSchemes.sorted().joinToString(),
            )
        }
    }

    @Test
    fun `the audit actually looked at something`() {
        val total = allKeys().sumOf { it.second.size }
        assertTrue("expected the colour objects to expose their keys by reflection, saw $total", total > 40)
    }

    /**
     * Read back through the generated getters rather than Kotlin reflection, which would need
     * `kotlin-reflect` on the test classpath for no benefit here.
     */
    private fun allKeys(): List<Pair<String, List<TextAttributesKey>>> =
        listOf<Any>(ArmAsmColors, GdbColors, LinkerColors).map { owner ->
            val keys = owner.javaClass.methods
                .filter { it.parameterCount == 0 && TextAttributesKey::class.java.isAssignableFrom(it.returnType) }
                .mapNotNull { it.invoke(owner) as? TextAttributesKey }
            owner.javaClass.simpleName to keys
        }
}
