package com.example.formats

import com.example.model.*
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.UUID

object HtmlFormat {

    fun exportToHtml(document: TypableDocument): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
        sb.append("<meta charset=\"UTF-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        sb.append("<title>${escapeHtml(document.title)}</title>\n")
        sb.append("""<style>
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    line-height: 1.6;
    color: #1e293b;
    max-width: 800px;
    margin: 40px auto;
    padding: 0 20px;
    background-color: #f8fafc;
  }
  .page {
    background: #ffffff;
    padding: 48px;
    border-radius: 8px;
    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1);
  }
  h1 { font-size: 2rem; color: #0f172a; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; }
  h2 { font-size: 1.5rem; color: #1e293b; }
  h3 { font-size: 1.25rem; color: #334155; }
  blockquote { border-left: 4px solid #38bdf8; padding-left: 16px; color: #64748b; margin: 16px 0; }
  table { width: 100%; border-collapse: collapse; margin: 16px 0; }
  th, td { border: 1px solid #cbd5e1; padding: 10px 14px; text-align: left; }
  th { background-color: #f1f5f9; font-weight: 600; }
  hr { border: none; height: 1px; background-color: #cbd5e1; margin: 24px 0; }
  .pagebreak { page-break-after: always; height: 0; }
</style>
</head>
<body>
<div class="page">
""")

        for (block in document.blocks) {
            when (block) {
                is DocumentBlock.Paragraph -> {
                    val tag = when (block.style) {
                        BlockStyle.TITLE -> "h1"
                        BlockStyle.SUBTITLE -> "h2"
                        BlockStyle.HEADING_1 -> "h1"
                        BlockStyle.HEADING_2 -> "h2"
                        BlockStyle.HEADING_3 -> "h3"
                        BlockStyle.QUOTE -> "blockquote"
                        BlockStyle.CODE -> "pre"
                        else -> "p"
                    }
                    val alignStyle = when (block.alignment) {
                        TextAlignment.CENTER -> " style=\"text-align:center;\""
                        TextAlignment.RIGHT -> " style=\"text-align:right;\""
                        TextAlignment.JUSTIFY -> " style=\"text-align:justify;\""
                        else -> ""
                    }
                    sb.append("<$tag$alignStyle>")
                    if (block.listType == ListType.BULLET) {
                        sb.append("&bull; ")
                    } else if (block.listType == ListType.NUMBERED) {
                        sb.append("${block.listIndex}. ")
                    }
                    for (run in block.runs) {
                        var styledText = escapeHtml(run.text)
                        if (run.isBold) styledText = "<strong>$styledText</strong>"
                        if (run.isItalic) styledText = "<em>$styledText</em>"
                        if (run.isUnderline) styledText = "<u>$styledText</u>"
                        if (run.isStrikethrough) styledText = "<s>$styledText</s>"
                        if (run.isSuperscript) styledText = "<sup>$styledText</sup>"
                        if (run.isSubscript) styledText = "<sub>$styledText</sub>"

                        val styles = mutableListOf<String>()
                        if (run.textColorHex != null) styles.add("color:${run.textColorHex}")
                        if (run.highlightColorHex != null) styles.add("background-color:${run.highlightColorHex}")
                        if (run.fontSizeSp != block.style.defaultFontSizeSp && run.fontSizeSp != 15f) {
                            styles.add("font-size:${run.fontSizeSp}px")
                        }

                        if (styles.isNotEmpty()) {
                            styledText = "<span style=\"${styles.joinToString(";")}\">$styledText</span>"
                        }
                        if (run.linkUrl != null) {
                            styledText = "<a href=\"${escapeHtml(run.linkUrl)}\">$styledText</a>"
                        }
                        sb.append(styledText)
                    }
                    sb.append("</$tag>\n")
                }
                is DocumentBlock.Table -> {
                    sb.append("<table>\n")
                    for (row in block.rows) {
                        sb.append("  <tr>\n")
                        val cellTag = if (row.isHeader) "th" else "td"
                        for (cell in row.cells) {
                            sb.append("    <$cellTag>${escapeHtml(cell.plainText)}</$cellTag>\n")
                        }
                        sb.append("  </tr>\n")
                    }
                    sb.append("</table>\n")
                }
                is DocumentBlock.Divider -> sb.append("<hr>\n")
                is DocumentBlock.PageBreak -> sb.append("<div class=\"pagebreak\"></div>\n")
            }
        }

        sb.append("</div>\n</body>\n</html>")
        return sb.toString()
    }

    fun importFromHtml(inputStream: InputStream): TypableDocument {
        val html = inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        val blocks = mutableListOf<DocumentBlock>()

        // Sanitize out scripts & styles
        val cleanHtml = html
            .replace(Regex("<script.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<style.*?</style>", RegexOption.DOT_MATCHES_ALL), "")

        val tagRegex = Regex("<(p|h1|h2|h3|blockquote|li)[^>]*>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)
        val matches = tagRegex.findAll(cleanHtml)

        for (match in matches) {
            val tagName = match.groupValues[1].lowercase()
            val innerContent = match.groupValues[2]

            val style = when (tagName) {
                "h1" -> BlockStyle.HEADING_1
                "h2" -> BlockStyle.HEADING_2
                "h3" -> BlockStyle.HEADING_3
                "blockquote" -> BlockStyle.QUOTE
                else -> BlockStyle.NORMAL
            }
            val listType = if (tagName == "li") ListType.BULLET else ListType.NONE

            // Strip nested HTML tags for clean text, preserving text
            val text = innerContent.replace(Regex("<[^>]+>"), "").trim()
            if (text.isNotEmpty()) {
                val isBold = innerContent.contains("<strong>") || innerContent.contains("<b>")
                val isItalic = innerContent.contains("<em>") || innerContent.contains("<i>")
                blocks.add(
                    DocumentBlock.Paragraph(
                        style = style,
                        listType = listType,
                        runs = listOf(TextRun(text = unescapeHtml(text), isBold = isBold, isItalic = isItalic))
                    )
                )
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(DocumentBlock.Paragraph(runs = listOf(TextRun(""))))
        }

        return TypableDocument(
            id = UUID.randomUUID().toString(),
            title = "Imported HTML Document",
            blocks = blocks
        )
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun unescapeHtml(str: String): String {
        return str.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&bull;", "•")
    }
}

object PlainTextFormat {
    fun exportToText(document: TypableDocument): String {
        return document.extractPlainText()
    }

    fun importFromText(inputStream: InputStream): TypableDocument {
        val text = inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        val lines = text.lines()
        val blocks = mutableListOf<DocumentBlock>()

        for (line in lines) {
            if (line.isNotBlank()) {
                blocks.add(DocumentBlock.Paragraph(runs = listOf(TextRun(line))))
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(DocumentBlock.Paragraph(runs = listOf(TextRun(""))))
        }

        return TypableDocument(
            id = UUID.randomUUID().toString(),
            title = "Imported Plain Text",
            blocks = blocks
        )
    }
}
