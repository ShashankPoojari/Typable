package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DocumentStats
import com.example.ui.components.*
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.SaveState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Dialog & Panel states
    var showPageSetup by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showInsertMenu by remember { mutableStateOf(false) }
    var showSymbolPicker by remember { mutableStateOf(false) }
    var showShortcuts by remember { mutableStateOf(false) }
    var showExpandedFormat by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showHighlightPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(uiState.document.title) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    val stats = remember(uiState.document) { uiState.document.computeStats() }

    // Intercept back navigation to guarantee auto-saving
    BackHandler {
        viewModel.explicitSave()
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            if (!uiState.isFocusMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.explicitSave()
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                renameText = uiState.document.title
                                showRenameDialog = true
                            }
                        ) {
                            Column {
                                Text(
                                    text = uiState.document.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                SaveStatusIndicator(saveState = uiState.saveState)
                            }
                        }
                    },
                    actions = {
                        // Search Button
                        IconButton(onClick = { viewModel.openSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "Find & Replace")
                        }

                        // Outline (Table of contents)
                        IconButton(onClick = { showOutline = true }) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Document Outline")
                        }

                        // Export & Share
                        IconButton(onClick = { showExportSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Export & Share")
                        }

                        // Focus Mode toggle
                        IconButton(onClick = { viewModel.toggleFocusMode() }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Focus Mode")
                        }

                        // More Actions
                        IconButton(onClick = { overflowMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (!uiState.isFocusMode) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    // Formatting Toolbar
                    FormattingToolbar(
                        activeBlock = viewModel.activeBlock,
                        canUndo = uiState.canUndo,
                        canRedo = uiState.canRedo,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onToggleBold = { viewModel.toggleBold() },
                        onToggleItalic = { viewModel.toggleItalic() },
                        onToggleUnderline = { viewModel.toggleUnderline() },
                        onToggleStrikethrough = { viewModel.toggleStrikethrough() },
                        onApplyStyle = { viewModel.applyStyle(it) },
                        onApplyAlignment = { viewModel.applyAlignment(it) },
                        onToggleBulletList = { viewModel.toggleBulletList() },
                        onToggleNumberedList = { viewModel.toggleNumberedList() },
                        onIndent = { viewModel.indent() },
                        onOutdent = { viewModel.outdent() },
                        onOpenColorPicker = { showExpandedFormat = true },
                        onOpenHighlightPicker = { showExpandedFormat = true },
                        onOpenInsertMenu = { showInsertMenu = true },
                        onOpenExpandedPanel = { showExpandedFormat = true }
                    )

                    // Minimalist Word Count Status Strip
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stats.wordCount} words • ${stats.characterCountWithSpaces} characters",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (uiState.statusMessage != null) {
                                Text(
                                    text = uiState.statusMessage ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "Estimated ${stats.pageCount} page${if (stats.pageCount > 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search & Replace Bar (when active)
                AnimatedVisibility(visible = uiState.isSearching) {
                    SearchReplaceBar(
                        query = uiState.searchQuery,
                        onQueryChanged = { viewModel.setSearchQuery(it) },
                        replacement = uiState.replaceQuery,
                        onReplacementChanged = { viewModel.setReplaceQuery(it) },
                        matchCase = uiState.matchCase,
                        onToggleMatchCase = { viewModel.toggleMatchCase() },
                        wholeWord = uiState.wholeWord,
                        onToggleWholeWord = { viewModel.toggleWholeWord() },
                        matches = uiState.searchMatches,
                        currentMatchIndex = uiState.currentMatchIndex,
                        onNextMatch = { viewModel.nextMatch() },
                        onPrevMatch = { viewModel.prevMatch() },
                        onReplaceCurrent = { viewModel.replaceCurrentMatch() },
                        onReplaceAll = { viewModel.replaceAllMatches() },
                        onClose = { viewModel.closeSearch() }
                    )
                }

                // Interactive Document Canvas
                DocumentCanvas(
                    document = uiState.document,
                    activeBlockIndex = uiState.activeBlockIndex,
                    onSelectBlock = { viewModel.selectBlock(it) },
                    onUpdateText = { idx, text -> viewModel.updateParagraphText(idx, text) },
                    onSplitParagraph = { idx, offset -> viewModel.splitParagraph(idx, offset) },
                    onMergeWithPrevious = { idx -> viewModel.mergeWithPrevious(idx) },
                    onDeleteBlock = { idx -> viewModel.deleteBlock(idx) },
                    onUpdateTableCell = { bIdx, rIdx, cIdx, text ->
                        viewModel.updateTableCell(bIdx, rIdx, cIdx, text)
                    },
                    onAddTableRow = { bIdx, rIdx -> viewModel.addTableRow(bIdx, rIdx) },
                    onDeleteTableRow = { bIdx, rIdx -> viewModel.deleteTableRow(bIdx, rIdx) },
                    onAddTableColumn = { bIdx -> viewModel.addTableColumn(bIdx) },
                    onDeleteTableColumn = { bIdx, cIdx -> viewModel.deleteTableColumn(bIdx, cIdx) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Floating Exit Focus Mode Button (only visible in focus mode)
            if (uiState.isFocusMode) {
                FloatingActionButton(
                    onClick = { viewModel.toggleFocusMode() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Focus Mode")
                }
            }
        }
    }

    // Dialogs
    if (showPageSetup) {
        PageSetupDialog(
            currentSettings = uiState.document.pageSettings,
            onSave = {
                viewModel.updatePageSettings(it)
                showPageSetup = false
            },
            onDismiss = { showPageSetup = false }
        )
    }

    if (showStats) {
        DocumentStatsDialog(
            stats = stats,
            onDismiss = { showStats = false }
        )
    }

    if (showOutline) {
        DocumentOutlineDialog(
            document = uiState.document,
            onSelectBlockIndex = { viewModel.selectBlock(it) },
            onDismiss = { showOutline = false }
        )
    }

    if (showExportSheet) {
        ExportSheetDialog(
            document = uiState.document,
            onExportDocx = {
                val uri = viewModel.exportToDocx(context)
                uri?.let { viewModel.shareExportedFile(context, it, "application/vnd.openxmlformats-officedocument.wordprocessingml.document") }
            },
            onExportPdf = {
                val uri = viewModel.exportToPdf(context)
                uri?.let { viewModel.shareExportedFile(context, it, "application/pdf") }
            },
            onExportMarkdown = {
                val uri = viewModel.exportToMarkdown(context)
                uri?.let { viewModel.shareExportedFile(context, it, "text/markdown") }
            },
            onExportHtml = {
                val uri = viewModel.exportToHtml(context)
                uri?.let { viewModel.shareExportedFile(context, it, "text/html") }
            },
            onExportTxt = {
                val uri = viewModel.exportToTxt(context)
                uri?.let { viewModel.shareExportedFile(context, it, "text/plain") }
            },
            onPrint = { viewModel.print(context) },
            onDismiss = { showExportSheet = false }
        )
    }

    if (showInsertMenu) {
        InsertMenuDialog(
            onInsertTable = { r, c -> viewModel.insertTable(r, c) },
            onInsertDivider = { viewModel.insertDivider() },
            onInsertPageBreak = { viewModel.insertPageBreak() },
            onOpenSymbolPicker = { showSymbolPicker = true },
            onDismiss = { showInsertMenu = false }
        )
    }

    if (showSymbolPicker) {
        SymbolPickerDialog(
            onSelectSymbol = { viewModel.insertSymbol(it) },
            onDismiss = { showSymbolPicker = false }
        )
    }

    if (showShortcuts) {
        KeyboardShortcutsDialog(onDismiss = { showShortcuts = false })
    }

    if (showExpandedFormat) {
        ExpandedFormattingBottomSheet(
            activeBlock = viewModel.activeBlock,
            onApplyFontFamily = { viewModel.applyFontFamily(it) },
            onApplyFontSizeDelta = { viewModel.applyFontSizeDelta(it) },
            onToggleSuperscript = { viewModel.toggleSuperscript() },
            onToggleSubscript = { viewModel.toggleSubscript() },
            onApplyColor = { viewModel.applyColor(it) },
            onApplyHighlight = { viewModel.applyHighlight(it) },
            onClearFormatting = { viewModel.clearFormatting() },
            onDismiss = { showExpandedFormat = false }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotBlank()) {
                        viewModel.updateTitle(renameText)
                    }
                    showRenameDialog = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (overflowMenuExpanded) {
        ModalBottomSheet(
            onDismissRequest = { overflowMenuExpanded = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Document Tools",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    onClick = {
                        overflowMenuExpanded = false
                        showPageSetup = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsOverscan, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Page Setup (Margins & Size)", fontSize = 14.sp)
                    }
                }

                Surface(
                    onClick = {
                        overflowMenuExpanded = false
                        showStats = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Document Statistics (Word & Page Count)", fontSize = 14.sp)
                    }
                }

                Surface(
                    onClick = {
                        overflowMenuExpanded = false
                        viewModel.print(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Print via Android PrintManager", fontSize = 14.sp)
                    }
                }

                Surface(
                    onClick = {
                        overflowMenuExpanded = false
                        showShortcuts = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Keyboard Shortcuts Reference", fontSize = 14.sp)
                    }
                }

                Surface(
                    onClick = {
                        overflowMenuExpanded = false
                        renameText = uiState.document.title
                        showRenameDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Rename Document", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SaveStatusIndicator(saveState: SaveState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (saveState) {
            SaveState.SAVED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Saved", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SaveState.SAVING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Saving...", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
            SaveState.DIRTY -> {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFFD97706), RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Unsaved", fontSize = 10.sp, color = Color(0xFFD97706))
            }
            SaveState.ERROR -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Save Error", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
