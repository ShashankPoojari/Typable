package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TypableDocument
import com.example.viewmodel.LibraryTab
import com.example.viewmodel.LibraryViewModel
import com.example.viewmodel.SortOption
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenDocument: (TypableDocument) -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var isSearchActive by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var renameDialogDoc by remember { mutableStateOf<TypableDocument?>(null) }
    var renameText by remember { mutableStateOf("") }
    var selectedOptionsDoc by remember { mutableStateOf<TypableDocument?>(null) }

    // SAF Document Picker launcher for importing .docx, .md, .html, .txt
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importFileFromUri(context, it) { importedDoc ->
                onOpenDocument(importedDoc)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search title or content...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("library_search_input"),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("T", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Typable",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Documents")
                        }

                        // Sort Dialog Trigger
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Options")
                        }

                        // Import from Device (SAF)
                        IconButton(onClick = {
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain",
                                    "text/markdown",
                                    "text/html",
                                    "*/*"
                                )
                            )
                        }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Import Document")
                        }

                        // Settings
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.createBlankDocument { doc -> onOpenDocument(doc) }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Document") },
                modifier = Modifier.testTag("new_document_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row (All, Pinned, Favorites, Trash)
            PrimaryTabRow(
                selectedTabIndex = uiState.currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                LibraryTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.currentTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (tab) {
                                        LibraryTab.ALL -> Icons.Outlined.Folder
                                        LibraryTab.PINNED -> Icons.Outlined.PushPin
                                        LibraryTab.FAVORITES -> Icons.Outlined.Star
                                        LibraryTab.TRASH -> Icons.Outlined.Delete
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                    )
                }
            }

            // Quick Templates Bar (shown when on ALL tab and not searching)
            if (uiState.currentTab == LibraryTab.ALL && uiState.searchQuery.isEmpty()) {
                TemplatesRow(
                    onSelectTemplate = { templateName ->
                        viewModel.createFromTemplate(templateName) { doc -> onOpenDocument(doc) }
                    }
                )
            }

            // Documents List or Empty State
            if (uiState.filteredDocuments.isEmpty()) {
                EmptyStateView(tab = uiState.currentTab, isSearching = uiState.searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = uiState.filteredDocuments,
                        key = { it.id }
                    ) { doc ->
                        DocumentCard(
                            document = doc,
                            onClick = { onOpenDocument(doc) },
                            onOptionsClick = { selectedOptionsDoc = doc }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Sort Dialog (safe modal, no window destructor errors)
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text("Sort Documents", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SortOption.values().forEach { option ->
                        val isSelected = uiState.sortOption == option
                        Surface(
                            onClick = {
                                viewModel.setSortOption(option)
                                showSortDialog = false
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
                                    text = option.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSortDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Central Document Options Bottom Sheet (safe window lifecycle)
    selectedOptionsDoc?.let { doc ->
        ModalBottomSheet(
            onDismissRequest = { selectedOptionsDoc = null },
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
                    text = doc.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (doc.isTrash) {
                    ActionSheetItem(Icons.Default.Restore, "Restore Document") {
                        selectedOptionsDoc = null
                        viewModel.restoreFromTrash(doc.id)
                    }
                    ActionSheetItem(Icons.Default.DeleteForever, "Delete Permanently", isDestructive = true) {
                        selectedOptionsDoc = null
                        viewModel.permanentlyDelete(doc.id)
                    }
                } else {
                    ActionSheetItem(
                        icon = Icons.Default.PushPin,
                        label = if (doc.isPinned) "Unpin from top" else "Pin to top"
                    ) {
                        selectedOptionsDoc = null
                        viewModel.togglePinned(doc.id)
                    }
                    ActionSheetItem(
                        icon = Icons.Default.Star,
                        label = if (doc.isFavorite) "Remove from favorites" else "Add to favorites"
                    ) {
                        selectedOptionsDoc = null
                        viewModel.toggleFavorite(doc.id)
                    }
                    ActionSheetItem(Icons.Default.Edit, "Rename") {
                        val currentDoc = doc
                        selectedOptionsDoc = null
                        renameDialogDoc = currentDoc
                        renameText = currentDoc.title
                    }
                    ActionSheetItem(Icons.Default.ContentCopy, "Duplicate") {
                        selectedOptionsDoc = null
                        viewModel.duplicateDocument(doc.id)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    ActionSheetItem(Icons.Default.DeleteOutline, "Move to Trash", isDestructive = true) {
                        selectedOptionsDoc = null
                        viewModel.moveToTrash(doc.id)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Rename Dialog
    renameDialogDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { renameDialogDoc = null },
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
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameDocument(doc.id, renameText)
                        }
                        renameDialogDoc = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogDoc = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ActionSheetItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TemplatesRow(onSelectTemplate: (String) -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text = "START FROM TEMPLATE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TemplateChip(Icons.AutoMirrored.Filled.Article, "Blank Document", "Start fresh") {
                    onSelectTemplate("Untitled Document")
                }
            }
            item {
                TemplateChip(Icons.Default.Mail, "Business Letter", "Formal correspondence") {
                    onSelectTemplate("Business Letter")
                }
            }
            item {
                TemplateChip(Icons.AutoMirrored.Filled.Assignment, "Project Proposal", "Scope & deliverables") {
                    onSelectTemplate("Project Proposal")
                }
            }
            item {
                TemplateChip(Icons.AutoMirrored.Filled.HelpOutline, "Welcome Guide", "Key features & tips") {
                    onSelectTemplate("Welcome")
                }
            }
        }
    }
}

@Composable
private fun TemplateChip(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun DocumentCard(
    document: TypableDocument,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val stats = remember(document) { document.computeStats() }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(document.modifiedAt) { dateFormat.format(Date(document.modifiedAt)) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("document_card_${document.id}"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (document.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (document.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${stats.wordCount} words",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${stats.pageCount} p.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Options Action
            IconButton(onClick = onOptionsClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Document Options")
            }
        }
    }
}

@Composable
fun EmptyStateView(tab: LibraryTab, isSearching: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = if (isSearching) "No matching documents"
                else when (tab) {
                    LibraryTab.ALL -> "No documents yet"
                    LibraryTab.PINNED -> "No pinned documents"
                    LibraryTab.FAVORITES -> "No favorite documents"
                    LibraryTab.TRASH -> "Trash is empty"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isSearching) "Try a different search term"
                else when (tab) {
                    LibraryTab.ALL -> "Tap 'New Document' or choose a template to begin writing."
                    LibraryTab.PINNED -> "Pin important files for instant access."
                    LibraryTab.FAVORITES -> "Star your best drafts to find them here."
                    LibraryTab.TRASH -> "Deleted documents will appear here."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
