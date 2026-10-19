# Changelog

All notable changes to the AI Notes application are documented in this file.

## [2.17.0] - 2026-10-12

### Fixed & Enhanced
- Note Editor Cursor Stability:
  - **Fixed Cursor Jump to End on Keystroke**: Keyed `remember(block.id)` instead of `remember(block.content)` in `EditorTextBlockItem` (`MovableImageComponents.kt`), preventing the text field state from recreating and forcing selection to `block.content.length` after every character typed.
  - **Smooth Mid-Text Editing**: Cursor now maintains its exact position and selection range as you type anywhere in the note.
  - **External State Sync**: Added programmatic text synchronization to handle external modifications (such as undo, redo, find & replace, and AI transforms) while preserving selection bounds.
- Export Distribution:
  - **Updated Export APK**: Generated and placed the latest APK build into `export/AINotes.apk`, `export/app-debug.apk`, and `export/ai-notes-debug.apk`.

## [2.16.0] - 2026-10-06

### Added & Enhanced
- Room Database Persistence Layer:
  - **KSP & Room Dependencies**: Integrated Kotlin Symbol Processing (`com.google.devtools.ksp`) and Room Database (`androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler`).
  - **`NoteEntity` Table Model**: Defined structured `@Entity(tableName = "notes")` schema with primary key, timestamps, JSON-serialized blocks/tags/history, category, tint, and pinning status.
  - **`NoteDao` Data Access Object**: Reactive `@Dao` queries exposing `Flow<List<NoteEntity>>`, single-item fetch, category filtering, atomic upserts (`OnConflictStrategy.REPLACE`), deletion, and count.
  - **`AppDatabase` SQLite Engine**: Abstract `@Database` singleton with destructive migration fallback and multi-threaded DAO access.
  - **`NoteRoomMapper`**: Type-safe domain-to-entity mapper for smooth interoperability with existing UI/ViewModels.
  - **Robolectric Test Suite**: Added `NoteRoomTest` verifying database creation, reactive flows, queries, and serialization mapping.
- Clean Note Editor Formatting Toolbar:
  - **Removed Emoji Button**: Streamlined the bottom formatting toolbar in `NoteEditorScreen.kt` by removing the redundant insert emoji button and dialog, providing more dedicated space for essential styling tools (AI assistant, color themes, lists, text formatting).

## [2.15.0] - 2026-10-06

### Added & Enhanced
- Dashboard 2D Drag & Move (Left, Right, Up, Down):
  - **Omnidirectional Drag Gestures**: Dragging notes on the dashboard now moves cards fluidly in both $X$ and $Y$ dimensions (`accumulatedDragX`, `accumulatedDragY`) with tactile haptic feedback and dynamic z-index elevation.
  - **4-Way Move Navigation Controls**: Added explicit Move Left (◀), Move Up (▲), Move Down (▼), and Move Right (▶) buttons to every note box on both Grid View and List View.
- Essential Action Buttons for Every Note Box:
  - **One-Tap Pin Toggle**: Instantly pin or unpin notes directly from the dashboard card header with visual amber/gold active indicator.
  - **Quick Color Palette Picker**: Added palette button with popup menu to change note background tint (Cream, Mint, Rose, Lavender, Sky, Teal, White) instantly.
  - **One-Tap Duplicate Button**: Clones notes directly from the card.
  - **Character Split Button**: High-contrast `Split [char]` button to split note content by character count.
  - **Protected Delete Button**: Delete button with confirmation dialog to prevent accidental data loss.

## [2.14.0] - 2026-10-06

### Added & Enhanced
- Responsive Floating Bottom Toolbar:
  - **Horizontal Scroll & Fixed Touch Target**: Added `Modifier.horizontalScroll(rememberScrollState())` to the bottom formatting toolbar in `NoteEditorScreen.kt`.
  - **Unsquished Theme & Formatting Icons**: Set standardized 40dp button sizes with 22dp icons, completely preventing the theme/palette (`ColorLens`) and bullet list buttons from getting squished, cropped, or compressed on compact phone screens.

## [2.13.0] - 2026-10-06

### Added & Enhanced
- Character Exclusion & Downward Flow Dynamic Reflow Engine:
  - **Downstream Character Reflow (`reflowTextAroundObstacle`)**: Treats the image on each line as replacing character positions. All text characters that do not fit in the left or right slots of an obstacle line are naturally pushed forward and downward into subsequent lines and below the image.
  - **Full Document Free Placement**: Images can be placed at any coordinate $(R, C)$ with automatic dynamic reflow of all preceding, beside, and succeeding text.
  - **100% Content Preservation**: Text content remains pristine and intact with zero dropped words or artificial whitespace.

## [2.12.0] - 2026-10-06

### Added & Enhanced
- Sub-Pixel Continuous Smooth Glide for Character & Line Dragging:
  - **Continuous Sub-Pixel Translation (`subPixelTranslationX/Y`)**: Eliminated layout jumping and visual stutter by maintaining exact 1:1 mathematical continuity between the finger position and layout step increments.
  - **Instant Multi-Line & Multi-Character Reflow**: Image glides seamlessly between characters ($X$) and across lines ($Y$) with continuous tactile haptic feedback.
  - **Zero Discontinuity Gesture Settling**: When finger is lifted, image lands with zero visual delay or offset snap.

## [2.11.0] - 2026-10-04

### Added & Enhanced
- Constant Image Dimensions & Omnidirectional Free Placement:
  - **Locked Image Width During Drag**: The image container is locked to a fixed width (`imageWidthDp`) based on `imageScale`, ensuring the image never stretches, shrinks, or changes size when dragged between character columns or line rows.
  - **Omnidirectional 2D Free Movement**: The image can be placed at any character column ($C = 0, 1, 2, 3 \dots$) and any line row ($R = 0, 1, 2, 3 \dots$) across paragraphs smoothly.
  - **Dynamic Right-Slot Sizing**: Right text slot flexes with `Modifier.weight(1f)` alongside the fixed-width image container for reliable text reflow.

## [2.10.0] - 2026-09-30

### Added & Enhanced
- Production-Grade `TextMeasurer` Exclusion Text Flow Layout:
  - **`MovableImageTextFlow` Custom Component**: Implemented multi-pass typographical measurement with Jetpack Compose `rememberTextMeasurer()` to compute real pixel line heights, line positions, and character widths.
  - **3-Zone Text Flow Around Obstacle**:
    - **Zone 1 (Above)**: Preceding lines rendered at 100% full width.
    - **Zone 2 (Beside Obstacle)**: Intersecting lines wrapped cleanly around the image with measured character capacity on left and right slots.
    - **Zone 3 (Below)**: Following lines and overflow continue at 100% full width with zero reserved image height.
  - **Robust Single-Finger 2D Dragging**: Driven by `detectDragGestures` with `change.consume()`, giving 60fps free movement (up, down, left, right, diagonal) without gesture conflicts or accidental pinch resize.

## [2.9.0] - 2026-09-30

### Added & Enhanced
- Top-Left Corner Anchor Position Model & Smooth 2D Multi-Directional Movement:
  - **Top-Left Corner Anchor Coordinate (`lineOffset`, `charOffset`)**: Image is positioned by its top-left corner $(R, C)$, enabling fluid movement in all 2D directions (Left, Right, Up, Down, and Diagonal $\nearrow \searrow \swarrow \nwarrow$).
  - **Clean Line Slicing Above & Below**:
    - Lines above `lineOffset` render untouched across 100% full width.
    - Lines touched by the image span ($R \le i < R + H$) wrap around the image at `charOffset` ($L_i\text{.left} + [\text{IMAGE}] + L_i\text{.right}$).
    - Lines below the image ($i \ge R + H$) render untouched across 100% full width with zero empty space.
  - **Smooth 60fps Dragging**: Updating `(lineOffset, charOffset)` modifies position coordinates directly without destroying or recreating composables mid-gesture.
  - **Isolated Pinch-to-Zoom**: Single-finger drag can no longer accidentally resize the image; two-finger pinch requires deliberate multi-touch contact.
  - **Top-Left Coordinate Navigation in Settings Dialog**: Added `▲ Line Up`, `Line Down ▼`, `◀ Char Left`, `Char Right ▶`, and `Reset (0,0)`.

## [2.8.0] - 2026-09-30

### Added & Enhanced
- Direct Character Slide ("Mengeser the Char") Interaction:
  - **Direct Character Sliding**: Switched from "stone pick-up" to instant character sliding ("mengeser"). Dragging immediately shifts the image between characters in real-time without requiring a long-press delay.
  - **Inline Grounded Feel**: Grounded elevation (`3.dp`) and flat 1.0x scale preserve natural inline text alignment.
  - **Tactile Haptic Feedback**: Delivers subtle mechanical tick feedback on each character shift.
  - **Vertical Line Shifting**: Dragging up/down shifts between lines and paragraphs smoothly.

## [2.7.0] - 2026-09-30

### Added & Enhanced
- Character-by-Character Horizontal Gliding & Column Positioning:
  - **Character-Level Horizontal Offset (`charOffset`)**: Situates the image at any precise character position inside sentences (`0. ......i love you`, `1. i......love you`, `2. i l......ove you`, `3. i lo......ve you`).
  - **Multi-Line Obstacle Span**: Spans ~2 to 3 lines high, splitting all overlapping lines at the same character column with subsequent lines continuing below at full width.
  - **Dynamic Horizontal Slide**: Dragging the image horizontally steps `charOffset` smoothly character-by-character with haptic feedback ticks.
  - **Stone Pick Up & Drop Vertical Settle**: Releasing the finger drops the stone into its landing paragraph and character position without mid-flight document churn.
  - **Image Settings Character Control**: Added `◀ Left`, `Right ▶`, and `Far Left (0)` buttons in the image settings dialog for pinpoint character adjustments.
  - **Pristine Raw Note Content**: Underlying text remains stored intact without inserting spaces, formatting tags, or newlines.

## [2.6.0] - 2026-09-30

### Added & Enhanced
- Object-in-Text Surrounding Flow & Navigation Bar Inset Handling:
  - **Surrounding Text Flow**: When an image is placed inside text as an object, text surrounds it on both sides (`Text Left   [IMAGE]   Text Right`), filling all available surrounding space like water around a boat.
  - **Responsive Dynamic Reflow**: Smaller images allow more text to fit around both sides; larger images leave less space for side text; full-size images continue text above and below.
  - **System Navigation Bar & Insets**: Fully respecting Android window insets (`WindowInsets.safeDrawing`, `navigationBarsPadding`, `imePadding`) across gesture pill and 3-button navigation.
  - **Bottom Clearance**: Note editor bottom toolbar and reading view content maintain safe margins above system navigation and keyboard with full bottom-scroll visibility.
  - **Pristine Note Text**: Layout reflow is handled at presentation layer without injecting artificial whitespace, spaces, or hardcoded breaks into user content.

## [2.5.0] - 2026-09-30

### Added & Enhanced
- Smooth Character & Word-Level Inline Gliding:
  - **Inline Word Gliding**: Dragging an image over text smoothly slides between words and sentences rather than jumping over large blocks, parting paragraphs at natural word boundaries.
  - **Fluid Water Flow Healing**: When an image moves out from between split text parts, adjacent text blocks seamlessly merge back into a single continuous paragraph (`mergeAdjacentTextBlocks`).
  - **Fluid 2D Tracking & Haptic Response**: Full 2D finger following with lift physics, left/right snap alignment with side-by-side text reflow, and two-finger pinch-to-resize.

## [2.4.0] - 2026-09-30

### Added & Enhanced
- Movable Image Objects, 2D Dragging, Pinch-to-Resize, and Flow Layout Text Reflow:
  - **Movable Image Objects**: Long-press any image to pick it up with dynamic physics lift, elevated drop shadow (`16.dp`), and finger following in 2D (`X`/`Y`).
  - **Free 2D Drag & Drop**: Dragging left or right snaps alignment (`left`, `right`, `center`). Dragging up or down across vertical threshold reorders the image across paragraphs and note blocks.
  - **Automatic Text Reflow**: Floating images (`left` or `right`) automatically reflow adjacent text blocks side-by-side without mutating the note's underlying raw text string (zero inserted spaces or newlines).
  - **Multi-Image Flow Layout**: Multiple adjacent images cluster inside `FlowRow`, fitting as many images as width permits and wrapping automatically.
  - **Two-Finger Pinch to Resize**: Continuous pinch gesture scales images dynamically between 20% and 100% while strictly maintaining aspect ratio. Text reflows instantaneously as width adjusts.
  - **Persistent Positions & Sizes**: Image positions, alignments, and scale factors are persisted in `NoteRepository` (`notes.json`) across app closures and restarts.
  - Added unit test suite `MovableImageComponentsTest.kt` verifying render grouping, floated text reflow, and multi-image cluster layouts.

## [2.3.0] - 2026-09-29

### Added & Enhanced
- Service Layer for Google Gemini API via Retrofit:
  - Retrofit `GeminiApi` interface defining `POST /v1beta/models/{model}:generateContent` with API key query param and header support.
  - Complete JSON DTO models for requests (Contents, Parts, GenerationConfig, SafetySettings, SystemInstructions) and responses (Candidates, PromptFeedback, UsageMetadata, Error).
  - High-performance `GeminiAiProvider` with custom OkHttpClient enforcing 60s connect/read/write timeouts, fallback key resolution with `BuildConfig.GEMINI_API_KEY`, and automatic model sanitization.
  - Comprehensive `AiRepository` and `MainViewModel` service methods: `generateNoteContent`, `generateOutline`, `brainstormIdeas`, `continueWriting`, `summarize` (standard, bullets, concise), `rewrite`, `fixGrammar`, `translate`, `autoTag`, and `autoCategorize`.
  - Added unit test suite in `GeminiServiceTest.kt` validating JSON serialization, deserialization, response parsing, and model sanitization logic.
  - Updated default model to `gemini-3.5-flash` per current Gemini API recommendations.

## [1.0.0] - 2026-09-28

### Added
- Complete offline-first note taking engine using atomic JSON persistence.
- Rich blocks architecture: Plain text, checklists with toggles, bullet lists, inline media images, and handwriting vector strokes.
- Fluid handwriting and sketching canvas with custom brush widths, palettes, and undo/clear controls.
- Speech recognition dictation using Android's native SpeechRecognizer.
- Distraction-free Focus Mode with dark teal canvas, serif typography, and minimal toolbar.
- Multi-page PDF export using standard Android PdfDocument via Storage Access Framework.
- Safe backup and restore engine packing notes and media into a ZIP file with path traversal defense.
- Text statistics calculation: characters, words, sentences, and paragraphs.
- Local search with exact/prefix ranking, category filtering, and sorting modes.
- Optional Google Gemini AI integration over direct REST: Summarize, Rewrite, Grammar Fix, Translation, Auto-tagging, Auto-categorization, and Smart Search re-ranking.
- Masked API key storage in local DataStore Preferences with zero logging.
- System device credential app lock protection.
- Productivity achievements screen with local progress tracking.
- Custom adaptive Material You app icon with dark teal gradient and notebook motif.

## [1.1.0] - 2026-09-28

### Added
- Dashboard notes draggable on hold to reorder and adjust custom position with real-time translation and lift elevation.
- One-tap Up/Down reorder adjustment buttons on each note card header.
- Dedicated Split Box Button on note cards with complete click isolation from editor navigation.
- Character-based Note Split Option Box: separate text based on exact character count (e.g. 100 chars in original note, 150 chars into new note) with live preview and category/tint preservation.

## [1.2.0] - 2026-09-28

### Added
- Custom expressive AI emoticon badge (`ExpressiveAiIcon`) with smart animated/canvas smiling face, gradient visor, and star sparkle, distinct from standard emoji buttons.
- Natural-language AI writing assistant dialog accepting arbitrary user instructions (e.g. "correct my writing", "make this sound more natural", "translate into Indonesian", "turn this into lyrics").
- Automatic context text targeting: defaults to highlighted/selected text when active, or entire note content, with explicit UI target chips and snippet preview.
- Non-destructive AI Result Preview dialog with side-by-side review, Replace Selection / Apply to Note, Insert Below, Copy to Clipboard, and snapshot Undo integration.
- Graceful offline/disabled dialog when Gemini AI master switch or API key is not configured, preserving normal note editing and zero text loss.

## [1.3.0] - 2026-09-28

### Added
- Rectangular "Formatted View" toggle button in the note editor toolbar positioned between Redo and Search (`Undo → Redo → Formatted View → Search → other tools`).
- Clear visual state for the toggle button with active container highlight, border, and dynamic Edit/View label.
- Clean, read-only formatted reading mode preserving actual note content and formatting in memory.
- Rich Markdown and block rendering: headings (`#`, `##`, `###`), bold (`**`/`__`), italic (`*`/`_`), strikethrough (`~~`), inline code (``` ` ```), multiline code blocks (```` ``` ````), blockquotes (`>`), dividers (`---`), numbered lists (`1.`), bullet lists (`-`/`*`/`•`), checklists, images, and handwriting strokes.
- Seamless switching between edit and reading view without losing cursor position, selection, or unsaved changes.
- Safe fallback rendering for unsupported block types preventing content loss or UI crashes.

## [1.4.0] - 2026-09-28

### Added
- Zero-lag large text file engine capable of smoothly editing and previewing massive notes without UI frame drops.
- Automatic text virtualization: auto-chunks large text blocks (>6,000 characters) into granular blocks for smooth `LazyColumn` recycling.
- Debounced undo/redo snapshots (1.5s windowing) avoiding full array copies on every single keystroke.
- Zero-allocation character and word count calculations using `derivedStateOf` and fast single-pass `TextStats`.
- Bounded dashboard card previews (120 chars) and optimized history snapshots without full-text string joins.

## [1.5.0] - 2026-09-28

### Added
- Enhanced in-note Search bar with Find Previous (`↑`) and Find Next (`↓`) navigation buttons.
- Real-time match counter badge (`1/5` matches) with auto-scroll and highlight jumping to matched blocks.
- Granular Replace options: "Replace" (replaces the current active match) and "All" (replaces all occurrences note-wide).
- Clear action buttons for quick text reset in find and replace fields.
- Full snapshot integration with Undo / Redo history for all replace operations.

## [1.6.0] - 2026-09-28

### Added
- Phone / Emulator Keyboard enhancements: Tap-to-write on any empty note canvas area with instant software keyboard popup and sentence capitalization.
- Built-in Sample Image Preset Library with 5 instant vector & canvas artwork photos (Alpine Sunset Lake, Cozy Workspace Desk, Creative Mind Map, Vintage Polaroid, and Analytics Chart).
- Freely draggable image block positioning with `⠿ Drag to Move` vertical drag handle and instant Up (`↑`) / Down (`↓`) repositioning buttons.
- Real-time text re-flow: Surrounding text blocks dynamically adjust and flow around relocated images.
- Image caption support: Note text and captions stay linked to the image and travel together when moved.

## [1.7.0] - 2026-09-28

### Added
- Window-style interactive image resizing:
  - Drag-corner handle (↘️ `OpenInFull`) to smoothly scale image width from 25% to 100% dynamically.
  - Quick window size presets (25%, 50%, 75%, 100%) and Alignment controls (Left, Center, Right).
- Precise "Place Image Between Text" system:
  - Real-time visual landing drop marker (`📍 Placing image between next text...`) during drag gestures.
  - "Place Between Paragraphs..." dialog (`📍`) to instantly teleport an image between any specific paragraphs.
  - Cursor-aware insertion: Inserting an image while editing text automatically splits the paragraph and places the image directly between the top and bottom halves.

## [2.3.0] - 2026-10-14

### Added & Fixed
- Note Editor "Select All" Across Entire Note:
  - Added dedicated "Select All Note Text" action in the 3-dot overflow menu and a one-tap "Select All" icon button (`Icons.Default.SelectAll`) in the bottom formatting toolbar.
  - Automatically unifies contiguous text blocks and highlights the complete note text.
  - Copies all note content (title, paragraphs, checklists, bullet points) cleanly to the system clipboard with instant haptic feedback and confirmation.
  - Launches the Full Note Selection Action Bar showing marked character count, "Copy all", "Share" via system share sheet, and "Deselect".
- Exported Latest APK:
  - Generated fresh debug APK (`export/AINotes.apk`, `export/app-debug.apk`, `export/ai-notes-debug.apk`).

## [2.2.0] - 2026-09-29

### Added & Changed
- Functional Interactive Calendar Tab:
  - Replaced the placeholder "Inbox" bottom navigation tab with a dedicated "Calendar" tab (`CalendarScreen.kt`).
  - Real interactive monthly calendar grid with month-by-month navigation (`<` / `>`) and quick jump to "Today".
  - Subtle visual marker dots indicate dates containing created notes, accurately mapped to `note.createdAt` timestamps from the local database.
  - Distinct highlights for Today vs Selected Date.
  - Tapping any date filters and displays notes created on that specific date in a scrollable list with title, content preview, creation time, category tag, tint, and attachment badges.
  - Tapping a note opens it directly in the full editor.
  - Clean empty state with "+ Create Note" action when a date has no notes.
  - 100% offline, zero network / external dependencies.

## [2.1.0] - 2026-09-29

### Added & Enhanced
- Hold-to-Drag Image Moving:
  - Added `detectDragGesturesAfterLongPress` directly to note image blocks.
  - Pressing and holding an image triggers haptic feedback, elevates the image (`translationY`, shadow, scaling), and lets users drag it smoothly up or down between text paragraphs.
  - Interactive status pill indicates dynamic drag position and target landing spot.
  - Normal 1-finger swipes scroll the note without delay; 2-finger pinches dynamically scale image size.
- Updated Google Gemini Model Catalog:
  - Added current official models: `gemini-3.5-flash`, `gemini-3.5-flash-lite`, `gemini-3.8-flash`, `gemini-3.1-pro-preview`, `gemini-2.5-flash`, `gemini-2.5-pro`, and `gemini-flash-latest`.
  - Added Custom Model Identifier input field in the Gemini settings dialog so users can apply any custom model endpoint instantly.

## [2.0.0] - 2026-09-29

### Added & Enhanced
- Two-Finger Pinch-to-Resize on Images:
  - Direct 2-finger pinch gesture smoothly resizes image width between 20% and 100% in real-time.
  - Resolved vertical scroll lock: Single-finger swipes over images pass directly to `LazyColumn`, allowing smooth scrolling up and down the note even when images sit at the top.
  - Image Size & Position Dialog: Tapping the bottom-right badge (`% 🤏`) opens quick width presets (`25%`, `50%`, `75%`, `100%`) and Alignment options (`Left`, `Center`, `Right`).
  - Top-left reordering pill with `↑` Move Up, `↓` Move Down, and `📍 Place Between Paragraphs`.
- Automatic Dashboard Return on Save:
  - Tapping the "Save" button in the Top App Bar saves changes, dismisses the keyboard, and automatically navigates back to the main dashboard.

## [1.9.1] - 2026-09-28

### Fixed
- Standardized Android Debug Signing Configuration:
  - Removed custom `debugConfig` block referencing local root `debug.keystore`.
  - Configured `debug` build type to use AGP's standard automatic debug keystore.
  - Hardened release signing to dynamically activate only when `KEYSTORE_PATH` is defined and file exists.
  - Guaranteed error-free `assembleDebug` execution when cloned freshly in AndroidIDE.

## [1.9.0] - 2026-09-28

### Changed
- Complete Build Configuration Alignment for AndroidIDE & On-Device Compilation:
  - Gradle Wrapper configured to official Gradle 8.6 distribution (`gradle-8.6-bin.zip`).
  - Android Gradle Plugin (AGP) pinned to `8.4.1`.
  - Kotlin pinned to `1.9.23` with Compose compiler extension `1.5.11`.
  - Java/JDK target compatibility set to JDK 17 (`JavaVersion.VERSION_17`, `jvmTarget = "17"`).
  - SDK parameters set to `compileSdk 34`, `targetSdk 34`, and `minSdk 24`.
  - Fully removed the `org.gradle.toolchains.foojay-resolver-convention` plugin and all automatic toolchain provisioning.
  - Enabled `android.useAndroidX=true` and `android.nonTransitiveRClass=true` in `gradle.properties`.
  - Verified clean compilation with zero warnings or errors.
