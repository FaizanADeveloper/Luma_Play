
# LumaPlay — Technical Architecture

| Field | Details |
|---|---|
| Project | LumaPlay |
| Application ID | `com.lumaplay.tv` |
| Platform | Android TV |
| Minimum SDK | API 30 — Android 11 |
| Language | Kotlin |
| UI Framework | Jetpack Compose for TV |
| Architecture | MVVM + Repository |
| Document Version | 1.0 |
| Status | Proposed implementation architecture |

## 1. Architecture Objectives

LumaPlay is a native, offline-first Android TV application
for discovering, browsing and playing locally accessible
video and audio files.

The architecture must:

- Separate UI, business logic and data access.
- Keep media scanning and metadata extraction off the
  main thread.
- Support reliable video and audio playback.
- Preserve playback progress and user preferences.
- Handle Android storage restrictions correctly.
- Remain responsive on devices with limited RAM.
- Support Android TV remote and D-pad navigation.
- Allow individual features to be developed and tested
  without destabilizing the entire application.

The approved Google Stitch designs are visual references.
Their exported web code must not be treated as native
Android implementation code.

## 2. Technology Stack

| Component | Technology |
|---|---|
| Programming language | Kotlin |
| Build system | Gradle with Kotlin DSL |
| UI | Jetpack Compose for TV |
| Design system | Material 3 for TV |
| Navigation | AndroidX Navigation for Compose |
| Presentation | ViewModel + StateFlow |
| Dependency injection | Hilt |
| Video and audio playback | AndroidX Media3 ExoPlayer |
| Media controls | Media3 MediaSession |
| Local database | Room |
| Preferences | DataStore |
| Media discovery | MediaStore |
| Folder access | Storage Access Framework |
| Background work | Coroutines; WorkManager where appropriate |
| Artwork loading | Coil with a compatible Compose integration |
| Logging | Android Logcat |
| Unit testing | JUnit and coroutine test utilities |
| UI testing | AndroidX Compose UI testing |

Dependency versions must be selected for compatibility
with the existing Android Studio project.

Do not upgrade Gradle, Kotlin or the Android Gradle Plugin
solely because a newer version exists.

## 3. Architectural Pattern

Use MVVM with a repository-based data layer.

### 3.1 Presentation Layer

Responsibilities:

- Render screens and playback controls.
- Handle D-pad focus and navigation.
- Observe ViewModel state.
- Display loading, empty, success and error states.
- Send user actions to ViewModels.

Compose screens must not directly scan storage, execute
database queries or manage ExoPlayer instances.

### 3.2 Domain Layer

Responsibilities:

- Define application use cases.
- Coordinate operations across repositories.
- Apply business rules.
- Expose operations that can be tested independently.

Examples:

- ScanMediaUseCase
- GetHomeContentUseCase
- GetVideoLibraryUseCase
- SearchMediaUseCase
- SavePlaybackProgressUseCase
- GetDeviceCapabilitiesUseCase

Introduce use-case classes where they simplify business
logic. Avoid creating unnecessary abstraction layers.

### 3.3 Data Layer

Responsibilities:

- Read accessible media using Android APIs.
- Maintain the local media index.
- Store playback history and preferences.
- Expose media information through repositories.
- Handle missing files and disconnected storage.

Repository interfaces should be independent of UI code.

## 4. Initial Project Structure

Begin with a single Gradle application module.

Do not split the application into multiple Gradle modules
until there is a demonstrated development or build need.

Recommended package structure:

```text
app/src/main/java/com/lumaplay/tv/
|
|-- MainActivity.kt
|-- LumaPlayApplication.kt
|
|-- core/
|   |-- navigation/
|   |-- theme/
|   |-- ui/
|   |-- model/
|   |-- common/
|   `-- di/
|
|-- data/
|   |-- local/
|   |   |-- database/
|   |   |-- dao/
|   |   `-- entity/
|   |
|   |-- media/
|   |   |-- scanner/
|   |   |-- metadata/
|   |   `-- storage/
|   |
|   |-- repository/
|   `-- preferences/
|
|-- domain/
|   |-- repository/
|   `-- usecase/
|
|-- playback/
|   |-- service/
|   |-- controller/
|   |-- video/
|   |-- audio/
|   `-- diagnostics/
|
|-- feature/
|   |-- onboarding/
|   |-- home/
|   |-- videos/
|   |-- video_details/
|   |-- music/
|   |-- folders/
|   |-- search/
|   `-- settings/
|
`-- util/
```

This structure is a target organization, not permission
to delete or replace the existing working project.

Refactor incrementally.

## 5. Application Entry Point

MainActivity shall:

1. Initialize the Compose application.
2. Apply the LumaPlay television theme.
3. Establish the root navigation structure.
4. Display onboarding or Home as appropriate.
5. Connect UI lifecycle events to relevant components.

Keep MainActivity small.

The Application class may initialize dependency injection
and application-wide infrastructure.

Do not perform full media scans inside MainActivity.

## 6. Navigation Architecture

Use a single primary navigation graph.

Main destinations:

- Onboarding
- Home
- Videos
- Video Details
- Video Player
- Music
- Music Player
- Folders
- Search
- Settings

The navigation system must preserve predictable
Back-button behavior.

The persistent navigation sidebar belongs to the main
application shell and should not appear over immersive
full-screen playback unless explicitly designed.

Each screen must define an appropriate initial focus
target and restore focus when returning from a child
screen where practical.

## 7. UI State Management

Each feature should have a dedicated ViewModel when its
state or business logic justifies one.

Represent screen state using immutable Kotlin data
classes and StateFlow.

Typical screen states:

- Loading
- Content
- Empty
- PermissionRequired
- Error

One-time navigation and transient UI messages must be
handled separately from persistent screen state.

Avoid storing Android Activity references inside
ViewModels.

Collect StateFlow using lifecycle-aware Compose APIs.

## 8. Media Discovery Architecture

### 8.1 MediaStore

Use MediaStore to discover accessible indexed videos
and audio files.

Query only the columns needed for the current operation.

Where permitted, capture:

- MediaStore identifier
- Content URI
- Display name
- Media type
- Duration
- Size
- Date added or modified
- Available metadata

A MediaStore item must be opened using its content URI,
not by assuming direct filesystem access.

### 8.2 Storage Access Framework

Use the Storage Access Framework when the user selects
an accessible folder or storage location.

Persist URI permissions only when Android grants them.

Document-provider support and USB access may vary by
device and firmware.

Do not assume that every USB filesystem or every folder
is accessible through the same API.

### 8.3 Permissions

Implement Android-version-aware permission handling.

For the initial Android 11 target, request applicable
media-reading permissions only when needed.

For newer Android versions, use the corresponding
media-specific permission model.

Do not request MANAGE_EXTERNAL_STORAGE by default.

If broad storage access is proposed later, require a
separate product and policy review before implementation.

### 8.4 Media Scanner

Create a MediaScanner component responsible for:

- Enumerating accessible media.
- Extracting supported metadata.
- Updating the Room media index.
- Reporting scan state and progress.
- Handling cancellation and failures.
- Reconciling removed or inaccessible files.

Scanning must run off the main thread.

Avoid launching overlapping full-library scans.

Scanning progress should represent actual work where
measurable; otherwise use an indeterminate indicator.

## 9. Local Database

Use Room for persistent media and playback information.

### 9.1 Proposed Entities

MediaItemEntity:
- id
- sourceUri
- sourceType
- mediaType
- displayName
- title
- durationMs
- sizeBytes
- dateAdded
- dateModified
- lastIndexedAt
- availabilityStatus

PlaybackProgressEntity:
- mediaId
- positionMs
- durationMs
- lastPlayedAt
- completed

StorageSourceEntity:
- id
- treeUri
- displayName
- sourceType
- lastScanAt
- availabilityStatus

Optional entities may be introduced for cached technical
metadata and user-created playlists.

Use stable identifiers and explicit relationships.

Do not use a filename alone as a unique media identity.

### 9.2 Database Operations

Repositories shall expose Flow-based queries where
continuous UI updates are useful.

Database writes must be transactional when a scan
modifies multiple related records.

Schema changes require versioned Room migrations.

Do not enable destructive migration as a default
production strategy.

## 10. Playback Architecture

### 10.1 Media3

Use AndroidX Media3 ExoPlayer as the primary playback
engine for both video and audio.

The playback subsystem shall be responsible for:

- Creating and releasing players.
- Loading content URIs.
- Controlling playback.
- Exposing player state.
- Selecting available tracks.
- Reporting playback errors.
- Saving playback progress.

### 10.2 MediaSession

Use Media3 MediaSession for supported media-button
handling and integration with Android playback controls.

For background music playback, use an appropriate
MediaSessionService with a managed player lifecycle.

The video player must coordinate correctly with the
application lifecycle and avoid duplicate active players.

Do not create a separate unmanaged ExoPlayer instance
every time a screen recomposes.

### 10.3 Playback Controller

Provide a playback controller abstraction that exposes
the actions required by the UI.

Examples:

- play
- pause
- seekTo
- playMedia
- selectAudioTrack
- selectSubtitleTrack
- setPlaybackSpeed
- observePlaybackState

Track-selection controls must display only tracks
actually available for the current media item.

### 10.4 Playback Progress

Save video playback progress periodically and on
appropriate lifecycle transitions.

Do not write to the database on every player position
update.

A completed-video threshold may be introduced as a
configurable business rule.

Resume behavior must be consistent across Home,
Video Details and Video Library.

## 11. Codec, HDR and Audio Capabilities

Media3 delegates decoding and rendering to the
available Android media pipeline.

Use relevant Android APIs to inspect device capabilities.

Important distinctions:

1. The source file contains a particular format.
2. The Android device reports support for that format.
3. Playback actually succeeds using a selected decoder.
4. The connected display or audio system receives the
   intended output format.

These are different facts and must not be conflated.

Potential video formats include H.264, HEVC, VP9 and AV1,
subject to actual device support.

Potential HDR formats include HDR10, HDR10+, HLG and
Dolby Vision, subject to hardware, software and display
capabilities.

### 11.1 Primary Audio Test Setup

The initial physical test configuration is:

Redmi Smart TV X50
    |
    | HDMI ARC
    v
Zebronics ZEB-JUKE BAR 9800 DWS PRO

Dolby Digital Plus / E-AC-3 Atmos may be feasible over
HDMI ARC where supported by the entire playback path.

Conventional HDMI ARC does not provide the bandwidth
required for lossless Dolby TrueHD Atmos passthrough.

Do not promise TrueHD Atmos output through this
connection.

DTS-family passthrough support must be established
through actual device and soundbar testing.

### 11.2 Diagnostics

The technical overlay may display:

- Source video codec
- Source audio codec
- Resolution and frame rate
- Detected HDR metadata
- Selected decoder, where available
- Selected audio and subtitle tracks
- Reported audio-device capabilities
- Playback errors

Do not display an "Atmos output active" or similar
claim solely from source-file metadata.

Where actual output cannot be verified, display
"Output status unavailable" or equivalent wording.

## 12. Music Playback

The music subsystem shall use Media3 playback
infrastructure.

It must support:

- Song playback
- Queue management
- Previous and next
- Shuffle
- Repeat
- Playback-state restoration where appropriate
- MediaSession integration

Music playback should remain functional when navigating
between library screens.

Background playback behavior must follow Android
foreground-service and media-playback requirements.

## 13. Search Architecture

Create a search repository that queries the local
media index.

Searchable content includes:

- Video titles
- Audio titles
- Artists
- Albums
- Folder names
- Available filenames

Use appropriate Room queries and indexes.

Debounce rapid text input where useful.

Search must remain functional offline.

### 13.1 Voice Search

Use Android SpeechRecognizer or a compatible
recognition intent where supported.

The app must:

- Request microphone permission when necessary.
- Provide an explicit microphone action.
- Show listening and recognition states.
- Insert recognized text into the search query.
- Preserve keyboard search as a fallback.

Prefer on-device recognition where supported.

Do not assume every Android TV has an installed
speech-recognition service or that recognition
always works offline.

Do not assume the app can intercept the dedicated
Google Assistant button on a television remote.

## 14. Preferences Architecture

Use DataStore for application preferences.

Examples:

- Preferred subtitle appearance
- Playback preferences
- Library scanning preferences
- Supported audio preferences
- Onboarding completion

Preferences must expose observable state to relevant
ViewModels.

Keep user preferences separate from indexed media data.

## 15. Artwork and Thumbnail Loading

Load artwork asynchronously.

Use bounded memory and disk caching.

Do not preload an entire large media library.

Generate video thumbnails only when appropriate and
cache results using a stable media identity.

Provide placeholder artwork when thumbnail extraction
fails or no embedded artwork is available.

## 16. Error Handling

Use explicit error states for:

- Permission denied
- Storage disconnected
- File missing
- Unsupported media
- Playback failure
- Metadata extraction failure
- Empty library
- Voice recognition unavailable
- Voice recognition failure

Errors must be understandable and recoverable where
possible.

Do not silently replace failed real-media operations
with fictional sample content.

## 17. Dependency Injection

Use Hilt to provide:

- Repositories
- Room database and DAOs
- DataStore
- Media scanner
- Metadata extractors
- Search components
- Playback-related dependencies

Choose component scopes deliberately.

Do not inject Activity contexts into singleton objects.

## 18. Testing Architecture

### Unit Tests

Test:

- Search filtering
- Scan reconciliation
- Media sorting
- Resume-position rules
- Playback state transformations
- Repository business logic

### Integration Tests

Test:

- Room queries and migrations
- MediaStore discovery
- SAF permissions
- Repository-to-database interactions
- Playback progress persistence

### UI Tests

Test:

- Initial D-pad focus
- Navigation between screens
- Back-button behavior
- Empty states
- Permission screens
- Playback controls

### Device Tests

Test on:

- Android TV API 30 emulator
- Primary Redmi Smart TV X50
- Connected HDMI ARC soundbar

The emulator is not sufficient to verify actual HDR,
hardware decoding or audio passthrough.

## 19. Development and Build Rules

The initial Android TV project has already been created,
built and launched successfully.

Preserve this working baseline.

Development must proceed incrementally.

For each implementation milestone:

1. Review the relevant requirements.
2. Review the approved Stitch designs.
3. Identify the files and dependencies to change.
4. Implement one coherent feature or subsystem.
5. Build the project.
6. Run relevant tests.
7. Review the changes.
8. Commit the verified result to Git.

Never claim that a feature is complete based only
on a successful compilation.

Functional behavior must also be verified.

## 20. Source Control and Safety

Use a private Git repository.

Commit source code, documentation and appropriate
design assets.

Exclude:

- Build outputs
- Local SDK paths
- IDE caches
- Credentials
- Signing keys
- Large temporary files

Maintain an independent backup of important project
files and original design exports.

Before executing agent-generated terminal commands,
review their exact effects.

Prefer narrowly scoped, one-time command approval.

Never grant blanket terminal permissions by default.

Destructive commands, broad cleanup operations,
project resets and filesystem-wide modifications
require explicit review and approval.

## 21. Architecture Acceptance Criteria

This architecture is considered implemented when:

- The application builds and launches on Android TV.
- Presentation, data and playback responsibilities
  are separated.
- Real accessible media can be scanned and indexed.
- Video and music playback use managed Media3 players.
- Playback progress persists correctly.
- Remote navigation works across implemented screens.
- Missing files and permissions are handled safely.
- Settings persist across application restarts.
- Device capability claims are based on actual evidence.
- Relevant tests pass on emulator and physical hardware.

Implementation details may evolve, but changes that
affect approved requirements must be documented.
