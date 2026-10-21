package com.example.engine

import com.example.model.*
import java.util.UUID

/**
 * High-performance, deterministic document manipulation engine.
 * Pure functional state transformations with robust undo/redo history.
 */
class DocumentEngine(initialDocument: TypableDocument) {

    var currentDocument: TypableDocument = initialDocument
        private set

    private val undoStack = mutableListOf<DocumentSnapshot>()
    private val redoStack = mutableListOf<DocumentSnapshot>()
    private val maxUndoLevels = 50

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    private fun pushSnapshot() {
        if (undoStack.size >= maxUndoLevels) {
            undoStack.removeAt(0)
        }
        undoStack.add(
            DocumentSnapshot(
                blocks = currentDocument.blocks,
                title = currentDocument.title,
                pageSettings = currentDocument.pageSettings
            )
        )
        redoStack.clear()
    }

    fun undo(): Boolean {
        if (!canUndo) return false
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(
            DocumentSnapshot(
                blocks = currentDocument.blocks,
                title = currentDocument.title,
                pageSettings = currentDocument.pageSettings
            )
        )
        currentDocument = currentDocument.copy(
            blocks = previous.blocks,
            title = previous.title,
            pageSettings = previous.pageSettings,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
        return true
    }

    fun redo(): Boolean {
        if (!canRedo) return false
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(
            DocumentSnapshot(
                blocks = currentDocument.blocks,
                title = currentDocument.title,
                pageSettings = currentDocument.pageSettings
            )
        )
        currentDocument = currentDocument.copy(
            blocks = next.blocks,
            title = next.title,
            pageSettings = next.pageSettings,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
        return true
    }

    fun setDocument(document: TypableDocument) {
        currentDocument = document
        undoStack.clear()
        redoStack.clear()
    }

    fun updateTitle(newTitle: String) {
        if (currentDocument.title == newTitle) return
        pushSnapshot()
        currentDocument = currentDocument.copy(
            title = newTitle,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun updatePageSettings(settings: PageSettings) {
        pushSnapshot()
        currentDocument = currentDocument.copy(
            pageSettings = settings,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    // --- Block Updates ---

    private var lastTextEditTime: Long = 0L
    private var lastEditedBlockIndex: Int = -1

    fun updateParagraphText(blockIndex: Int, newText: String) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return
        if (block.plainText == newText) return

        val now = System.currentTimeMillis()
        if (lastEditedBlockIndex != blockIndex || now - lastTextEditTime > 800L) {
            pushSnapshot()
        }
        lastTextEditTime = now
        lastEditedBlockIndex = blockIndex

        val baseRun = block.runs.firstOrNull() ?: TextRun("")
        val updatedRuns = listOf(baseRun.copy(text = newText))

        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = block.copy(runs = updatedRuns)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = now,
            version = currentDocument.version + 1
        )
    }

    fun splitParagraph(
        blockIndex: Int,
        cursorOffset: Int,
        explicitBeforeText: String? = null,
        explicitAfterText: String? = null
    ): Int {
        if (blockIndex !in currentDocument.blocks.indices) return blockIndex
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return blockIndex

        val fullText = explicitBeforeText?.let { it + (explicitAfterText ?: "") } ?: block.plainText

        // Check if pressing Enter on an empty list bullet/numbered item -> exit list mode
        if (block.listType != ListType.NONE && fullText.isBlank()) {
            pushSnapshot()
            lastEditedBlockIndex = -1
            val newBlocks = currentDocument.blocks.toMutableList()
            newBlocks[blockIndex] = block.copy(listType = ListType.NONE, listIndex = 1)
            currentDocument = currentDocument.copy(
                blocks = newBlocks,
                modifiedAt = System.currentTimeMillis(),
                version = currentDocument.version + 1
            )
            return blockIndex
        }

        pushSnapshot()
        lastEditedBlockIndex = -1

        val beforeText = explicitBeforeText ?: run {
            val safeOffset = cursorOffset.coerceIn(0, fullText.length)
            fullText.substring(0, safeOffset)
        }
        val afterText = explicitAfterText ?: run {
            val safeOffset = cursorOffset.coerceIn(0, fullText.length)
            fullText.substring(safeOffset)
        }

        val baseRun = block.runs.firstOrNull() ?: TextRun("")

        // Check if list continuation
        val nextListType = block.listType
        val nextListIndex = if (nextListType == ListType.NUMBERED) block.listIndex + 1 else 1

        val blockBefore = block.copy(runs = listOf(baseRun.copy(text = beforeText)))
        val blockAfter = DocumentBlock.Paragraph(
            id = UUID.randomUUID().toString(),
            style = if (block.style.isHeading) BlockStyle.NORMAL else block.style,
            alignment = block.alignment,
            lineSpacing = block.lineSpacing,
            indentationLevel = block.indentationLevel,
            listType = nextListType,
            listIndex = nextListIndex,
            runs = listOf(baseRun.copy(text = afterText))
        )

        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = blockBefore
        newBlocks.add(blockIndex + 1, blockAfter)

        // Renumber succeeding numbered list items if applicable
        if (nextListType == ListType.NUMBERED) {
            var counter = nextListIndex + 1
            for (i in (blockIndex + 2) until newBlocks.size) {
                val nextP = newBlocks[i] as? DocumentBlock.Paragraph ?: break
                if (nextP.listType == ListType.NUMBERED) {
                    newBlocks[i] = nextP.copy(listIndex = counter++)
                } else break
            }
        }

        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
        return blockIndex + 1
    }

    fun mergeWithPrevious(blockIndex: Int): Int {
        if (blockIndex !in currentDocument.blocks.indices) return blockIndex
        val currentBlock = currentDocument.blocks[blockIndex]

        lastEditedBlockIndex = -1

        if (currentBlock is DocumentBlock.Paragraph) {
            // 1. If indented, pressing backspace at start reduces indent (outdent)
            if (currentBlock.indentationLevel > 0) {
                pushSnapshot()
                val newBlocks = currentDocument.blocks.toMutableList()
                newBlocks[blockIndex] = currentBlock.copy(indentationLevel = currentBlock.indentationLevel - 1)
                currentDocument = currentDocument.copy(
                    blocks = newBlocks,
                    modifiedAt = System.currentTimeMillis(),
                    version = currentDocument.version + 1
                )
                return blockIndex
            }

            // 2. If list item with empty text, backspace turns off list formatting
            if (currentBlock.listType != ListType.NONE && currentBlock.plainText.isEmpty()) {
                pushSnapshot()
                val newBlocks = currentDocument.blocks.toMutableList()
                newBlocks[blockIndex] = currentBlock.copy(listType = ListType.NONE, listIndex = 1)
                currentDocument = currentDocument.copy(
                    blocks = newBlocks,
                    modifiedAt = System.currentTimeMillis(),
                    version = currentDocument.version + 1
                )
                return blockIndex
            }

            // 3. If heading/title with empty text, backspace resets to Normal paragraph
            if (currentBlock.style != BlockStyle.NORMAL && currentBlock.plainText.isEmpty()) {
                pushSnapshot()
                val newBlocks = currentDocument.blocks.toMutableList()
                newBlocks[blockIndex] = currentBlock.copy(style = BlockStyle.NORMAL)
                currentDocument = currentDocument.copy(
                    blocks = newBlocks,
                    modifiedAt = System.currentTimeMillis(),
                    version = currentDocument.version + 1
                )
                return blockIndex
            }
        }

        // 4. Cannot merge if first block
        if (blockIndex <= 0) return blockIndex

        val prevBlock = currentDocument.blocks[blockIndex - 1]

        // 5. If previous block is divider or page break, remove that previous block
        if (prevBlock is DocumentBlock.Divider || prevBlock is DocumentBlock.PageBreak) {
            pushSnapshot()
            val newBlocks = currentDocument.blocks.toMutableList()
            newBlocks.removeAt(blockIndex - 1)
            currentDocument = currentDocument.copy(
                blocks = newBlocks,
                modifiedAt = System.currentTimeMillis(),
                version = currentDocument.version + 1
            )
            return (blockIndex - 1).coerceAtLeast(0)
        }

        // 6. If current block is an empty paragraph, simply delete it
        if (currentBlock is DocumentBlock.Paragraph && currentBlock.plainText.isEmpty() && currentDocument.blocks.size > 1) {
            pushSnapshot()
            val newBlocks = currentDocument.blocks.toMutableList()
            newBlocks.removeAt(blockIndex)
            currentDocument = currentDocument.copy(
                blocks = newBlocks,
                modifiedAt = System.currentTimeMillis(),
                version = currentDocument.version + 1
            )
            return (blockIndex - 1).coerceAtLeast(0)
        }

        // 7. If both are paragraphs, concatenate text
        if (currentBlock is DocumentBlock.Paragraph && prevBlock is DocumentBlock.Paragraph) {
            pushSnapshot()
            val basePrevRun = prevBlock.runs.firstOrNull() ?: TextRun("")
            val mergedText = prevBlock.plainText + currentBlock.plainText
            val mergedRuns = listOf(basePrevRun.copy(text = mergedText))

            val newBlocks = currentDocument.blocks.toMutableList()
            newBlocks[blockIndex - 1] = prevBlock.copy(runs = mergedRuns)
            newBlocks.removeAt(blockIndex)

            currentDocument = currentDocument.copy(
                blocks = newBlocks,
                modifiedAt = System.currentTimeMillis(),
                version = currentDocument.version + 1
            )
            return blockIndex - 1
        }
        return blockIndex
    }

    // --- Formatting Toggles ---

    fun toggleBold(blockIndex: Int) {
        transformBlockRuns(blockIndex) { it.copy(isBold = !it.isBold) }
    }

    fun toggleItalic(blockIndex: Int) {
        transformBlockRuns(blockIndex) { it.copy(isItalic = !it.isItalic) }
    }

    fun toggleUnderline(blockIndex: Int) {
        transformBlockRuns(blockIndex) { it.copy(isUnderline = !it.isUnderline) }
    }

    fun toggleStrikethrough(blockIndex: Int) {
        transformBlockRuns(blockIndex) { it.copy(isStrikethrough = !it.isStrikethrough) }
    }

    fun toggleSuperscript(blockIndex: Int) {
        transformBlockRuns(blockIndex) { it.copy(isSuperscript = !it.isSuperscript, isSubscript = false) }
    }

    fun toggleSubscript(blockIndex: Int) {
        transformBlockRuns(blockIndex) { it.copy(isSubscript = !it.isSubscript, isSuperscript = false) }
    }

    fun applyColor(blockIndex: Int, colorHex: String?) {
        transformBlockRuns(blockIndex) { it.copy(textColorHex = colorHex) }
    }

    fun applyHighlight(blockIndex: Int, highlightHex: String?) {
        transformBlockRuns(blockIndex) { it.copy(highlightColorHex = highlightHex) }
    }

    fun applyFontFamily(blockIndex: Int, font: DocumentFont, customFontName: String? = null) {
        transformBlockRuns(blockIndex) { it.copy(fontFamily = font, customFontName = customFontName) }
    }

    fun applyFontSize(blockIndex: Int, deltaSp: Float) {
        transformBlockRuns(blockIndex) {
            val newSize = (it.fontSizeSp + deltaSp).coerceIn(10f, 48f)
            it.copy(fontSizeSp = newSize)
        }
    }

    fun clearFormatting(blockIndex: Int) {
        transformBlockRuns(blockIndex) {
            TextRun(
                text = it.text,
                fontSizeSp = 15f
            )
        }
    }

    fun applyStyle(blockIndex: Int, style: BlockStyle) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return

        pushSnapshot()
        val updatedRuns = block.runs.map {
            it.copy(
                fontSizeSp = style.defaultFontSizeSp,
                isBold = style.isHeading
            )
        }
        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = block.copy(style = style, runs = updatedRuns)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun applyAlignment(blockIndex: Int, alignment: TextAlignment) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return

        pushSnapshot()
        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = block.copy(alignment = alignment)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun toggleBulletList(blockIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return

        pushSnapshot()
        val nextType = if (block.listType == ListType.BULLET) ListType.NONE else ListType.BULLET
        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = block.copy(listType = nextType)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun toggleNumberedList(blockIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return

        pushSnapshot()
        val nextType = if (block.listType == ListType.NUMBERED) ListType.NONE else ListType.NUMBERED
        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = block.copy(listType = nextType, listIndex = 1)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun indentBlock(blockIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return
        if (block.indentationLevel >= 4) return

        pushSnapshot()
        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = block.copy(indentationLevel = block.indentationLevel + 1)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun outdentBlock(blockIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return
        if (block.indentationLevel <= 0) return

        pushSnapshot()
        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = block.copy(indentationLevel = block.indentationLevel - 1)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    // --- Table Operations ---

    fun insertTable(blockIndex: Int, rows: Int = 3, cols: Int = 3): Int {
        pushSnapshot()
        val tableRows = mutableListOf<TableRow>()
        for (r in 0 until rows) {
            val cells = mutableListOf<TableCell>()
            for (c in 0 until cols) {
                cells.add(TableCell(runs = listOf(TextRun(if (r == 0) "Header ${c + 1}" else "Data"))))
            }
            tableRows.add(TableRow(cells = cells, isHeader = r == 0))
        }
        val tableBlock = DocumentBlock.Table(rows = tableRows)
        val newBlocks = currentDocument.blocks.toMutableList()
        val insertPos = (blockIndex + 1).coerceIn(0, newBlocks.size)
        newBlocks.add(insertPos, tableBlock)

        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
        return insertPos
    }

    fun updateTableCell(blockIndex: Int, rowIndex: Int, colIndex: Int, text: String) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val table = currentDocument.blocks[blockIndex] as? DocumentBlock.Table ?: return
        if (rowIndex !in table.rows.indices) return
        val row = table.rows[rowIndex]
        if (colIndex !in row.cells.indices) return

        pushSnapshot()
        val newCells = row.cells.toMutableList()
        newCells[colIndex] = newCells[colIndex].copy(runs = listOf(TextRun(text)))

        val newRows = table.rows.toMutableList()
        newRows[rowIndex] = row.copy(cells = newCells)

        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = table.copy(rows = newRows)

        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun addTableRow(blockIndex: Int, afterRowIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val table = currentDocument.blocks[blockIndex] as? DocumentBlock.Table ?: return
        val colCount = table.rows.firstOrNull()?.cells?.size ?: 3

        pushSnapshot()
        val newCells = (0 until colCount).map { TableCell(runs = listOf(TextRun(""))) }
        val newRow = TableRow(cells = newCells, isHeader = false)

        val newRows = table.rows.toMutableList()
        val insertPos = (afterRowIndex + 1).coerceIn(0, newRows.size)
        newRows.add(insertPos, newRow)

        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = table.copy(rows = newRows)

        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun deleteTableRow(blockIndex: Int, rowIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val table = currentDocument.blocks[blockIndex] as? DocumentBlock.Table ?: return
        if (table.rows.size <= 1 || rowIndex !in table.rows.indices) return

        pushSnapshot()
        val newRows = table.rows.toMutableList()
        newRows.removeAt(rowIndex)

        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = table.copy(rows = newRows)

        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun addTableColumn(blockIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val table = currentDocument.blocks[blockIndex] as? DocumentBlock.Table ?: return

        pushSnapshot()
        val newRows = table.rows.map { row ->
            val updatedCells = row.cells.toMutableList()
            updatedCells.add(TableCell(runs = listOf(TextRun(""))))
            row.copy(cells = updatedCells)
        }

        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = table.copy(rows = newRows)

        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    fun deleteTableColumn(blockIndex: Int, colIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val table = currentDocument.blocks[blockIndex] as? DocumentBlock.Table ?: return
        if (table.rows.firstOrNull()?.cells?.size ?: 0 <= 1) return

        pushSnapshot()
        val newRows = table.rows.map { row ->
            if (colIndex in row.cells.indices) {
                val updatedCells = row.cells.toMutableList()
                updatedCells.removeAt(colIndex)
                row.copy(cells = updatedCells)
            } else row
        }

        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = table.copy(rows = newRows)

        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    // --- Dividers & Page Breaks ---

    fun insertDivider(blockIndex: Int): Int {
        pushSnapshot()
        val newBlocks = currentDocument.blocks.toMutableList()
        val insertPos = (blockIndex + 1).coerceIn(0, newBlocks.size)
        newBlocks.add(insertPos, DocumentBlock.Divider())
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
        return insertPos
    }

    fun insertPageBreak(blockIndex: Int): Int {
        pushSnapshot()
        val newBlocks = currentDocument.blocks.toMutableList()
        val insertPos = (blockIndex + 1).coerceIn(0, newBlocks.size)
        newBlocks.add(insertPos, DocumentBlock.PageBreak())
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
        return insertPos
    }

    fun insertBlocks(atIndex: Int, newBlocks: List<DocumentBlock>): Int {
        if (newBlocks.isEmpty()) return atIndex
        pushSnapshot()
        val mutable = currentDocument.blocks.toMutableList()
        val insertPos = (atIndex + 1).coerceIn(0, mutable.size)
        mutable.addAll(insertPos, newBlocks)
        currentDocument = currentDocument.copy(
            blocks = mutable,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
        return insertPos + newBlocks.size - 1
    }

    fun deleteBlock(blockIndex: Int) {
        if (blockIndex !in currentDocument.blocks.indices) return
        if (currentDocument.blocks.size <= 1) return // Keep at least one block

        pushSnapshot()
        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks.removeAt(blockIndex)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }

    // --- Search & Replace ---

    data class SearchMatch(
        val blockIndex: Int,
        val startIndex: Int,
        val length: Int,
        val snippet: String
    )

    fun findMatches(query: String, matchCase: Boolean, wholeWord: Boolean): List<SearchMatch> {
        if (query.isBlank()) return emptyList()
        val matches = mutableListOf<SearchMatch>()

        for (i in currentDocument.blocks.indices) {
            val block = currentDocument.blocks[i]
            val text = when (block) {
                is DocumentBlock.Paragraph -> block.plainText
                is DocumentBlock.Table -> block.rows.flatMap { it.cells }.joinToString(" ") { it.plainText }
                else -> ""
            }
            if (text.isEmpty()) continue

            var searchPattern = Regex.escape(query)
            if (wholeWord) {
                searchPattern = "\\b$searchPattern\\b"
            }
            val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = Regex(searchPattern, options)

            for (m in regex.findAll(text)) {
                val start = m.range.first
                val snippetStart = (start - 15).coerceAtLeast(0)
                val snippetEnd = (m.range.last + 16).coerceAtMost(text.length)
                val snippet = text.substring(snippetStart, snippetEnd)
                matches.add(SearchMatch(blockIndex = i, startIndex = start, length = query.length, snippet = snippet))
            }
        }
        return matches
    }

    fun replaceMatch(match: SearchMatch, replacement: String): Boolean {
        if (match.blockIndex !in currentDocument.blocks.indices) return false
        val block = currentDocument.blocks[match.blockIndex] as? DocumentBlock.Paragraph ?: return false
        val text = block.plainText
        if (match.startIndex + match.length > text.length) return false

        pushSnapshot()
        val newText = text.substring(0, match.startIndex) + replacement + text.substring(match.startIndex + match.length)
        updateParagraphText(match.blockIndex, newText)
        return true
    }

    fun replaceAllMatches(query: String, replacement: String, matchCase: Boolean, wholeWord: Boolean): Int {
        val matches = findMatches(query, matchCase, wholeWord)
        if (matches.isEmpty()) return 0

        pushSnapshot()
        var replacedCount = 0
        val newBlocks = currentDocument.blocks.toMutableList()

        for (i in newBlocks.indices) {
            val block = newBlocks[i] as? DocumentBlock.Paragraph ?: continue
            val text = block.plainText
            var searchPattern = Regex.escape(query)
            if (wholeWord) searchPattern = "\\b$searchPattern\\b"
            val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val regex = Regex(searchPattern, options)

            if (regex.containsMatchIn(text)) {
                val newText = regex.replace(text, replacement)
                val baseRun = block.runs.firstOrNull() ?: TextRun("")
                newBlocks[i] = block.copy(runs = listOf(baseRun.copy(text = newText)))
                replacedCount++
            }
        }

        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
        return replacedCount
    }

    private fun transformBlockRuns(blockIndex: Int, transform: (TextRun) -> TextRun) {
        if (blockIndex !in currentDocument.blocks.indices) return
        val block = currentDocument.blocks[blockIndex] as? DocumentBlock.Paragraph ?: return

        pushSnapshot()
        val updatedRuns = block.runs.map { transform(it) }
        val newBlocks = currentDocument.blocks.toMutableList()
        newBlocks[blockIndex] = block.copy(runs = updatedRuns)
        currentDocument = currentDocument.copy(
            blocks = newBlocks,
            modifiedAt = System.currentTimeMillis(),
            version = currentDocument.version + 1
        )
    }
}
