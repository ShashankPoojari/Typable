package com.example.storage

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.DocumentFont
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object CustomFontManager {

    private const val FONTS_DIR = "custom_fonts"

    fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, FONTS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listCustomFonts(context: Context): List<String> {
        val dir = getFontsDir(context)
        val files = dir.listFiles { _, name ->
            val lower = name.lowercase(Locale.ROOT)
            lower.endsWith(".ttf") || lower.endsWith(".otf")
        } ?: return emptyList()

        return files.map { file ->
            file.nameWithoutExtension
        }.sorted()
    }

    fun importFontFromUri(context: Context, uri: Uri): Result<String> {
        return runCatching {
            var originalName = "ImportedFont"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    originalName = cursor.getString(nameIndex) ?: originalName
                }
            }

            // Sanitize file name
            val cleanBaseName = originalName
                .substringBeforeLast(".")
                .replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
                .trim()
                .ifEmpty { "CustomFont_${System.currentTimeMillis()}" }

            val extension = if (originalName.lowercase(Locale.ROOT).endsWith(".otf")) "otf" else "ttf"
            val targetFile = File(getFontsDir(context), "$cleanBaseName.$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Could not read font stream")

            // Validate that Android can parse this typeface
            try {
                val typeface = Typeface.createFromFile(targetFile)
                if (typeface == null) {
                    targetFile.delete()
                    throw IllegalArgumentException("Invalid font file")
                }
            } catch (e: Exception) {
                targetFile.delete()
                throw e
            }

            cleanBaseName
        }
    }

    fun deleteCustomFont(context: Context, fontName: String): Boolean {
        val dir = getFontsDir(context)
        val ttf = File(dir, "$fontName.ttf")
        val otf = File(dir, "$fontName.otf")
        var deleted = false
        if (ttf.exists()) deleted = ttf.delete() || deleted
        if (otf.exists()) deleted = otf.delete() || deleted
        return deleted
    }

    fun getComposeFontFamily(
        context: Context,
        font: DocumentFont,
        customFontName: String? = null
    ): FontFamily {
        return when (font) {
            DocumentFont.SYSTEM_SANS -> FontFamily.Default
            DocumentFont.SERIF -> FontFamily.Serif
            DocumentFont.MONOSPACE -> FontFamily.Monospace
            DocumentFont.PLAYFAIR_DISPLAY -> FontFamily(Font(R.font.playfair_display))
            DocumentFont.MERRIWEATHER -> FontFamily(Font(R.font.merriweather))
            DocumentFont.MONTSERRAT -> FontFamily(Font(R.font.montserrat))
            DocumentFont.LORA -> FontFamily(Font(R.font.lora))
            DocumentFont.ROBOTO_MONO -> FontFamily(Font(R.font.roboto_mono))
            DocumentFont.CAVEAT -> FontFamily(Font(R.font.caveat))
            DocumentFont.CUSTOM -> {
                if (!customFontName.isNullOrBlank()) {
                    val dir = getFontsDir(context)
                    val fontFile = File(dir, "$customFontName.ttf").takeIf { it.exists() }
                        ?: File(dir, "$customFontName.otf").takeIf { it.exists() }

                    if (fontFile != null) {
                        try {
                            val typeface = Typeface.createFromFile(fontFile)
                            FontFamily(typeface)
                        } catch (_: Exception) {
                            FontFamily.Default
                        }
                    } else FontFamily.Default
                } else FontFamily.Default
            }
        }
    }

    fun getAndroidTypeface(
        context: Context,
        font: DocumentFont,
        customFontName: String? = null,
        style: Int = Typeface.NORMAL
    ): Typeface {
        return when (font) {
            DocumentFont.SYSTEM_SANS -> Typeface.create(Typeface.DEFAULT, style)
            DocumentFont.SERIF -> Typeface.create(Typeface.SERIF, style)
            DocumentFont.MONOSPACE -> Typeface.create(Typeface.MONOSPACE, style)
            DocumentFont.PLAYFAIR_DISPLAY -> {
                try {
                    ResourcesCompat.getFont(context, R.font.playfair_display) ?: Typeface.create(Typeface.SERIF, style)
                } catch (_: Exception) {
                    Typeface.create(Typeface.SERIF, style)
                }
            }
            DocumentFont.MERRIWEATHER -> {
                try {
                    ResourcesCompat.getFont(context, R.font.merriweather) ?: Typeface.create(Typeface.SERIF, style)
                } catch (_: Exception) {
                    Typeface.create(Typeface.SERIF, style)
                }
            }
            DocumentFont.MONTSERRAT -> {
                try {
                    ResourcesCompat.getFont(context, R.font.montserrat) ?: Typeface.create(Typeface.DEFAULT, style)
                } catch (_: Exception) {
                    Typeface.create(Typeface.DEFAULT, style)
                }
            }
            DocumentFont.LORA -> {
                try {
                    ResourcesCompat.getFont(context, R.font.lora) ?: Typeface.create(Typeface.SERIF, style)
                } catch (_: Exception) {
                    Typeface.create(Typeface.SERIF, style)
                }
            }
            DocumentFont.ROBOTO_MONO -> {
                try {
                    ResourcesCompat.getFont(context, R.font.roboto_mono) ?: Typeface.create(Typeface.MONOSPACE, style)
                } catch (_: Exception) {
                    Typeface.create(Typeface.MONOSPACE, style)
                }
            }
            DocumentFont.CAVEAT -> {
                try {
                    ResourcesCompat.getFont(context, R.font.caveat) ?: Typeface.create(Typeface.DEFAULT, style)
                } catch (_: Exception) {
                    Typeface.create(Typeface.DEFAULT, style)
                }
            }
            DocumentFont.CUSTOM -> {
                if (!customFontName.isNullOrBlank()) {
                    val dir = getFontsDir(context)
                    val fontFile = File(dir, "$customFontName.ttf").takeIf { it.exists() }
                        ?: File(dir, "$customFontName.otf").takeIf { it.exists() }

                    if (fontFile != null) {
                        try {
                            Typeface.createFromFile(fontFile)
                        } catch (_: Exception) {
                            Typeface.create(Typeface.DEFAULT, style)
                        }
                    } else Typeface.create(Typeface.DEFAULT, style)
                } else Typeface.create(Typeface.DEFAULT, style)
            }
        }
    }
}
