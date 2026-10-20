package com.example.model

import java.util.UUID

/**
 * Supported page sizes with physical dimensions in typographical points (1 pt = 1/72 inch).
 */
enum class PageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 × 297 mm)", 595, 842),
    LETTER("US Letter (8.5 × 11 in)", 612, 792),
    LEGAL("US Legal (8.5 × 14 in)", 612, 1008)
}

enum class PageOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

enum class PageMargins(val displayName: String, val marginPt: Int) {
    NORMAL("Normal (1 in / 72 pt)", 72),
    NARROW("Narrow (0.5 in / 36 pt)", 36),
    WIDE("Wide (1.5 in / 108 pt)", 108)
}

enum class DocumentFont(val displayName: String, val fontName: String) {
    SYSTEM_SANS("Sans-Serif", "sans-serif"),
    SERIF("Serif (Classic)", "serif"),
    MONOSPACE("Monospace (Code)", "monospace")
}

enum class BlockStyle(val displayName: String, val defaultFontSizeSp: Float, val isHeading: Boolean = false) {
    NORMAL("Normal Text", 15f),
    TITLE("Title", 28f, true),
    SUBTITLE("Subtitle", 20f, true),
    HEADING_1("Heading 1", 24f, true),
    HEADING_2("Heading 2", 20f, true),
    HEADING_3("Heading 3", 17f, true),
    QUOTE("Blockquote", 15f),
    CODE("Code Block", 13f)
}

enum class TextAlignment(val displayName: String) {
    LEFT("Align Left"),
    CENTER("Center"),
    RIGHT("Align Right"),
    JUSTIFY("Justify")
}

enum class ListType {
    NONE,
    BULLET,
    NUMBERED
}

data class PageSettings(
    val pageSize: PageSize = PageSize.A4,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val margins: PageMargins = PageMargins.NORMAL,
    val backgroundColorHex: String = "#FFFFFF",
    val showPageNumbers: Boolean = true,
    val headerText: String = "",
    val footerText: String = ""
) {
    val activeWidthPt: Int
        get() = if (orientation == PageOrientation.PORTRAIT) pageSize.widthPt else pageSize.heightPt

    val activeHeightPt: Int
        get() = if (orientation == PageOrientation.PORTRAIT) pageSize.heightPt else pageSize.widthPt

    val contentWidthPt: Int
        get() = activeWidthPt - (margins.marginPt * 2)

    val contentHeightPt: Int
        get() = activeHeightPt - (margins.marginPt * 2)
}

/**
 * TextRun represents a formatted contiguous sequence of text within a paragraph.
 */
data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isSuperscript: Boolean = false,
    val isSubscript: Boolean = false,
    val fontSizeSp: Float = 15f,
    val textColorHex: String? = null,
    val highlightColorHex: String? = null,
    val fontFamily: DocumentFont = DocumentFont.SYSTEM_SANS,
    val linkUrl: String? = null
)

/**
 * Table cell model.
 */
data class TableCell(
    val id: String = UUID.randomUUID().toString(),
    val runs: List<TextRun> = listOf(TextRun("")),
    val alignment: TextAlignment = TextAlignment.LEFT,
    val backgroundColorHex: String? = null
) {
    val plainText: String
        get() = runs.joinToString("") { it.text }
}

data class TableRow(
    val id: String = UUID.randomUUID().toString(),
    val cells: List<TableCell> = emptyList(),
    val isHeader: Boolean = false
)

/**
 * Base sealed class for document elements.
 */
sealed class DocumentBlock {
    abstract val id: String

    data class Paragraph(
        override val id: String = UUID.randomUUID().toString(),
        val style: BlockStyle = BlockStyle.NORMAL,
        val alignment: TextAlignment = TextAlignment.LEFT,
        val lineSpacing: Float = 1.15f,
        val indentationLevel: Int = 0,
        val listType: ListType = ListType.NONE,
        val listIndex: Int = 1,
        val runs: List<TextRun> = listOf(TextRun(""))
    ) : DocumentBlock() {
        val plainText: String
            get() = runs.joinToString("") { it.text }
    }

    data class Table(
        override val id: String = UUID.randomUUID().toString(),
        val rows: List<TableRow> = emptyList(),
        val columnWeights: List<Float> = emptyList()
    ) : DocumentBlock()

    data class Divider(
        override val id: String = UUID.randomUUID().toString()
    ) : DocumentBlock()

    data class PageBreak(
        override val id: String = UUID.randomUUID().toString()
    ) : DocumentBlock()
}

/**
 * Canonical document representation in Typable.
 */
data class TypableDocument(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Document",
    val author: String = "Typable User",
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val deletedAt: Long? = null,
    val pageSettings: PageSettings = PageSettings(),
    val blocks: List<DocumentBlock> = listOf(DocumentBlock.Paragraph()),
    val version: Int = 1
) {
    /**
     * Computes complete text content across all blocks.
     */
    fun extractPlainText(): String {
        val sb = StringBuilder()
        for (block in blocks) {
            when (block) {
                is DocumentBlock.Paragraph -> {
                    sb.append(block.plainText).append("\n")
                }
                is DocumentBlock.Table -> {
                    for (row in block.rows) {
                        sb.append(row.cells.joinToString(" | ") { it.plainText }).append("\n")
                    }
                }
                is DocumentBlock.Divider -> sb.append("---\n")
                is DocumentBlock.PageBreak -> sb.append("\n[Page Break]\n\n")
            }
        }
        return sb.toString()
    }

    /**
     * Real-time statistics.
     */
    fun computeStats(): DocumentStats {
        val fullText = extractPlainText()
        // Regex word splitting respecting unicode word boundaries
        val words = fullText.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.any { ch -> ch.isLetterOrDigit() } }

        val charCountWithSpaces = fullText.length
        val charCountWithoutSpaces = fullText.count { !it.isWhitespace() }
        val paragraphCount = blocks.count { it is DocumentBlock.Paragraph }

        // Realistic estimated page count based on word & block count
        val approxLinesPerWord = 0.15f
        val linesEstimate = words.size * approxLinesPerWord + (paragraphCount * 1.2f)
        val linesPerPage = 42
        val pageCount = maxOf(1, kotlin.math.ceil(linesEstimate / linesPerPage).toInt() + blocks.count { it is DocumentBlock.PageBreak })

        val readingTimeMinutes = maxOf(1, kotlin.math.ceil(words.size / 200.0).toInt())

        return DocumentStats(
            wordCount = words.size,
            characterCountWithSpaces = charCountWithSpaces,
            characterCountWithoutSpaces = charCountWithoutSpaces,
            paragraphCount = paragraphCount,
            pageCount = pageCount,
            readingTimeMinutes = readingTimeMinutes
        )
    }
}

data class DocumentStats(
    val wordCount: Int,
    val characterCountWithSpaces: Int,
    val characterCountWithoutSpaces: Int,
    val paragraphCount: Int,
    val pageCount: Int,
    val readingTimeMinutes: Int
)

/**
 * Snapshot for Undo/Redo stack.
 */
data class DocumentSnapshot(
    val blocks: List<DocumentBlock>,
    val title: String,
    val pageSettings: PageSettings,
    val activeBlockIndex: Int = 0,
    val cursorOffset: Int = 0
)
