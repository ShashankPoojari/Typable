package com.example.formats

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.model.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Genuine vector PDF generation and printing engine for Typable.
 * Adheres to page dimensions, margins, typography, tables, headers, footers, and page numbers.
 */
object PdfExporter {

    fun exportToPdf(document: TypableDocument, outputStream: OutputStream) {
        val pdfDoc = PdfDocument()

        val pageWidth = document.pageSettings.activeWidthPt
        val pageHeight = document.pageSettings.activeHeightPt
        val margin = document.pageSettings.margins.marginPt.toFloat()
        val contentWidth = pageWidth - (margin * 2)

        // Text paints
        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headerFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }

        // Layout pass: split content into pages
        val pages = layoutDocumentPages(document, contentWidth, pageHeight - (margin * 2) - 40f)

        var pageNum = 1
        val totalPages = maxOf(1, pages.size)

        for (pageBlocks in pages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            try {
                canvas.drawColor(Color.parseColor(document.pageSettings.backgroundColorHex))
            } catch (e: Exception) {
                canvas.drawColor(Color.WHITE)
            }

            // Header text
            if (document.pageSettings.headerText.isNotBlank()) {
                canvas.drawText(document.pageSettings.headerText, margin, margin - 15f, headerFooterPaint)
                canvas.drawLine(margin, margin - 8f, pageWidth - margin, margin - 8f, linePaint)
            }

            // Footer text & Page numbering
            val footerY = pageHeight - margin + 20f
            if (document.pageSettings.footerText.isNotBlank()) {
                canvas.drawText(document.pageSettings.footerText, margin, footerY, headerFooterPaint)
            }
            if (document.pageSettings.showPageNumbers) {
                val pageStr = "Page $pageNum of $totalPages"
                val strWidth = headerFooterPaint.measureText(pageStr)
                canvas.drawText(pageStr, pageWidth - margin - strWidth, footerY, headerFooterPaint)
            }
            canvas.drawLine(margin, pageHeight - margin + 8f, pageWidth - margin, pageHeight - margin + 8f, linePaint)

            // Draw block content
            var currentY = margin + 10f

            for (renderable in pageBlocks) {
                when (renderable) {
                    is RenderableParagraph -> {
                        for (line in renderable.lines) {
                            var curX = margin + renderable.indentX
                            if (renderable.listMarker != null && line == renderable.lines.firstOrNull()) {
                                canvas.drawText(renderable.listMarker, margin + renderable.indentX - 16f, currentY, normalPaint)
                            }
                            for (segment in line.segments) {
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    textSize = segment.fontSizePt
                                    color = segment.color
                                    val style = when {
                                        segment.isBold && segment.isItalic -> Typeface.BOLD_ITALIC
                                        segment.isBold -> Typeface.BOLD
                                        segment.isItalic -> Typeface.ITALIC
                                        else -> Typeface.NORMAL
                                    }
                                    typeface = when (segment.font) {
                                        DocumentFont.SERIF -> Typeface.create(Typeface.SERIF, style)
                                        DocumentFont.MONOSPACE -> Typeface.create(Typeface.MONOSPACE, style)
                                        else -> Typeface.create(Typeface.DEFAULT, style)
                                    }
                                    isUnderlineText = segment.isUnderline
                                    isStrikeThruText = segment.isStrike
                                }

                                if (segment.highlightColor != null) {
                                    val hlPaint = Paint().apply { color = segment.highlightColor }
                                    val textW = paint.measureText(segment.text)
                                    canvas.drawRect(curX, currentY - segment.fontSizePt + 2f, curX + textW, currentY + 3f, hlPaint)
                                }

                                canvas.drawText(segment.text, curX, currentY, paint)
                                curX += paint.measureText(segment.text)
                            }
                            currentY += line.lineHeightPt
                        }
                        currentY += renderable.spaceAfterPt
                    }
                    is RenderableDivider -> {
                        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                        currentY += 16f
                    }
                    is RenderableTable -> {
                        val rowHeight = 22f
                        val colWidth = contentWidth / renderable.columnCount
                        var tableY = currentY

                        for (rIdx in renderable.table.rows.indices) {
                            val row = renderable.table.rows[rIdx]
                            val isHeader = row.isHeader

                            if (isHeader) {
                                val bgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
                                canvas.drawRect(margin, tableY, margin + contentWidth, tableY + rowHeight, bgPaint)
                            }

                            for (cIdx in row.cells.indices) {
                                val cell = row.cells[cIdx]
                                val cellX = margin + (cIdx * colWidth)

                                // Cell borders
                                canvas.drawRect(cellX, tableY, cellX + colWidth, tableY + rowHeight, linePaint)

                                val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    textSize = 10f
                                    color = Color.parseColor("#1E293B")
                                    typeface = if (isHeader) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                                }
                                val cellText = cell.plainText
                                canvas.drawText(cellText, cellX + 6f, tableY + 15f, cellPaint)
                            }
                            tableY += rowHeight
                        }
                        currentY = tableY + 16f
                    }
                }
            }

            pdfDoc.finishPage(page)
            pageNum++
        }

        pdfDoc.writeTo(outputStream)
        pdfDoc.close()
    }

    fun printDocument(context: Context, document: TypableDocument) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Typable - ${document.title}"

        printManager.print(jobName, object : PrintDocumentAdapter() {
            private var tempPdfFile: File? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val pdi = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(pdi, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (destination == null) {
                    callback?.onWriteFailed("Destination is null")
                    return
                }

                try {
                    FileOutputStream(destination.fileDescriptor).use { fos ->
                        exportToPdf(document, fos)
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }, null)
    }

    // --- Pagination and layout helper structures ---

    private sealed class RenderableItem
    private data class RenderableParagraph(
        val lines: List<RenderableLine>,
        val indentX: Float,
        val spaceAfterPt: Float,
        val listMarker: String?
    ) : RenderableItem()
    private data class RenderableLine(val segments: List<RenderableSegment>, val lineHeightPt: Float)
    private data class RenderableSegment(
        val text: String,
        val fontSizePt: Float,
        val isBold: Boolean,
        val isItalic: Boolean,
        val isUnderline: Boolean,
        val isStrike: Boolean,
        val color: Int,
        val highlightColor: Int?,
        val font: DocumentFont
    )
    private class RenderableDivider : RenderableItem()
    private data class RenderableTable(val table: DocumentBlock.Table, val columnCount: Int) : RenderableItem()

    private fun layoutDocumentPages(
        document: TypableDocument,
        contentWidth: Float,
        maxPageContentHeight: Float
    ): List<List<RenderableItem>> {
        val pages = mutableListOf<MutableList<RenderableItem>>()
        var currentPage = mutableListOf<RenderableItem>()
        var currentHeight = 0f

        fun pushPage() {
            pages.add(currentPage)
            currentPage = mutableListOf()
            currentHeight = 0f
        }

        for (block in document.blocks) {
            when (block) {
                is DocumentBlock.PageBreak -> {
                    pushPage()
                }
                is DocumentBlock.Divider -> {
                    if (currentHeight + 20f > maxPageContentHeight && currentPage.isNotEmpty()) {
                        pushPage()
                    }
                    currentPage.add(RenderableDivider())
                    currentHeight += 20f
                }
                is DocumentBlock.Table -> {
                    val colCount = maxOf(1, block.rows.firstOrNull()?.cells?.size ?: 1)
                    val tableH = block.rows.size * 22f + 16f
                    if (currentHeight + tableH > maxPageContentHeight && currentPage.isNotEmpty()) {
                        pushPage()
                    }
                    currentPage.add(RenderableTable(block, colCount))
                    currentHeight += tableH
                }
                is DocumentBlock.Paragraph -> {
                    val (renderable, blockHeight) = layoutParagraph(block, contentWidth)
                    if (currentHeight + blockHeight > maxPageContentHeight && currentPage.isNotEmpty()) {
                        pushPage()
                    }
                    currentPage.add(renderable)
                    currentHeight += blockHeight
                }
            }
        }

        if (currentPage.isNotEmpty() || pages.isEmpty()) {
            pages.add(currentPage)
        }

        return pages
    }

    private fun layoutParagraph(block: DocumentBlock.Paragraph, contentWidth: Float): Pair<RenderableParagraph, Float> {
        val indentX = block.indentationLevel * 20f
        val effectiveWidth = contentWidth - indentX
        val lines = mutableListOf<RenderableLine>()

        var currentLineSegments = mutableListOf<RenderableSegment>()
        var currentLineWidth = 0f
        var maxLineH = 16f

        val marker = when (block.listType) {
            ListType.BULLET -> "•"
            ListType.NUMBERED -> "${block.listIndex}."
            ListType.NONE -> null
        }

        val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (run in block.runs) {
            val fontSizePt = (run.fontSizeSp * 0.75f).coerceAtLeast(8f)
            val lineH = fontSizePt * block.lineSpacing * 1.3f
            if (lineH > maxLineH) maxLineH = lineH

            tempPaint.textSize = fontSizePt
            tempPaint.typeface = if (run.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

            val words = run.text.split(Regex("(?<=\\s)|(?=\\s)"))
            for (word in words) {
                val wordW = tempPaint.measureText(word)
                if (currentLineWidth + wordW > effectiveWidth && currentLineSegments.isNotEmpty()) {
                    lines.add(RenderableLine(currentLineSegments, maxLineH))
                    currentLineSegments = mutableListOf()
                    currentLineWidth = 0f
                }

                val seg = RenderableSegment(
                    text = word,
                    fontSizePt = fontSizePt,
                    isBold = run.isBold,
                    isItalic = run.isItalic,
                    isUnderline = run.isUnderline,
                    isStrike = run.isStrikethrough,
                    color = run.textColorHex?.let { tryParseColor(it, Color.parseColor("#1E293B")) } ?: Color.parseColor("#1E293B"),
                    highlightColor = run.highlightColorHex?.let { tryParseColor(it, null) },
                    font = run.fontFamily
                )
                currentLineSegments.add(seg)
                currentLineWidth += wordW
            }
        }

        if (currentLineSegments.isNotEmpty() || lines.isEmpty()) {
            lines.add(RenderableLine(currentLineSegments, maxLineH))
        }

        val totalHeight = lines.sumOf { it.lineHeightPt.toDouble() }.toFloat() + 8f
        return Pair(RenderableParagraph(lines, indentX, 8f, marker), totalHeight)
    }

    private fun tryParseColor(hex: String, default: Int?): Int? {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            default
        }
    }
}
