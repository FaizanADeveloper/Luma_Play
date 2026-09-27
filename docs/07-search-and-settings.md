
# LumaPlay — Search and Settings Specification

| Field | Details |
|---|---|
| Project | LumaPlay |
| Application ID | `com.lumaplay.tv` |
| Platform | Android TV |
| Minimum SDK | Android 11 (API 30) |
| Search Engine | Local Room database |
| Voice Recognition | Android SpeechRecognizer |
| Preferences | Jetpack DataStore |
| UI | Jetpack Compose for TV |
| Document Version | 1.0 |
| Status | Development specification |

## 1. Purpose

This document defines the universal search system,
voice search workflow, application settings and
device capability diagnostics.

All functionality must be designed for Android TV
and operated using a standard television remote.

Search results must reflect actual indexed media.
Settings must accurately represent implemented
features and available device capabilities.

## 2. Functional Scope

This specification covers:

- Universal local media search
- Search result presentation
- TV remote keyboard input
- Voice recognition
- Microphone permission handling
- Search history, if approved
- Library and storage settings
- Video playback settings
- Audio settings
- Subtitle preferences
- Device capability diagnostics
- Application information
- Persistent user preferences

The search system must not depend on cloud services
to find locally indexed media.

## 3. Universal Search

### 3.1 Searchable Content

LumaPlay shall search its local media index.

Searchable categories include:

- Videos
- Songs
- Artists
- Albums
- Folders
- Available filenames

Only accessible, indexed media shall appear as
immediately playable search results.

Previously indexed but unavailable media may
appear with a clearly identified unavailable
status if the approved design supports it.

### 3.2 Search Architecture

Use a dedicated SearchRepository.

The repository shall query Room through
appropriate DAO interfaces.

The SearchViewModel shall manage:

- Current query
- Search state
- Search results
- Result categories
- Loading state
- Empty state
- Error state
- Voice recognition state

Compose screens must not execute database
queries directly.

### 3.3 Search Behavior

Text search shall:

1. Accept a query from the TV keyboard.
2. Normalize input where appropriate.
3. Search the local database.
4. Return relevant matching records.
5. Group results according to the approved design.
6. Update the screen without blocking navigation.

Search should be case-insensitive.

Ignore unnecessary leading and trailing whitespace.

Avoid issuing expensive queries for every individual
keystroke when debouncing would improve performance.

Do not repeatedly extract metadata from original
media files during ordinary searches.

### 3.4 Search Matching

The initial implementation shall support
partial text matching.

Examples:

- A video title matches part of its name.
- A song matches its title.
- An artist query returns matching artists.
- An album query returns matching albums.
- A filename query returns matching media.
- A folder query returns matching indexed folders.

More advanced fuzzy matching may be considered
after the initial search system is functional.

Do not introduce a remote search API for
local media discovery.

## 4. Search Result Presentation

Search results shall follow the approved
Google Stitch design.

Where appropriate, results may be grouped into:

- Videos
- Music
- Artists
- Albums
- Folders

Each result must display suitable information.

Video results may include:

- Thumbnail
- Title
- Duration
- Available resolution

Music results may include:

- Artwork
- Track title
- Artist
- Album

Folder results may include:

- Folder name
- Storage source
- Available media count

Unavailable metadata must not be fabricated.

## 5. Search Result Actions

Selecting a video shall open its details
screen or the approved playback destination.

Selecting a song shall open or start
the appropriate music playback workflow.

Selecting an artist or album shall open
the relevant filtered music collection.

Selecting a folder shall open
the corresponding folder browser.

Navigation must remain consistent with
the rest of LumaPlay.

## 6. Search Screen States

Implement explicit UI states.

### Idle

The user has not entered a query.

Display the approved initial search interface.

### Searching

A search operation is in progress.

Provide appropriate visual feedback.

### Results

Matching media has been found.

Display the results in the approved layout.

### No Results

No indexed media matches the query.

Provide a clear message.

### Empty Library

No searchable media has been indexed.

Offer an appropriate library scan action.

### Error

A search operation has failed.

Provide an understandable message
and a retry action where appropriate.

## 7. TV Remote Search Input

The search field must be focusable
using the D-pad.

Selecting the field shall invoke
an appropriate Android TV text-input method.

Users must be able to:

- Enter text
- Delete text
- Submit a query
- Navigate search results
- Return to the search field
- Exit the Search screen

Do not require a physical keyboard,
touchscreen or mouse.

## 8. Voice Search

Voice search shall be available through
an explicit microphone action on
the Search screen where supported.

Use Android SpeechRecognizer or an
appropriate compatible recognition mechanism.

Voice recognition is an optional input method.

Text search remains the primary
guaranteed search mechanism.

## 9. Voice Search Workflow

The intended workflow is:

1. User opens Search.
2. User selects the microphone action.
3. LumaPlay checks microphone permission.
4. LumaPlay checks recognition availability.
5. A listening overlay appears.
6. The user speaks a search query.
7. Recognized text is displayed.
8. The text populates the search field.
9. The local media index is searched.
10. Matching results appear.

Recognition failure must not prevent
subsequent text searches.

## 10. Microphone Permission

Request RECORD_AUDIO only when
microphone access is needed.

If permission has not been granted,
explain why it is required.

If the user denies permission:

- Dismiss the permission workflow.
- Explain that voice search is unavailable.
- Preserve access to keyboard search.
- Avoid repeated unsolicited requests.

If permission is permanently denied,
provide appropriate instructions
for changing it in Android settings.

## 11. Recognition Availability

Before starting voice recognition,
check whether a compatible recognition
service is available.

If no service is available,
disable the voice action or display
a useful explanation.

Prefer on-device recognition
where the platform and installed
recognition services support it.

Do not promise that voice recognition
will function offline on every TV.

Do not silently transmit media files
or library contents to a speech service.

The dedicated Assistant button on a
physical TV remote is not assumed
to be available to LumaPlay.

## 12. Voice Search UI

The active listening overlay shall
follow the approved Stitch design.

Required states:

- Ready
- Listening
- Processing
- Recognition successful
- No speech detected
- Permission required
- Service unavailable
- Recognition error

The overlay must be dismissible
using the Back button.

Display recognized text when available.

Do not leave the microphone
listening indefinitely after
the user cancels the operation.

## 13. Voice Recognition Lifecycle

The recognition component must:

- Start only after a user action.
- Stop when the user cancels.
- Release resources when no longer needed.
- Avoid simultaneous recognition sessions.
- Handle Activity lifecycle changes.
- Handle recognition timeouts and errors.

Do not store raw microphone recordings
as part of the initial implementation.

## 14. Search History

Search history is optional
and requires product approval.

If implemented, it shall:

- Store queries locally.
- Be accessible through the Search screen.
- Allow users to clear stored history.
- Avoid storing microphone audio.
- Respect application data clearing.

Do not add account synchronization
or cloud-based search history.

## 15. Settings Architecture

The Settings screen shall follow
the approved Google Stitch design.

Settings must be organized into
understandable categories.

Recommended categories:

1. Library and Storage
2. Video Playback
3. Audio
4. Subtitles
5. Device Capabilities
6. About

Use native Compose for TV components.

Every setting must be accessible
using a standard TV remote.

## 16. Library and Storage Settings

The library section shall provide
access to relevant storage controls.

Required information:

- Available indexed media counts
- Authorized storage locations
- Source availability
- Last successful scan
- Current scanning status

Required actions:

- Start a manual scan
- Select an accessible folder
- Review authorized storage sources
- Remove a source from the library,
  subject to approved behavior

Removing a source from LumaPlay
must not delete the original
media files or storage folder.

Any action that removes indexed
records must be clearly explained.

## 17. Scan Settings

Expose scanning preferences only
when their behavior is implemented.

Possible settings include:

- Manual rescan
- Automatic scan on app launch
- Automatic scan of available sources

Automatic scanning is subject
to performance and platform limitations.

Do not promise immediate USB
detection on every Android TV.

Do not start repeated full scans
without a clear need.

## 18. Video Playback Settings

Provide implemented playback
preferences through DataStore.

Potential preferences include:

- Resume playback behavior
- Default playback speed
- Preferred subtitle language
- Preferred audio language
- Supported aspect-ratio preference

The final set of exposed settings
must correspond to real
Media3 player functionality.

Do not display nonfunctional controls.

## 19. Audio Settings

Audio settings must reflect
the actual Android audio pipeline.

Potential options include:

- Automatic output behavior
- Supported decoded audio output
- Supported passthrough preferences
- Preferred audio language

The exact available options
must be validated during
implementation and physical testing.

Do not claim that the app
can force an unsupported codec
through HDMI ARC.

## 20. Dolby Audio Information

The device diagnostics section
may display information about
reported audio capabilities.

Distinguish between:

- Source audio format
- Android-reported decoder support
- Android-reported output capabilities
- Confirmed active output format

The presence of an Atmos track
does not prove Atmos output.

Conventional HDMI ARC does not
support lossless Dolby TrueHD
Atmos passthrough.

Dolby Digital Plus Atmos may
work over HDMI ARC when supported
by the complete playback chain.

Do not present unsupported
passthrough settings as functional.

## 21. Subtitle Settings

Provide supported subtitle
appearance preferences.

Potential settings include:

- Enable or disable subtitles
- Preferred language
- Text size
- Text color
- Background appearance
- Caption opacity, where supported

Respect the limitations
of the active subtitle renderer.

Some embedded subtitle formats
may not support every
appearance customization.

Changes should be reflected
in playback where supported.

## 22. Device Capabilities

Provide a device diagnostics screen.

Use appropriate Android APIs
to retrieve available information.

Potential fields include:

- Device manufacturer
- Device model
- Android version
- Available video decoders
- Available audio decoders
- Reported HDR capabilities
- Relevant audio output capabilities

Device capability detection
must be performed safely.

Do not assume that an advertised
codec guarantees every profile,
level, bitrate or resolution.

## 23. Primary Hardware Configuration

The initial physical test device is:

Redmi Smart TV X50
Android 11
4K UHD display

Audio output:

HDMI ARC
    |
    v
Zebronics ZEB-JUKE BAR 9800 DWS PRO

The UI must remain responsive
and readable on the 4K television.

The 1920 × 1080 Stitch design
is a reference canvas, not a
video playback resolution limit.

Actual HDR and multichannel
audio behavior must be tested
on physical hardware.

## 24. Capability Presentation Rules

Every capability label must
have a clear meaning.

Examples:

"Source: HEVC"

Indicates the source video codec.

"Device reports HEVC decoder"

Indicates reported decoder availability.

"Source: Dolby Digital Plus"

Indicates source audio metadata.

"Audio output: Unverified"

Indicates that the actual output
format has not been confirmed.

Do not display an "Atmos active"
badge based solely on source metadata.

Do not fabricate unsupported
device capability information.

## 25. About Screen

The About screen shall display:

- LumaPlay name
- Application version
- Relevant application information
- Applicable acknowledgements
- Appropriate open-source licenses

The exact presentation shall
follow the approved design.

Do not include fictional
certifications or partnerships.

## 26. Preference Persistence

Use Jetpack DataStore
for small application preferences.

The settings repository shall
expose observable preference state.

ViewModels shall consume this state
and update the corresponding UI.

Changes must persist across
normal application restarts.

Do not store large media collections,
thumbnails or playback history
in DataStore.

Use Room for structured
media and playback data.

## 27. Settings Validation

Settings must be validated
before they are applied.

If a preference is unsupported
on the active device:

- Disable the control, or
- Explain its limitations, or
- Use a supported fallback.

Do not silently claim
that an unsupported
setting has been activated.

## 28. D-pad Navigation

All Search and Settings screens
must be fully remote-accessible.

Requirements:

- Clear initial focus
- Predictable directional navigation
- Visible focus indicators
- Consistent Select behavior
- Consistent Back behavior
- Appropriate dialog focus
- Focus restoration after dialogs

The TV remote must be sufficient
for every essential workflow.

## 29. Error Handling

Handle at least:

- Empty media index
- Search database failure
- No matching search results
- Microphone permission denied
- Speech service unavailable
- Voice recognition timeout
- Voice recognition error
- Storage source unavailable
- Unsupported device setting
- Preference persistence failure

Provide a useful user-facing
message and appropriate
recovery action where possible.

Do not expose raw stack traces
in production UI.

## 30. Performance Requirements

Search must remain responsive
on the primary Android TV.

Use appropriate database indexes.

Avoid expensive queries
on the main thread.

Debounce rapid query updates
where useful.

Cancel obsolete search work
when a newer query replaces it.

Settings screens must not
perform expensive device scans
during every recomposition.

Cache relatively stable
capability information
where appropriate.

## 31. Privacy Requirements

Core text search operates locally.

LumaPlay shall not upload
media files or library
metadata to a server.

Voice recognition may use
an installed recognition
service that requires network access.

The UI must not imply that
all voice recognition is
necessarily processed offline.

Request microphone permission
only for voice-related features.

Do not retain raw microphone
recordings by default.

## 32. Search Test Cases

### SS-001 — Video Search

Enter part of an indexed
video title.

Expected:
Matching video results appear.

### SS-002 — Music Search

Enter a song or artist name.

Expected:
Relevant indexed music appears.

### SS-003 — Empty Query

Clear the search field.

Expected:
The approved idle search
state is displayed.

### SS-004 — No Results

Enter a query that matches
no indexed media.

Expected:
A clear no-results state appears.

### SS-005 — Offline Search

Disconnect internet access
and perform a text search.

Expected:
Indexed local media remains searchable.

### SS-006 — Voice Permission Denied

Deny microphone permission.

Expected:
Text search remains functional.

### SS-007 — Voice Service Unavailable

Test without a compatible
speech recognition service.

Expected:
The application handles
the limitation gracefully.

### SS-008 — Voice Search Success

Speak a recognizable
media search query.

Expected:
Recognized text populates
the search field and
local results are displayed.

### SS-009 — Voice Cancellation

Start voice recognition
and press Back.

Expected:
Recognition is cancelled
and the Search screen
remains usable.

## 33. Settings Test Cases

### SS-010 — Preference Persistence

Change an implemented setting
and restart the application.

Expected:
The preference persists.

### SS-011 — Library Rescan

Start a manual scan
from Settings.

Expected:
The actual scanner starts
and reports its state.

### SS-012 — Unsupported Audio Option

Test an unsupported
audio output configuration.

Expected:
The UI does not falsely
claim the option is active.

### SS-013 — Device Diagnostics

Open device capabilities.

Expected:
Available information reflects
actual platform-reported data.

### SS-014 — Remote Navigation

Navigate all Settings categories
using the physical TV remote.

Expected:
All essential controls
are accessible.

### SS-015 — 4K Television

Open Search and Settings
on the physical 4K TV.

Expected:
The interface scales correctly,
remains readable and does
not clip essential controls.

## 34. Acceptance Criteria

The Search and Settings subsystem
is complete when:

1. Text search works offline.

2. Videos, songs, artists,
   albums and folders are
   searchable where indexed.

3. Search results open the
   correct destination.

4. Search is fully accessible
   using the TV remote.

5. Voice recognition works
   where a compatible service
   is available.

6. Voice recognition failure
   does not disable text search.

7. Implemented preferences
   persist across app restarts.

8. Library settings control
   real scanning operations.

9. Device diagnostics report
   actual available information.

10. Unsupported capabilities
    are not advertised as active.

11. Relevant emulator and
    physical-TV tests pass.

## 35. Implementation Boundaries

Do not implement:

- Cloud media search
- Mandatory online accounts
- Cloud-synchronized search history
- Remote media recommendations
- Always-on microphone listening
- Interception of protected
  Assistant remote buttons
- Unsupported audio output controls

Do not add casting or mirroring
features to LumaPlay.

Those features belong to
the separate future
LumaCast project.

## 36. Open Decisions

The following require
approval or device validation:

- Final search result ordering
- Whether search history is included
- Final voice recognition UX
- Available audio output settings
- Supported subtitle appearance controls
- Automatic library scanning policy
- Final device diagnostics fields

Record approved decisions
before implementation.

Do not silently expand
the product scope.
