package com.example.formats

import com.example.model.TypableDocument
import com.example.storage.DocumentStorage
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * Lossless Typable JSON Document Archive format (.json / .typable).
 * Preserves 100% of formatting, custom fonts, exact margins, table structures, and metadata.
 */
object JsonBackupFormat {

    fun exportToJson(document: TypableDocument, outputStream: OutputStream, storage: DocumentStorage) {
        val json = storage.serializeDocument(document)
        outputStream.write(json.toByteArray(StandardCharsets.UTF_8))
    }

    fun importFromJson(inputStream: InputStream, storage: DocumentStorage): TypableDocument {
        val json = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        return storage.deserializeDocument(json)
    }
}
