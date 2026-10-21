package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import com.example.storage.CustomFontManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DocumentFont
import com.example.storage.AppThemeMode
import com.example.storage.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    val prefs by preferencesManager.preferencesFlow.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var customFontsList by remember { mutableStateOf(CustomFontManager.listCustomFonts(context)) }

    val fontImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val res = CustomFontManager.importFontFromUri(context, uri)
            res.onSuccess { fontName ->
                customFontsList = CustomFontManager.listCustomFonts(context)
                scope.launch {
                    snackbarHostState.showSnackbar("Font '$fontName' imported successfully!")
                }
            }.onFailure { e ->
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to import font: ${e.message ?: "Invalid TTF/OTF font file"}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Mode
            SettingsSection(title = "APPEARANCE & THEME") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionCard(
                            title = "System",
                            isSelected = prefs.themeMode == AppThemeMode.SYSTEM,
                            onClick = { preferencesManager.setThemeMode(AppThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            title = "Light",
                            isSelected = prefs.themeMode == AppThemeMode.LIGHT,
                            onClick = { preferencesManager.setThemeMode(AppThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            title = "Dark",
                            isSelected = prefs.themeMode == AppThemeMode.DARK,
                            onClick = { preferencesManager.setThemeMode(AppThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionCard(
                            title = "Sepia Paper",
                            isSelected = prefs.themeMode == AppThemeMode.SEPIA,
                            onClick = { preferencesManager.setThemeMode(AppThemeMode.SEPIA) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            title = "Midnight OLED",
                            isSelected = prefs.themeMode == AppThemeMode.MIDNIGHT,
                            onClick = { preferencesManager.setThemeMode(AppThemeMode.MIDNIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Paper Color Tint
            SettingsSection(title = "DEFAULT CANVAS PAPER TINT") {
                val papers = listOf(
                    "#FFFFFF" to "Pure White",
                    "#FDFBF7" to "Warm Ivory",
                    "#F6F1E9" to "Soft Sepia",
                    "#F1F5F9" to "Slate Tint",
                    "#1E293B" to "Midnight Paper"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    papers.forEach { (hex, name) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = prefs.paperColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { preferencesManager.setPaperColor(hex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = name,
                                    tint = if (hex == "#1E293B") Color.White else Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Typography Defaults & Font Management
            SettingsSection(title = "TYPOGRAPHY & OPEN SOURCE FONTS") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Pre-bundled Google Fonts & System Typefaces", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DocumentFont.values().filter { it != DocumentFont.CUSTOM }.forEach { font ->
                            val previewFamily = CustomFontManager.getComposeFontFamily(context, font)
                            FilterChip(
                                selected = prefs.defaultFontFamily == font,
                                onClick = { preferencesManager.setDefaultFont(font) },
                                label = {
                                    Text(
                                        text = font.displayName,
                                        fontFamily = previewFamily
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Default Font Size", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(13f, 14f, 15f, 16f, 18f).forEach { size ->
                            FilterChip(
                                selected = prefs.defaultFontSizeSp == size,
                                onClick = { preferencesManager.setDefaultFontSize(size) },
                                label = { Text("${size.toInt()} sp") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Custom & Imported Fonts Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Imported Open Source Fonts", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("${customFontsList.size} custom font(s) installed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalButton(
                            onClick = {
                                fontImportLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype", "application/octet-stream", "*/*"))
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import .ttf / .otf", fontSize = 13.sp)
                        }
                    }

                    if (customFontsList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customFontsList.forEach { customFontName ->
                                val customPreviewFamily = CustomFontManager.getComposeFontFamily(context, DocumentFont.CUSTOM, customFontName)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = customFontName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = customPreviewFamily
                                            )
                                            Text(
                                                text = "The quick brown fox jumps over the lazy dog",
                                                fontSize = 12.sp,
                                                fontFamily = customPreviewFamily,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                CustomFontManager.deleteCustomFont(context, customFontName)
                                                customFontsList = CustomFontManager.listCustomFonts(context)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Deleted font: $customFontName")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Font",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Open Source / Google Fonts Guide Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "How to Import Open Source Fonts",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "1. Visit fonts.google.com or font repositories (like Font Squirrel or GitHub SIL-licensed fonts) in your browser.\n2. Download any font family (.ttf or .otf file).\n3. Tap 'Import .ttf / .otf' above or in the editor's font menu to import and immediately use it in your documents.",
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Editor Behavior
            SettingsSection(title = "EDITOR BEHAVIOR & INTERFACE") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Show Word Count Bar", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Real-time live word and character counts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.showWordCountBar,
                            onCheckedChange = { preferencesManager.setShowWordCount(it) }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Show Page Number Indicator", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Live estimated pages on document canvas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = prefs.showPageIndicator,
                            onCheckedChange = { preferencesManager.setShowPageIndicator(it) }
                        )
                    }
                }
            }

            // Privacy & Offline Architecture
            SettingsSection(title = "SECURITY & DATA SOVEREIGNTY") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("100% Offline & Private", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "• Zero internet access: android.permission.INTERNET is omitted.\n• No cloud telemetry, no analytics, no advertising SDKs.\n• Strictly deterministic: No generative AI models, no text scrapers.\n• All documents remain stored exclusively on your device.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // About Typable
            SettingsSection(title = "ABOUT TYPABLE") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Typable for Android", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Version 1.0.0 (Release Build)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Application ID: shashank.word.typable", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "A professional mobile word processor engineered with Kotlin, Jetpack Compose, Material Design 3, native OpenXML (.docx) formatting, vector PDF rendering, and Markdown support.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Reset Preferences
            OutlinedButton(
                onClick = { preferencesManager.resetPreferences() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Preferences")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
fun ThemeOptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
