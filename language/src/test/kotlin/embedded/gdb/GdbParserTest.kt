package com.asulcons.embedded.gdb

import com.asulcons.embedded.EmbeddedParsingTestCase
import com.asulcons.embedded.gdb.parser.GdbParserDefinition
import com.asulcons.embedded.gdb.psi.GdbBlock
import com.asulcons.embedded.gdb.psi.GdbCommand
import com.asulcons.embedded.gdb.psi.GdbBlockBody
import com.asulcons.embedded.gdb.psi.GdbElementTypes
import com.asulcons.embedded.gdb.psi.GdbFile
import com.asulcons.embedded.gdb.psi.GdbRawBody
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

class GdbParserTest : EmbeddedParsingTestCase("", "gdb", GdbParserDefinition()) {

    fun testTypicalQemuSessionParsesCleanly() {
        assertNoErrors(
            """
                # Attach to QEMU started with -s -S
                set architecture aarch64
                set disassemble-next-line on
                target extended-remote :1234

                file build/kernel.elf
                load

                define regs
                    info registers x0 x1 x2 x3
                    x/4i ${'$'}pc
                end

                break _start
                continue
            """.trimIndent(),
        )
    }

    fun testBlocksNest() {
        val file = parse(
            """
                define outer
                    if ${'$'}pc == 0
                        while ${'$'}sp
                            stepi
                        end
                    end
                end
            """.trimIndent() + "\n",
        )
        assertEquals(3, PsiTreeUtil.findChildrenOfType(file, GdbBlock::class.java).size)
    }

    fun testUnclosedBlockIsReported() {
        val errors = errorsIn("define broken\n  stepi\n")
        assertFalse("expected an error for the block with no 'end'", errors.isEmpty())
    }

    fun testStrayEndIsReported() {
        val errors = errorsIn("continue\nend\n")
        assertFalse("expected an error for the unmatched 'end'", errors.isEmpty())
    }

    fun testElseIsOnlyValidInsideAnIf() {
        assertNoErrors("if 1\n  continue\nelse\n  quit\nend\n")
        assertFalse(errorsIn("else\n  quit\n").isEmpty())
    }

    fun testPythonBodyIsNotParsedAsGdbCommands() {
        val file = parse(
            """
                python
                import gdb
                if True:
                    gdb.execute("info registers")
                end
            """.trimIndent() + "\n",
        )
        assertTrue(errorsIn(file.text).isEmpty())
        // The body is one opaque token, so its `if`/`import` lines never become GDB commands.
        val commands = PsiTreeUtil.findChildrenOfType(file, GdbCommand::class.java)
        assertTrue("python body leaked into the GDB command tree: $commands", commands.isEmpty())
    }

    fun testAVerbatimBodyIsADirectChildOfTheBlockBody() {
        // The formatter finds `python`/`document` blocks by looking for this child and then refuses to
        // touch them; if the shape changes, reformatting silently starts reindenting the Python.
        val file = parse("python\nif True:\n    pass\nend\n")
        val body = PsiTreeUtil.findChildOfType(file, GdbBlockBody::class.java)
        assertNotNull(body)
        assertNotNull(
            "the formatter locates verbatim bodies through this child",
            body!!.node.findChildByType(GdbElementTypes.RAW_BODY),
        )
    }

    fun testAPythonBodyIsAnInjectionHostButADocumentBodyIsNot() {
        val python = parse("python\nimport gdb\nend\n")
        val pythonBody = PsiTreeUtil.findChildOfType(python, GdbRawBody::class.java)
        assertNotNull(pythonBody)
        assertTrue("a python body should host injected Python", pythonBody!!.isValidHost)
        assertEquals("import gdb", pythonBody.text)

        val document = parse("document regs\nShow the registers.\nend\n")
        val documentBody = PsiTreeUtil.findChildOfType(document, GdbRawBody::class.java)
        assertNotNull(documentBody)
        assertFalse("a document body is prose, not code", documentBody!!.isValidHost)
    }

    fun testUserDefinedCommandsAreCollected() {
        val file = parse("define reset\n  monitor system_reset\nend\n") as GdbFile
        assertEquals(setOf("reset"), file.definedCommands)
    }

    fun testCommandNameDropsItsFormatSuffix() {
        val file = parse("x/16xw ${'$'}sp\n")
        val command = PsiTreeUtil.findChildOfType(file, GdbCommand::class.java)
        assertNotNull(command)
        assertEquals("x", command!!.commandName)
    }

    fun testTwoWordCommandsExposeAQualifiedName() {
        val file = parse("target extended-remote :1234\n")
        val command = PsiTreeUtil.findChildOfType(file, GdbCommand::class.java)
        assertNotNull(command)
        assertEquals("target extended-remote", command!!.qualifiedName)
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
