package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.storage.CustomFontManager

/**
 * High-contrast color resolver:
 * Ensures custom or default font color is always easily visible against the paper background,
 * eliminating invisible text under dark theme, sepia, or custom paper tints.
 */
fun resolveSafeTextColor(customHex: String?, paperHex: String, defaultInk: Color): Color {
    if (customHex.isNullOrBlank()) return defaultInk
    return try {
        val color = Color(android.graphics.Color.parseColor(customHex))
        val paper = Color(android.graphics.Color.parseColor(paperHex))
        val lumText = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
        val lumPaper = 0.2126f * paper.red + 0.7152f * paper.green + 0.0722f * paper.blue
        val lighter = maxOf(lumText, lumPaper)
        val darker = minOf(lumText, lumPaper)
        val ratio = (lighter + 0.05f) / (darker + 0.05f)
        if (ratio < 1.75f) defaultInk else color
    } catch (_: Exception) {
        defaultInk
    }
}

@Composable
fun DocumentCanvas(
    document: TypableDocument,
    activeBlockIndex: Int,
    isPrintLayout: Boolean = true,
    showRuler: Boolean = true,
    zoomScale: Float = 1.0f,
    requestedCursorOffset: Int? = null,
    onConsumeRequestedCursor: () -> Unit = {},
    onSelectBlock: (Int) -> Unit,
    onUpdateText: (Int, String) -> Unit,
    onSplitParagraph: (Int, Int) -> Unit = { _, _ -> },
    onSplitParagraphWithText: (Int, Int, String, String) -> Unit = { idx, offset, _, _ -> onSplitParagraph(idx, offset) },
    onMergeWithPrevious: (Int) -> Unit,
    onDeleteBlock: (Int) -> Unit,
    onUpdateTableCell: (Int, Int, Int, String) -> Unit,
    onAddTableRow: (Int, Int) -> Unit,
    onDeleteTableRow: (Int, Int) -> Unit,
    onAddTableColumn: (Int) -> Unit,
    onDeleteTableColumn: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Determine paper background color
    val paperColorHex = document.pageSettings.backgroundColorHex
    val paperColor = try {
        Color(android.graphics.Color.parseColor(paperColorHex))
    } catch (e: Exception) {
        Color.White
    }

    // Compute high-contrast default ink color and placeholder color based on paper background
    val defaultInkColor = remember(paperColorHex) {
        val r = paperColor.red
        val g = paperColor.green
        val b = paperColor.blue
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        if (luminance < 0.5f) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }

    val placeholderColor = remember(paperColorHex) {
        val r = paperColor.red
        val g = paperColor.green
        val b = paperColor.blue
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        if (luminance < 0.5f) Color(0xFF94A3B8) else Color(0xFF64748B)
    }

    // Smooth auto-scroll when active block changes
    LaunchedEffect(activeBlockIndex) {
        if (activeBlockIndex in document.blocks.indices) {
            try {
                listState.animateScrollToItem(activeBlockIndex)
            } catch (_: Exception) {}
        }
    }

    // Margins in dp
    val marginPadding = when (document.pageSettings.margins) {
        PageMargins.NARROW -> 16.dp
        PageMargins.WIDE -> 36.dp
        PageMargins.NORMAL -> 24.dp
    }

    // Workspace background: neutral desk color in Print Layout, surface in Mobile View
    val workspaceBackground = if (isPrintLayout) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    } else {
        paperColor
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(workspaceBackground)
            .testTag("document_canvas_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal Word Processor Typographic Ruler (Signature MS Word & Google Docs feature)
        if (isPrintLayout && showRuler) {
            WordProcessorRuler(
                modifier = Modifier
                    .widthIn(max = (840 * zoomScale).dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                marginPadding = marginPadding * zoomScale
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Simulated Paper Sheet with subtle shadow in Print Layout mode
            Surface(
                modifier = Modifier
                    .widthIn(max = if (isPrintLayout) (840 * zoomScale).dp else 1200.dp)
                    .fillMaxSize()
                    .then(
                        if (isPrintLayout) {
                            Modifier
                                .padding(horizontal = (12 * zoomScale).coerceAtLeast(4f).dp, vertical = (8 * zoomScale).coerceAtLeast(4f).dp)
                                .shadow(elevation = 3.dp, shape = RoundedCornerShape(2.dp))
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ),
                color = paperColor,
                shape = if (isPrintLayout) RoundedCornerShape(2.dp) else RoundedCornerShape(0.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = marginPadding * (if (isPrintLayout) zoomScale else 1.0f),
                            vertical = if (isPrintLayout) marginPadding * zoomScale else 12.dp
                        )
                        .testTag("document_blocks_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header (if configured)
                    if (document.pageSettings.headerText.isNotBlank()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Text(
                                    text = document.pageSettings.headerText,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = defaultInkColor.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                HorizontalDivider(
                                    color = defaultInkColor.copy(alpha = 0.2f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    val isDocumentEntirelyEmpty = document.blocks.size <= 1 &&
                            document.blocks.all { it is DocumentBlock.Paragraph && it.plainText.isEmpty() }

                    // Document Blocks
                    itemsIndexed(
                        items = document.blocks,
                        key = { _, block -> block.id }
                    ) { index, block ->
                        val isActive = index == activeBlockIndex

                        when (block) {
                            is DocumentBlock.Paragraph -> {
                                val showPlaceholder = (index == 0 && isDocumentEntirelyEmpty) ||
                                        (isActive && block.style.isHeading && block.plainText.isEmpty())
                                ParagraphBlockItem(
                                    paragraph = block,
                                    isActive = isActive,
                                    paperColorHex = paperColorHex,
                                    defaultInkColor = defaultInkColor,
                                    placeholderColor = placeholderColor,
                                    showPlaceholder = showPlaceholder,
                                    requestedCursorOffset = if (isActive) requestedCursorOffset else null,
                                    onConsumeRequestedCursor = onConsumeRequestedCursor,
                                    onFocused = { onSelectBlock(index) },
                                    onTextChanged = { newText -> onUpdateText(index, newText) },
                                    onEnterPressedWithText = { before, after ->
                                        onSplitParagraphWithText(index, before.length, before, after)
                                    },
                                    onBackspaceAtStart = { onMergeWithPrevious(index) }
                                )
                            }
                            is DocumentBlock.Table -> {
                                TableBlockItem(
                                    table = block,
                                    isActive = isActive,
                                    defaultInkColor = defaultInkColor,
                                    onFocused = { onSelectBlock(index) },
                                    onCellChanged = { rowIdx, colIdx, text ->
                                        onUpdateTableCell(index, rowIdx, colIdx, text)
                                    },
                                    onAddRow = { rowIdx -> onAddTableRow(index, rowIdx) },
                                    onDeleteRow = { rowIdx -> onDeleteTableRow(index, rowIdx) },
                                    onAddColumn = { onAddTableColumn(index) },
                                    onDeleteColumn = { colIdx -> onDeleteTableColumn(index, colIdx) },
                                    onDeleteTable = { onDeleteBlock(index) }
                                )
                            }
                            is DocumentBlock.Divider -> {
                                DividerBlockItem(
                                    isActive = isActive,
                                    onClick = { onSelectBlock(index) },
                                    onDelete = { onDeleteBlock(index) }
                                )
                            }
                            is DocumentBlock.PageBreak -> {
                                PageBreakBlockItem(
                                    isActive = isActive,
                                    isPrintLayout = isPrintLayout,
                                    onClick = { onSelectBlock(index) },
                                    onDelete = { onDeleteBlock(index) }
                                )
                            }
                        }
                    }

                    // Footer (if configured)
                    if (document.pageSettings.footerText.isNotBlank() || document.pageSettings.showPageNumbers) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp)) {
                                HorizontalDivider(
                                    color = defaultInkColor.copy(alpha = 0.2f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = document.pageSettings.footerText,
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color = defaultInkColor.copy(alpha = 0.6f)
                                        )
                                    )
                                    if (document.pageSettings.showPageNumbers) {
                                        Text(
                                            text = "Page 1 of ${document.computeStats().pageCount}",
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                color = defaultInkColor.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom padding for toolbar overlay
                    item {
                        Spacer(modifier = Modifier.height(160.dp))
                    }
                }
            }
        }
    }
}

/**
 * Authentic Word Processor Typographic Ruler.
 * Renders inch markings, tick intervals, and left/right margin markers.
 */
@Composable
fun WordProcessorRuler(
    modifier: Modifier = Modifier,
    marginPadding: androidx.compose.ui.unit.Dp = 24.dp
) {
    val rulerBg = MaterialTheme.colorScheme.surface
    val tickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val markerColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
        color = rulerBg,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val marginPx = with(androidx.compose.ui.platform.LocalDensity.current) { marginPadding.toPx() }
            val usableWidth = (totalWidthPx - (marginPx * 2)).coerceAtLeast(100f)

            // Canvas drawing ruler ticks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val inchCount = 7
                val tickStep = usableWidth / inchCount

                // Left margin marker (downward triangle)
                val leftX = marginPx
                drawLine(
                    color = markerColor,
                    start = Offset(leftX, 0f),
                    end = Offset(leftX, size.height),
                    strokeWidth = 2f
                )

                // Right margin marker
                val rightX = totalWidthPx - marginPx
                drawLine(
                    color = markerColor,
                    start = Offset(rightX, 0f),
                    end = Offset(rightX, size.height),
                    strokeWidth = 2f
                )

                // Draw tick marks across usable page width
                for (i in 0..inchCount) {
                    val x = leftX + (i * tickStep)
                    if (x in 0f..totalWidthPx) {
                        // Major inch tick
                        drawLine(
                            color = tickColor,
                            start = Offset(x, size.height - 12f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.5f
                        )

                        // Subdivisions (quarter & half inch)
                        if (i < inchCount) {
                            val halfX = x + (tickStep * 0.5f)
                            drawLine(
                                color = tickColor,
                                start = Offset(halfX, size.height - 8f),
                                end = Offset(halfX, size.height),
                                strokeWidth = 1f
                            )
                            val q1X = x + (tickStep * 0.25f)
                            drawLine(
                                color = tickColor,
                                start = Offset(q1X, size.height - 5f),
                                end = Offset(q1X, size.height),
                                strokeWidth = 0.8f
                            )
                            val q3X = x + (tickStep * 0.75f)
                            drawLine(
                                color = tickColor,
                                start = Offset(q3X, size.height - 5f),
                                end = Offset(q3X, size.height),
                                strokeWidth = 0.8f
                            )
                        }
                    }
                }
            }

            // Number indicators overlay
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = marginPadding, end = marginPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                for (inch in 1..6) {
                    Text(
                        text = inch.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ParagraphBlockItem(
    paragraph: DocumentBlock.Paragraph,
    isActive: Boolean,
    paperColorHex: String,
    defaultInkColor: Color,
    placeholderColor: Color,
    showPlaceholder: Boolean = false,
    requestedCursorOffset: Int? = null,
    onConsumeRequestedCursor: () -> Unit = {},
    onFocused: () -> Unit,
    onTextChanged: (String) -> Unit,
    onEnterPressedWithText: (String, String) -> Unit,
    onBackspaceAtStart: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    // Preserve local cursor and text; only sync from model when external update occurs
    var textFieldValue by remember(paragraph.id) {
        val initialOffset = requestedCursorOffset?.coerceIn(0, paragraph.plainText.length) ?: 0
        mutableStateOf(TextFieldValue(text = paragraph.plainText, selection = TextRange(initialOffset)))
    }

    var lastSentText by remember(paragraph.id) { mutableStateOf(paragraph.plainText) }

    // Handle cursor jump requests (e.g. from Enter split or Backspace merge)
    LaunchedEffect(isActive, requestedCursorOffset) {
        if (isActive && requestedCursorOffset != null) {
            val safePos = requestedCursorOffset.coerceIn(0, textFieldValue.text.length)
            textFieldValue = textFieldValue.copy(selection = TextRange(safePos))
            onConsumeRequestedCursor()
        }
    }

    // External model updates (Undo, Redo, Find & Replace, external import)
    LaunchedEffect(paragraph.plainText) {
        if (paragraph.plainText != lastSentText) {
            lastSentText = paragraph.plainText
            val safeStart = textFieldValue.selection.start.coerceIn(0, paragraph.plainText.length)
            val safeEnd = textFieldValue.selection.end.coerceIn(0, paragraph.plainText.length)
            textFieldValue = TextFieldValue(
                text = paragraph.plainText,
                selection = TextRange(safeStart, safeEnd)
            )
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val textAlign = when (paragraph.alignment) {
        TextAlignment.CENTER -> TextAlign.Center
        TextAlignment.RIGHT -> TextAlign.End
        TextAlignment.JUSTIFY -> TextAlign.Justify
        else -> TextAlign.Start
    }

    val styleFontWeight = if (paragraph.style.isHeading) FontWeight.Bold else FontWeight.Normal
    val styleFontStyle = if (paragraph.style == BlockStyle.QUOTE) FontStyle.Italic else FontStyle.Normal
    val styleFontSize = paragraph.style.defaultFontSizeSp.sp

    val baseRun = paragraph.runs.firstOrNull() ?: TextRun("")
    val context = LocalContext.current
    val fontFamily = CustomFontManager.getComposeFontFamily(
        context = context,
        font = baseRun.fontFamily,
        customFontName = baseRun.customFontName
    )

    // Guaranteed high-contrast font color against paper background
    val textColor = resolveSafeTextColor(
        customHex = baseRun.textColorHex,
        paperHex = paperColorHex,
        defaultInk = defaultInkColor
    )

    val highlightColor = baseRun.highlightColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { Color.Transparent }
    } ?: Color.Transparent

    val textDecoration = when {
        baseRun.isUnderline && baseRun.isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        baseRun.isUnderline -> TextDecoration.Underline
        baseRun.isStrikethrough -> TextDecoration.LineThrough
        else -> TextDecoration.None
    }

    val indentPadding = (paragraph.indentationLevel * 18).dp
    val cursorColor = if (defaultInkColor.red > 0.5f) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentPadding),
        verticalAlignment = Alignment.Top
    ) {
        // Quote border bar
        if (paragraph.style == BlockStyle.QUOTE) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .padding(end = 8.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // List Marker
        if (paragraph.listType == ListType.BULLET) {
            Text(
                text = "•",
                fontSize = (paragraph.style.defaultFontSizeSp + 2).sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
            )
        } else if (paragraph.listType == ListType.NUMBERED) {
            Text(
                text = "${paragraph.listIndex}.",
                fontSize = paragraph.style.defaultFontSizeSp.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
            )
        }

        // Text Field
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val newText = newValue.text

                // Detect Enter key pressed
                if (newText.contains("\n")) {
                    val enterPos = newText.indexOf('\n')
                    val beforeEnter = newText.substring(0, enterPos)
                    val afterEnter = if (enterPos + 1 <= newText.length) newText.substring(enterPos + 1) else ""
                    textFieldValue = TextFieldValue(text = beforeEnter, selection = TextRange(beforeEnter.length))
                    lastSentText = beforeEnter
                    onEnterPressedWithText(beforeEnter, afterEnter)
                    return@BasicTextField
                }

                // Detect backspace at beginning of block
                if (textFieldValue.text.isEmpty() && newText.isEmpty()) {
                    onBackspaceAtStart()
                    return@BasicTextField
                }

                textFieldValue = newValue
                if (newValue.text != lastSentText) {
                    lastSentText = newValue.text
                    onTextChanged(newValue.text)
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Backspace) {
                        if (textFieldValue.selection.collapsed && textFieldValue.selection.start == 0) {
                            onBackspaceAtStart()
                            true
                        } else false
                    } else false
                }
                .onFocusChanged { state ->
                    if (state.isFocused) onFocused()
                }
                .testTag("paragraph_input_${paragraph.id}"),
            textStyle = TextStyle(
                color = textColor,
                fontSize = if (baseRun.fontSizeSp != 15f) baseRun.fontSizeSp.sp else styleFontSize,
                fontWeight = if (baseRun.isBold) FontWeight.Bold else styleFontWeight,
                fontStyle = if (baseRun.isItalic) FontStyle.Italic else styleFontStyle,
                fontFamily = fontFamily,
                textAlign = textAlign,
                textDecoration = textDecoration,
                lineHeight = (styleFontSize.value * paragraph.lineSpacing * 1.35f).sp,
                background = highlightColor
            ),
            cursorBrush = SolidColor(cursorColor),
            singleLine = false,
            maxLines = Int.MAX_VALUE,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = (styleFontSize.value * 1.35f).dp)
                        .then(
                            if (paragraph.style == BlockStyle.CODE) {
                                Modifier
                                    .background(defaultInkColor.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            } else Modifier
                        )
                ) {
                    if (showPlaceholder && textFieldValue.text.isEmpty()) {
                        Text(
                            text = if (paragraph.style.isHeading) "Heading..." else "Start typing...",
                            style = TextStyle(
                                color = placeholderColor,
                                fontSize = styleFontSize,
                                fontStyle = styleFontStyle,
                                textAlign = textAlign
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun TableBlockItem(
    table: DocumentBlock.Table,
    isActive: Boolean,
    defaultInkColor: Color,
    onFocused: () -> Unit,
    onCellChanged: (Int, Int, String) -> Unit,
    onAddRow: (Int) -> Unit,
    onDeleteRow: (Int) -> Unit,
    onAddColumn: () -> Unit,
    onDeleteColumn: (Int) -> Unit,
    onDeleteTable: () -> Unit
) {
    var selectedRowIndex by remember { mutableStateOf(0) }
    var selectedColIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary else defaultInkColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable { onFocused() }
            .testTag("table_block")
    ) {
        // Table Context Bar (visible when table is selected)
        if (isActive) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalButton(
                            onClick = { onAddRow(selectedRowIndex) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("+ Row", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { onAddColumn() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("+ Col", fontSize = 11.sp)
                        }

                        if (table.rows.size > 1) {
                            OutlinedButton(
                                onClick = { onDeleteRow(selectedRowIndex) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("- Row", fontSize = 11.sp)
                            }
                        }

                        if ((table.rows.firstOrNull()?.cells?.size ?: 0) > 1) {
                            OutlinedButton(
                                onClick = { onDeleteColumn(selectedColIndex) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("- Col", fontSize = 11.sp)
                            }
                        }
                    }

                    IconButton(
                        onClick = onDeleteTable,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Table",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Table Grid
        table.rows.forEachIndexed { rIdx, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (row.isHeader) defaultInkColor.copy(alpha = 0.08f)
                        else Color.Transparent
                    )
            ) {
                row.cells.forEachIndexed { cIdx, cell ->
                    val isCellSelected = isActive && selectedRowIndex == rIdx && selectedColIndex == cIdx

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 0.5.dp,
                                color = defaultInkColor.copy(alpha = 0.2f)
                            )
                            .then(
                                if (isCellSelected) {
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                } else Modifier
                            )
                            .padding(8.dp)
                            .clickable {
                                selectedRowIndex = rIdx
                                selectedColIndex = cIdx
                                onFocused()
                            }
                    ) {
                        var cellText by remember(cell.id) { mutableStateOf(cell.plainText) }
                        var lastSentCellText by remember(cell.id) { mutableStateOf(cell.plainText) }

                        LaunchedEffect(cell.plainText) {
                            if (cell.plainText != lastSentCellText) {
                                lastSentCellText = cell.plainText
                                cellText = cell.plainText
                            }
                        }

                        BasicTextField(
                            value = cellText,
                            onValueChange = { newTxt ->
                                cellText = newTxt
                                if (newTxt != lastSentCellText) {
                                    lastSentCellText = newTxt
                                    onCellChanged(rIdx, cIdx, newTxt)
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = if (row.isHeader) FontWeight.Bold else FontWeight.Normal,
                                color = defaultInkColor
                            ),
                            cursorBrush = SolidColor(if (defaultInkColor.red > 0.5f) Color(0xFF38BDF8) else Color(0xFF0284C7)),
                            singleLine = false,
                            maxLines = 10,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = true
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (cellText.isEmpty()) {
                                        Text(
                                            text = if (row.isHeader) "Header" else "—",
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                color = defaultInkColor.copy(alpha = 0.35f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DividerBlockItem(
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = if (isActive) 2.dp else 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        if (isActive) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Delete divider", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PageBreakBlockItem(
    isActive: Boolean,
    isPrintLayout: Boolean = true,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isPrintLayout) 16.dp else 8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // In Print Layout, page breaks look like physical sheet separations
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertPageBreak,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Page Break", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete page break",
                            modifier = Modifier.size(14.dp).clickable { onDelete() },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
