package com.asulcons.embedded

import com.intellij.lang.ParserDefinition
import com.intellij.testFramework.ParsingTestCase

/**
 * Base class for the parser tests: it only pins the data path, so each language's test file is just
 * its cases.
 */
abstract class EmbeddedParsingTestCase(
    dataPath: String,
    fileExtension: String,
    vararg definitions: ParserDefinition,
) : ParsingTestCase(dataPath, fileExtension, *definitions) {

    override fun getTestDataPath(): String = "src/test/testData"
}
