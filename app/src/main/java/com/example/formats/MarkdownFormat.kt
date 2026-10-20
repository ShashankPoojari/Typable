package com.example.formats

import com.example.model.*
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

object MarkdownFormat {

    fun exportToMarkdown(document: TypableDocument): String {
        val sb = StringBuilder()
        for (block in document.blocks) {
            when (block) {
                is DocumentBlock.Paragraph -> {
                    val prefix = when (block.style) {
                        BlockStyle.TITLE -> "# "
                        BlockStyle.SUBTITLE -> "## "
                        BlockStyle.HEADING_1 -> "# "
                        BlockStyle.HEADING_2 -> "## "
                        BlockStyle.HEADING_3 -> "### "
                        BlockStyle.QUOTE -> "> "
                        BlockStyle.CODE -> "```\n"
                        else -> {
                            when (block.listType) {
                                ListType.BULLET -> "• ".padStart(block.indentationLevel * 2 + 2)
                                ListType.NUMBERED -> "${block.listIndex}. ".padStart(block.indentationLevel * 2 + 3)
                                ListType.NONE -> "  ".repeat(block.indentationLevel)
                            }
                        }
                    }
                    sb.append(prefix)

                    for (run in block.runs) {
                        var text = run.text
                        if (!block.style.isHeading) {
                            if (run.isBold && run.isItalic) text = "***$text***"
                            else if (run.isBold) text = "**$text**"
                            else if (run.isItalic) text = "*$text*"
                        } else {
                            if (run.isItalic) text = "*$text*"
                        }
                        if (run.isStrikethrough) text = "~~$text~~"
                        if (run.linkUrl != null) text = "[$text](${run.linkUrl})"
                        sb.append(text)
                    }

                    if (block.style == BlockStyle.CODE) {
                        sb.append("\n```")
                    }
                    sb.append("\n\n")
                }
                is DocumentBlock.Table -> {
                    if (block.rows.isNotEmpty()) {
                        val headerRow = block.rows.first()
                        sb.append("| ").append(headerRow.cells.joinToString(" | ") { it.plainText }).append(" |\n")
                        sb.append("| ").append(headerRow.cells.joinToString(" | ") { "---" }).append(" |\n")
                        for (i in 1 until block.rows.size) {
                            val r = block.rows[i]
                            sb.append("| ").append(r.cells.joinToString(" | ") { it.plainText }).append(" |\n")
                        }
                        sb.append("\n")
                    }
                }
                is DocumentBlock.Divider -> {
                    sb.append("---\n\n")
                }
                is DocumentBlock.PageBreak -> {
                    sb.append("\n<!-- pagebreak -->\n\n")
                }
            }
        }
        return sb.toString().trimEnd() + "\n"
    }

    fun importFromMarkdown(inputStream: InputStream): TypableDocument {
        val content = inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        val lines = content.lines()
        val blocks = mutableListOf<DocumentBlock>()
        var docTitle = "Imported Markdown"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            when {
                trimmed.startsWith("### ") -> {
                    blocks.add(DocumentBlock.Paragraph(style = BlockStyle.HEADING_3, runs = parseMarkdownInline(trimmed.removePrefix("### "))))
                }
                trimmed.startsWith("## ") -> {
                    blocks.add(DocumentBlock.Paragraph(style = BlockStyle.HEADING_2, runs = parseMarkdownInline(trimmed.removePrefix("## "))))
                }
                trimmed.startsWith("# ") -> {
                    val headingText = trimmed.removePrefix("# ")
                    if (blocks.isEmpty()) docTitle = headingText.take(40)
                    blocks.add(DocumentBlock.Paragraph(style = BlockStyle.HEADING_1, runs = parseMarkdownInline(headingText)))
                }
                trimmed.startsWith("> ") -> {
                    blocks.add(DocumentBlock.Paragraph(style = BlockStyle.QUOTE, runs = parseMarkdownInline(trimmed.removePrefix("> "))))
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val bulletText = trimmed.substring(2)
                    blocks.add(DocumentBlock.Paragraph(listType = ListType.BULLET, runs = parseMarkdownInline(bulletText)))
                }
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val num = trimmed.substringBefore(".").toIntOrNull() ?: 1
                    val numText = trimmed.substringAfter(". ")
                    blocks.add(DocumentBlock.Paragraph(listType = ListType.NUMBERED, listIndex = num, runs = parseMarkdownInline(numText)))
                }
                trimmed == "---" || trimmed == "***" -> {
                    blocks.add(DocumentBlock.Divider())
                }
                else -> {
                    blocks.add(DocumentBlock.Paragraph(style = BlockStyle.NORMAL, runs = parseMarkdownInline(trimmed)))
                }
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(DocumentBlock.Paragraph(runs = listOf(TextRun(""))))
        }

        return TypableDocument(
            id = UUID.randomUUID().toString(),
            title = docTitle,
            blocks = blocks
        )
    }

    private fun parseMarkdownInline(text: String): List<TextRun> {
        val runs = mutableListOf<TextRun>()
        // Simple regex scan for **bold**, *italic*, ~~strike~~
        var remaining = text
        val pattern = Regex("(\\*\\*|\\*|~~)(.*?)\\1")

        var lastIdx = 0
        val matches = pattern.findAll(remaining).toList()

        if (matches.isEmpty()) {
            return listOf(TextRun(text))
        }

        for (m in matches) {
            val start = m.range.first
            if (start > lastIdx) {
                runs.add(TextRun(remaining.substring(lastIdx, start)))
            }
            val marker = m.groupValues[1]
            val matchedText = m.groupValues[2]
            runs.add(
                TextRun(
                    text = matchedText,
                    isBold = marker == "**",
                    isItalic = marker == "*",
                    isStrikethrough = marker == "~~"
                )
            )
            lastIdx = m.range.last + 1
        }
        if (lastIdx < remaining.length) {
            runs.add(TextRun(remaining.substring(lastIdx)))
        }

        return runs
    }
}
