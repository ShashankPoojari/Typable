package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DocumentEngine

@Composable
fun SearchReplaceBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    replacement: String,
    onReplacementChanged: (String) -> Unit,
    matchCase: Boolean,
    onToggleMatchCase: () -> Unit,
    wholeWord: Boolean,
    onToggleWholeWord: () -> Unit,
    matches: List<DocumentEngine.SearchMatch>,
    currentMatchIndex: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplaceRow by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_replace_bar"),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Find Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    placeholder = { Text("Find...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("search_query_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChanged("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                // Match count indicator
                Text(
                    text = if (query.isBlank()) ""
                    else if (matches.isEmpty()) "0 of 0"
                    else "${currentMatchIndex + 1} of ${matches.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Match options toggles
                FilterChip(
                    selected = matchCase,
                    onClick = onToggleMatchCase,
                    label = { Text("Aa", fontSize = 11.sp) },
                    modifier = Modifier.height(32.dp).testTag("match_case_chip")
                )

                IconButton(
                    onClick = onPrevMatch,
                    enabled = matches.isNotEmpty(),
                    modifier = Modifier.size(34.dp).testTag("prev_match_button")
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
                }

                IconButton(
                    onClick = onNextMatch,
                    enabled = matches.isNotEmpty(),
                    modifier = Modifier.size(34.dp).testTag("next_match_button")
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                }

                IconButton(
                    onClick = { showReplaceRow = !showReplaceRow },
                    modifier = Modifier.size(34.dp).testTag("toggle_replace_button")
                ) {
                    Icon(
                        imageVector = if (showReplaceRow) Icons.Default.ExpandLess else Icons.Default.FindReplace,
                        contentDescription = "Toggle Replace",
                        tint = if (showReplaceRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close search")
                }
            }

            // Replace Row (Expanded)
            if (showReplaceRow) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = replacement,
                        onValueChange = onReplacementChanged,
                        placeholder = { Text("Replace with...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("replace_input"),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )

                    Button(
                        onClick = onReplaceCurrent,
                        enabled = matches.isNotEmpty(),
                        modifier = Modifier.height(38.dp).testTag("replace_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Replace", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onReplaceAll,
                        enabled = matches.isNotEmpty(),
                        modifier = Modifier.height(38.dp).testTag("replace_all_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("All", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
