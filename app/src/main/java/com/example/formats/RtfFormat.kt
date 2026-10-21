package com.example.formats

import com.example.model.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * High-fidelity Rich Text Format (.rtf) generator and parser.
 * Fully compatible with Microsoft Word, Apple TextEdit, WordPad, and LibreOffice.
 */
object RtfFormat {

    fun exportToRtf(document: TypableDocument, outputStream: OutputStream) {
        val sb = StringBuilder()
        // RTF Header
        sb.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\nouicompat\\deflang1033\n")

        // Font Table
        sb.append("{\\fonttbl")
        sb.append("{\\f0\\fnil\\fcharset0 Arial;}")
        sb.append("{\\f1\\froman\\fcharset0 Times New Roman;}")
        sb.append("{\\f2\\fmodern\\fcharset0 Courier New;}")
        sb.append("{\\f3\\fnil\\fcharset0 Georgia;}")
        sb.append("}\n")

        // Color Table
        sb.append("{\\colortbl ;\\red0\\green0\\blue0;\\red37\\green99\\blue235;\\red220\\green38\\blue38;\\red234\\green179\\blue8;\\red22\\green163\\blue74;\\red100\\green116\\blue139;}\n")

        // Page settings (1 pt = 20 twips)
        val ps = document.pageSettings
        val pageW = ps.activeWidthPt * 20
        val pageH = ps.activeHeightPt * 20
        val marginTwips = ps.margins.marginPt * 20
        sb.append("\\paperw$pageW\\paperh$pageH")
        sb.append("\\margl$marginTwips\\margr$marginTwips\\margt$marginTwips\\margb$marginTwips\n")

        // Blocks
        for (block in document.blocks) {
            when (block) {
                is DocumentBlock.Paragraph -> {
                    sb.append(renderParagraph(block))
                }
                is DocumentBlock.Table -> {
                    sb.append(renderTable(block))
                }
                is DocumentBlock.Divider -> {
                    sb.append("\\pard\\brdrb\\brdrs\\brdrw10\\brsp20\\par\\pard\\par\n")
                }
                is DocumentBlock.PageBreak -> {
                    sb.append("\\page\n")
                }
            }
        }

        sb.append("}\n")
        outputStream.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
    }

    private fun renderParagraph(paragraph: DocumentBlock.Paragraph): String {
        val sb = StringBuilder()
        sb.append("\\pard")

        // Indentation (1 level = 36 pt = 720 twips)
        if (paragraph.indentationLevel > 0) {
            val indentTwips = paragraph.indentationLevel * 720
            sb.append("\\li$indentTwips")
        }

        // Alignment
        when (paragraph.alignment) {
            TextAlignment.LEFT -> sb.append("\\ql")
            TextAlignment.CENTER -> sb.append("\\qc")
            TextAlignment.RIGHT -> sb.append("\\qr")
            TextAlignment.JUSTIFY -> sb.append("\\qj")
        }

        // Spacing
        val lineSpacingMult = (paragraph.lineSpacing * 240).toInt()
        sb.append("\\sl$lineSpacingMult\\slmult1")

        // List prefix
        val prefix = when (paragraph.listType) {
            ListType.BULLET -> "{\\pntext\\bullet\\tab}"
            ListType.NUMBERED -> "{\\pntext\\b ${paragraph.listIndex}.\\tab}"
            ListType.NONE -> ""
        }
        sb.append(" ")
        sb.append(prefix)

        // Block style defaults
        val isHeading = paragraph.style.isHeading
        val styleFontSizeHalfPt = (paragraph.style.defaultFontSizeSp * 2).toInt()

        for (run in paragraph.runs) {
            sb.append("{")
            // Font family
            when (run.fontFamily) {
                DocumentFont.SERIF, DocumentFont.PLAYFAIR_DISPLAY, DocumentFont.MERRIWEATHER, DocumentFont.LORA -> sb.append("\\f1")
                DocumentFont.MONOSPACE, DocumentFont.ROBOTO_MONO -> sb.append("\\f2")
                else -> sb.append("\\f0")
            }

            // Size (in half-points: 12pt = \fs24)
            val fontSize = if (run.fontSizeSp != 15f) (run.fontSizeSp * 2).toInt() else styleFontSizeHalfPt
            sb.append("\\fs$fontSize")

            // Styles
            if (run.isBold || isHeading) sb.append("\\b")
            if (run.isItalic) sb.append("\\i")
            if (run.isUnderline) sb.append("\\ul")
            if (run.isStrikethrough) sb.append("\\strike")
            if (run.isSuperscript) sb.append("\\super")
            if (run.isSubscript) sb.append("\\sub")

            // Colors
            if (run.textColorHex != null && run.textColorHex.length == 7) {
                sb.append("\\cf2")
            }
            if (run.highlightColorHex != null) {
                sb.append("\\highlight4")
            }

            sb.append(" ")
            sb.append(escapeRtf(run.text))
            sb.append("}")
        }

        sb.append("\\par\n")
        return sb.toString()
    }

    private fun renderTable(table: DocumentBlock.Table): String {
        val sb = StringBuilder()
        val numCols = (table.rows.firstOrNull()?.cells?.size ?: 1).coerceAtLeast(1)
        val colWidthTwips = 8000 / numCols

        for (row in table.rows) {
            sb.append("\\trowd\\trgaph108")
            for (c in 1..numCols) {
                sb.append("\\cellx${c * colWidthTwips}")
            }
            sb.append("\n")

            for (cell in row.cells) {
                val text = cell.plainText
                sb.append("\\intbl ")
                if (row.isHeader) sb.append("{\\b ")
                sb.append(escapeRtf(text))
                if (row.isHeader) sb.append("}")
                sb.append("\\cell ")
            }
            sb.append("\\row\n")
        }
        sb.append("\\pard\\par\n")
        return sb.toString()
    }

    private fun escapeRtf(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '{' -> sb.append("\\{")
                '}' -> sb.append("\\}")
                '\n' -> sb.append("\\line ")
                else -> {
                    val code = ch.code
                    if (code in 32..126) {
                        sb.append(ch)
                    } else if (code <= 255) {
                        sb.append(String.format("\\'%02x", code))
                    } else {
                        sb.append("\\u${code}?")
                    }
                }
            }
        }
        return sb.toString()
    }

    fun importFromRtf(inputStream: InputStream): TypableDocument {
        val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        val blocks = mutableListOf<DocumentBlock>()

        var inControl = false
        var controlWord = StringBuilder()
        var currentText = StringBuilder()
        var isBold = false
        var isItalic = false
        var isUnderline = false
        val currentRuns = mutableListOf<TextRun>()

        fun flushRun() {
            if (currentText.isNotEmpty()) {
                currentRuns.add(
                    TextRun(
                        text = currentText.toString(),
                        isBold = isBold,
                        isItalic = isItalic,
                        isUnderline = isUnderline
                    )
                )
                currentText.clear()
            }
        }

        fun flushParagraph() {
            flushRun()
            if (currentRuns.isNotEmpty()) {
                blocks.add(DocumentBlock.Paragraph(runs = currentRuns.toList()))
                currentRuns.clear()
            }
        }

        var i = 0
        val len = content.length
        while (i < len) {
            val ch = content[i]
            when {
                ch == '{' || ch == '}' -> {
                    // Scope change
                    i++
                }
                ch == '\\' -> {
                    i++
                    if (i < len) {
                        val next = content[i]
                        if (next == '\\' || next == '{' || next == '}') {
                            currentText.append(next)
                            i++
                        } else if (next == '\'') {
                            // Hex escape \'hh
                            i++
                            if (i + 1 < len) {
                                val hex = content.substring(i, i + 2)
                                val code = hex.toIntOrNull(16) ?: 32
                                currentText.append(code.toChar())
                                i += 2
                            }
                        } else {
                            // Read control word
                            controlWord.clear()
                            while (i < len && content[i].isLetter()) {
                                controlWord.append(content[i])
                                i++
                            }
                            // Check parameter numbers
                            val param = StringBuilder()
                            var isNegative = false
                            if (i < len && content[i] == '-') {
                                isNegative = true
                                i++
                            }
                            while (i < len && content[i].isDigit()) {
                                param.append(content[i])
                                i++
                            }
                            // Optional space delimiter
                            if (i < len && content[i] == ' ') {
                                i++
                            }

                            val cmd = controlWord.toString().lowercase()
                            when (cmd) {
                                "par", "line" -> flushParagraph()
                                "page" -> {
                                    flushParagraph()
                                    blocks.add(DocumentBlock.PageBreak())
                                }
                                "b" -> {
                                    flushRun()
                                    isBold = param.toString() != "0"
                                }
                                "i" -> {
                                    flushRun()
                                    isItalic = param.toString() != "0"
                                }
                                "ul", "ulnone" -> {
                                    flushRun()
                                    isUnderline = cmd == "ul"
                                }
                                "u" -> {
                                    val code = param.toString().toIntOrNull()
                                    if (code != null) {
                                        currentText.append(code.toChar())
                                    }
                                }
                            }
                        }
                    }
                }
                ch == '\r' || ch == '\n' -> {
                    i++
                }
                else -> {
                    currentText.append(ch)
                    i++
                }
            }
        }

        flushParagraph()

        if (blocks.isEmpty()) {
            blocks.add(DocumentBlock.Paragraph(runs = listOf(TextRun("Imported empty document"))))
        }

        val firstText = (blocks.firstOrNull() as? DocumentBlock.Paragraph)?.plainText?.trim()
        val docTitle = if (!firstText.isNullOrBlank()) firstText.take(40) else "Imported RTF Document"

        return TypableDocument(
            id = UUID.randomUUID().toString(),
            title = docTitle,
            blocks = blocks
        )
    }
}
