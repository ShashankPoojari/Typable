package com.example.formats

import com.example.model.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Native, dependency-free OASIS OpenDocument Text (.odt) generator and parser.
 * Produces genuine LibreOffice, OpenOffice, and Google Docs compatible .odt files.
 */
object OdtFormat {

    fun exportToOdt(document: TypableDocument, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            // 1. mimetype (MUST be first, uncompressed per ODF spec)
            val mimeBytes = "application/vnd.oasis.opendocument.text".toByteArray(StandardCharsets.US_ASCII)
            val mimeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimeBytes.size.toLong()
                compressedSize = mimeBytes.size.toLong()
                crc = CRC32().apply { update(mimeBytes) }.value
            }
            zos.putNextEntry(mimeEntry)
            zos.write(mimeBytes)
            zos.closeEntry()

            // 2. META-INF/manifest.xml
            addZipEntry(zos, "META-INF/manifest.xml", buildManifestXml())

            // 3. content.xml
            addZipEntry(zos, "content.xml", buildContentXml(document))

            // 4. styles.xml
            addZipEntry(zos, "styles.xml", buildStylesXml())
        }
    }

    private fun addZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun buildManifestXml(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
  <manifest:file-entry manifest:full-path="/" manifest:version="1.2" manifest:media-type="application/vnd.oasis.opendocument.text"/>
  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
</manifest:manifest>"""
    }

    private fun buildStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
  xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
  xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
  office:version="1.2">
  <office:styles>
    <style:default-style style:family="paragraph">
      <style:text-properties fo:font-size="11pt" fo:font-family="Arial"/>
    </style:default-style>
  </office:styles>
</office:document-styles>"""
    }

    private fun buildContentXml(document: TypableDocument): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
  xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
  xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
  xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
  office:version="1.2">
  <office:automatic-styles>
    <style:style style:name="Bold" style:family="text">
      <style:text-properties fo:font-weight="bold"/>
    </style:style>
    <style:style style:name="Italic" style:family="text">
      <style:text-properties fo:font-style="italic"/>
    </style:style>
    <style:style style:name="Underline" style:family="text">
      <style:text-properties style:text-underline-style="solid"/>
    </style:style>
    <style:style style:name="Strike" style:family="text">
      <style:text-properties style:text-line-through-style="solid"/>
    </style:style>
    <style:style style:name="Align-center" style:family="paragraph">
      <style:paragraph-properties fo:text-align="center"/>
    </style:style>
    <style:style style:name="Align-right" style:family="paragraph">
      <style:paragraph-properties fo:text-align="end"/>
    </style:style>
    <style:style style:name="Align-justify" style:family="paragraph">
      <style:paragraph-properties fo:text-align="justify"/>
    </style:style>
    <style:style style:name="Title" style:family="paragraph">
      <style:paragraph-properties fo:text-align="center"/>
      <style:text-properties fo:font-size="24pt" fo:font-weight="bold"/>
    </style:style>
    <style:style style:name="Heading1" style:family="paragraph">
      <style:text-properties fo:font-size="18pt" fo:font-weight="bold"/>
    </style:style>
    <style:style style:name="Heading2" style:family="paragraph">
      <style:text-properties fo:font-size="14pt" fo:font-weight="bold"/>
    </style:style>
    <style:style style:name="Heading3" style:family="paragraph">
      <style:text-properties fo:font-size="12pt" fo:font-weight="bold"/>
    </style:style>
    <style:style style:name="Blockquote" style:family="paragraph">
      <style:paragraph-properties fo:margin-left="0.5in" fo:margin-right="0.5in"/>
      <style:text-properties fo:font-style="italic"/>
    </style:style>
    <style:style style:name="CodeBlock" style:family="paragraph">
      <style:text-properties fo:font-family="Courier New" fo:font-size="10pt"/>
    </style:style>
  </office:automatic-styles>
  <office:body>
    <office:text>
""")

        for (block in document.blocks) {
            when (block) {
                is DocumentBlock.Paragraph -> {
                    val pTag = when (block.style) {
                        BlockStyle.TITLE -> "<text:p text:style-name=\"Title\">"
                        BlockStyle.HEADING_1 -> "<text:h text:outline-level=\"1\" text:style-name=\"Heading1\">"
                        BlockStyle.HEADING_2 -> "<text:h text:outline-level=\"2\" text:style-name=\"Heading2\">"
                        BlockStyle.HEADING_3 -> "<text:h text:outline-level=\"3\" text:style-name=\"Heading3\">"
                        BlockStyle.QUOTE -> "<text:p text:style-name=\"Blockquote\">"
                        BlockStyle.CODE -> "<text:p text:style-name=\"CodeBlock\">"
                        else -> {
                            val alignStyle = when (block.alignment) {
                                TextAlignment.CENTER -> " text:style-name=\"Align-center\""
                                TextAlignment.RIGHT -> " text:style-name=\"Align-right\""
                                TextAlignment.JUSTIFY -> " text:style-name=\"Align-justify\""
                                else -> ""
                            }
                            "<text:p$alignStyle>"
                        }
                    }
                    val closeTag = if (block.style.isHeading && block.style != BlockStyle.TITLE) "</text:h>" else "</text:p>"

                    sb.append("      ").append(pTag)
                    for (run in block.runs) {
                        val text = escapeXml(run.text)
                        var runContent = text
                        if (run.isStrikethrough) runContent = "<text:span text:style-name=\"Strike\">$runContent</text:span>"
                        if (run.isUnderline) runContent = "<text:span text:style-name=\"Underline\">$runContent</text:span>"
                        if (run.isItalic) runContent = "<text:span text:style-name=\"Italic\">$runContent</text:span>"
                        if (run.isBold) runContent = "<text:span text:style-name=\"Bold\">$runContent</text:span>"
                        sb.append(runContent)
                    }
                    sb.append(closeTag).append("\n")
                }
                is DocumentBlock.Table -> {
                    sb.append("      <table:table table:name=\"Table_${block.id.take(8)}\">\n")
                    val colCount = (block.rows.firstOrNull()?.cells?.size ?: 1).coerceAtLeast(1)
                    for (c in 0 until colCount) {
                        sb.append("        <table:table-column/>\n")
                    }
                    for (row in block.rows) {
                        sb.append("        <table:table-row>\n")
                        for (cell in row.cells) {
                            sb.append("          <table:table-cell office:value-type=\"string\">\n")
                            sb.append("            <text:p>${escapeXml(cell.plainText)}</text:p>\n")
                            sb.append("          </table:table-cell>\n")
                        }
                        sb.append("        </table:table-row>\n")
                    }
                    sb.append("      </table:table>\n")
                }
                is DocumentBlock.Divider -> {
                    sb.append("      <text:p text:style-name=\"Blockquote\">――――――――――――――――――――――――</text:p>\n")
                }
                is DocumentBlock.PageBreak -> {
                    sb.append("      <text:p><text:soft-page-break/></text:p>\n")
                }
            }
        }

        sb.append("""    </office:text>
  </office:body>
</office:document-content>""")
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun importFromOdt(inputStream: InputStream): TypableDocument {
        val blocks = mutableListOf<DocumentBlock>()
        var docTitle = "Imported Document"

        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (entry.name == "content.xml") {
                    val xmlContent = zis.readBytes().toString(StandardCharsets.UTF_8)
                    parseOdtContentXml(xmlContent, blocks)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (blocks.isNotEmpty()) {
            val firstText = (blocks.firstOrNull() as? DocumentBlock.Paragraph)?.plainText?.trim()
            if (!firstText.isNullOrBlank()) {
                docTitle = firstText.take(40)
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

    private fun parseOdtContentXml(xml: String, outBlocks: MutableList<DocumentBlock>) {
        val paragraphRegex = Regex("<text:[ph]([^>]*)>(.*?)</text:[ph]>", RegexOption.DOT_MATCHES_ALL)
        val tagStripRegex = Regex("<[^>]+>")

        for (match in paragraphRegex.findAll(xml)) {
            val attr = match.groupValues[1]
            val innerXml = match.groupValues[2]

            val isHeading = match.value.startsWith("<text:h")
            val style = when {
                attr.contains("Title", ignoreCase = true) -> BlockStyle.TITLE
                isHeading && attr.contains("1") -> BlockStyle.HEADING_1
                isHeading && attr.contains("2") -> BlockStyle.HEADING_2
                isHeading -> BlockStyle.HEADING_3
                attr.contains("Blockquote", ignoreCase = true) -> BlockStyle.QUOTE
                attr.contains("Code", ignoreCase = true) -> BlockStyle.CODE
                else -> BlockStyle.NORMAL
            }

            // Extract spans or plain text
            val text = tagStripRegex.replace(innerXml, "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")

            val isBold = innerXml.contains("Bold", ignoreCase = true) || isHeading
            val isItalic = innerXml.contains("Italic", ignoreCase = true) || style == BlockStyle.QUOTE

            outBlocks.add(
                DocumentBlock.Paragraph(
                    runs = listOf(
                        TextRun(
                            text = text,
                            isBold = isBold,
                            isItalic = isItalic,
                            fontSizeSp = style.defaultFontSizeSp
                        )
                    ),
                    style = style
                )
            )
        }
    }
}
