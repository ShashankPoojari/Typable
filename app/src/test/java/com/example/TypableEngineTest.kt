package com.example

import com.example.engine.DocumentEngine
import com.example.formats.DocxFormat
import com.example.formats.HtmlFormat
import com.example.formats.MarkdownFormat
import com.example.formats.PlainTextFormat
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class TypableEngineTest {

    @Test
    fun testWordCountAndStatisticsCalculation() {
        val doc = TypableDocument(
            title = "Test Stats",
            blocks = listOf(
                DocumentBlock.Paragraph(
                    runs = listOf(TextRun("The quick brown fox jumps over the lazy dog."))
                ),
                DocumentBlock.Paragraph(
                    runs = listOf(TextRun("Typable is a professional word processor."))
                )
            )
        )
        val stats = doc.computeStats()
        assertEquals(15, stats.wordCount)
        assertEquals(2, stats.paragraphCount)
        assertTrue(stats.characterCountWithSpaces > 0)
        assertTrue(stats.characterCountWithoutSpaces > 0)
        assertEquals(1, stats.pageCount)
    }

    @Test
    fun testParagraphSplittingAndMerging() {
        val initialDoc = TypableDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    runs = listOf(TextRun("Hello World"))
                )
            )
        )
        val engine = DocumentEngine(initialDoc)

        // Split after "Hello " at offset 6
        val nextIdx = engine.splitParagraph(0, 6)
        assertEquals(1, nextIdx)
        assertEquals(2, engine.currentDocument.blocks.size)

        val firstP = engine.currentDocument.blocks[0] as DocumentBlock.Paragraph
        val secondP = engine.currentDocument.blocks[1] as DocumentBlock.Paragraph

        assertEquals("Hello ", firstP.plainText)
        assertEquals("World", secondP.plainText)

        // Undo split
        assertTrue(engine.canUndo)
        engine.undo()
        assertEquals(1, engine.currentDocument.blocks.size)
        assertEquals("Hello World", (engine.currentDocument.blocks[0] as DocumentBlock.Paragraph).plainText)

        // Redo split
        assertTrue(engine.canRedo)
        engine.redo()
        assertEquals(2, engine.currentDocument.blocks.size)

        // Merge back
        val mergedIdx = engine.mergeWithPrevious(1)
        assertEquals(0, mergedIdx)
        assertEquals(1, engine.currentDocument.blocks.size)
    }

    @Test
    fun testFormattingToggles() {
        val doc = TypableDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(
                    runs = listOf(TextRun("Sample Text", isBold = false))
                )
            )
        )
        val engine = DocumentEngine(doc)

        // Toggle bold
        engine.toggleBold(0)
        val boldP = engine.currentDocument.blocks[0] as DocumentBlock.Paragraph
        assertTrue(boldP.runs.first().isBold)

        // Apply style
        engine.applyStyle(0, BlockStyle.HEADING_1)
        val h1P = engine.currentDocument.blocks[0] as DocumentBlock.Paragraph
        assertEquals(BlockStyle.HEADING_1, h1P.style)
        assertEquals(24f, h1P.runs.first().fontSizeSp)

        // Apply alignment
        engine.applyAlignment(0, TextAlignment.CENTER)
        val centerP = engine.currentDocument.blocks[0] as DocumentBlock.Paragraph
        assertEquals(TextAlignment.CENTER, centerP.alignment)
    }

    @Test
    fun testSearchAndReplace() {
        val doc = TypableDocument(
            blocks = listOf(
                DocumentBlock.Paragraph(runs = listOf(TextRun("The quick blue car passed another blue car.")))
            )
        )
        val engine = DocumentEngine(doc)

        val matches = engine.findMatches("blue", matchCase = false, wholeWord = true)
        assertEquals(2, matches.size)

        val replacedCount = engine.replaceAllMatches("blue", "red", matchCase = false, wholeWord = true)
        assertEquals(1, replacedCount) // 1 block affected

        val updatedP = engine.currentDocument.blocks[0] as DocumentBlock.Paragraph
        assertEquals("The quick red car passed another red car.", updatedP.plainText)
    }

    @Test
    fun testTableOperations() {
        val doc = TypableDocument(blocks = listOf(DocumentBlock.Paragraph()))
        val engine = DocumentEngine(doc)

        // Insert table 3x3
        val tableIdx = engine.insertTable(0, rows = 3, cols = 3)
        assertEquals(1, tableIdx)
        assertEquals(2, engine.currentDocument.blocks.size)

        val table = engine.currentDocument.blocks[1] as DocumentBlock.Table
        assertEquals(3, table.rows.size)
        assertEquals(3, table.rows[0].cells.size)

        // Update cell text
        engine.updateTableCell(1, 0, 0, "Quarter")
        val updatedTable = engine.currentDocument.blocks[1] as DocumentBlock.Table
        assertEquals("Quarter", updatedTable.rows[0].cells[0].plainText)

        // Add row
        engine.addTableRow(1, 0)
        val tableAfterAdd = engine.currentDocument.blocks[1] as DocumentBlock.Table
        assertEquals(4, tableAfterAdd.rows.size)

        // Delete row
        engine.deleteTableRow(1, 1)
        val tableAfterDel = engine.currentDocument.blocks[1] as DocumentBlock.Table
        assertEquals(3, tableAfterDel.rows.size)
    }

    @Test
    fun testMarkdownExportAndImport() {
        val doc = TypableDocument(
            title = "Markdown Document",
            blocks = listOf(
                DocumentBlock.Paragraph(style = BlockStyle.HEADING_1, runs = listOf(TextRun("Heading One", isBold = true))),
                DocumentBlock.Paragraph(style = BlockStyle.NORMAL, runs = listOf(TextRun("Regular text with "), TextRun("bold words", isBold = true))),
                DocumentBlock.Paragraph(listType = ListType.BULLET, runs = listOf(TextRun("First bullet"))),
                DocumentBlock.Paragraph(listType = ListType.BULLET, runs = listOf(TextRun("Second bullet")))
            )
        )
        val exported = MarkdownFormat.exportToMarkdown(doc)
        assertTrue(exported.contains("# Heading One"))
        assertTrue(exported.contains("**bold words**"))
        assertTrue(exported.contains("• First bullet"))

        val imported = MarkdownFormat.importFromMarkdown(ByteArrayInputStream(exported.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(4, imported.blocks.size)
    }

    @Test
    fun testHtmlExportAndImport() {
        val doc = TypableDocument(
            title = "HTML Document",
            blocks = listOf(
                DocumentBlock.Paragraph(style = BlockStyle.HEADING_1, runs = listOf(TextRun("Main Title"))),
                DocumentBlock.Paragraph(runs = listOf(TextRun("Paragraph content here.")))
            )
        )
        val html = HtmlFormat.exportToHtml(doc)
        assertTrue(html.contains("<h1>Main Title</h1>"))
        assertTrue(html.contains("<p>Paragraph content here.</p>"))

        val imported = HtmlFormat.importFromHtml(ByteArrayInputStream(html.toByteArray(StandardCharsets.UTF_8)))
        assertTrue(imported.blocks.isNotEmpty())
    }

    @Test
    fun testDocxExportPackaging() {
        val doc = TypableDocument(
            title = "Docx Export Test",
            blocks = listOf(
                DocumentBlock.Paragraph(style = BlockStyle.TITLE, runs = listOf(TextRun("Typable Document", isBold = true))),
                DocumentBlock.Paragraph(style = BlockStyle.NORMAL, runs = listOf(TextRun("This is OpenXML test.")))
            )
        )
        val baos = ByteArrayOutputStream()
        DocxFormat.exportToDocx(doc, baos)
        val bytes = baos.toByteArray()

        // Valid Zip file starts with PK (0x50, 0x4B)
        assertTrue(bytes.size > 100)
        assertEquals(0x50.toByte(), bytes[0])
        assertEquals(0x4B.toByte(), bytes[1])

        // Parse back
        val imported = DocxFormat.importFromDocx(ByteArrayInputStream(bytes))
        assertTrue(imported.blocks.isNotEmpty())
    }
}
