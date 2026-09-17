package com.tbtechs.folioreader.classroom

import com.tbtechs.folioreader.data.classroom.WordMatchEngine
import com.tbtechs.folioreader.domain.model.PdfWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WordMatchEngineTest {

    private lateinit var engine: WordMatchEngine

    @Before
    fun setup() {
        engine = WordMatchEngine()
    }

    @Test
    fun `loadWords sets cursor to zero`() {
        val words = listOf(
            PdfWord("Once"),
            PdfWord("upon"),
            PdfWord("a"),
            PdfWord("time")
        )
        engine.loadWords(words)
        assertEquals(0, engine.cursorIndex)
        assertEquals(4, engine.words.size)
    }

    @Test
    fun `processSpokenText finds exact and prefix match within window`() {
        val words = listOf(
            PdfWord("Once"),
            PdfWord("upon"),
            PdfWord("a"),
            PdfWord("time"),
            PdfWord("there"),
            PdfWord("lived")
        )
        engine.loadWords(words)

        // "upon" matches index 1
        val match1 = engine.processSpokenText("upon a")
        assertEquals(1, match1)
        assertEquals(1, engine.cursorIndex)

        // next spoken text "lived"
        val match2 = engine.processSpokenText("there lived")
        assertEquals(4, match2)
        assertEquals(4, engine.cursorIndex)
    }

    @Test
    fun `processSpokenText returns null when no words match`() {
        val words = listOf(
            PdfWord("Once"),
            PdfWord("upon"),
            PdfWord("a")
        )
        engine.loadWords(words)
        val match = engine.processSpokenText("elephant dinosaur")
        assertNull(match)
        assertEquals(0, engine.cursorIndex)
    }

    @Test
    fun `advance and back correctly move cursor with boundary clamps`() {
        val words = listOf(
            PdfWord("first"),
            PdfWord("second"),
            PdfWord("third")
        )
        engine.loadWords(words)
        assertEquals(0, engine.cursorIndex)

        engine.advance(2)
        assertEquals(2, engine.cursorIndex)

        engine.advance(5)
        assertEquals(2, engine.cursorIndex)

        engine.back(1)
        assertEquals(1, engine.cursorIndex)

        engine.back(10)
        assertEquals(0, engine.cursorIndex)
    }
}
