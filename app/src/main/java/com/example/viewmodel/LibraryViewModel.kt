package com.example.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.formats.DocxFormat
import com.example.formats.HtmlFormat
import com.example.formats.MarkdownFormat
import com.example.formats.PlainTextFormat
import com.example.model.TypableDocument
import com.example.storage.DocumentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryTab {
    ALL,
    PINNED,
    FAVORITES,
    TRASH
}

enum class SortOption(val displayName: String) {
    MODIFIED_DESC("Recently Modified"),
    MODIFIED_ASC("Oldest Modified"),
    TITLE_ASC("Title (A–Z)"),
    TITLE_DESC("Title (Z–A)"),
    WORDS_DESC("Word Count")
}

data class LibraryUiState(
    val documents: List<TypableDocument> = emptyList(),
    val filteredDocuments: List<TypableDocument> = emptyList(),
    val currentTab: LibraryTab = LibraryTab.ALL,
    val sortOption: SortOption = SortOption.MODIFIED_DESC,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedDocumentId: String? = null,
    val statusMessage: String? = null
)

class LibraryViewModel(private val storage: DocumentStorage) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val allDocs = storage.getAllDocuments()
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    documents = allDocs,
                    isLoading = false
                )
                applyFilterAndSort()
            }
        }
    }

    fun setTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
        applyFilterAndSort()
    }

    fun setSortOption(sort: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = sort)
        applyFilterAndSort()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val all = _uiState.value.documents
        val tab = _uiState.value.currentTab
        val query = _uiState.value.searchQuery.trim().lowercase()

        val tabFiltered = all.filter { doc ->
            when (tab) {
                LibraryTab.ALL -> !doc.isTrash
                LibraryTab.PINNED -> !doc.isTrash && doc.isPinned
                LibraryTab.FAVORITES -> !doc.isTrash && doc.isFavorite
                LibraryTab.TRASH -> doc.isTrash
            }
        }

        val searchFiltered = if (query.isEmpty()) {
            tabFiltered
        } else {
            tabFiltered.filter { doc ->
                doc.title.lowercase().contains(query) ||
                        doc.extractPlainText().lowercase().contains(query)
            }
        }

        val sorted = when (_uiState.value.sortOption) {
            SortOption.MODIFIED_DESC -> searchFiltered.sortedByDescending { it.modifiedAt }
            SortOption.MODIFIED_ASC -> searchFiltered.sortedBy { it.modifiedAt }
            SortOption.TITLE_ASC -> searchFiltered.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> searchFiltered.sortedByDescending { it.title.lowercase() }
            SortOption.WORDS_DESC -> searchFiltered.sortedByDescending { it.computeStats().wordCount }
        }

        _uiState.value = _uiState.value.copy(filteredDocuments = sorted)
    }

    fun createBlankDocument(onCreated: (TypableDocument) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = storage.createBlankDocument("Untitled Document")
            withContext(Dispatchers.Main) {
                loadDocuments()
                onCreated(doc)
            }
        }
    }

    fun createFromTemplate(templateName: String, onCreated: (TypableDocument) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = when (templateName) {
                "Welcome" -> storage.createTemplateWelcomeGuide()
                "Business Letter" -> storage.createTemplateBusinessLetter()
                "Project Proposal" -> storage.createProjectProposalTemplate()
                else -> storage.createBlankDocument(templateName)
            }
            withContext(Dispatchers.Main) {
                loadDocuments()
                onCreated(doc)
            }
        }
    }

    fun duplicateDocument(docId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storage.duplicateDocument(docId)
            loadDocuments()
            showStatus("Document duplicated")
        }
    }

    fun renameDocument(docId: String, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storage.renameDocument(docId, newTitle)
            loadDocuments()
            showStatus("Document renamed")
        }
    }

    fun togglePinned(docId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storage.togglePinned(docId)
            loadDocuments()
        }
    }

    fun toggleFavorite(docId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storage.toggleFavorite(docId)
            loadDocuments()
        }
    }

    fun moveToTrash(docId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storage.moveToTrash(docId)
            loadDocuments()
            showStatus("Moved to Trash")
        }
    }

    fun restoreFromTrash(docId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storage.restoreFromTrash(docId)
            loadDocuments()
            showStatus("Document restored")
        }
    }

    fun permanentlyDelete(docId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storage.permanentlyDeleteDocument(docId)
            loadDocuments()
            showStatus("Permanently deleted")
        }
    }

    fun importFileFromUri(context: Context, uri: Uri, onImported: (TypableDocument) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: ""
                val filename = getFileName(context, uri).lowercase()

                val importedDoc: TypableDocument = contentResolver.openInputStream(uri)?.use { stream ->
                    when {
                        filename.endsWith(".docx") || mimeType.contains("wordprocessingml") -> {
                            DocxFormat.importFromDocx(stream)
                        }
                        filename.endsWith(".md") || mimeType.contains("markdown") -> {
                            MarkdownFormat.importFromMarkdown(stream)
                        }
                        filename.endsWith(".html") || filename.endsWith(".htm") || mimeType.contains("html") -> {
                            HtmlFormat.importFromHtml(stream)
                        }
                        else -> {
                            PlainTextFormat.importFromText(stream)
                        }
                    }
                } ?: return@launch

                val finalDoc = if (importedDoc.title.isBlank() || importedDoc.title == "Untitled Document") {
                    importedDoc.copy(title = filename.substringBeforeLast(".").ifBlank { "Imported Document" })
                } else importedDoc

                storage.saveDocument(finalDoc)
                withContext(Dispatchers.Main) {
                    loadDocuments()
                    showStatus("Imported '${finalDoc.title}'")
                    onImported(finalDoc)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showStatus("Import error: ${e.message}")
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "document.txt"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun showStatus(message: String) {
        _uiState.value = _uiState.value.copy(statusMessage = message)
    }
}
