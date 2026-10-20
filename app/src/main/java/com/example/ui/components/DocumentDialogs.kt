package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun PageSetupDialog(
    currentSettings: PageSettings,
    onSave: (PageSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var pageSize by remember { mutableStateOf(currentSettings.pageSize) }
    var orientation by remember { mutableStateOf(currentSettings.orientation) }
    var margins by remember { mutableStateOf(currentSettings.margins) }
    var headerText by remember { mutableStateOf(currentSettings.headerText) }
    var footerText by remember { mutableStateOf(currentSettings.footerText) }
    var showPageNumbers by remember { mutableStateOf(currentSettings.showPageNumbers) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Page Setup", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Page Size
                Text("Page Dimensions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageSize.values().forEach { size ->
                        FilterChip(
                            selected = pageSize == size,
                            onClick = { pageSize = size },
                            label = { Text(size.name, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Orientation
                Text("Orientation", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageOrientation.values().forEach { orient ->
                        FilterChip(
                            selected = orientation == orient,
                            onClick = { orientation = orient },
                            label = { Text(orient.displayName, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Margins
                Text("Margins", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageMargins.values().forEach { m ->
                        FilterChip(
                            selected = margins == m,
                            onClick = { margins = m },
                            label = { Text(m.name, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                // Header & Footer
                OutlinedTextField(
                    value = headerText,
                    onValueChange = { headerText = it },
                    label = { Text("Header Text (Top of page)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = footerText,
                    onValueChange = { footerText = it },
                    label = { Text("Footer Text (Bottom of page)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Show Page Numbers", fontSize = 14.sp)
                    Switch(
                        checked = showPageNumbers,
                        onCheckedChange = { showPageNumbers = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentSettings.copy(
                            pageSize = pageSize,
                            orientation = orientation,
                            margins = margins,
                            headerText = headerText,
                            footerText = footerText,
                            showPageNumbers = showPageNumbers
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DocumentStatsDialog(
    stats: DocumentStats,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Document Statistics", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatRow("Word Count", stats.wordCount.toString())
                StatRow("Characters (with spaces)", stats.characterCountWithSpaces.toString())
                StatRow("Characters (no spaces)", stats.characterCountWithoutSpaces.toString())
                StatRow("Paragraphs", stats.paragraphCount.toString())
                StatRow("Estimated Pages", stats.pageCount.toString())
                StatRow("Estimated Reading Time", "${stats.readingTimeMinutes} min")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}

@Composable
fun DocumentOutlineDialog(
    document: TypableDocument,
    onSelectBlockIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val headingBlocks = remember(document.blocks) {
        document.blocks.mapIndexedNotNull { index, block ->
            if (block is DocumentBlock.Paragraph && block.style.isHeading && block.plainText.isNotBlank()) {
                Triple(index, block.style, block.plainText)
            } else null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Document Outline", fontWeight = FontWeight.Bold) },
        text = {
            if (headingBlocks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No headings found. Add Title or Heading 1–3 styles to build an outline.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(headingBlocks) { (index, style, text) ->
                        val indent = when (style) {
                            BlockStyle.TITLE -> 0.dp
                            BlockStyle.HEADING_1 -> 8.dp
                            BlockStyle.HEADING_2 -> 16.dp
                            BlockStyle.HEADING_3 -> 24.dp
                            else -> 0.dp
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = indent)
                                .clickable {
                                    onSelectBlockIndex(index)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = text,
                                    fontSize = 14.sp,
                                    fontWeight = if (style == BlockStyle.TITLE || style == BlockStyle.HEADING_1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun InsertMenuDialog(
    onInsertTable: (rows: Int, cols: Int) -> Unit,
    onInsertDivider: () -> Unit,
    onInsertPageBreak: () -> Unit,
    onOpenSymbolPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    var isInsertingTable by remember { mutableStateOf(false) }
    var tableRows by remember { mutableStateOf(3) }
    var tableCols by remember { mutableStateOf(3) }

    if (isInsertingTable) {
        AlertDialog(
            onDismissRequest = { isInsertingTable = false },
            title = { Text("Insert Table") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rows: $tableRows")
                        Row {
                            FilledTonalIconButton(onClick = { if (tableRows > 1) tableRows-- }) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            FilledTonalIconButton(onClick = { if (tableRows < 10) tableRows++ }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Columns: $tableCols")
                        Row {
                            FilledTonalIconButton(onClick = { if (tableCols > 1) tableCols-- }) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            FilledTonalIconButton(onClick = { if (tableCols < 6) tableCols++ }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onInsertTable(tableRows, tableCols)
                    isInsertingTable = false
                    onDismiss()
                }) {
                    Text("Insert")
                }
            },
            dismissButton = {
                TextButton(onClick = { isInsertingTable = false }) { Text("Cancel") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Insert Elements") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InsertOptionItem(Icons.Default.TableChart, "Table Grid", "Insert custom rows & columns") {
                        isInsertingTable = true
                    }
                    InsertOptionItem(Icons.Default.HorizontalRule, "Horizontal Divider", "Visual thematic break") {
                        onInsertDivider()
                        onDismiss()
                    }
                    InsertOptionItem(Icons.Default.InsertPageBreak, "Page Break", "Force new printing page") {
                        onInsertPageBreak()
                        onDismiss()
                    }
                    InsertOptionItem(Icons.Default.EmojiSymbols, "Special Symbol", "Currency, Greek, typography characters") {
                        onOpenSymbolPicker()
                        onDismiss()
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InsertOptionItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SymbolPickerDialog(
    onSelectSymbol: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val symbols = listOf(
        "©", "®", "™", "§", "¶", "†", "‡",
        "—", "–", "…", "“", "”", "‘", "’",
        "«", "»", "•", "°", "±", "×", "÷",
        "≠", "≈", "≤", "≥", "∞", "√", "∑",
        "$", "€", "£", "¥", "₹", "¢", "₿",
        "α", "β", "γ", "δ", "π", "θ", "λ"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Special Character") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (row in symbols.chunked(7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (sym in row) {
                            Surface(
                                onClick = {
                                    onSelectSymbol(sym)
                                    onDismiss()
                                },
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(sym, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun KeyboardShortcutsDialog(onDismiss: () -> Unit) {
    val shortcuts = listOf(
        "Ctrl + B" to "Toggle Bold",
        "Ctrl + I" to "Toggle Italic",
        "Ctrl + U" to "Toggle Underline",
        "Ctrl + Z" to "Undo Last Action",
        "Ctrl + Y / Shift+Z" to "Redo",
        "Ctrl + S" to "Explicit Document Save",
        "Ctrl + F" to "Find and Replace",
        "Ctrl + A" to "Select All Text",
        "Enter" to "New Paragraph / Next List Item",
        "Backspace (at start)" to "Merge with Previous Block"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Keyboard Shortcuts") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shortcuts.forEach { (key, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = key,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got it") }
        }
    )
}

@Composable
fun ExportSheetDialog(
    document: TypableDocument,
    onExportDocx: () -> Unit,
    onExportPdf: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportHtml: () -> Unit,
    onExportTxt: () -> Unit,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Export & Share Document", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExportOptionCard(
                    icon = Icons.Default.Description,
                    title = "Microsoft Word (.docx)",
                    badge = "DOCX",
                    onClick = {
                        onExportDocx()
                        onDismiss()
                    }
                )
                ExportOptionCard(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Vector PDF Document (.pdf)",
                    badge = "PDF",
                    onClick = {
                        onExportPdf()
                        onDismiss()
                    }
                )
                ExportOptionCard(
                    icon = Icons.Default.Code,
                    title = "Markdown (.md)",
                    badge = "MD",
                    onClick = {
                        onExportMarkdown()
                        onDismiss()
                    }
                )
                ExportOptionCard(
                    icon = Icons.Default.Html,
                    title = "Web Page (.html)",
                    badge = "HTML",
                    onClick = {
                        onExportHtml()
                        onDismiss()
                    }
                )
                ExportOptionCard(
                    icon = Icons.Default.TextFields,
                    title = "Plain Text (.txt)",
                    badge = "TXT",
                    onClick = {
                        onExportTxt()
                        onDismiss()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedButton(
                    onClick = {
                        onPrint()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print via Android PrintManager")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ExportOptionCard(icon: ImageVector, title: String, badge: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CustomTableDialog(
    onInsertTable: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var rows by remember { mutableIntStateOf(3) }
    var cols by remember { mutableIntStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Table", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Number of Rows: $rows", fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { if (rows > 1) rows-- }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Rows")
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(onClick = { if (rows < 20) rows++ }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Rows")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Number of Columns: $cols", fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { if (cols > 1) cols-- }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Columns")
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(onClick = { if (cols < 8) cols++ }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Columns")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onInsertTable(rows, cols)
                onDismiss()
            }) {
                Text("Insert Table")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
