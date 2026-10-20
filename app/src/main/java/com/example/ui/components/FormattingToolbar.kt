package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun FormattingToolbar(
    activeBlock: DocumentBlock?,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onApplyStyle: (BlockStyle) -> Unit,
    onApplyAlignment: (TextAlignment) -> Unit,
    onToggleBulletList: () -> Unit,
    onToggleNumberedList: () -> Unit,
    onIndent: () -> Unit,
    onOutdent: () -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenHighlightPicker: () -> Unit,
    onOpenInsertMenu: () -> Unit,
    onOpenExpandedPanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val currentStyle = (activeBlock as? DocumentBlock.Paragraph)?.style ?: BlockStyle.NORMAL
    val currentAlign = (activeBlock as? DocumentBlock.Paragraph)?.alignment ?: TextAlignment.LEFT
    val currentList = (activeBlock as? DocumentBlock.Paragraph)?.listType ?: ListType.NONE
    val currentRun = (activeBlock as? DocumentBlock.Paragraph)?.runs?.firstOrNull() ?: TextRun("")

    var showStylePickerModal by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("formatting_toolbar"),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Undo / Redo
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                enabled = canUndo,
                onClick = onUndo,
                testTag = "undo_button"
            )
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                enabled = canRedo,
                onClick = onRedo,
                testTag = "redo_button"
            )

            ToolbarDivider()

            // Style Picker Button
            OutlinedButton(
                onClick = { showStylePickerModal = true },
                modifier = Modifier
                    .height(38.dp)
                    .testTag("style_dropdown_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = currentStyle.displayName,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Style",
                    modifier = Modifier.size(18.dp)
                )
            }

            ToolbarDivider()

            // Bold
            ToolbarToggleButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Bold",
                isActive = currentRun.isBold,
                onClick = onToggleBold,
                testTag = "format_bold_button"
            )

            // Italic
            ToolbarToggleButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                isActive = currentRun.isItalic,
                onClick = onToggleItalic,
                testTag = "format_italic_button"
            )

            // Underline
            ToolbarToggleButton(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                isActive = currentRun.isUnderline,
                onClick = onToggleUnderline,
                testTag = "format_underline_button"
            )

            // Strikethrough
            ToolbarToggleButton(
                icon = Icons.Default.FormatStrikethrough,
                contentDescription = "Strikethrough",
                isActive = currentRun.isStrikethrough,
                onClick = onToggleStrikethrough,
                testTag = "format_strikethrough_button"
            )

            ToolbarDivider()

            // Alignments
            ToolbarToggleButton(
                icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                contentDescription = "Align Left",
                isActive = currentAlign == TextAlignment.LEFT,
                onClick = { onApplyAlignment(TextAlignment.LEFT) },
                testTag = "align_left_button"
            )
            ToolbarToggleButton(
                icon = Icons.Default.FormatAlignCenter,
                contentDescription = "Center",
                isActive = currentAlign == TextAlignment.CENTER,
                onClick = { onApplyAlignment(TextAlignment.CENTER) },
                testTag = "align_center_button"
            )
            ToolbarToggleButton(
                icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                contentDescription = "Align Right",
                isActive = currentAlign == TextAlignment.RIGHT,
                onClick = { onApplyAlignment(TextAlignment.RIGHT) },
                testTag = "align_right_button"
            )
            ToolbarToggleButton(
                icon = Icons.Default.FormatAlignJustify,
                contentDescription = "Justify",
                isActive = currentAlign == TextAlignment.JUSTIFY,
                onClick = { onApplyAlignment(TextAlignment.JUSTIFY) },
                testTag = "align_justify_button"
            )

            ToolbarDivider()

            // Lists
            ToolbarToggleButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "Bullet List",
                isActive = currentList == ListType.BULLET,
                onClick = onToggleBulletList,
                testTag = "bullet_list_button"
            )
            ToolbarToggleButton(
                icon = Icons.Default.FormatListNumbered,
                contentDescription = "Numbered List",
                isActive = currentList == ListType.NUMBERED,
                onClick = onToggleNumberedList,
                testTag = "numbered_list_button"
            )

            // Indent & Outdent
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                contentDescription = "Decrease Indent",
                enabled = ((activeBlock as? DocumentBlock.Paragraph)?.indentationLevel ?: 0) > 0,
                onClick = onOutdent,
                testTag = "outdent_button"
            )
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                contentDescription = "Increase Indent",
                onClick = onIndent,
                testTag = "indent_button"
            )

            ToolbarDivider()

            // Colors
            ToolbarIconButton(
                icon = Icons.Default.FormatColorText,
                contentDescription = "Text Color",
                onClick = onOpenColorPicker,
                testTag = "text_color_button"
            )
            ToolbarIconButton(
                icon = Icons.Default.Highlight,
                contentDescription = "Highlight",
                onClick = onOpenHighlightPicker,
                testTag = "highlight_button"
            )

            // Insert Menu (+)
            ToolbarIconButton(
                icon = Icons.Default.AddCircleOutline,
                contentDescription = "Insert Objects",
                onClick = onOpenInsertMenu,
                testTag = "insert_menu_button"
            )

            // More Typography (Aa)
            FilledTonalIconButton(
                onClick = onOpenExpandedPanel,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("expanded_format_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Detailed Typography Panel",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showStylePickerModal) {
        AlertDialog(
            onDismissRequest = { showStylePickerModal = false },
            title = { Text("Choose Paragraph Style", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BlockStyle.values().forEach { style ->
                        val isSelected = style == currentStyle
                        Surface(
                            onClick = {
                                onApplyStyle(style)
                                showStylePickerModal = false
                            },
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
                TextButton(onClick = { showStylePickerModal = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = ""
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(38.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
fun ToolbarToggleButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    FilledIconToggleButton(
        checked = isActive,
        onCheckedChange = { onClick() },
        modifier = modifier
            .size(38.dp)
            .testTag(testTag),
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.primary,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    )
}
