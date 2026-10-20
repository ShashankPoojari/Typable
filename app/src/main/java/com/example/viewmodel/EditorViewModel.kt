package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.DocumentEngine
import com.example.formats.*
import com.example.model.*
import com.example.storage.DocumentStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class SaveState {
    SAVED,
    SAVING,
    DIRTY,
    ERROR
}

data class EditorUiState(
    val document: TypableDocument,
    val activeBlockIndex: Int = 0,
    val saveState: SaveState = SaveState.SAVED,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isFocusMode: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val searchMatches: List<DocumentEngine.SearchMatch> = emptyList(),
    val currentMatchIndex: Int = 0,
    val statusMessage: String? = null
)

class EditorViewModel(
    private val storage: DocumentStorage,
    initialDocument: TypableDocument
) : ViewModel() {

    private val engine = DocumentEngine(initialDocument)

    private val _uiState = MutableStateFlow(
        EditorUiState(
            document = initialDocument,
            canUndo = engine.canUndo,
            canRedo = engine.canRedo
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autosaveJob: Job? = null

    val activeBlock: DocumentBlock?
        get() {
            val idx = _uiState.value.activeBlockIndex
            return if (idx in _uiState.value.document.blocks.indices) {
                _uiState.value.document.blocks[idx]
            } else null
        }

    fun selectBlock(index: Int) {
        if (index in _uiState.value.document.blocks.indices) {
            _uiState.value = _uiState.value.copy(activeBlockIndex = index)
        }
    }

    fun updateTitle(title: String) {
        engine.updateTitle(title)
        syncStateAfterEdit()
    }

    fun updatePageSettings(settings: PageSettings) {
        engine.updatePageSettings(settings)
        syncStateAfterEdit()
    }

    fun updateParagraphText(blockIndex: Int, text: String) {
        engine.updateParagraphText(blockIndex, text)
        syncStateAfterEdit()
    }

    fun splitParagraph(blockIndex: Int, cursorOffset: Int) {
        val newIndex = engine.splitParagraph(blockIndex, cursorOffset)
        syncStateAfterEdit()
        _uiState.value = _uiState.value.copy(activeBlockIndex = newIndex)
    }

    fun mergeWithPrevious(blockIndex: Int) {
        val newIndex = engine.mergeWithPrevious(blockIndex)
        syncStateAfterEdit()
        _uiState.value = _uiState.value.copy(activeBlockIndex = newIndex)
    }

    fun toggleBold() {
        engine.toggleBold(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun toggleItalic() {
        engine.toggleItalic(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun toggleUnderline() {
        engine.toggleUnderline(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun toggleStrikethrough() {
        engine.toggleStrikethrough(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun toggleSuperscript() {
        engine.toggleSuperscript(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun toggleSubscript() {
        engine.toggleSubscript(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun applyColor(hex: String?) {
        engine.applyColor(_uiState.value.activeBlockIndex, hex)
        syncStateAfterEdit()
    }

    fun applyHighlight(hex: String?) {
        engine.applyHighlight(_uiState.value.activeBlockIndex, hex)
        syncStateAfterEdit()
    }

    fun applyFontFamily(font: DocumentFont) {
        engine.applyFontFamily(_uiState.value.activeBlockIndex, font)
        syncStateAfterEdit()
    }

    fun applyFontSizeDelta(deltaSp: Float) {
        engine.applyFontSize(_uiState.value.activeBlockIndex, deltaSp)
        syncStateAfterEdit()
    }

    fun clearFormatting() {
        engine.clearFormatting(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun applyStyle(style: BlockStyle) {
        engine.applyStyle(_uiState.value.activeBlockIndex, style)
        syncStateAfterEdit()
    }

    fun applyAlignment(alignment: TextAlignment) {
        engine.applyAlignment(_uiState.value.activeBlockIndex, alignment)
        syncStateAfterEdit()
    }

    fun toggleBulletList() {
        engine.toggleBulletList(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun toggleNumberedList() {
        engine.toggleNumberedList(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun indent() {
        engine.indentBlock(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun outdent() {
        engine.outdentBlock(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
    }

    fun insertTable(rows: Int, cols: Int) {
        val newIndex = engine.insertTable(_uiState.value.activeBlockIndex, rows, cols)
        syncStateAfterEdit()
        _uiState.value = _uiState.value.copy(activeBlockIndex = newIndex)
    }

    fun updateTableCell(blockIndex: Int, rowIndex: Int, colIndex: Int, text: String) {
        engine.updateTableCell(blockIndex, rowIndex, colIndex, text)
        syncStateAfterEdit()
    }

    fun addTableRow(blockIndex: Int, afterRowIndex: Int) {
        engine.addTableRow(blockIndex, afterRowIndex)
        syncStateAfterEdit()
    }

    fun deleteTableRow(blockIndex: Int, rowIndex: Int) {
        engine.deleteTableRow(blockIndex, rowIndex)
        syncStateAfterEdit()
    }

    fun addTableColumn(blockIndex: Int) {
        engine.addTableColumn(blockIndex)
        syncStateAfterEdit()
    }

    fun deleteTableColumn(blockIndex: Int, colIndex: Int) {
        engine.deleteTableColumn(blockIndex, colIndex)
        syncStateAfterEdit()
    }

    fun insertDivider() {
        val newIndex = engine.insertDivider(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
        _uiState.value = _uiState.value.copy(activeBlockIndex = newIndex)
    }

    fun insertPageBreak() {
        val newIndex = engine.insertPageBreak(_uiState.value.activeBlockIndex)
        syncStateAfterEdit()
        _uiState.value = _uiState.value.copy(activeBlockIndex = newIndex)
    }

    fun deleteBlock(blockIndex: Int) {
        engine.deleteBlock(blockIndex)
        val safeIndex = (blockIndex - 1).coerceAtLeast(0)
        syncStateAfterEdit()
        _uiState.value = _uiState.value.copy(activeBlockIndex = safeIndex)
    }

    fun insertSymbol(symbol: String) {
        val block = activeBlock as? DocumentBlock.Paragraph ?: return
        val currentText = block.plainText
        updateParagraphText(_uiState.value.activeBlockIndex, currentText + symbol)
    }

    fun undo() {
        if (engine.undo()) {
            _uiState.value = _uiState.value.copy(
                document = engine.currentDocument,
                canUndo = engine.canUndo,
                canRedo = engine.canRedo,
                saveState = SaveState.DIRTY
            )
            scheduleAutosave()
        }
    }

    fun redo() {
        if (engine.redo()) {
            _uiState.value = _uiState.value.copy(
                document = engine.currentDocument,
                canUndo = engine.canUndo,
                canRedo = engine.canRedo,
                saveState = SaveState.DIRTY
            )
            scheduleAutosave()
        }
    }

    fun toggleFocusMode() {
        _uiState.value = _uiState.value.copy(isFocusMode = !_uiState.value.isFocusMode)
    }

    // --- Search & Replace ---

    fun openSearch() {
        _uiState.value = _uiState.value.copy(isSearching = true)
    }

    fun closeSearch() {
        _uiState.value = _uiState.value.copy(
            isSearching = false,
            searchQuery = "",
            replaceQuery = "",
            searchMatches = emptyList(),
            currentMatchIndex = 0
        )
    }

    fun setSearchQuery(query: String) {
        val matches = engine.findMatches(query, _uiState.value.matchCase, _uiState.value.wholeWord)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            searchMatches = matches,
            currentMatchIndex = 0
        )
        if (matches.isNotEmpty()) {
            selectBlock(matches[0].blockIndex)
        }
    }

    fun setReplaceQuery(replace: String) {
        _uiState.value = _uiState.value.copy(replaceQuery = replace)
    }

    fun toggleMatchCase() {
        val newMatchCase = !_uiState.value.matchCase
        val matches = engine.findMatches(_uiState.value.searchQuery, newMatchCase, _uiState.value.wholeWord)
        _uiState.value = _uiState.value.copy(
            matchCase = newMatchCase,
            searchMatches = matches,
            currentMatchIndex = 0
        )
    }

    fun toggleWholeWord() {
        val newWholeWord = !_uiState.value.wholeWord
        val matches = engine.findMatches(_uiState.value.searchQuery, _uiState.value.matchCase, newWholeWord)
        _uiState.value = _uiState.value.copy(
            wholeWord = newWholeWord,
            searchMatches = matches,
            currentMatchIndex = 0
        )
    }

    fun nextMatch() {
        val matches = _uiState.value.searchMatches
        if (matches.isNotEmpty()) {
            val nextIdx = (_uiState.value.currentMatchIndex + 1) % matches.size
            _uiState.value = _uiState.value.copy(currentMatchIndex = nextIdx)
            selectBlock(matches[nextIdx].blockIndex)
        }
    }

    fun prevMatch() {
        val matches = _uiState.value.searchMatches
        if (matches.isNotEmpty()) {
            val prevIdx = if (_uiState.value.currentMatchIndex <= 0) matches.size - 1 else _uiState.value.currentMatchIndex - 1
            _uiState.value = _uiState.value.copy(currentMatchIndex = prevIdx)
            selectBlock(matches[prevIdx].blockIndex)
        }
    }

    fun replaceCurrentMatch() {
        val matches = _uiState.value.searchMatches
        val idx = _uiState.value.currentMatchIndex
        if (idx in matches.indices) {
            val match = matches[idx]
            engine.replaceMatch(match, _uiState.value.replaceQuery)
            syncStateAfterEdit()
            setSearchQuery(_uiState.value.searchQuery)
        }
    }

    fun replaceAllMatches() {
        val count = engine.replaceAllMatches(
            _uiState.value.searchQuery,
            _uiState.value.replaceQuery,
            _uiState.value.matchCase,
            _uiState.value.wholeWord
        )
        syncStateAfterEdit()
        setSearchQuery(_uiState.value.searchQuery)
        showStatus("Replaced $count occurrences")
    }

    // --- Autosave & Persistence ---

    fun explicitSave(): Boolean {
        _uiState.value = _uiState.value.copy(saveState = SaveState.SAVING)
        val success = storage.saveDocument(engine.currentDocument)
        _uiState.value = _uiState.value.copy(
            saveState = if (success) SaveState.SAVED else SaveState.ERROR
        )
        return success
    }

    private fun syncStateAfterEdit() {
        _uiState.value = _uiState.value.copy(
            document = engine.currentDocument,
            canUndo = engine.canUndo,
            canRedo = engine.canRedo,
            saveState = SaveState.DIRTY
        )
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(1500)
            _uiState.value = _uiState.value.copy(saveState = SaveState.SAVING)
            val success = storage.saveDocument(engine.currentDocument)
            _uiState.value = _uiState.value.copy(
                saveState = if (success) SaveState.SAVED else SaveState.ERROR
            )
        }
    }

    fun showStatus(message: String) {
        _uiState.value = _uiState.value.copy(statusMessage = message)
        viewModelScope.launch {
            delay(3000)
            if (_uiState.value.statusMessage == message) {
                _uiState.value = _uiState.value.copy(statusMessage = null)
            }
        }
    }

    // --- File Exports ---

    fun exportToDocx(context: Context): Uri? {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "${sanitizeFilename(engine.currentDocument.title)}.docx")
        return try {
            FileOutputStream(file).use { fos ->
                DocxFormat.exportToDocx(engine.currentDocument, fos)
            }
            showStatus("DOCX exported successfully")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            showStatus("DOCX export error: ${e.message}")
            null
        }
    }

    fun exportToPdf(context: Context): Uri? {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "${sanitizeFilename(engine.currentDocument.title)}.pdf")
        return try {
            FileOutputStream(file).use { fos ->
                PdfExporter.exportToPdf(engine.currentDocument, fos)
            }
            showStatus("PDF exported successfully")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            showStatus("PDF export error: ${e.message}")
            null
        }
    }

    fun exportToMarkdown(context: Context): Uri? {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "${sanitizeFilename(engine.currentDocument.title)}.md")
        return try {
            val mdText = MarkdownFormat.exportToMarkdown(engine.currentDocument)
            file.writeText(mdText)
            showStatus("Markdown exported successfully")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            showStatus("Markdown export error: ${e.message}")
            null
        }
    }

    fun exportToHtml(context: Context): Uri? {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "${sanitizeFilename(engine.currentDocument.title)}.html")
        return try {
            val htmlText = HtmlFormat.exportToHtml(engine.currentDocument)
            file.writeText(htmlText)
            showStatus("HTML exported successfully")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            showStatus("HTML export error: ${e.message}")
            null
        }
    }

    fun exportToTxt(context: Context): Uri? {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "${sanitizeFilename(engine.currentDocument.title)}.txt")
        return try {
            val txt = PlainTextFormat.exportToText(engine.currentDocument)
            file.writeText(txt)
            showStatus("Text exported successfully")
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            showStatus("Text export error: ${e.message}")
            null
        }
    }

    fun print(context: Context) {
        PdfExporter.printDocument(context, engine.currentDocument)
    }

    fun shareExportedFile(context: Context, uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, engine.currentDocument.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Document"))
    }

    private fun sanitizeFilename(title: String): String {
        return title.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").trim().ifEmpty { "document" }
    }
}
