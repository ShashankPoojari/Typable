package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedFormattingBottomSheet(
    activeBlock: DocumentBlock?,
    onApplyFontFamily: (DocumentFont) -> Unit,
    onApplyFontSizeDelta: (Float) -> Unit,
    onToggleSuperscript: () -> Unit,
    onToggleSubscript: () -> Unit,
    onApplyColor: (String?) -> Unit,
    onApplyHighlight: (String?) -> Unit,
    onClearFormatting: () -> Unit,
    onDismiss: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentRun = (activeBlock as? DocumentBlock.Paragraph)?.runs?.firstOrNull() ?: TextRun("")
    val currentFontSize = currentRun.fontSizeSp.toInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("expanded_formatting_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Typography & Formatting",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Font Family Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Font Family", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocumentFont.values().forEach { font ->
                        val isSelected = currentRun.fontFamily == font
                        OutlinedButton(
                            onClick = { onApplyFontFamily(font) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(
                                text = font.displayName.substringBefore(" "),
                                fontSize = 12.sp,
                                fontFamily = when (font) {
                                    DocumentFont.SERIF -> FontFamily.Serif
                                    DocumentFont.MONOSPACE -> FontFamily.Monospace
                                    else -> FontFamily.Default
                                },
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

                    Text(
                        text = "$currentFontSize sp",
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.titleMedium
                    )

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
                        val color = hex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.onSurface
                        val isSelected = currentRun.textColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onApplyColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = label,
                                    tint = if (hex == null || hex == "#0F172A") Color.White else Color.Black,
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

            // Superscript / Subscript / Clear
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

            OutlinedButton(
                onClick = onClearFormatting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.FormatClear, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Direct Formatting")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
