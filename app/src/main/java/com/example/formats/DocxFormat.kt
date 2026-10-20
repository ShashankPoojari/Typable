package com.example.formats

import com.example.model.*
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Native, dependency-free OpenXML DOCX generator and parser.
 * Produces genuine Microsoft Word .docx files compatible with MS Word, LibreOffice, and Google Docs.
 */
object DocxFormat {

    fun exportToDocx(document: TypableDocument, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            // 1. [Content_Types].xml
            addZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())

            // 2. _rels/.rels
            addZipEntry(zos, "_rels/.rels", buildRootRelsXml())

            // 3. word/_rels/document.xml.rels
            addZipEntry(zos, "word/_rels/document.xml.rels", buildDocumentRelsXml())

            // 4. word/styles.xml
            addZipEntry(zos, "word/styles.xml", buildStylesXml())

            // 5. word/document.xml
            addZipEntry(zos, "word/document.xml", buildDocumentXml(document))
        }
    }

    fun importFromDocx(inputStream: InputStream): TypableDocument {
        val blocks = mutableListOf<DocumentBlock>()
        var docTitle = "Imported Document"

        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlContent = zis.readBytes().toString(StandardCharsets.UTF_8)
                    val parsedBlocks = parseWordDocumentXml(xmlContent)
                    blocks.addAll(parsedBlocks)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (blocks.isNotEmpty()) {
            val firstBlock = blocks.firstOrNull()
            if (firstBlock is DocumentBlock.Paragraph && firstBlock.plainText.isNotBlank()) {
                docTitle = firstBlock.plainText.take(40).trim()
            }
        } else {
            blocks.add(DocumentBlock.Paragraph(runs = listOf(TextRun("Imported empty document"))))
        }

        return TypableDocument(
            id = UUID.randomUUID().toString(),
            title = docTitle,
            blocks = blocks
        )
    }

    private fun addZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun buildContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""
    }

    private fun buildRootRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
    }

    private fun buildDocumentRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
        <w:sz w:val="24"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:styleId="Normal" w:default="1">
    <w:name w:val="Normal"/>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:rPr>
      <w:b/>
      <w:sz w:val="48"/>
      <w:color w:val="0284C7"/>
    </w:rPr>
  </w:style>
</w:styles>"""
    }

    private fun buildDocumentXml(document: TypableDocument): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
""")

        for (block in document.blocks) {
            when (block) {
                is DocumentBlock.Paragraph -> {
                    sb.append("    <w:p>\n")
                    // Paragraph properties
                    sb.append("      <w:pPr>\n")
                    when (block.alignment) {
                        TextAlignment.CENTER -> sb.append("        <w:jc w:val=\"center\"/>\n")
                        TextAlignment.RIGHT -> sb.append("        <w:jc w:val=\"right\"/>\n")
                        TextAlignment.JUSTIFY -> sb.append("        <w:jc w:val=\"both\"/>\n")
                        else -> sb.append("        <w:jc w:val=\"left\"/>\n")
                    }
                    if (block.style.isHeading) {
                        val headingLevel = when (block.style) {
                            BlockStyle.TITLE, BlockStyle.HEADING_1 -> 1
                            BlockStyle.SUBTITLE, BlockStyle.HEADING_2 -> 2
                            BlockStyle.HEADING_3 -> 3
                            else -> 1
                        }
                        sb.append("        <w:pStyle w:val=\"Heading$headingLevel\"/>\n")
                    }
                    if (block.indentationLevel > 0) {
                        val indVal = block.indentationLevel * 720
                        sb.append("        <w:ind w:left=\"$indVal\"/>\n")
                    }
                    sb.append("      </w:pPr>\n")

                    // Bullet / number prefix if applicable
                    if (block.listType == ListType.BULLET) {
                        sb.append("      <w:r><w:t>• </w:t></w:r>\n")
                    } else if (block.listType == ListType.NUMBERED) {
                        sb.append("      <w:r><w:t>${block.listIndex}. </w:t></w:r>\n")
                    }

                    // Runs
                    for (run in block.runs) {
                        sb.append("      <w:r>\n")
                        sb.append("        <w:rPr>\n")
                        if (run.isBold) sb.append("          <w:b/>\n")
                        if (run.isItalic) sb.append("          <w:i/>\n")
                        if (run.isUnderline) sb.append("          <w:u w:val=\"single\"/>\n")
                        if (run.isStrikethrough) sb.append("          <w:strike/>\n")
                        if (run.textColorHex != null) {
                            val cleanHex = run.textColorHex.removePrefix("#")
                            sb.append("          <w:color w:val=\"$cleanHex\"/>\n")
                        }
                        val halfPoints = (run.fontSizeSp * 2).toInt()
                        sb.append("          <w:sz w:val=\"$halfPoints\"/>\n")
                        sb.append("        </w:rPr>\n")
                        sb.append("        <w:t xml:space=\"preserve\">${escapeXml(run.text)}</w:t>\n")
                        sb.append("      </w:r>\n")
                    }
                    sb.append("    </w:p>\n")
                }
                is DocumentBlock.Table -> {
                    sb.append("    <w:tbl>\n")
                    sb.append("      <w:tblPr>\n")
                    sb.append("        <w:tblBorders>\n")
                    sb.append("          <w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>\n")
                    sb.append("          <w:left w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>\n")
                    sb.append("          <w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>\n")
                    sb.append("          <w:right w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"CBD5E1\"/>\n")
                    sb.append("          <w:insideH w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"E2E8F0\"/>\n")
                    sb.append("          <w:insideV w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"E2E8F0\"/>\n")
                    sb.append("        </w:tblBorders>\n")
                    sb.append("      </w:tblPr>\n")
                    for (row in block.rows) {
                        sb.append("      <w:tr>\n")
                        for (cell in row.cells) {
                            sb.append("        <w:tc>\n")
                            sb.append("          <w:tcPr>\n")
                            if (row.isHeader) {
                                sb.append("            <w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"F1F5F9\"/>\n")
                            }
                            sb.append("          </w:tcPr>\n")
                            sb.append("          <w:p>\n")
                            for (run in cell.runs) {
                                sb.append("            <w:r>\n")
                                if (run.isBold || row.isHeader) {
                                    sb.append("              <w:rPr><w:b/></w:rPr>\n")
                                }
                                sb.append("              <w:t xml:space=\"preserve\">${escapeXml(run.text)}</w:t>\n")
                                sb.append("            </w:r>\n")
                            }
                            sb.append("          </w:p>\n")
                            sb.append("        </w:tc>\n")
                        }
                        sb.append("      </w:tr>\n")
                    }
                    sb.append("    </w:tbl>\n")
                }
                is DocumentBlock.Divider -> {
                    sb.append("    <w:p><w:pPr><w:pBdr><w:bottom w:val=\"single\" w:sz=\"6\" w:space=\"1\" w:color=\"94A3B8\"/></w:pBdr></w:pPr></w:p>\n")
                }
                is DocumentBlock.PageBreak -> {
                    sb.append("    <w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n")
                }
            }
        }

        // Section properties (Page layout & margins)
        val wPt = document.pageSettings.activeWidthPt * 20 // 1 pt = 20 dxa in WordML
        val hPt = document.pageSettings.activeHeightPt * 20
        val mPt = document.pageSettings.margins.marginPt * 20
        sb.append("""    <w:sectPr>
      <w:pgSz w:w="$wPt" w:h="$hPt"/>
      <w:pgMar w:top="$mPt" w:right="$mPt" w:bottom="$mPt" w:left="$mPt"/>
    </w:sectPr>
  </w:body>
</w:document>""")

        return sb.toString()
    }

    private fun parseWordDocumentXml(xml: String): List<DocumentBlock> {
        val blocks = mutableListOf<DocumentBlock>()

        // Extract tables first or paragraphs
        // Clean regex-based tag parser for WordML paragraphs <w:p>...</w:p>
        val pRegex = Regex("<w:p[ >](.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
        val matches = pRegex.findAll(xml)

        for (match in matches) {
            val pContent = match.groupValues[1]

            // Check style
            var style = BlockStyle.NORMAL
            if (pContent.contains("Heading1") || pContent.contains("heading 1") || pContent.contains("Title")) {
                style = BlockStyle.HEADING_1
            } else if (pContent.contains("Heading2") || pContent.contains("heading 2") || pContent.contains("Subtitle")) {
                style = BlockStyle.HEADING_2
            } else if (pContent.contains("Heading3") || pContent.contains("heading 3")) {
                style = BlockStyle.HEADING_3
            }

            // Extract runs <w:r>...</w:r>
            val rRegex = Regex("<w:r[ >](.*?)</w:r>", RegexOption.DOT_MATCHES_ALL)
            val rMatches = rRegex.findAll(pContent)
            val runs = mutableListOf<TextRun>()

            for (rMatch in rMatches) {
                val rContent = rMatch.groupValues[1]
                val tRegex = Regex("<w:t[^>]*>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                val tMatch = tRegex.find(rContent)
                val text = tMatch?.groupValues?.get(1)?.let { unescapeXml(it) } ?: ""

                if (text.isNotEmpty()) {
                    val isBold = rContent.contains("<w:b/>") || rContent.contains("<w:b ")
                    val isItalic = rContent.contains("<w:i/>") || rContent.contains("<w:i ")
                    val isUnderline = rContent.contains("<w:u ")
                    val isStrike = rContent.contains("<w:strike")

                    runs.add(
                        TextRun(
                            text = text,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            isStrikethrough = isStrike,
                            fontSizeSp = style.defaultFontSizeSp
                        )
                    )
                }
            }

            if (runs.isNotEmpty()) {
                blocks.add(
                    DocumentBlock.Paragraph(
                        style = style,
                        runs = runs
                    )
                )
            }
        }

        return blocks
    }

    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun unescapeXml(str: String): String {
        return str.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }
}
