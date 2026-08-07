package com.asulcons.embedded

import com.asulcons.embedded.armv8.spec.A64Spec
import com.asulcons.embedded.gdb.spec.GdbCommands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "did you mean" matcher behind every unknown-name quick fix.
 *
 * The cases that matter are the short names: a three-letter mnemonic only affords a budget of one
 * edit, so the distance function has to charge a transposition as one rather than two or the most
 * common typo of all gets no fix offered.
 */
class SuggestionsTest {

    @Test
    fun `a transposed mnemonic suggests the real one`() {
        assertEquals(listOf("mov"), Suggestions.closestMatches("mvo", A64Spec.ALL_MNEMONICS, limit = 1))
        assertTrue("ldr" in Suggestions.closestMatches("lrd", A64Spec.ALL_MNEMONICS))
        assertTrue("stp" in Suggestions.closestMatches("spt", A64Spec.ALL_MNEMONICS))
    }

    @Test
    fun `a transposed region name suggests the declared one`() {
        assertEquals(listOf("SRAM"), Suggestions.closestMatches("SRMA", listOf("FLASH", "RAM", "SRAM")))
    }

    @Test
    fun `a missing or extra character is matched`() {
        assertTrue(".section" in Suggestions.closestMatches(".secton", A64Spec.DIRECTIVES))
        assertTrue(".global" in Suggestions.closestMatches(".globall", A64Spec.DIRECTIVES))
        assertTrue("continue" in Suggestions.closestMatches("contineu", GdbCommands.COMMANDS.keys))
    }

    @Test
    fun `unrelated words suggest nothing`() {
        assertEquals(emptyList<String>(), Suggestions.closestMatches("qqqqqqqq", A64Spec.ALL_MNEMONICS))
        assertEquals(emptyList<String>(), Suggestions.closestMatches("", A64Spec.ALL_MNEMONICS))
    }

    @Test
    fun `results are ordered by closeness and capped`() {
        val matches = Suggestions.closestMatches("addd", A64Spec.ALL_MNEMONICS, limit = 3)
        assertTrue(matches.size <= 3)
        assertEquals("add", matches.first())
    }

    @Test
    fun `an exact match costs nothing and comes first`() {
        assertEquals("mov", Suggestions.closestMatches("mov", A64Spec.ALL_MNEMONICS, limit = 1).single())
    }
}
