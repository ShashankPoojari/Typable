package com.example.storage

import android.content.Context
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Robust, atomic local file storage and metadata manager for Typable documents.
 */
class DocumentStorage(private val context: Context) {

    private val documentsDir: File by lazy {
        val dir = File(context.filesDir, "typable_docs")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    init {
        // Initialize sample templates on first run if no documents exist
        initializeDefaultDocumentsIfEmpty()
    }

    @Synchronized
    fun saveDocument(document: TypableDocument): Boolean {
        return try {
            val file = File(documentsDir, "${document.id}.json")
            val tempFile = File(documentsDir, "${document.id}.tmp")
            val backupFile = File(documentsDir, "${document.id}.bak")

            val jsonString = serializeDocument(document)

            // Write to temp file first with atomic flush
            FileOutputStream(tempFile).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
                fos.fd.sync()
            }

            // If main file exists, backup to .bak
            if (file.exists()) {
                if (backupFile.exists()) backupFile.delete()
                file.copyTo(backupFile, overwrite = true)
            }

            // Atomic rename
            val renamed = tempFile.renameTo(file)
            if (!renamed) {
                // Fallback copy if rename fails
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Synchronized
    fun loadDocument(id: String): TypableDocument? {
        val file = File(documentsDir, "$id.json")
        val backupFile = File(documentsDir, "$id.bak")

        if (!file.exists() && backupFile.exists()) {
            // Restore from backup
            try {
                backupFile.copyTo(file, overwrite = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (!file.exists()) return null

        return try {
            val jsonString = file.readText(StandardCharsets.UTF_8)
            deserializeDocument(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            // Attempt recovery from backup
            if (backupFile.exists()) {
                try {
                    val backupJson = backupFile.readText(StandardCharsets.UTF_8)
                    deserializeDocument(backupJson)
                } catch (be: Exception) {
                    be.printStackTrace()
                    null
                }
            } else {
                null
            }
        }
    }

    @Synchronized
    fun getAllDocuments(): List<TypableDocument> {
        val list = mutableListOf<TypableDocument>()
        val files = documentsDir.listFiles { _, name -> name.endsWith(".json") } ?: return emptyList()

        for (file in files) {
            try {
                val jsonString = file.readText(StandardCharsets.UTF_8)
                val doc = deserializeDocument(jsonString)
                list.add(doc)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list.sortedByDescending { it.modifiedAt }
    }

    @Synchronized
    fun duplicateDocument(documentId: String, newTitleSuffix: String = " (Copy)"): TypableDocument? {
        val original = loadDocument(documentId) ?: return null
        val duplicated = original.copy(
            id = UUID.randomUUID().toString(),
            title = original.title + newTitleSuffix,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            isPinned = false,
            isTrash = false,
            deletedAt = null
        )
        val success = saveDocument(duplicated)
        return if (success) duplicated else null
    }

    @Synchronized
    fun moveToTrash(documentId: String): Boolean {
        val doc = loadDocument(documentId) ?: return false
        val updated = doc.copy(
            isTrash = true,
            deletedAt = System.currentTimeMillis(),
            isPinned = false
        )
        return saveDocument(updated)
    }

    @Synchronized
    fun restoreFromTrash(documentId: String): Boolean {
        val doc = loadDocument(documentId) ?: return false
        val updated = doc.copy(
            isTrash = false,
            deletedAt = null
        )
        return saveDocument(updated)
    }

    @Synchronized
    fun permanentlyDeleteDocument(documentId: String): Boolean {
        val file = File(documentsDir, "$documentId.json")
        val backupFile = File(documentsDir, "$documentId.bak")
        var deleted = true
        if (file.exists()) deleted = file.delete()
        if (backupFile.exists()) backupFile.delete()
        return deleted
    }

    @Synchronized
    fun toggleFavorite(documentId: String): Boolean {
        val doc = loadDocument(documentId) ?: return false
        val updated = doc.copy(isFavorite = !doc.isFavorite, modifiedAt = System.currentTimeMillis())
        return saveDocument(updated)
    }

    @Synchronized
    fun togglePinned(documentId: String): Boolean {
        val doc = loadDocument(documentId) ?: return false
        val updated = doc.copy(isPinned = !doc.isPinned, modifiedAt = System.currentTimeMillis())
        return saveDocument(updated)
    }

    @Synchronized
    fun renameDocument(documentId: String, newTitle: String): Boolean {
        val doc = loadDocument(documentId) ?: return false
        val updated = doc.copy(title = newTitle.trim(), modifiedAt = System.currentTimeMillis())
        return saveDocument(updated)
    }

    // --- Serialization & Deserialization ---

    fun serializeDocument(doc: TypableDocument): String {
        val root = JSONObject()
        root.put("id", doc.id)
        root.put("title", doc.title)
        root.put("author", doc.author)
        root.put("createdAt", doc.createdAt)
        root.put("modifiedAt", doc.modifiedAt)
        root.put("isPinned", doc.isPinned)
        root.put("isFavorite", doc.isFavorite)
        root.put("isTrash", doc.isTrash)
        if (doc.deletedAt != null) root.put("deletedAt", doc.deletedAt)
        root.put("version", doc.version)

        // Page settings
        val pageObj = JSONObject()
        pageObj.put("pageSize", doc.pageSettings.pageSize.name)
        pageObj.put("orientation", doc.pageSettings.orientation.name)
        pageObj.put("margins", doc.pageSettings.margins.name)
        pageObj.put("backgroundColorHex", doc.pageSettings.backgroundColorHex)
        pageObj.put("showPageNumbers", doc.pageSettings.showPageNumbers)
        pageObj.put("headerText", doc.pageSettings.headerText)
        pageObj.put("footerText", doc.pageSettings.footerText)
        root.put("pageSettings", pageObj)

        // Blocks
        val blocksArr = JSONArray()
        for (block in doc.blocks) {
            val bObj = JSONObject()
            bObj.put("id", block.id)
            when (block) {
                is DocumentBlock.Paragraph -> {
                    bObj.put("type", "paragraph")
                    bObj.put("style", block.style.name)
                    bObj.put("alignment", block.alignment.name)
                    bObj.put("lineSpacing", block.lineSpacing.toDouble())
                    bObj.put("indentationLevel", block.indentationLevel)
                    bObj.put("listType", block.listType.name)
                    bObj.put("listIndex", block.listIndex)

                    val runsArr = JSONArray()
                    for (run in block.runs) {
                        val rObj = JSONObject()
                        rObj.put("text", run.text)
                        rObj.put("bold", run.isBold)
                        rObj.put("italic", run.isItalic)
                        rObj.put("underline", run.isUnderline)
                        rObj.put("strike", run.isStrikethrough)
                        rObj.put("superscript", run.isSuperscript)
                        rObj.put("subscript", run.isSubscript)
                        rObj.put("fontSizeSp", run.fontSizeSp.toDouble())
                        if (run.textColorHex != null) rObj.put("color", run.textColorHex)
                        if (run.highlightColorHex != null) rObj.put("highlight", run.highlightColorHex)
                        rObj.put("fontFamily", run.fontFamily.name)
                        if (run.linkUrl != null) rObj.put("linkUrl", run.linkUrl)
                        runsArr.put(rObj)
                    }
                    bObj.put("runs", runsArr)
                }
                is DocumentBlock.Table -> {
                    bObj.put("type", "table")
                    val rowsArr = JSONArray()
                    for (row in block.rows) {
                        val rowObj = JSONObject()
                        rowObj.put("id", row.id)
                        rowObj.put("isHeader", row.isHeader)
                        val cellsArr = JSONArray()
                        for (cell in row.cells) {
                            val cObj = JSONObject()
                            cObj.put("id", cell.id)
                            cObj.put("alignment", cell.alignment.name)
                            if (cell.backgroundColorHex != null) cObj.put("bgColor", cell.backgroundColorHex)
                            val cRuns = JSONArray()
                            for (r in cell.runs) {
                                val rObj = JSONObject()
                                rObj.put("text", r.text)
                                rObj.put("bold", r.isBold)
                                rObj.put("italic", r.isItalic)
                                rObj.put("fontSizeSp", r.fontSizeSp.toDouble())
                                cRuns.put(rObj)
                            }
                            cObj.put("runs", cRuns)
                            cellsArr.put(cObj)
                        }
                        rowObj.put("cells", cellsArr)
                        rowsArr.put(rowObj)
                    }
                    bObj.put("rows", rowsArr)
                }
                is DocumentBlock.Divider -> {
                    bObj.put("type", "divider")
                }
                is DocumentBlock.PageBreak -> {
                    bObj.put("type", "pageBreak")
                }
            }
            blocksArr.put(bObj)
        }
        root.put("blocks", blocksArr)

        return root.toString(2)
    }

    fun deserializeDocument(jsonString: String): TypableDocument {
        val root = JSONObject(jsonString)
        val id = root.optString("id", UUID.randomUUID().toString())
        val title = root.optString("title", "Untitled Document")
        val author = root.optString("author", "Typable User")
        val createdAt = root.optLong("createdAt", System.currentTimeMillis())
        val modifiedAt = root.optLong("modifiedAt", System.currentTimeMillis())
        val isPinned = root.optBoolean("isPinned", false)
        val isFavorite = root.optBoolean("isFavorite", false)
        val isTrash = root.optBoolean("isTrash", false)
        val deletedAt = if (root.has("deletedAt")) root.getLong("deletedAt") else null
        val version = root.optInt("version", 1)

        // Page settings
        val pageSettings = if (root.has("pageSettings")) {
            val pObj = root.getJSONObject("pageSettings")
            PageSettings(
                pageSize = safeValueOf(pObj.optString("pageSize"), PageSize.A4),
                orientation = safeValueOf(pObj.optString("orientation"), PageOrientation.PORTRAIT),
                margins = safeValueOf(pObj.optString("margins"), PageMargins.NORMAL),
                backgroundColorHex = pObj.optString("backgroundColorHex", "#FFFFFF"),
                showPageNumbers = pObj.optBoolean("showPageNumbers", true),
                headerText = pObj.optString("headerText", ""),
                footerText = pObj.optString("footerText", "")
            )
        } else {
            PageSettings()
        }

        // Blocks
        val blocks = mutableListOf<DocumentBlock>()
        val blocksArr = root.optJSONArray("blocks")
        if (blocksArr != null && blocksArr.length() > 0) {
            for (i in 0 until blocksArr.length()) {
                val bObj = blocksArr.getJSONObject(i)
                val blockId = bObj.optString("id", UUID.randomUUID().toString())
                val type = bObj.optString("type", "paragraph")

                when (type) {
                    "table" -> {
                        val rows = mutableListOf<TableRow>()
                        val rowsArr = bObj.optJSONArray("rows")
                        if (rowsArr != null) {
                            for (r in 0 until rowsArr.length()) {
                                val rowObj = rowsArr.getJSONObject(r)
                                val rowId = rowObj.optString("id", UUID.randomUUID().toString())
                                val isHeader = rowObj.optBoolean("isHeader", false)
                                val cells = mutableListOf<TableCell>()
                                val cellsArr = rowObj.optJSONArray("cells")
                                if (cellsArr != null) {
                                    for (c in 0 until cellsArr.length()) {
                                        val cObj = cellsArr.getJSONObject(c)
                                        val cellId = cObj.optString("id", UUID.randomUUID().toString())
                                        val align = safeValueOf(cObj.optString("alignment"), TextAlignment.LEFT)
                                        val bgColor = if (cObj.has("bgColor")) cObj.getString("bgColor") else null
                                        val runs = mutableListOf<TextRun>()
                                        val cRuns = cObj.optJSONArray("runs")
                                        if (cRuns != null) {
                                            for (k in 0 until cRuns.length()) {
                                                val rObj = cRuns.getJSONObject(k)
                                                runs.add(
                                                    TextRun(
                                                        text = rObj.optString("text", ""),
                                                        isBold = rObj.optBoolean("bold", false),
                                                        isItalic = rObj.optBoolean("italic", false),
                                                        fontSizeSp = rObj.optDouble("fontSizeSp", 14.0).toFloat()
                                                    )
                                                )
                                            }
                                        }
                                        if (runs.isEmpty()) runs.add(TextRun(""))
                                        cells.add(TableCell(id = cellId, runs = runs, alignment = align, backgroundColorHex = bgColor))
                                    }
                                }
                                rows.add(TableRow(id = rowId, cells = cells, isHeader = isHeader))
                            }
                        }
                        blocks.add(DocumentBlock.Table(id = blockId, rows = rows))
                    }
                    "divider" -> {
                        blocks.add(DocumentBlock.Divider(id = blockId))
                    }
                    "pageBreak" -> {
                        blocks.add(DocumentBlock.PageBreak(id = blockId))
                    }
                    else -> {
                        // Paragraph
                        val style = safeValueOf(bObj.optString("style"), BlockStyle.NORMAL)
                        val alignment = safeValueOf(bObj.optString("alignment"), TextAlignment.LEFT)
                        val lineSpacing = bObj.optDouble("lineSpacing", 1.15).toFloat()
                        val indent = bObj.optInt("indentationLevel", 0)
                        val listType = safeValueOf(bObj.optString("listType"), ListType.NONE)
                        val listIndex = bObj.optInt("listIndex", 1)

                        val runs = mutableListOf<TextRun>()
                        val runsArr = bObj.optJSONArray("runs")
                        if (runsArr != null) {
                            for (j in 0 until runsArr.length()) {
                                val rObj = runsArr.getJSONObject(j)
                                runs.add(
                                    TextRun(
                                        text = rObj.optString("text", ""),
                                        isBold = rObj.optBoolean("bold", false),
                                        isItalic = rObj.optBoolean("italic", false),
                                        isUnderline = rObj.optBoolean("underline", false),
                                        isStrikethrough = rObj.optBoolean("strike", false),
                                        isSuperscript = rObj.optBoolean("superscript", false),
                                        isSubscript = rObj.optBoolean("subscript", false),
                                        fontSizeSp = rObj.optDouble("fontSizeSp", style.defaultFontSizeSp.toDouble()).toFloat(),
                                        textColorHex = if (rObj.has("color")) rObj.getString("color") else null,
                                        highlightColorHex = if (rObj.has("highlight")) rObj.getString("highlight") else null,
                                        fontFamily = safeValueOf(rObj.optString("fontFamily"), DocumentFont.SYSTEM_SANS),
                                        linkUrl = if (rObj.has("linkUrl")) rObj.getString("linkUrl") else null
                                    )
                                )
                            }
                        }
                        if (runs.isEmpty()) runs.add(TextRun(""))
                        blocks.add(
                            DocumentBlock.Paragraph(
                                id = blockId,
                                style = style,
                                alignment = alignment,
                                lineSpacing = lineSpacing,
                                indentationLevel = indent,
                                listType = listType,
                                listIndex = listIndex,
                                runs = runs
                            )
                        )
                    }
                }
            }
        }
        if (blocks.isEmpty()) {
            blocks.add(DocumentBlock.Paragraph())
        }

        return TypableDocument(
            id = id,
            title = title,
            author = author,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            isPinned = isPinned,
            isFavorite = isFavorite,
            isTrash = isTrash,
            deletedAt = deletedAt,
            pageSettings = pageSettings,
            blocks = blocks,
            version = version
        )
    }

    private inline fun <reified T : Enum<T>> safeValueOf(value: String?, default: T): T {
        if (value == null) return default
        return try {
            java.lang.Enum.valueOf(T::class.java, value)
        } catch (e: Exception) {
            default
        }
    }

    // --- Built-in Templates ---

    private fun initializeDefaultDocumentsIfEmpty() {
        val files = documentsDir.listFiles { _, name -> name.endsWith(".json") }
        if (files == null || files.isEmpty()) {
            createTemplateWelcomeGuide()
            createTemplateBusinessLetter()
        }
    }

    fun createTemplateWelcomeGuide(): TypableDocument {
        val doc = TypableDocument(
            id = UUID.randomUUID().toString(),
            title = "Welcome to Typable",
            author = "Typable Architect",
            isPinned = true,
            isFavorite = true,
            blocks = listOf(
                DocumentBlock.Paragraph(
                    style = BlockStyle.TITLE,
                    alignment = TextAlignment.LEFT,
                    runs = listOf(TextRun("Welcome to Typable", isBold = true, fontSizeSp = 28f, textColorHex = "#0284C7"))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.SUBTITLE,
                    runs = listOf(TextRun("The Professional, Minimalist Word Processor for Android", isItalic = true, fontSizeSp = 18f))
                ),
                DocumentBlock.Divider(),
                DocumentBlock.Paragraph(
                    style = BlockStyle.HEADING_1,
                    runs = listOf(TextRun("1. Pure Writing Experience", isBold = true, fontSizeSp = 22f))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    runs = listOf(
                        TextRun("Typable is built with a clear philosophy: "),
                        TextRun("typography-first, distraction-free, and deterministic. ", isBold = true),
                        TextRun("There are no AI text generators, no ads, no trackers, and no mandatory cloud accounts. Everything is stored securely on your device.")
                    )
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.HEADING_2,
                    runs = listOf(TextRun("Key Capabilities", isBold = true, fontSizeSp = 18f))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    listType = ListType.BULLET,
                    runs = listOf(TextRun("Full Rich Text: Bold, Italic, Underline, Strikethrough, Colors, and Highlights."))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    listType = ListType.BULLET,
                    runs = listOf(TextRun("Document Structure: Heading 1–3, Title, Blockquotes, and Lists."))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    listType = ListType.BULLET,
                    runs = listOf(TextRun("Genuine Export: Microsoft Word (.docx), PDF with page numbers, Markdown, HTML, and Plain Text."))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    listType = ListType.BULLET,
                    runs = listOf(TextRun("Page Layout: Configurable A4, US Letter, US Legal, Margins, Headers & Footers."))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.HEADING_2,
                    runs = listOf(TextRun("Feature Matrix Overview", isBold = true, fontSizeSp = 18f))
                ),
                DocumentBlock.Table(
                    rows = listOf(
                        TableRow(
                            isHeader = true,
                            cells = listOf(
                                TableCell(runs = listOf(TextRun("Format", isBold = true))),
                                TableCell(runs = listOf(TextRun("Import", isBold = true))),
                                TableCell(runs = listOf(TextRun("Export", isBold = true)))
                            )
                        ),
                        TableRow(
                            cells = listOf(
                                TableCell(runs = listOf(TextRun("DOCX"))),
                                TableCell(runs = listOf(TextRun("Read & Edit"))),
                                TableCell(runs = listOf(TextRun("Native OpenXML")))
                            )
                        ),
                        TableRow(
                            cells = listOf(
                                TableCell(runs = listOf(TextRun("PDF"))),
                                TableCell(runs = listOf(TextRun("N/A (Print format)"))),
                                TableCell(runs = listOf(TextRun("Vector Paginated")))
                            )
                        ),
                        TableRow(
                            cells = listOf(
                                TableCell(runs = listOf(TextRun("Markdown"))),
                                TableCell(runs = listOf(TextRun("Full Syntax"))),
                                TableCell(runs = listOf(TextRun("Standard .md")))
                            )
                        )
                    )
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.QUOTE,
                    runs = listOf(TextRun("“Simplicity is about subtracting the obvious and adding the meaningful.” — John Maeda", isItalic = true))
                )
            )
        )
        saveDocument(doc)
        return doc
    }

    fun createTemplateBusinessLetter(): TypableDocument {
        val doc = TypableDocument(
            id = UUID.randomUUID().toString(),
            title = "Business Letter Template",
            author = "Typable User",
            blocks = listOf(
                DocumentBlock.Paragraph(
                    style = BlockStyle.TITLE,
                    runs = listOf(TextRun("Apex Systems International", isBold = true, fontSizeSp = 24f))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    runs = listOf(TextRun("100 Innovation Boulevard • Silicon Valley, CA 94025\ncontact@apexsystems.example • (555) 019-2834"))
                ),
                DocumentBlock.Divider(),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    runs = listOf(TextRun("October 20, 2026\n\nRecipient Name\nDirector of Engineering\nGlobal Enterprise Solutions\n450 Commerce Way, Suite 800\nNew York, NY 10001"))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.HEADING_2,
                    runs = listOf(TextRun("Subject: Strategic Partnership Proposal", isBold = true, fontSizeSp = 18f))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    runs = listOf(TextRun("Dear Partner,\n\nI am writing to formally present our proposal for expanding our collaborative enterprise infrastructure. Over the past quarter, our mutual engineering teams have identified substantial synergies that will significantly decrease latency while bolstering data sovereignty."))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    runs = listOf(TextRun("Key Milestones in Q4:\n• Architecture validation and protocol alignment\n• Offline-first edge node synchronization\n• Final integration review and performance sign-off"))
                ),
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    runs = listOf(TextRun("We look forward to reviewing these specifications during our joint steering committee next week.\n\nSincerely,\n\nExecutive Vice President\nApex Systems International"))
                )
            )
        )
        saveDocument(doc)
        return doc
    }

    fun createBlankDocument(title: String = "Untitled Document"): TypableDocument {
        val doc = TypableDocument(
            id = UUID.randomUUID().toString(),
            title = title,
            blocks = listOf(
                DocumentBlock.Paragraph(
                    style = BlockStyle.NORMAL,
                    runs = listOf(TextRun(""))
                )
            )
        )
        saveDocument(doc)
        return doc
    }

    fun createProjectProposalTemplate(): TypableDocument {
        val doc = TypableDocument(
            id = UUID.randomUUID().toString(),
            title = "Project Proposal",
            blocks = listOf(
                DocumentBlock.Paragraph(style = BlockStyle.TITLE, runs = listOf(TextRun("Project Architecture Proposal", isBold = true))),
                DocumentBlock.Paragraph(style = BlockStyle.SUBTITLE, runs = listOf(TextRun("Prepared for Product Evaluation", isItalic = true))),
                DocumentBlock.Divider(),
                DocumentBlock.Paragraph(style = BlockStyle.HEADING_1, runs = listOf(TextRun("1. Executive Summary", isBold = true))),
                DocumentBlock.Paragraph(style = BlockStyle.NORMAL, runs = listOf(TextRun("This document delineates the technical specifications and operational deliverables for the new initiative."))),
                DocumentBlock.Paragraph(style = BlockStyle.HEADING_1, runs = listOf(TextRun("2. Scope & Deliverables", isBold = true))),
                DocumentBlock.Paragraph(style = BlockStyle.NORMAL, listType = ListType.NUMBERED, listIndex = 1, runs = listOf(TextRun("High-performance core editing engine."))),
                DocumentBlock.Paragraph(style = BlockStyle.NORMAL, listType = ListType.NUMBERED, listIndex = 2, runs = listOf(TextRun("Multi-format importer and exporter (DOCX, PDF, Markdown)."))),
                DocumentBlock.Paragraph(style = BlockStyle.NORMAL, listType = ListType.NUMBERED, listIndex = 3, runs = listOf(TextRun("Automated test coverage and offline data persistence.")))
            )
        )
        saveDocument(doc)
        return doc
    }
}
