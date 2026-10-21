package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.model.DocumentFont
import com.example.storage.CustomFontManager
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import com.example.model.PageMargins
import com.example.model.PageOrientation
import com.example.model.PageSize
import com.example.ui.components.*
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.SaveState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Ribbon & View state (Microsoft Word & Google Docs layout)
    var activeRibbonTab by remember { mutableStateOf(RibbonTab.HOME) }
    var isRibbonExpanded by remember { mutableStateOf(true) }
    var showRuler by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Dialog & Panel states
    var showPageSetup by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showInsertMenu by remember { mutableStateOf(false) }
    var showCustomTableDialog by remember { mutableStateOf(false) }
    var showSymbolPicker by remember { mutableStateOf(false) }
    var showShortcuts by remember { mutableStateOf(false) }
    var showExpandedFormat by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(uiState.document.title) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    val stats = remember(uiState.document) { uiState.document.computeStats() }

    val defaultInkColor = remember(uiState.document.pageSettings.backgroundColorHex) {
        val paperColor = try {
            Color(android.graphics.Color.parseColor(uiState.document.pageSettings.backgroundColorHex))
        } catch (_: Exception) {
            Color.White
        }
        val lum = 0.299f * paperColor.red + 0.587f * paperColor.green + 0.114f * paperColor.blue
        if (lum < 0.5f) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = CustomFontManager.importFontFromUri(context, uri)
            result.onSuccess { fontName ->
                viewModel.applyFontFamily(DocumentFont.CUSTOM, fontName)
                scope.launch {
                    snackbarHostState.showSnackbar("Font '$fontName' imported and applied successfully!")
                }
            }.onFailure { err ->
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to import font: ${err.message ?: "Invalid TTF/OTF font file"}")
                }
            }
        }
    }

    val importDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAndInsertFromFile(context, uri) { success ->
                if (success) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Document content imported successfully!")
                    }
                }
            }
        }
    }

    // Intercept back navigation to guarantee auto-saving
    BackHandler {
        viewModel.explicitSave()
        onNavigateBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!uiState.isFocusMode) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // --- Signature Word Processor Top Bar (MS Word / Docs Header) ---
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    viewModel.explicitSave()
                                    onNavigateBack()
                                },
                                modifier = Modifier.testTag("editor_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        renameText = uiState.document.title
                                        showRenameDialog = true
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = uiState.document.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename",
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    SaveStatusIndicator(
                                        saveState = uiState.saveState,
                                        onRetry = { viewModel.explicitSave() }
                                    )
                                }
                            }
                        },
                        actions = {
                            // Quick Undo
                            IconButton(
                                onClick = { viewModel.undo() },
                                enabled = uiState.canUndo,
                                modifier = Modifier.testTag("topbar_undo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }

                            // Quick Redo
                            IconButton(
                                onClick = { viewModel.redo() },
                                enabled = uiState.canRedo,
                                modifier = Modifier.testTag("topbar_redo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }

                            // Share / Export
                            IconButton(onClick = { showExportSheet = true }) {
                                Icon(Icons.Default.Share, contentDescription = "Export & Share")
                            }

                            // Overflow Menu
                            IconButton(onClick = { overflowMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // --- Famous Word Processor Command Ribbon (Home, Insert, Layout, Review, View, File) ---
                    WordProcessorRibbon(
                        activeBlock = viewModel.activeBlock,
                        canUndo = uiState.canUndo,
                        canRedo = uiState.canRedo,
                        document = uiState.document,
                        isPrintLayout = uiState.isPrintLayoutMode,
                        showRuler = showRuler,
                        zoomScale = zoomScale,
                        defaultInkColor = defaultInkColor,
                        isRibbonExpanded = isRibbonExpanded,
                        activeTab = activeRibbonTab,
                        onTabSelected = {
                            if (activeRibbonTab == it && isRibbonExpanded) {
                                isRibbonExpanded = false
                            } else {
                                activeRibbonTab = it
                                isRibbonExpanded = true
                            }
                        },
                        onToggleRibbonExpanded = { isRibbonExpanded = !isRibbonExpanded },
                        // Home Tab Actions
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onToggleBold = { viewModel.toggleBold() },
                        onToggleItalic = { viewModel.toggleItalic() },
                        onToggleUnderline = { viewModel.toggleUnderline() },
                        onToggleStrikethrough = { viewModel.toggleStrikethrough() },
                        onToggleSuperscript = { viewModel.toggleSuperscript() },
                        onToggleSubscript = { viewModel.toggleSubscript() },
                        onApplyStyle = { viewModel.applyStyle(it) },
                        onApplyAlignment = { viewModel.applyAlignment(it) },
                        onToggleBulletList = { viewModel.toggleBulletList() },
                        onToggleNumberedList = { viewModel.toggleNumberedList() },
                        onIndent = { viewModel.indent() },
                        onOutdent = { viewModel.outdent() },
                        onApplyFontFamily = { font, customName -> viewModel.applyFontFamily(font, customName) },
                        onImportFontClick = {
                            fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype", "application/octet-stream", "*/*"))
                        },
                        onApplyFontSizeDelta = { viewModel.applyFontSizeDelta(it) },
                        onApplyColor = { viewModel.applyColor(it) },
                        onApplyHighlight = { viewModel.applyHighlight(it) },
                        onClearFormatting = { viewModel.clearFormatting() },
                        onOpenExpandedPanel = { showExpandedFormat = true },
                        // Insert Tab Actions
                        onInsertTable = { r, c -> viewModel.insertTable(r, c) },
                        onInsertDivider = { viewModel.insertDivider() },
                        onInsertPageBreak = { viewModel.insertPageBreak() },
                        onInsertSymbol = { viewModel.insertSymbol(it) },
                        onInsertDateTime = {
                            val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
                            viewModel.insertSymbol(dateStr)
                        },
                        onOpenCustomTableDialog = { showCustomTableDialog = true },
                        onOpenSymbolPickerDialog = { showSymbolPicker = true },
                        // Layout Tab Actions
                        onUpdateMargins = {
                            viewModel.updatePageSettings(uiState.document.pageSettings.copy(margins = it))
                        },
                        onUpdateOrientation = {
                            viewModel.updatePageSettings(uiState.document.pageSettings.copy(orientation = it))
                        },
                        onUpdatePageSize = {
                            viewModel.updatePageSettings(uiState.document.pageSettings.copy(pageSize = it))
                        },
                        onUpdatePaperColor = {
                            viewModel.updatePageSettings(uiState.document.pageSettings.copy(backgroundColorHex = it))
                        },
                        onOpenPageSetupDialog = { showPageSetup = true },
                        // Review Tab Actions
                        onOpenStatsDialog = { showStats = true },
                        onOpenOutlineDialog = { showOutline = true },
                        onOpenSearch = { viewModel.openSearch() },
                        // View Tab Actions
                        onTogglePrintLayout = { viewModel.togglePrintLayoutMode() },
                        onToggleRuler = { showRuler = !showRuler },
                        onSetZoom = { zoomScale = it },
                        onToggleFocusMode = { viewModel.toggleFocusMode() },
                        // File Tab Actions
                        onExplicitSave = { viewModel.explicitSave() },
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
                        onShare = { showExportSheet = true }
                    )
                }
            }
        },
        bottomBar = {
            if (!uiState.isFocusMode) {
                // --- Classic Word Processor Bottom Status Bar (MS Word / LibreOffice) ---
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Section: Page count, Words, Characters, Save status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Clickable Page Number indicator
                            Surface(
                                onClick = { showOutline = true },
                                color = Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Page 1 of ${stats.pageCount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))

                            // Clickable Word & Character counter
                            Surface(
                                onClick = { showStats = true },
                                color = Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${stats.wordCount} words",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            if (uiState.statusMessage != null) {
                                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text(
                                    text = uiState.statusMessage ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Right Section: Layout View Toggles, Ruler, and Zoom Stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Print Layout View Icon Toggle
                            IconButton(
                                onClick = {
                                    if (!uiState.isPrintLayoutMode) viewModel.togglePrintLayoutMode()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Print Layout",
                                    modifier = Modifier.size(17.dp),
                                    tint = if (uiState.isPrintLayoutMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }

                            // Web Reflow View Icon Toggle
                            IconButton(
                                onClick = {
                                    if (uiState.isPrintLayoutMode) viewModel.togglePrintLayoutMode()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Article,
                                    contentDescription = "Web Reflow Layout",
                                    modifier = Modifier.size(17.dp),
                                    tint = if (!uiState.isPrintLayoutMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }

                            // Ruler Toggle
                            IconButton(
                                onClick = { showRuler = !showRuler },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Straighten,
                                    contentDescription = "Toggle Ruler",
                                    modifier = Modifier.size(17.dp),
                                    tint = if (showRuler) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }

                            // Zoom -
                            IconButton(
                                onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.70f) },
                                modifier = Modifier.size(30.dp),
                                enabled = zoomScale > 0.70f
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(14.dp))
                            }

                            // Zoom Percentage indicator
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { zoomScale = 1.0f }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            // Zoom +
                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(1.50f) },
                                modifier = Modifier.size(30.dp),
                                enabled = zoomScale < 1.50f
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(14.dp))
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
                .imePadding()
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

                // Interactive Document Canvas (with Print Layout, Ruler, and Zoom Scale)
                DocumentCanvas(
                    document = uiState.document,
                    activeBlockIndex = uiState.activeBlockIndex,
                    isPrintLayout = uiState.isPrintLayoutMode,
                    showRuler = showRuler,
                    zoomScale = zoomScale,
                    requestedCursorOffset = uiState.requestedCursorOffset,
                    onConsumeRequestedCursor = { viewModel.consumeRequestedCursor() },
                    onSelectBlock = { viewModel.selectBlock(it) },
                    onUpdateText = { idx, text -> viewModel.updateParagraphText(idx, text) },
                    onSplitParagraph = { idx, offset -> viewModel.splitParagraph(idx, offset) },
                    onSplitParagraphWithText = { idx, offset, before, after ->
                        viewModel.splitParagraph(idx, offset, before, after)
                    },
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

    // Dialogs & Modals
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

    if (showCustomTableDialog) {
        CustomTableDialog(
            onInsertTable = { r, c ->
                viewModel.insertTable(r, c)
                showCustomTableDialog = false
            },
            onDismiss = { showCustomTableDialog = false }
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
            onExportOdt = {
                val uri = viewModel.exportToOdt(context)
                uri?.let { viewModel.shareExportedFile(context, it, "application/vnd.oasis.opendocument.text") }
            },
            onExportRtf = {
                val uri = viewModel.exportToRtf(context)
                uri?.let { viewModel.shareExportedFile(context, it, "application/rtf") }
            },
            onExportEpub = {
                val uri = viewModel.exportToEpub(context)
                uri?.let { viewModel.shareExportedFile(context, it, "application/epub+zip") }
            },
            onExportMarkdown = {
                val uri = viewModel.exportToMarkdown(context)
                uri?.let { viewModel.shareExportedFile(context, it, "text/markdown") }
            },
            onExportHtml = {
                val uri = viewModel.exportToHtml(context)
                uri?.let { viewModel.shareExportedFile(context, it, "text/html") }
            },
            onExportCsv = {
                val uri = viewModel.exportToCsv(context)
                uri?.let { viewModel.shareExportedFile(context, it, "text/csv") }
            },
            onExportJson = {
                val uri = viewModel.exportToJson(context)
                uri?.let { viewModel.shareExportedFile(context, it, "application/json") }
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
            onToggleBold = { viewModel.toggleBold() },
            onToggleItalic = { viewModel.toggleItalic() },
            onToggleUnderline = { viewModel.toggleUnderline() },
            onToggleStrikethrough = { viewModel.toggleStrikethrough() },
            onToggleSuperscript = { viewModel.toggleSuperscript() },
            onToggleSubscript = { viewModel.toggleSubscript() },
            onApplyColor = { viewModel.applyColor(it) },
            onApplyHighlight = { viewModel.applyHighlight(it) },
            onClearFormatting = { viewModel.clearFormatting() },
            onApplyStyle = { viewModel.applyStyle(it) },
            onApplyAlignment = { viewModel.applyAlignment(it) },
            onToggleBulletList = { viewModel.toggleBulletList() },
            onToggleNumberedList = { viewModel.toggleNumberedList() },
            onIndent = { viewModel.indent() },
            onOutdent = { viewModel.outdent() },
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
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotBlank()) {
                        viewModel.updateTitle(renameText.trim())
                    }
                    showRenameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Overflow Bottom Sheet / Menu
    if (overflowMenuExpanded) {
        ModalBottomSheet(
            onDismissRequest = { overflowMenuExpanded = false },
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text("Document Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                Surface(
                    onClick = {
                        overflowMenuExpanded = false
                        showExportSheet = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Export Document (DOCX, PDF, ODT, RTF, EPUB...)", fontSize = 14.sp)
                    }
                }

                Surface(
                    onClick = {
                        overflowMenuExpanded = false
                        importDocLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.oasis.opendocument.text",
                                "application/rtf",
                                "text/rtf",
                                "application/epub+zip",
                                "application/json",
                                "text/csv",
                                "text/plain",
                                "text/markdown",
                                "text/html",
                                "*/*"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Import & Insert from External Document...", fontSize = 14.sp)
                    }
                }

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
                        Text("Page Setup (Margins & Dimensions)", fontSize = 14.sp)
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
fun SaveStatusIndicator(
    saveState: SaveState,
    onRetry: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (saveState == SaveState.ERROR && onRetry != null) {
            Modifier.clickable { onRetry() }
        } else Modifier
    ) {
        when (saveState) {
            SaveState.SAVED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text("Saved to device", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(
                    text = if (onRetry != null) "Save error (tap to retry)" else "Save error",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
