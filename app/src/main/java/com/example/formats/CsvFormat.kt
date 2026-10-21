package com.example.formats

import com.example.model.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Tabular CSV format support (.csv).
 * Exports document tables or text to CSV, and imports CSV rows as interactive Table blocks.
 */
object CsvFormat {

    fun exportToCsv(document: TypableDocument, outputStream: OutputStream) {
        val sb = StringBuilder()
        val tables = document.blocks.filterIsInstance<DocumentBlock.Table>()

        if (tables.isNotEmpty()) {
            for ((tIdx, table) in tables.withIndex()) {
                if (tables.size > 1) {
                    sb.append("\"--- Table ${tIdx + 1} ---\"\n")
                }
                for (row in table.rows) {
                    val line = row.cells.joinToString(",") { cell ->
                        escapeCsvCell(cell.plainText)
                    }
                    sb.append(line).append("\n")
                }
                sb.append("\n")
            }
        } else {
            // If no table exists, export paragraphs line-by-line as single column CSV
            sb.append("\"Paragraph Text\",\"Style\"\n")
            for (block in document.blocks) {
                if (block is DocumentBlock.Paragraph) {
                    sb.append(escapeCsvCell(block.plainText))
                        .append(",")
                        .append(escapeCsvCell(block.style.displayName))
                        .append("\n")
                }
            }
        }

        outputStream.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
    }

    private fun escapeCsvCell(text: String): String {
        val sanitized = text.replace("\"", "\"\"")
        return "\"$sanitized\""
    }

    fun importFromCsv(inputStream: InputStream): TypableDocument {
        val lines = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readLines() }
        val blocks = mutableListOf<DocumentBlock>()

        val rows = mutableListOf<List<String>>()
        for (line in lines) {
            if (line.isBlank()) continue
            rows.add(parseCsvLine(line))
        }

        if (rows.isNotEmpty()) {
            val maxCols = rows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
            val tableRows = rows.mapIndexed { rIdx, rowValues ->
                val cells = (0 until maxCols).map { cIdx ->
                    val cellText = rowValues.getOrNull(cIdx) ?: ""
                    TableCell(
                        runs = listOf(
                            TextRun(
                                text = cellText,
                                isBold = rIdx == 0
                            )
                        )
                    )
                }
                TableRow(cells = cells, isHeader = rIdx == 0)
            }

            blocks.add(
                DocumentBlock.Paragraph(
                    runs = listOf(TextRun("Imported Spreadsheet Data")),
                    style = BlockStyle.TITLE
                )
            )
            blocks.add(
                DocumentBlock.Table(
                    rows = tableRows,
                    columnWeights = List(maxCols) { 1f / maxCols }
                )
            )
        } else {
            blocks.add(DocumentBlock.Paragraph(runs = listOf(TextRun("Empty CSV document"))))
        }

        return TypableDocument(
            id = UUID.randomUUID().toString(),
            title = "Imported CSV Spreadsheet",
            blocks = blocks
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = line.length

        while (i < len) {
            val ch = line[i]
            when {
                ch == '\"' -> {
                    if (inQuotes && i + 1 < len && line[i + 1] == '\"') {
                        cur.append('\"')
                        i += 2
                        continue
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    cells.add(cur.toString().trim())
                    cur.clear()
                }
                else -> {
                    cur.append(ch)
                }
            }
            i++
        }
        cells.add(cur.toString().trim())
        return cells
    }
}
