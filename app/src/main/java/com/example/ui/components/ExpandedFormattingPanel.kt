package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedFormattingBottomSheet(
    activeBlock: DocumentBlock?,
    onApplyFontFamily: (DocumentFont) -> Unit,
    onApplyFontSizeDelta: (Float) -> Unit,
    onToggleBold: () -> Unit = {},
    onToggleItalic: () -> Unit = {},
    onToggleUnderline: () -> Unit = {},
    onToggleStrikethrough: () -> Unit = {},
    onToggleSuperscript: () -> Unit = {},
    onToggleSubscript: () -> Unit = {},
    onApplyColor: (String?) -> Unit = {},
    onApplyHighlight: (String?) -> Unit = {},
    onClearFormatting: () -> Unit = {},
    onApplyStyle: (BlockStyle) -> Unit = {},
    onApplyAlignment: (TextAlignment) -> Unit = {},
    onToggleBulletList: () -> Unit = {},
    onToggleNumberedList: () -> Unit = {},
    onIndent: () -> Unit = {},
    onOutdent: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) } // 0: Text, 1: Paragraph

    val paragraph = activeBlock as? DocumentBlock.Paragraph
    val currentRun = paragraph?.runs?.firstOrNull() ?: TextRun("")
    val currentFontSize = currentRun.fontSizeSp.toInt()
    val currentStyle = paragraph?.style ?: BlockStyle.NORMAL
    val currentAlign = paragraph?.alignment ?: TextAlignment.LEFT
    val currentList = paragraph?.listType ?: ListType.NONE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("expanded_formatting_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .navigationBarsPadding()
        ) {
            // Google Docs / MS Word Style Two-Tab Header
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "TEXT",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "PARAGRAPH",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    // --- TEXT TAB ---

                    // Quick Character Format Toggles Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolbarToggleButton(
                            icon = Icons.Default.FormatBold,
                            contentDescription = "Bold",
                            isActive = currentRun.isBold,
                            onClick = onToggleBold,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarToggleButton(
                            icon = Icons.Default.FormatItalic,
                            contentDescription = "Italic",
                            isActive = currentRun.isItalic,
                            onClick = onToggleItalic,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarToggleButton(
                            icon = Icons.Default.FormatUnderlined,
                            contentDescription = "Underline",
                            isActive = currentRun.isUnderline,
                            onClick = onToggleUnderline,
                            modifier = Modifier.weight(1f)
                        )
                        ToolbarToggleButton(
                            icon = Icons.Default.FormatStrikethrough,
                            contentDescription = "Strikethrough",
                            isActive = currentRun.isStrikethrough,
                            onClick = onToggleStrikethrough,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Font Family Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Font Family (Google Fonts & System)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            DocumentFont.values().filter { it != DocumentFont.CUSTOM }.forEach { font ->
                                val isSelected = currentRun.fontFamily == font
                                val previewFamily = com.example.storage.CustomFontManager.getComposeFontFamily(context, font)
                                OutlinedButton(
                                    onClick = { onApplyFontFamily(font) },
                                    modifier = Modifier.height(40.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Text(
                                        text = font.displayName,
                                        fontSize = 12.sp,
                                        fontFamily = previewFamily,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Font Size Stepper
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Font Size", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTonalIconButton(
                                onClick = { onApplyFontSizeDelta(-1f) },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease size")
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "$currentFontSize pt",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onApplyFontSizeDelta(1f) },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase size")
                            }
                        }
                    }

                    // Text Color Swatches
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Text Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val textColors = listOf(
                            null to "Default",
                            "#0F172A" to "Slate",
                            "#0284C7" to "Sky",
                            "#16A34A" to "Emerald",
                            "#DC2626" to "Rose",
                            "#D97706" to "Amber",
                            "#9333EA" to "Purple"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            textColors.forEach { (hex, label) ->
                                val color = hex?.let { try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { Color.Gray } } ?: MaterialTheme.colorScheme.onSurface
                                val isSelected = currentRun.textColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (hex == null) MaterialTheme.colorScheme.surfaceVariant else color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable { onApplyColor(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hex == null && !isSelected) {
                                        Text("A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else if (isSelected) {
                                        val lum = if (hex == null) 0.5f else (0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue)
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = label,
                                            tint = if (lum > 0.5f) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Highlight Swatches
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Highlight Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val highlights = listOf(
                            null to "None",
                            "#FEF08A" to "Yellow",
                            "#BAE6FD" to "Sky",
                            "#BBF7D0" to "Lime",
                            "#FECDD3" to "Rose",
                            "#FED7AA" to "Peach"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            highlights.forEach { (hex, label) ->
                                val color = hex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Transparent
                                val isSelected = currentRun.highlightColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (hex == null) MaterialTheme.colorScheme.surfaceVariant else color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable { onApplyHighlight(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hex == null) {
                                        Icon(Icons.Default.Block, contentDescription = "None", modifier = Modifier.size(16.dp))
                                    } else if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = label, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Superscript / Subscript
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentRun.isSuperscript,
                            onClick = onToggleSuperscript,
                            label = { Text("x² Superscript") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentRun.isSubscript,
                            onClick = onToggleSubscript,
                            label = { Text("x₂ Subscript") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Clear Formatting Button
                    OutlinedButton(
                        onClick = onClearFormatting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.FormatClear, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Direct Formatting")
                    }

                } else {
                    // --- PARAGRAPH TAB ---

                    // Alignments
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Alignment", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ToolbarToggleButton(
                                icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                                contentDescription = "Align Left",
                                isActive = currentAlign == TextAlignment.LEFT,
                                onClick = { onApplyAlignment(TextAlignment.LEFT) },
                                modifier = Modifier.weight(1f)
                            )
                            ToolbarToggleButton(
                                icon = Icons.Default.FormatAlignCenter,
                                contentDescription = "Center",
                                isActive = currentAlign == TextAlignment.CENTER,
                                onClick = { onApplyAlignment(TextAlignment.CENTER) },
                                modifier = Modifier.weight(1f)
                            )
                            ToolbarToggleButton(
                                icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                                contentDescription = "Align Right",
                                isActive = currentAlign == TextAlignment.RIGHT,
                                onClick = { onApplyAlignment(TextAlignment.RIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ToolbarToggleButton(
                                icon = Icons.Default.FormatAlignJustify,
                                contentDescription = "Justify",
                                isActive = currentAlign == TextAlignment.JUSTIFY,
                                onClick = { onApplyAlignment(TextAlignment.JUSTIFY) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Line Spacing
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Line Spacing", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currentSpacing = paragraph?.lineSpacing ?: 1.15f
                            listOf(1.0f to "1.0", 1.15f to "1.15", 1.5f to "1.5", 2.0f to "2.0").forEach { (spacing, label) ->
                                val isSelected = kotlin.math.abs(currentSpacing - spacing) < 0.05f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { /* Handled in block formatting */ },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Indentation & Lists
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Lists & Indentation", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ToolbarToggleButton(
                                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                contentDescription = "Bullet List",
                                isActive = currentList == ListType.BULLET,
                                onClick = onToggleBulletList,
                                modifier = Modifier.weight(1f)
                            )
                            ToolbarToggleButton(
                                icon = Icons.Default.FormatListNumbered,
                                contentDescription = "Numbered List",
                                isActive = currentList == ListType.NUMBERED,
                                onClick = onToggleNumberedList,
                                modifier = Modifier.weight(1f)
                            )
                            ToolbarIconButton(
                                icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                                contentDescription = "Decrease Indent",
                                enabled = (paragraph?.indentationLevel ?: 0) > 0,
                                onClick = onOutdent,
                                modifier = Modifier.weight(1f)
                            )
                            ToolbarIconButton(
                                icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                                contentDescription = "Increase Indent",
                                onClick = onIndent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Paragraph Styles Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Paragraph Style", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BlockStyle.values().forEach { style ->
                            val isSelected = style == currentStyle
                            Surface(
                                onClick = { onApplyStyle(style) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                }
            }
        }
    }
}
