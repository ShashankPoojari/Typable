package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standard Ribbon Tabs in famous word processors (MS Word, Google Docs, Apple Pages).
 */
enum class RibbonTab(val displayName: String) {
    HOME("Home"),
    INSERT("Insert"),
    LAYOUT("Layout"),
    REVIEW("Review"),
    VIEW("View"),
    FILE("File")
}

@Composable
fun WordProcessorRibbon(
    activeBlock: DocumentBlock?,
    canUndo: Boolean,
    canRedo: Boolean,
    document: TypableDocument,
    isPrintLayout: Boolean,
    showRuler: Boolean,
    zoomScale: Float,
    isRibbonExpanded: Boolean,
    activeTab: RibbonTab,
    onTabSelected: (RibbonTab) -> Unit,
    onToggleRibbonExpanded: () -> Unit,
    // Home actions
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleSuperscript: () -> Unit,
    onToggleSubscript: () -> Unit,
    onApplyStyle: (BlockStyle) -> Unit,
    onApplyAlignment: (TextAlignment) -> Unit,
    onToggleBulletList: () -> Unit,
    onToggleNumberedList: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onApplyFontFamily: (DocumentFont) -> Unit,
    onApplyFontSizeDelta: (Float) -> Unit,
    onApplyColor: (String?) -> Unit,
    onApplyHighlight: (String?) -> Unit,
    onClearFormatting: () -> Unit,
    onOpenExpandedPanel: () -> Unit,
    // Insert actions
    onInsertTable: (Int, Int) -> Unit,
    onInsertDivider: () -> Unit,
    onInsertPageBreak: () -> Unit,
    onInsertSymbol: (String) -> Unit,
    onInsertDateTime: () -> Unit,
    onOpenCustomTableDialog: () -> Unit,
    onOpenSymbolPickerDialog: () -> Unit,
    // Layout actions
    onUpdateMargins: (PageMargins) -> Unit,
    onUpdateOrientation: (PageOrientation) -> Unit,
    onUpdatePageSize: (PageSize) -> Unit,
    onUpdatePaperColor: (String) -> Unit,
    onOpenPageSetupDialog: () -> Unit,
    // Review actions
    onOpenStatsDialog: () -> Unit,
    onOpenOutlineDialog: () -> Unit,
    onOpenSearch: () -> Unit,
    // View actions
    onTogglePrintLayout: () -> Unit,
    onToggleRuler: () -> Unit,
    onSetZoom: (Float) -> Unit,
    onToggleFocusMode: () -> Unit,
    // File actions
    onExplicitSave: () -> Unit,
    onExportDocx: () -> Unit,
    onExportPdf: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportHtml: () -> Unit,
    onExportTxt: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("word_processor_ribbon"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- 1. Ribbon Tabs Header ---
            RibbonTabHeaderRow(
                activeTab = activeTab,
                isRibbonExpanded = isRibbonExpanded,
                onTabSelected = onTabSelected,
                onToggleRibbonExpanded = onToggleRibbonExpanded
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // --- 2. Active Tab Ribbon Commands Strip ---
            AnimatedVisibility(
                visible = isRibbonExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow ?: MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (activeTab) {
                        RibbonTab.HOME -> HomeRibbonCommands(
                            activeBlock = activeBlock,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            onUndo = onUndo,
                            onRedo = onRedo,
                            onToggleBold = onToggleBold,
                            onToggleItalic = onToggleItalic,
                            onToggleUnderline = onToggleUnderline,
                            onToggleStrikethrough = onToggleStrikethrough,
                            onToggleSuperscript = onToggleSuperscript,
                            onToggleSubscript = onToggleSubscript,
                            onApplyStyle = onApplyStyle,
                            onApplyAlignment = onApplyAlignment,
                            onToggleBulletList = onToggleBulletList,
                            onToggleNumberedList = onToggleNumberedList,
                            onIndent = onIndent,
                            onOutdent = onOutdent,
                            onApplyFontFamily = onApplyFontFamily,
                            onApplyFontSizeDelta = onApplyFontSizeDelta,
                            onApplyColor = onApplyColor,
                            onApplyHighlight = onApplyHighlight,
                            onClearFormatting = onClearFormatting,
                            onOpenExpandedPanel = onOpenExpandedPanel
                        )

                        RibbonTab.INSERT -> InsertRibbonCommands(
                            onInsertTable = onInsertTable,
                            onInsertDivider = onInsertDivider,
                            onInsertPageBreak = onInsertPageBreak,
                            onInsertSymbol = onInsertSymbol,
                            onInsertDateTime = onInsertDateTime,
                            onOpenCustomTableDialog = onOpenCustomTableDialog,
                            onOpenSymbolPickerDialog = onOpenSymbolPickerDialog
                        )

                        RibbonTab.LAYOUT -> LayoutRibbonCommands(
                            settings = document.pageSettings,
                            onUpdateMargins = onUpdateMargins,
                            onUpdateOrientation = onUpdateOrientation,
                            onUpdatePageSize = onUpdatePageSize,
                            onUpdatePaperColor = onUpdatePaperColor,
                            onOpenPageSetupDialog = onOpenPageSetupDialog
                        )

                        RibbonTab.REVIEW -> ReviewRibbonCommands(
                            document = document,
                            onOpenStatsDialog = onOpenStatsDialog,
                            onOpenOutlineDialog = onOpenOutlineDialog,
                            onOpenSearch = onOpenSearch
                        )

                        RibbonTab.VIEW -> ViewRibbonCommands(
                            isPrintLayout = isPrintLayout,
                            showRuler = showRuler,
                            zoomScale = zoomScale,
                            onTogglePrintLayout = onTogglePrintLayout,
                            onToggleRuler = onToggleRuler,
                            onSetZoom = onSetZoom,
                            onToggleFocusMode = onToggleFocusMode
                        )

                        RibbonTab.FILE -> FileRibbonCommands(
                            onExplicitSave = onExplicitSave,
                            onExportDocx = onExportDocx,
                            onExportPdf = onExportPdf,
                            onExportMarkdown = onExportMarkdown,
                            onExportHtml = onExportHtml,
                            onExportTxt = onExportTxt,
                            onPrint = onPrint,
                            onShare = onShare
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab Row for Ribbon Tabs: Home | Insert | Layout | Review | View | File
 */
@Composable
private fun RibbonTabHeaderRow(
    activeTab: RibbonTab,
    isRibbonExpanded: Boolean,
    onTabSelected: (RibbonTab) -> Unit,
    onToggleRibbonExpanded: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RibbonTab.values().forEach { tab ->
                val isSelected = tab == activeTab
                Surface(
                    onClick = {
                        onTabSelected(tab)
                    },
                    color = if (isSelected && isRibbonExpanded) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = tab.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.5.dp)
                                .background(
                                    if (isSelected && isRibbonExpanded) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }

        // Chevron to collapse / expand ribbon (MS Word / Docs signature control)
        IconButton(
            onClick = onToggleRibbonExpanded,
            modifier = Modifier
                .size(34.dp)
                .testTag("toggle_ribbon_button")
        ) {
            Icon(
                imageVector = if (isRibbonExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isRibbonExpanded) "Collapse Ribbon" else "Expand Ribbon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * HOME Ribbon: Formatting, Typography, Paragraph, Styles
 */
@Composable
private fun HomeRibbonCommands(
    activeBlock: DocumentBlock?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleSuperscript: () -> Unit,
    onToggleSubscript: () -> Unit,
    onApplyStyle: (BlockStyle) -> Unit,
    onApplyAlignment: (TextAlignment) -> Unit,
    onToggleBulletList: () -> Unit,
    onToggleNumberedList: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onApplyFontFamily: (DocumentFont) -> Unit,
    onApplyFontSizeDelta: (Float) -> Unit,
    onApplyColor: (String?) -> Unit,
    onApplyHighlight: (String?) -> Unit,
    onClearFormatting: () -> Unit,
    onOpenExpandedPanel: () -> Unit
) {
    val scrollState = rememberScrollState()
    val paragraph = activeBlock as? DocumentBlock.Paragraph
    val currentRun = paragraph?.runs?.firstOrNull() ?: TextRun("")
    val currentStyle = paragraph?.style ?: BlockStyle.NORMAL
    val currentAlign = paragraph?.alignment ?: TextAlignment.LEFT
    val currentList = paragraph?.listType ?: ListType.NONE
    val currentFontSize = currentRun.fontSizeSp.toInt()

    var showStylePickerModal by remember { mutableStateOf(false) }
    var showColorPickerMenu by remember { mutableStateOf(false) }
    var showHighlightPickerMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // --- Group 1: Undo / Redo ---
        RibbonIconButton(icon = Icons.AutoMirrored.Filled.Undo, label = "Undo", enabled = canUndo, onClick = onUndo)
        RibbonIconButton(icon = Icons.AutoMirrored.Filled.Redo, label = "Redo", enabled = canRedo, onClick = onRedo)

        RibbonGroupDivider()

        // --- Group 2: Paragraph Styles Dropdown ---
        OutlinedButton(
            onClick = { showStylePickerModal = true },
            modifier = Modifier.height(36.dp).testTag("ribbon_style_dropdown"),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(text = currentStyle.displayName, fontSize = 12.sp, maxLines = 1)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        RibbonGroupDivider()

        // --- Group 3: Font Family Dropdown ---
        Box {
            OutlinedButton(
                onClick = { showFontMenu = true },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(text = currentRun.fontFamily.displayName, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                DocumentFont.values().forEach { font ->
                    DropdownMenuItem(
                        text = { Text(font.displayName, fontSize = 13.sp) },
                        onClick = {
                            onApplyFontFamily(font)
                            showFontMenu = false
                        },
                        trailingIcon = if (currentRun.fontFamily == font) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        // --- Group 4: Font Size Stepper ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                .height(36.dp)
                .padding(horizontal = 4.dp)
        ) {
            IconButton(
                onClick = { onApplyFontSizeDelta(-1f) },
                modifier = Modifier.size(28.dp),
                enabled = currentFontSize > 8
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease Font Size", modifier = Modifier.size(14.dp))
            }
            Text(
                text = "${currentFontSize}pt",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = { onApplyFontSizeDelta(1f) },
                modifier = Modifier.size(28.dp),
                enabled = currentFontSize < 72
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase Font Size", modifier = Modifier.size(14.dp))
            }
        }

        RibbonGroupDivider()

        // --- Group 5: Character Styles (Bold, Italic, Underline, Strikethrough, Super, Sub) ---
        RibbonToggleButton(icon = Icons.Default.FormatBold, label = "Bold", isActive = currentRun.isBold, onClick = onToggleBold)
        RibbonToggleButton(icon = Icons.Default.FormatItalic, label = "Italic", isActive = currentRun.isItalic, onClick = onToggleItalic)
        RibbonToggleButton(icon = Icons.Default.FormatUnderlined, label = "Underline", isActive = currentRun.isUnderline, onClick = onToggleUnderline)
        RibbonToggleButton(icon = Icons.Default.FormatStrikethrough, label = "Strikethrough", isActive = currentRun.isStrikethrough, onClick = onToggleStrikethrough)
        RibbonToggleButton(icon = Icons.Default.Superscript, label = "Superscript", isActive = currentRun.isSuperscript, onClick = onToggleSuperscript)
        RibbonToggleButton(icon = Icons.Default.Subscript, label = "Subscript", isActive = currentRun.isSubscript, onClick = onToggleSubscript)

        RibbonGroupDivider()

        // --- Group 6: Font Color & Highlight ---
        Box {
            RibbonColorButton(
                icon = Icons.Default.FormatColorText,
                label = "Text Color",
                barColor = currentRun.textColorHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null } } ?: MaterialTheme.colorScheme.onSurface,
                onClick = { showColorPickerMenu = true }
            )
            DropdownMenu(expanded = showColorPickerMenu, onDismissRequest = { showColorPickerMenu = false }) {
                Text("Select Text Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                ColorPaletteRow(
                    colors = listOf(null, "#000000", "#1E293B", "#1E40AF", "#047857", "#B91C1C", "#7C3AED", "#B45309"),
                    onSelectColor = {
                        onApplyColor(it)
                        showColorPickerMenu = false
                    }
                )
            }
        }

        Box {
            RibbonColorButton(
                icon = Icons.Default.Highlight,
                label = "Highlight Color",
                barColor = currentRun.highlightColorHex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null } } ?: Color.Yellow,
                onClick = { showHighlightPickerMenu = true }
            )
            DropdownMenu(expanded = showHighlightPickerMenu, onDismissRequest = { showHighlightPickerMenu = false }) {
                Text("Select Highlight Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                ColorPaletteRow(
                    colors = listOf(null, "#FEF08A", "#BBF7D0", "#BAE6FD", "#FBCFE8", "#FED7AA", "#E9D5FF"),
                    onSelectColor = {
                        onApplyHighlight(it)
                        showHighlightPickerMenu = false
                    }
                )
            }
        }

        RibbonGroupDivider()

        // --- Group 7: Alignment ---
        RibbonToggleButton(icon = Icons.AutoMirrored.Filled.FormatAlignLeft, label = "Align Left", isActive = currentAlign == TextAlignment.LEFT, onClick = { onApplyAlignment(TextAlignment.LEFT) })
        RibbonToggleButton(icon = Icons.Default.FormatAlignCenter, label = "Center", isActive = currentAlign == TextAlignment.CENTER, onClick = { onApplyAlignment(TextAlignment.CENTER) })
        RibbonToggleButton(icon = Icons.AutoMirrored.Filled.FormatAlignRight, label = "Align Right", isActive = currentAlign == TextAlignment.RIGHT, onClick = { onApplyAlignment(TextAlignment.RIGHT) })
        RibbonToggleButton(icon = Icons.Default.FormatAlignJustify, label = "Justify", isActive = currentAlign == TextAlignment.JUSTIFY, onClick = { onApplyAlignment(TextAlignment.JUSTIFY) })

        RibbonGroupDivider()

        // --- Group 8: Lists, Indent, Spacing ---
        RibbonToggleButton(icon = Icons.AutoMirrored.Filled.FormatListBulleted, label = "Bullet List", isActive = currentList == ListType.BULLET, onClick = onToggleBulletList)
        RibbonToggleButton(icon = Icons.Default.FormatListNumbered, label = "Numbered List", isActive = currentList == ListType.NUMBERED, onClick = onToggleNumberedList)
        RibbonIconButton(icon = Icons.AutoMirrored.Filled.FormatIndentDecrease, label = "Decrease Indent", enabled = ((activeBlock as? DocumentBlock.Paragraph)?.indentationLevel ?: 0) > 0, onClick = onOutdent)
        RibbonIconButton(icon = Icons.AutoMirrored.Filled.FormatIndentIncrease, label = "Increase Indent", enabled = true, onClick = onIndent)

        RibbonGroupDivider()

        // Clear formatting
        RibbonIconButton(icon = Icons.Default.FormatClear, label = "Clear Formatting", enabled = true, onClick = onClearFormatting)

        // Detailed Panel button
        FilledTonalIconButton(
            onClick = onOpenExpandedPanel,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Advanced Typography", modifier = Modifier.size(18.dp))
        }
    }

    // Modal dialog for block styles
    if (showStylePickerModal) {
        ParagraphStylesDialog(
            currentStyle = currentStyle,
            onSelect = {
                onApplyStyle(it)
                showStylePickerModal = false
            },
            onDismiss = { showStylePickerModal = false }
        )
    }
}

/**
 * INSERT Ribbon: Tables, Page Breaks, Horizontal Dividers, Symbols, Date/Time
 */
@Composable
private fun InsertRibbonCommands(
    onInsertTable: (Int, Int) -> Unit,
    onInsertDivider: () -> Unit,
    onInsertPageBreak: () -> Unit,
    onInsertSymbol: (String) -> Unit,
    onInsertDateTime: () -> Unit,
    onOpenCustomTableDialog: () -> Unit,
    onOpenSymbolPickerDialog: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showTablePresetMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Table Insertion Group
        Box {
            RibbonActionButton(
                icon = Icons.Default.TableChart,
                label = "Table",
                onClick = { showTablePresetMenu = true }
            )

            DropdownMenu(expanded = showTablePresetMenu, onDismissRequest = { showTablePresetMenu = false }) {
                Text("Insert Table", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                DropdownMenuItem(
                    text = { Text("2 × 2 Table", fontSize = 13.sp) },
                    onClick = {
                        onInsertTable(2, 2)
                        showTablePresetMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("3 × 3 Table", fontSize = 13.sp) },
                    onClick = {
                        onInsertTable(3, 3)
                        showTablePresetMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("4 × 3 Table", fontSize = 13.sp) },
                    onClick = {
                        onInsertTable(4, 3)
                        showTablePresetMenu = false
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Custom Table...", fontSize = 13.sp) },
                    onClick = {
                        showTablePresetMenu = false
                        onOpenCustomTableDialog()
                    }
                )
            }
        }

        RibbonGroupDivider()

        // Page Break
        RibbonActionButton(
            icon = Icons.Default.InsertPageBreak,
            label = "Page Break",
            onClick = onInsertPageBreak
        )

        // Horizontal Divider
        RibbonActionButton(
            icon = Icons.Default.HorizontalRule,
            label = "Divider Line",
            onClick = onInsertDivider
        )

        RibbonGroupDivider()

        // Quick Symbols Row
        val quickSymbols = listOf("©", "®", "™", "—", "•", "§", "€", "£", "¥", "±", "°", "✓", "★")
        Text("Quick Symbols:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        quickSymbols.forEach { sym ->
            FilledTonalButton(
                onClick = { onInsertSymbol(sym) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(sym, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // More Symbols dialog button
        OutlinedButton(
            onClick = onOpenSymbolPickerDialog,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("More...", fontSize = 11.sp)
        }

        RibbonGroupDivider()

        // Date & Time Stamp
        RibbonActionButton(
            icon = Icons.Default.CalendarToday,
            label = "Date & Time",
            onClick = onInsertDateTime
        )
    }
}

/**
 * LAYOUT Ribbon: Margins, Orientation, Paper Size, Paper Tint, Page Setup
 */
@Composable
private fun LayoutRibbonCommands(
    settings: PageSettings,
    onUpdateMargins: (PageMargins) -> Unit,
    onUpdateOrientation: (PageOrientation) -> Unit,
    onUpdatePageSize: (PageSize) -> Unit,
    onUpdatePaperColor: (String) -> Unit,
    onOpenPageSetupDialog: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Margins Group
        Text("Margins:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PageMargins.values().forEach { margin ->
            val isSelected = settings.margins == margin
            FilterChip(
                selected = isSelected,
                onClick = { onUpdateMargins(margin) },
                label = { Text(margin.displayName.split(" ")[0], fontSize = 11.sp) },
                modifier = Modifier.height(32.dp)
            )
        }

        RibbonGroupDivider()

        // Orientation Group
        Text("Orientation:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PageOrientation.values().forEach { orient ->
            val isSelected = settings.orientation == orient
            FilterChip(
                selected = isSelected,
                onClick = { onUpdateOrientation(orient) },
                label = { Text(orient.displayName, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (orient == PageOrientation.PORTRAIT) Icons.Default.StayCurrentPortrait else Icons.Default.StayCurrentLandscape,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.height(32.dp)
            )
        }

        RibbonGroupDivider()

        // Paper Size Group
        Text("Size:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PageSize.values().forEach { size ->
            val isSelected = settings.pageSize == size
            FilterChip(
                selected = isSelected,
                onClick = { onUpdatePageSize(size) },
                label = { Text(size.name, fontSize = 11.sp) },
                modifier = Modifier.height(32.dp)
            )
        }

        RibbonGroupDivider()

        // Paper Background Tint
        Text("Paper Tint:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val paperColors = listOf(
            "#FFFFFF" to "White",
            "#FDFBF7" to "Ivory",
            "#F7F2E7" to "Sepia",
            "#F1F5F9" to "Slate",
            "#1E293B" to "Dark"
        )
        paperColors.forEach { (hex, _) ->
            val isSelected = settings.backgroundColorHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(hex)))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onUpdatePaperColor(hex) }
            )
        }

        RibbonGroupDivider()

        // Full Page Setup Dialog
        OutlinedButton(
            onClick = onOpenPageSetupDialog,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Page Setup...", fontSize = 11.sp)
        }
    }
}

/**
 * REVIEW Ribbon: Word Count, Document Statistics, Outline, Find & Replace
 */
@Composable
private fun ReviewRibbonCommands(
    document: TypableDocument,
    onOpenStatsDialog: () -> Unit,
    onOpenOutlineDialog: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val scrollState = rememberScrollState()
    val stats = remember(document) { document.computeStats() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Live Word Count Pill
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
            onClick = onOpenStatsDialog,
            modifier = Modifier.height(34.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stats.wordCount} words • ${stats.characterCountWithSpaces} chars",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        RibbonGroupDivider()

        // Document Outline / Headings Navigator
        RibbonActionButton(
            icon = Icons.Default.MenuBook,
            label = "Document Outline",
            onClick = onOpenOutlineDialog
        )

        // Find & Replace
        RibbonActionButton(
            icon = Icons.Default.Search,
            label = "Find & Replace",
            onClick = onOpenSearch
        )

        RibbonGroupDivider()

        // Readability / Reading Time Estimate
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "~${stats.readingTimeMinutes} min reading time",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * VIEW Ribbon: Print Layout vs Continuous View, Show/Hide Ruler, Zoom Levels, Focus Mode
 */
@Composable
private fun ViewRibbonCommands(
    isPrintLayout: Boolean,
    showRuler: Boolean,
    zoomScale: Float,
    onTogglePrintLayout: () -> Unit,
    onToggleRuler: () -> Unit,
    onSetZoom: (Float) -> Unit,
    onToggleFocusMode: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Document View Mode Switcher
        Text("Document View:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilterChip(
            selected = isPrintLayout,
            onClick = { if (!isPrintLayout) onTogglePrintLayout() },
            label = { Text("Print Layout (Page View)", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp)) },
            modifier = Modifier.height(32.dp)
        )
        FilterChip(
            selected = !isPrintLayout,
            onClick = { if (isPrintLayout) onTogglePrintLayout() },
            label = { Text("Continuous Flow (Web View)", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Article, contentDescription = null, modifier = Modifier.size(14.dp)) },
            modifier = Modifier.height(32.dp)
        )

        RibbonGroupDivider()

        // Ruler Toggle
        FilterChip(
            selected = showRuler,
            onClick = onToggleRuler,
            label = { Text("Typographic Ruler", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(14.dp)) },
            modifier = Modifier.height(32.dp)
        )

        RibbonGroupDivider()

        // Zoom Stepper
        Text("Zoom:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                .height(32.dp)
                .padding(horizontal = 4.dp)
        ) {
            IconButton(
                onClick = { onSetZoom((zoomScale - 0.15f).coerceAtLeast(0.70f)) },
                modifier = Modifier.size(24.dp),
                enabled = zoomScale > 0.70f
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(14.dp))
            }
            Text(
                text = "${(zoomScale * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(
                onClick = { onSetZoom((zoomScale + 0.15f).coerceAtMost(1.50f)) },
                modifier = Modifier.size(24.dp),
                enabled = zoomScale < 1.50f
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(14.dp))
            }
        }

        // 100% Reset Button
        if (zoomScale != 1.0f) {
            TextButton(
                onClick = { onSetZoom(1.0f) },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Text("100%", fontSize = 11.sp)
            }
        }

        RibbonGroupDivider()

        // Focus Mode / Immersive Mode
        RibbonActionButton(
            icon = Icons.Default.Fullscreen,
            label = "Focus Mode",
            onClick = onToggleFocusMode
        )
    }
}

/**
 * FILE Ribbon: Save, Export to DOCX, PDF, Markdown, HTML, TXT, Print, Share
 */
@Composable
private fun FileRibbonCommands(
    onExplicitSave: () -> Unit,
    onExportDocx: () -> Unit,
    onExportPdf: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportHtml: () -> Unit,
    onExportTxt: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Quick Save
        FilledTonalButton(
            onClick = onExplicitSave,
            modifier = Modifier.height(34.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save Document", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        RibbonGroupDivider()

        // Export as Microsoft Word DOCX
        RibbonActionButton(
            icon = Icons.Default.Description,
            label = "Word (.docx)",
            onClick = onExportDocx
        )

        // Export as PDF
        RibbonActionButton(
            icon = Icons.Default.PictureAsPdf,
            label = "PDF (.pdf)",
            onClick = onExportPdf
        )

        // Export as Markdown
        RibbonActionButton(
            icon = Icons.Default.Code,
            label = "Markdown (.md)",
            onClick = onExportMarkdown
        )

        // Export as HTML
        RibbonActionButton(
            icon = Icons.Default.Language,
            label = "HTML (.html)",
            onClick = onExportHtml
        )

        // Export as TXT
        RibbonActionButton(
            icon = Icons.Default.Notes,
            label = "Plain Text (.txt)",
            onClick = onExportTxt
        )

        RibbonGroupDivider()

        // Print
        RibbonActionButton(
            icon = Icons.Default.Print,
            label = "Print",
            onClick = onPrint
        )

        // Share
        RibbonActionButton(
            icon = Icons.Default.Share,
            label = "Share File",
            onClick = onShare
        )
    }
}

// ---------------- UI Helpers & Components ----------------

@Composable
fun RibbonActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun RibbonIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
fun RibbonToggleButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    FilledIconToggleButton(
        checked = isActive,
        onCheckedChange = { onClick() },
        modifier = Modifier.size(34.dp),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.primary,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun RibbonColorButton(
    icon: ImageVector,
    label: String,
    barColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(34.dp),
        shape = RoundedCornerShape(6.dp),
        color = Color.Transparent
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(3.dp)
                    .background(barColor, shape = RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun RibbonGroupDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
fun ColorPaletteRow(
    colors: List<String?>,
    onSelectColor: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { hex ->
            if (hex == null) {
                // Auto / None option
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onSelectColor(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "None", modifier = Modifier.size(14.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                        .clickable { onSelectColor(hex) }
                )
            }
        }
    }
}

@Composable
fun ParagraphStylesDialog(
    currentStyle: BlockStyle,
    onSelect: (BlockStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Paragraph Style", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BlockStyle.values().forEach { style ->
                    val isSelected = style == currentStyle
                    Surface(
                        onClick = { onSelect(style) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = style.displayName,
                                fontSize = when (style) {
                                    BlockStyle.TITLE -> 18.sp
                                    BlockStyle.HEADING_1 -> 16.sp
                                    BlockStyle.HEADING_2 -> 15.sp
                                    else -> 14.sp
                                },
                                fontWeight = if (style.isHeading) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
