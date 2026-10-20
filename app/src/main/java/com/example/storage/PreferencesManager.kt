package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DocumentFont
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    SEPIA,
    MIDNIGHT
}

data class AppPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val paperColorHex: String = "#FFFFFF",
    val defaultFontFamily: DocumentFont = DocumentFont.SYSTEM_SANS,
    val defaultFontSizeSp: Float = 15f,
    val autosaveDelaySeconds: Int = 2,
    val showWordCountBar: Boolean = true,
    val showPageIndicator: Boolean = true,
    val isFocusModeActive: Boolean = false,
    val compactToolbar: Boolean = false
)

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("typable_preferences", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<AppPreferences> = _preferencesFlow.asStateFlow()

    private fun loadPreferences(): AppPreferences {
        val themeStr = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val themeMode = try { AppThemeMode.valueOf(themeStr) } catch (e: Exception) { AppThemeMode.SYSTEM }

        val fontStr = prefs.getString("default_font", DocumentFont.SYSTEM_SANS.name) ?: DocumentFont.SYSTEM_SANS.name
        val font = try { DocumentFont.valueOf(fontStr) } catch (e: Exception) { DocumentFont.SYSTEM_SANS }

        return AppPreferences(
            themeMode = themeMode,
            paperColorHex = prefs.getString("paper_color", "#FFFFFF") ?: "#FFFFFF",
            defaultFontFamily = font,
            defaultFontSizeSp = prefs.getFloat("default_font_size", 15f),
            autosaveDelaySeconds = prefs.getInt("autosave_delay", 2),
            showWordCountBar = prefs.getBoolean("show_word_count", true),
            showPageIndicator = prefs.getBoolean("show_page_indicator", true),
            compactToolbar = prefs.getBoolean("compact_toolbar", false)
        )
    }

    fun setThemeMode(theme: AppThemeMode) {
        prefs.edit().putString("theme_mode", theme.name).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(themeMode = theme)
    }

    fun setPaperColor(hex: String) {
        prefs.edit().putString("paper_color", hex).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(paperColorHex = hex)
    }

    fun setDefaultFont(font: DocumentFont) {
        prefs.edit().putString("default_font", font.name).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(defaultFontFamily = font)
    }

    fun setDefaultFontSize(sizeSp: Float) {
        prefs.edit().putFloat("default_font_size", sizeSp).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(defaultFontSizeSp = sizeSp)
    }

    fun setAutosaveDelay(seconds: Int) {
        prefs.edit().putInt("autosave_delay", seconds).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(autosaveDelaySeconds = seconds)
    }

    fun setShowWordCount(show: Boolean) {
        prefs.edit().putBoolean("show_word_count", show).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(showWordCountBar = show)
    }

    fun setShowPageIndicator(show: Boolean) {
        prefs.edit().putBoolean("show_page_indicator", show).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(showPageIndicator = show)
    }

    fun setCompactToolbar(compact: Boolean) {
        prefs.edit().putBoolean("compact_toolbar", compact).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(compactToolbar = compact)
    }

    fun resetPreferences() {
        prefs.edit().clear().apply()
        _preferencesFlow.value = AppPreferences()
    }
}
