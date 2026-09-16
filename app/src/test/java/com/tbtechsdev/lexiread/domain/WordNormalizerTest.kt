package com.tbtechsdev.lexiread.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordNormalizerTest {

    @Test
    fun wordNormalizer_running_returnsRunOrRunn() {
        val result = WordNormalizer("running")
        assertTrue(result == "run" || result == "runn")
    }

    @Test
    fun wordNormalizer_stringent_returnsStringentUnchanged() {
        val result = WordNormalizer("stringent")
        assertEquals("stringent", result)
    }

    @Test
    fun wordNormalizer_stripsPunctuationAndConvertsToLowercase() {
        val result1 = WordNormalizer("Running!")
        assertTrue(result1 == "run" || result1 == "runn")

        val result2 = WordNormalizer("\"STRINGENT\",")
        assertEquals("stringent", result2)
    }

    @Test
    fun wordNormalizer_stripsCommonSuffixes() {
        assertEquals("walk", WordNormalizer("walked"))
        assertEquals("box", WordNormalizer("boxes"))
        assertEquals("cat", WordNormalizer("cats"))
        assertEquals("quick", WordNormalizer("quickly"))
        assertEquals("fast", WordNormalizer("faster"))
        assertEquals("fast", WordNormalizer("fastest"))
    }

    @Test
    fun wordNormalizer_preservesShortWordsAndSpecialCases() {
        assertEquals("is", WordNormalizer("is"))
        assertEquals("glass", WordNormalizer("glass"))
        assertEquals("class", WordNormalizer("class"))
        assertEquals("red", WordNormalizer("red"))
    }
}
