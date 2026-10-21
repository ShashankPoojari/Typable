package com.example.formats

import com.example.model.*
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Standard EPUB 3.0 eBook publication generator and parser.
 * Creates eBooks compatible with Apple Books, Google Play Books, Kindle, and Calibre.
 */
object EpubFormat {

    fun exportToEpub(document: TypableDocument, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            // 1. mimetype (Uncompressed, first entry)
            val mimeBytes = "application/epub+zip".toByteArray(StandardCharsets.US_ASCII)
            val mimeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimeBytes.size.toLong()
                compressedSize = mimeBytes.size.toLong()
                crc = CRC32().apply { update(mimeBytes) }.value
            }
            zos.putNextEntry(mimeEntry)
            zos.write(mimeBytes)
            zos.closeEntry()

            // 2. META-INF/container.xml
            addZipEntry(zos, "META-INF/container.xml", buildContainerXml())

            // 3. OEBPS/style.css
            addZipEntry(zos, "OEBPS/style.css", buildCss())

            // 4. OEBPS/chapter1.xhtml
            addZipEntry(zos, "OEBPS/chapter1.xhtml", buildChapterXhtml(document))

            // 5. OEBPS/content.opf
            addZipEntry(zos, "OEBPS/content.opf", buildOpf(document))

            // 6. OEBPS/toc.ncx (EPUB 2 backward compatibility)
            addZipEntry(zos, "OEBPS/toc.ncx", buildTocNcx(document))
        }
    }

    private fun addZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun buildContainerXml(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""
    }

    private fun buildCss(): String {
        return """
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Georgia, serif;
    margin: 5% 8%;
    line-height: 1.6;
    color: #1e293b;
}
h1, h2, h3 { color: #0f172a; font-weight: 700; margin-top: 1.5em; margin-bottom: 0.5em; }
h1.title { text-align: center; font-size: 2.2em; margin-top: 2em; margin-bottom: 1em; }
p { margin: 0 0 1em 0; }
blockquote { border-left: 4px solid #3b82f6; padding-left: 1rem; margin: 1.5rem 0; color: #475569; font-style: italic; }
pre, code { font-family: monospace; background: #f1f5f9; padding: 2px 4px; border-radius: 4px; font-size: 0.9em; }
table { width: 100%; border-collapse: collapse; margin: 1.5rem 0; }
th, td { border: 1px solid #cbd5e1; padding: 8px 12px; text-align: left; }
th { background-color: #f8fafc; font-weight: 600; }
hr { border: none; border-top: 1px solid #e2e8f0; margin: 2em 0; }
"""
    }

    private fun buildChapterXhtml(document: TypableDocument): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en" lang="en">
<head>
  <meta charset="UTF-8" />
  <title>${escapeXml(document.title)}</title>
  <link rel="stylesheet" type="text/css" href="style.css" />
</head>
<body>
""")

        for (block in document.blocks) {
            when (block) {
                is DocumentBlock.Paragraph -> {
                    val tag = when (block.style) {
                        BlockStyle.TITLE -> "h1 class=\"title\""
                        BlockStyle.HEADING_1 -> "h1"
                        BlockStyle.HEADING_2 -> "h2"
                        BlockStyle.HEADING_3 -> "h3"
                        BlockStyle.QUOTE -> "blockquote"
                        BlockStyle.CODE -> "pre"
                        else -> "p"
                    }
                    val tagName = tag.substringBefore(" ")
                    val alignStyle = when (block.alignment) {
                        TextAlignment.CENTER -> " style=\"text-align: center;\""
                        TextAlignment.RIGHT -> " style=\"text-align: right;\""
                        TextAlignment.JUSTIFY -> " style=\"text-align: justify;\""
                        else -> ""
                    }

                    sb.append("  <$tag$alignStyle>")
                    for (run in block.runs) {
                        var text = escapeXml(run.text)
                        if (run.isBold) text = "<strong>$text</strong>"
                        if (run.isItalic) text = "<em>$text</em>"
                        if (run.isUnderline) text = "<u>$text</u>"
                        if (run.isStrikethrough) text = "<s>$text</s>"
                        if (run.isSuperscript) text = "<sup>$text</sup>"
                        if (run.isSubscript) text = "<sub>$text</sub>"
                        sb.append(text)
                    }
                    sb.append("</$tagName>\n")
                }
                is DocumentBlock.Table -> {
                    sb.append("  <table>\n")
                    for (row in block.rows) {
                        sb.append("    <tr>\n")
                        val cellTag = if (row.isHeader) "th" else "td"
                        for (cell in row.cells) {
                            sb.append("      <$cellTag>${escapeXml(cell.plainText)}</$cellTag>\n")
                        }
                        sb.append("    </tr>\n")
                    }
                    sb.append("  </table>\n")
                }
                is DocumentBlock.Divider -> {
                    sb.append("  <hr />\n")
                }
                is DocumentBlock.PageBreak -> {
                    sb.append("  <div style=\"page-break-after: always;\"></div>\n")
                }
            }
        }

        sb.append("""</body>
</html>""")
        return sb.toString()
    }

    private fun buildOpf(document: TypableDocument): String {
        val dateIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(document.modifiedAt))

        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="pub-id" version="3.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="pub-id">urn:uuid:${document.id}</dc:identifier>
    <dc:title>${escapeXml(document.title)}</dc:title>
    <dc:language>en</dc:language>
    <dc:creator>${escapeXml(document.author.ifBlank { "Typable Author" })}</dc:creator>
    <dc:date>$dateIso</dc:date>
    <meta property="dcterms:modified">$dateIso</meta>
  </metadata>
  <manifest>
    <item id="style" href="style.css" media-type="text/css"/>
    <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
  </manifest>
  <spine toc="ncx">
    <itemref idref="chapter1"/>
  </spine>
</package>"""
    }

    private fun buildTocNcx(document: TypableDocument): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="urn:uuid:${document.id}"/>
    <meta name="dtb:depth" content="1"/>
    <meta name="dtb:totalPageCount" content="0"/>
    <meta name="dtb:maxPageNumber" content="0"/>
  </head>
  <docTitle>
    <text>${escapeXml(document.title)}</text>
  </docTitle>
  <navMap>
    <navPoint id="navPoint-1" playOrder="1">
      <navLabel>
        <text>${escapeXml(document.title)}</text>
      </navLabel>
      <content src="chapter1.xhtml"/>
    </navPoint>
  </navMap>
</ncx>"""
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun importFromEpub(inputStream: InputStream): TypableDocument {
        val blocks = mutableListOf<DocumentBlock>()
        var docTitle = "Imported eBook"

        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase(Locale.ROOT)
                if (name.endsWith(".xhtml") || name.endsWith(".html")) {
                    val html = zis.readBytes().toString(StandardCharsets.UTF_8)
                    val parsed = HtmlFormat.importFromHtml(html.byteInputStream())
                    blocks.addAll(parsed.blocks)
                    if (docTitle == "Imported eBook" && parsed.title != "Imported Document") {
                        docTitle = parsed.title
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(DocumentBlock.Paragraph(runs = listOf(TextRun("Imported empty eBook"))))
        }

        return TypableDocument(
            id = UUID.randomUUID().toString(),
            title = docTitle,
            blocks = blocks
        )
    }
}
