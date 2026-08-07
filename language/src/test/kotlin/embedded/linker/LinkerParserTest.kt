package com.asulcons.embedded.linker

import com.asulcons.embedded.EmbeddedParsingTestCase
import com.asulcons.embedded.linker.parser.LinkerParserDefinition
import com.asulcons.embedded.linker.psi.LinkerOutputSection
import com.asulcons.embedded.linker.psi.LinkerScriptFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class LinkerParserTest : EmbeddedParsingTestCase("", "ld", LinkerParserDefinition()) {

    private val bareMetalScript = """
        OUTPUT_FORMAT("elf64-littleaarch64")
        OUTPUT_ARCH(aarch64)
        ENTRY(_start)

        MEMORY
        {
            FLASH (rx)  : ORIGIN = 0x00000000, LENGTH = 2M
            RAM   (rwx) : ORIGIN = 0x40000000, LENGTH = 128M
        }

        SECTIONS
        {
            .text : ALIGN(8)
            {
                KEEP(*(.text.boot))
                *(.text .text.*)
                *(.rodata .rodata.*)
            } > FLASH

            .data : ALIGN(8)
            {
                __data_start = .;
                *(.data .data.*)
                __data_end = .;
            } > RAM AT> FLASH

            .bss (NOLOAD) : ALIGN(16)
            {
                __bss_start = .;
                *(.bss .bss.* COMMON)
                . = ALIGN(16);
                __bss_end = .;
            } > RAM

            __stack_top = ORIGIN(RAM) + LENGTH(RAM);

            /DISCARD/ : { *(.comment) *(.note.*) }
        }
    """.trimIndent() + "\n"

    fun testBareMetalScriptParsesCleanly() {
        assertNoErrors(bareMetalScript)
    }

    fun testMemoryRegionsAreCollectedWithOriginAndLength() {
        val file = parse(bareMetalScript) as LinkerScriptFile
        assertEquals(listOf("FLASH", "RAM"), file.memoryRegions.map { it.name })
        val flash = file.findMemoryRegion("FLASH")
        assertNotNull(flash)
        assertNotNull(flash!!.origin)
        assertNotNull(flash.length)
        assertEquals("rx", flash.attributes)
    }

    fun testOutputSectionsKnowTheirRegions() {
        val file = parse(bareMetalScript) as LinkerScriptFile
        val sections = file.outputSections.associateBy { it.sectionName }
        assertEquals(
            listOf(".text", ".data", ".bss", "/DISCARD/"),
            file.outputSections.map { it.sectionName },
        )
        assertEquals(listOf("FLASH"), sections[".text"]!!.regionReferences.map { it.regionName })
        // `> RAM AT> FLASH` places the section in RAM but loads it from FLASH.
        assertEquals(listOf("RAM", "FLASH"), sections[".data"]!!.regionReferences.map { it.regionName })
    }

    fun testRegionReferenceResolvesToItsDeclaration() {
        val file = parse(bareMetalScript) as LinkerScriptFile
        val section = file.outputSections.first { it.sectionName == ".text" }
        val reference = section.regionReferences.single()
        assertSame(file.findMemoryRegion("FLASH"), reference.reference.resolve())
    }

    fun testMissingColonAfterASectionNameIsReported() {
        val errors = errorsIn("SECTIONS {\n  .text { *(.text) } > RAM\n}\n")
        assertFalse("expected an error for the missing ':'", errors.isEmpty())
    }

    fun testMissingBraceIsReported() {
        val errors = errorsIn("MEMORY {\n  RAM (rwx) : ORIGIN = 0, LENGTH = 1M\n")
        assertFalse("expected an error for the unclosed MEMORY block", errors.isEmpty())
    }

    fun testAssignmentWithoutSemicolonIsReported() {
        val errors = errorsIn("SECTIONS {\n  __end = .\n}\n")
        assertFalse("expected an error for the missing ';'", errors.isEmpty())
    }

    fun testWildcardsAreNotReadAsMultiplication() {
        val file = parse("SECTIONS { .text : { *(.text*) *libc.a:*(.rodata) } }\n")
        assertNotNull(PsiTreeUtil.findChildOfType(file, LinkerOutputSection::class.java))
        assertNoErrors(file.text)
    }

    private fun parse(text: String): PsiFile {
        val file = createPsiFile("test", text)
        ensureParsed(file)
        return file
    }

    private fun errorsIn(text: String): List<PsiErrorElement> =
        PsiTreeUtil.findChildrenOfType(parse(text), PsiErrorElement::class.java).toList()

    private fun assertNoErrors(text: String) {
        val errors = errorsIn(text)
        assertTrue(
            "unexpected syntax errors: " + errors.joinToString { "'${it.text}' — ${it.errorDescription}" },
            errors.isEmpty(),
        )
    }
}
