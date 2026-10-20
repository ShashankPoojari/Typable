package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.TypableDocument
import com.example.storage.DocumentStorage
import com.example.storage.PreferencesManager
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.TypableTheme
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.LibraryViewModel

sealed class AppScreen {
    object Library : AppScreen()
    data class Editor(val document: TypableDocument) : AppScreen()
    object Settings : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val documentStorage by lazy { DocumentStorage(applicationContext) }
    private val preferencesManager by lazy { PreferencesManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by preferencesManager.preferencesFlow.collectAsState()

            TypableTheme(themeMode = preferences.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Library) }
                    val libraryViewModel = remember { LibraryViewModel(documentStorage) }

                    when (val screen = currentScreen) {
                        is AppScreen.Library -> {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                onOpenDocument = { doc ->
                                    currentScreen = AppScreen.Editor(doc)
                                },
                                onOpenSettings = {
                                    currentScreen = AppScreen.Settings
                                }
                            )
                        }

                        is AppScreen.Editor -> {
                            // Create or remember EditorViewModel scoped to this document
                            val editorViewModel = remember(screen.document.id) {
                                EditorViewModel(documentStorage, screen.document)
                            }

                            EditorScreen(
                                viewModel = editorViewModel,
                                onNavigateBack = {
                                    libraryViewModel.loadDocuments()
                                    currentScreen = AppScreen.Library
                                }
                            )
                        }

                        is AppScreen.Settings -> {
                            BackHandler {
                                currentScreen = AppScreen.Library
                            }

                            SettingsScreen(
                                preferencesManager = preferencesManager,
                                onNavigateBack = {
                                    currentScreen = AppScreen.Library
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
