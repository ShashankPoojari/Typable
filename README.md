# Typable — Professional Android Word Processor

[![Android CI](https://github.com/shashankpoojari/typable/actions/workflows/build-apk.yml/badge.svg)](https://github.com/shashankpoojari/typable/actions/workflows/build-apk.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.2-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20M3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%20Zero%20AI-16A34A.svg?style=flat)](#privacy--security-guarantee)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**Typable** is a professional, advanced, customizable mobile word processor engineered natively for Android phones, foldables, and tablets. Designed with a minimalist, typography-first aesthetic and a distraction-free philosophy, Typable provides desktop-grade document composition, rich-text styling, multi-format export/import, and crash-proof persistence without mandatory accounts, subscriptions, cloud backends, or internet connectivity.

---

## Key Highlights & Philosophy

- **Offline-First & Sovereign:** Your documents belong to you. All files are stored directly on your device. The application does not declare the `android.permission.INTERNET` permission, guaranteeing that no data can ever leave your phone.
- **Zero AI Features:** Typable is strictly deterministic word-processing software. There are no language models, no generative text tools, no remote summarization APIs, and no background semantic scrapers.
- **Desktop-Grade Document Engine:** Built on a canonical structured document object graph (paragraphs, text runs, tables, dividers, page breaks) rather than flat unformatted text.
- **Genuine File Interoperability:** Generates native OpenXML Microsoft Word (`.docx`) files, authentic paginated vector PDFs (`.pdf`), standard Markdown (`.md`), semantic HTML5 (`.html`), and plain text (`.txt`).
- **Atomic File Persistence:** Write-ahead `.tmp` flushing with filesystem `fsync`, automatic `.bak` disaster snapshots, and atomic file renaming prevent document corruption under all circumstances.

---

## Feature Tour

### 1. Document Editing Engine
- **Rich Character Formatting:** Bold, Italic, Underline, Strikethrough, Superscript ($x^2$), Subscript ($x_2$), custom text colors, and background highlights.
- **Structural Styles:** Title, Subtitle, Heading 1, Heading 2, Heading 3, Blockquotes, and Monospace Code blocks.
- **Dynamic Paragraph Formatting:** Text alignments (Left, Center, Right, Justify), multi-level block indentation, and customizable line spacing.
- **Intelligent Lists:** Bulleted lists and auto-incrementing numbered lists with sequential re-indexing, Enter-to-continue, and empty-to-exit behaviors.
- **Interactive Tables:** Full table grids with custom rows and columns, cell selection and editing, inline row/column addition, and deletion controls.
- **Thematic Breaks & Page Breaks:** Insert horizontal dividers and manual page breaks that are recognized during printing and export.
- **Multi-Level Undo & Redo:** Up to 50 levels of functional state history with automatic keystroke batching and redo clearing on branching edits.

### 2. Page Setup & Printing
- **Standard Physical Paper Sizes:** A4 (210 × 297 mm / 595 × 842 pt), US Letter (8.5 × 11 in / 612 × 792 pt), and US Legal (8.5 × 14 in / 612 × 1008 pt).
- **Page Orientations:** Portrait and Landscape modes.
- **Configurable Margins:** Normal (1.0 in / 72 pt), Narrow (0.5 in / 36 pt), and Wide (1.5 in / 108 pt).
- **Running Headers & Footers:** Customizable header text, footer text, and automatic dynamic page numbering (*"Page X of Y"*).
- **Android Print Integration:** Native `PrintDocumentAdapter` connecting directly to Android's system `PrintManager` for wireless, physical, and virtual printing.

### 3. Import & Export Formats

| Format | Extension | Operations | Description |
| :--- | :--- | :--- | :--- |
| **Microsoft Word** | `.docx` | Read, Edit, Export | Native OpenXML package (`word/document.xml`, `styles.xml`, `[Content_Types].xml`). Compatible with MS Word, LibreOffice, and Google Docs. |
| **Vector PDF** | `.pdf` | Export & Print | Genuine multi-page vector layout using Android's native `PdfDocument` engine with headers, footers, page numbering, and table borders. |
| **Markdown** | `.md` | Read, Edit, Export | Clean standard Markdown syntax supporting headings (`#`), bold (`**`), italic (`*`), strikethrough (`~~`), lists, tables, and blockquotes. |
| **Web Page** | `.html` | Read, Edit, Export | Semantic HTML5 markup with embedded responsive print styles and sanitized tag parsing. |
| **Plain Text** | `.txt` | Read, Edit, Export | Clean UTF-8 encoded text with preserved paragraph breaks. |

### 4. Document Library & Management
- **Category Tabs:** Fast navigation between **All Documents**, **Pinned**, **Favorites**, and **Trash**.
- **Real-Time Live Search:** Sub-millisecond filtering across document titles and full indexed body text.
- **Sorting Options:** Sort by Recently Modified, Oldest Modified, Title (A–Z), Title (Z–A), and Word Count.
- **Ready-to-Use Templates:**
  - *Blank Document:* Clean slate for open writing.
  - *Business Letter:* Formal executive correspondence format.
  - *Project Proposal:* Deliverables, scope, and technical roadmap.
  - *Welcome Guide:* Feature overview and keyboard shortcut guide.
- **Document Operations:** Duplicate, Rename, Pin, Favorite, Soft-delete to Trash, Restore from Trash, and Permanent Deletion.
- **Storage Access Framework (SAF):** Seamless file import from device storage or external file pickers without broad storage permissions.

### 5. Utilities & Research Tools
- **Deterministic Search & Replace:** Query navigation with match counter (*"3 of 12"*), case-sensitive matching, whole-word matching, single replace, and batch replace all.
- **Document Outline:** Dynamic Table of Contents automatically extracted from Heading 1–3 and Title styles with single-tap navigation to any section.
- **Real-Time Statistics:** Words, characters (with spaces), characters (without spaces), paragraph count, estimated pages, and estimated reading time.
- **Special Character Picker:** Typographical marks (em dash, en dash, quotes, pilcrow ¶, section §, dagger †), mathematical operators ($\infty, \sum, \sqrt{}, \ne$), currency symbols (\$, €, £, ¥, ₹, ₿), and Greek letters.
- **Keyboard Shortcuts Reference:** Quick access guide for physical or external Bluetooth keyboards (`Ctrl+B`, `Ctrl+I`, `Ctrl+U`, `Ctrl+Z`, `Ctrl+Y`, `Ctrl+S`, `Ctrl+F`, etc.).

### 6. Customization & Theming
- **5 Theme Modes:**
  - **System:** Synchronizes automatically with system dark/light mode and Material You dynamic colors (Android 12+).
  - **Light:** Clean slate and crisp white document layout.
  - **Dark:** Elegant charcoal and slate dark mode.
  - **Sepia Warm Paper:** Gentle warm ivory paper palette designed for long reading and writing sessions.
  - **Midnight OLED:** Deep true-black palette optimized for AMOLED screens.
- **Paper Canvas Tints:** Pure White (`#FFFFFF`), Warm Ivory (`#FDFBF7`), Soft Sepia (`#F6F1E9`), Slate Tint (`#F1F5F9`), and Midnight Paper (`#1E293B`).
- **Focus Mode:** Single-tap distraction-free editing that hides system bars, headers, and toolbars.

---

## Privacy & Security Guarantee

```
                    ┌───────────────────────────────────┐
                    │      TYPABLE PRIVACY AUDIT        │
                    ├───────────────────────────────────┤
                    │  [x] android.permission.INTERNET  │ ──> OMITTED (Offline)
                    │  [x] Cloud Accounts / Sign-in     │ ──> OMITTED (Local only)
                    │  [x] Telemetry & Analytics SDKs   │ ──> OMITTED (Zero tracking)
                    │  [x] Generative AI / LLM APIs     │ ──> OMITTED (Deterministic)
                    │  [x] MANAGE_EXTERNAL_STORAGE      │ ──> OMITTED (SAF Sandbox)
                    │  [✓] Android FileProvider         │ ──> Implemented for sharing
                    │  [✓] Atomic File I/O + Backup     │ ──> Implemented (.tmp / .bak)
                    └───────────────────────────────────┘
```

---

## Technical Specifications & Architecture

- **Application ID:** `shashank.word.typable`
- **Minimum SDK:** API 24 (Android 7.0 Nougat)
- **Target SDK / Compile SDK:** API 36 (Android 16)
- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose (BOM 2024.09.00) + Material 3 (1.3.1)
- **Build System:** Gradle 9.3.1 with Kotlin DSL (`.gradle.kts`)
- **Architecture Pattern:** MVVM (Model-View-ViewModel) with Kotlin Coroutines and `StateFlow`
- **File Sharing:** Secure `androidx.core.content.FileProvider` content URIs with transient read flags

---

## Building from Source

### Prerequisites
- JDK 17 or higher
- Android SDK with Platforms 24 through 36 and Build-Tools
- Git

### Build Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/shashankpoojari/typable.git
   cd typable
   ```

2. **Run local unit and Robolectric tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Build the Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   *(Requires signing keystore configuration via environment variables `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`)*

---

## GitHub Actions Automated CI/CD

Typable includes a continuous integration workflow located in [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml).

### Workflow Features
- **Triggers:** Automatically runs on every push or pull request to `main` or `master`, as well as on manual trigger (`workflow_dispatch`).
- **Automated Verification:** Executes the full suite of unit and Robolectric tests prior to packaging.
- **Artifact Generation:** Automatically uploads the compiled Debug APK (`Typable-Debug-APK`) to the GitHub Actions run summary tab for immediate download and testing on devices.
- **Automated GitHub Releases:** When pushing a version tag (e.g., `git tag v1.0.0 && git push --tags`), the workflow automatically creates a GitHub Release and attaches both Debug and Release APKs with auto-generated release notes.

---

## License

Copyright (c) 2026 Shashank Poojari.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
