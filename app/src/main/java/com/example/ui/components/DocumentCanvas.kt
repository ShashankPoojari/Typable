package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun DocumentCanvas(
    document: TypableDocument,
    activeBlockIndex: Int,
    onSelectBlock: (Int) -> Unit,
    onUpdateText: (Int, String) -> Unit,
    onSplitParagraph: (Int, Int) -> Unit,
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
    val paperColor = try {
        Color(android.graphics.Color.parseColor(document.pageSettings.backgroundColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surface
    }

    // Margins in dp
    val marginPadding = when (document.pageSettings.margins) {
        PageMargins.NARROW -> 16.dp
        PageMargins.WIDE -> 36.dp
        PageMargins.NORMAL -> 24.dp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("document_canvas_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Simulated Paper Sheet with subtle shadow
        Surface(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(4.dp)),
            color = paperColor,
            shape = RoundedCornerShape(4.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = marginPadding, vertical = marginPadding)
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
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Document Blocks
                itemsIndexed(
                    items = document.blocks,
                    key = { _, block -> block.id }
                ) { index, block ->
                    val isActive = index == activeBlockIndex

                    when (block) {
                        is DocumentBlock.Paragraph -> {
                            ParagraphBlockItem(
                                paragraph = block,
                                isActive = isActive,
                                onFocused = { onSelectBlock(index) },
                                onTextChanged = { newText -> onUpdateText(index, newText) },
                                onEnterPressed = { cursorOffset -> onSplitParagraph(index, cursorOffset) },
                                onBackspaceAtStart = { onMergeWithPrevious(index) }
                            )
                        }
                        is DocumentBlock.Table -> {
                            TableBlockItem(
                                table = block,
                                isActive = isActive,
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
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                )
                                if (document.pageSettings.showPageNumbers) {
                                    Text(
                                        text = "Page 1 of ${document.computeStats().pageCount}",
                                        style = TextStyle(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom padding for toolbar overlay
                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
fun ParagraphBlockItem(
    paragraph: DocumentBlock.Paragraph,
    isActive: Boolean,
    onFocused: () -> Unit,
    onTextChanged: (String) -> Unit,
    onEnterPressed: (Int) -> Unit,
    onBackspaceAtStart: () -> Unit
) {
    var textFieldValue by remember(paragraph.plainText) {
        mutableStateOf(TextFieldValue(text = paragraph.plainText, selection = TextRange(paragraph.plainText.length)))
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
    val fontFamily = when (baseRun.fontFamily) {
        DocumentFont.SERIF -> FontFamily.Serif
        DocumentFont.MONOSPACE -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    val textColor = baseRun.textColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { MaterialTheme.colorScheme.onSurface }
    } ?: MaterialTheme.colorScheme.onSurface

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentPadding)
            .then(
                if (paragraph.style == BlockStyle.QUOTE) {
                    Modifier
                        .border(width = 0.dp, color = Color.Transparent)
                        .padding(start = 4.dp)
                } else Modifier
            ),
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
                val oldText = textFieldValue.text
                val newText = newValue.text

                // Detect Enter key pressed
                if (newText.contains("\n")) {
                    val enterPos = newText.indexOf('\n')
                    val cleanText = newText.replace("\n", "")
                    onTextChanged(cleanText)
                    onEnterPressed(enterPos)
                    return@BasicTextField
                }

                // Detect backspace at beginning of block
                if (oldText.isEmpty() && newText.isEmpty()) {
                    onBackspaceAtStart()
                    return@BasicTextField
                }

                textFieldValue = newValue
                onTextChanged(newValue.text)
            },
            modifier = Modifier
                .weight(1f)
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
                lineHeight = (styleFontSize.value * paragraph.lineSpacing * 1.3f).sp,
                background = highlightColor
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Default
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (paragraph.style == BlockStyle.CODE) {
                                Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            } else Modifier
                        )
                ) {
                    if (textFieldValue.text.isEmpty() && !isActive) {
                        Text(
                            text = if (paragraph.style.isHeading) "Heading..." else "Type something...",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp)
            )
            .clip(RoundedCornerShape(6.dp))
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
                        if (row.isHeader) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    )
            ) {
                row.cells.forEachIndexed { cIdx, cell ->
                    val isCellSelected = isActive && selectedRowIndex == rIdx && selectedColIndex == cIdx

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                            .then(
                                if (isCellSelected) {
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                } else Modifier
                            )
                            .padding(8.dp)
                            .clickable {
                                selectedRowIndex = rIdx
                                selectedColIndex = cIdx
                                onFocused()
                            }
                    ) {
                        var cellText by remember(cell.plainText) { mutableStateOf(cell.plainText) }

                        BasicTextField(
                            value = cellText,
                            onValueChange = { newTxt ->
                                cellText = newTxt
                                onCellChanged(rIdx, cIdx, newTxt)
                            },
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = if (row.isHeader) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onClick() },
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
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.InsertPageBreak,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Page Break", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (isActive) {
                    Spacer(modifier = Modifier.width(4.dp))
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
