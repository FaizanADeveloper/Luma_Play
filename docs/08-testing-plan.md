
# LumaPlay — Testing and Quality Assurance Plan

| Field | Details |
|---|---|
| Project | LumaPlay |
| Application ID | `com.lumaplay.tv` |
| Platform | Android TV |
| Minimum SDK | Android 11 (API 30) |
| Primary Emulator | Android TV, API 30, 1920 × 1080 |
| Physical Test TV | Redmi Smart TV X50, 4K UHD |
| Audio Test Device | Zebronics ZEB-JUKE BAR 9800 DWS PRO |
| Audio Connection | HDMI ARC |
| Document Version | 1.0 |
| Status | Development test plan |

## 1. Purpose

This document establishes the testing strategy,
test environments, acceptance criteria and
release-readiness requirements for LumaPlay.

The purpose of testing is to verify that the
application behaves correctly with real local
media on Android TV hardware.

A successful Gradle build alone does not
establish that a feature works.

Features must be tested at the appropriate
unit, integration, UI and device levels.

## 2. Testing Objectives

The testing process shall verify:

- Successful application installation and launch
- Correct D-pad navigation and focus behavior
- Accurate implementation of approved Stitch designs
- Safe Android permission handling
- Reliable local media discovery
- Compatible USB storage access
- Persistent media indexing
- Video and music playback
- Playback resume and history
- Audio and subtitle track selection
- Universal text search
- Supported voice search
- Settings persistence
- Device capability reporting
- Error handling and recovery
- Performance and resource management
- Physical 4K and HDR playback
- Supported HDMI ARC audio output

Testing must distinguish verified functionality
from functionality that has only been implemented
or partially tested.

## 3. Test Environments

### 3.1 Development Environment

Primary development tools:

- Android Studio
- Gradle
- Android Emulator
- Android Logcat
- Android Profiler
- JUnit
- AndroidX testing libraries

Google Antigravity may assist with implementation
and test generation.

Agent-generated code and test results must
still be reviewed.

### 3.2 Android TV Emulator

Initial emulator configuration:

- Android TV
- Android 11
- API 30
- 1920 × 1080 display
- D-pad remote input

The emulator shall be used for:

- Application startup
- Navigation
- Screen layout
- Focus management
- Permission flows where supported
- Database operations
- Search
- Basic media playback
- Error-state testing
- Automated UI tests

The emulator cannot establish that physical
4K, HDR or HDMI ARC output works correctly.

### 3.3 Physical Television

Primary physical test device:

Redmi Smart TV X50
Android 11
4K UHD display

The physical TV shall be used for:

- Actual remote-control behavior
- Real USB storage access
- 4K video playback
- Hardware decoding
- HDR format compatibility
- Real-world playback performance
- Audio output compatibility
- Long-duration stability testing

Record the actual firmware and Android
build information when physical testing begins.

### 3.4 Audio Equipment

Primary audio test configuration:

Redmi Smart TV X50
        |
        | HDMI ARC
        v
Zebronics ZEB-JUKE BAR 9800 DWS PRO

Verify available audio output modes
through actual playback and relevant
television or soundbar indicators.

Do not assume that source audio metadata
proves the format received by the soundbar.

## 4. Test Levels

### 4.1 Unit Testing

Unit tests shall verify isolated
business logic.

Examples:

- Media sorting
- Search matching
- Resume-position calculations
- Scan reconciliation
- Metadata transformations
- Settings validation
- Playback-state transformations

Unit tests should not depend
on physical television hardware.

### 4.2 Integration Testing

Integration tests shall verify
interactions between components.

Examples:

- Repository and Room database
- Scanner and media index
- Playback progress and database
- DataStore and Settings ViewModel
- Search repository and database
- SAF permissions and storage access

Use controlled test data
where appropriate.

### 4.3 UI Testing

UI tests shall verify:

- Screen navigation
- Focus behavior
- Button activation
- Dialog behavior
- Loading states
- Empty states
- Error states
- Back-button behavior

Essential workflows must work
without touch or mouse input.

### 4.4 Physical Device Testing

Physical testing shall verify
hardware-dependent behavior
that cannot be established
using the emulator.

Examples:

- USB access
- High-bitrate 4K playback
- HDR output
- HDMI ARC audio
- Soundbar compatibility
- Long-duration stability

## 5. Test Status Definitions

Each test shall have one
of the following statuses:

NOT RUN

The test has not been executed.

PASS

The observed result matches
the expected result.

FAIL

The observed result does not
match the expected result.

BLOCKED

The test cannot currently
be executed because of a
missing dependency or environment.

NOT APPLICABLE

The test does not apply
to the current configuration.

Do not mark an unexecuted
test as PASS.

## 6. Test Evidence

For every executed test,
record:

- Test ID
- Test date
- Application build or commit
- Test environment
- Test media, where relevant
- Actual result
- Status
- Relevant logs or screenshots
- Known limitations

For hardware-dependent tests,
record the actual device
and connection configuration.

## 7. Application Startup Tests

### TC-APP-001 — Fresh Installation

Install LumaPlay on
the Android TV emulator.

Expected:

The application launches
without crashing.

The correct initial screen
is displayed.

### TC-APP-002 — Subsequent Launch

Close and reopen LumaPlay.

Expected:

The application opens
successfully and retains
appropriate saved state.

### TC-APP-003 — Offline Launch

Disable internet connectivity
and launch LumaPlay.

Expected:

The application launches.

Core local-media functionality
does not require internet access.

### TC-APP-004 — Process Restart

Terminate the application
process and relaunch it.

Expected:

The application recovers
without database corruption.

## 8. UI and Navigation Tests

### TC-UI-001 — Home Screen

Open the Home screen.

Expected:

The layout follows the
approved Stitch reference.

Dynamic media sections
display actual indexed data.

### TC-UI-002 — Sidebar Navigation

Navigate between all
primary sidebar destinations.

Expected:

Every destination is
reachable using the D-pad.

### TC-UI-003 — Focus Visibility

Navigate through interactive
elements on each screen.

Expected:

The focused element is
always visually identifiable.

### TC-UI-004 — Focus Restoration

Open a media details screen,
then press Back.

Expected:

Focus returns to the
previous media card
where practical.

### TC-UI-005 — Dialog Focus

Open and dismiss
a settings or track dialog.

Expected:

Focus remains inside
the active dialog.

After dismissal, focus
returns predictably.

### TC-UI-006 — Empty Library

Launch the application
with no indexed media.

Expected:

Appropriate empty states
appear without fictional
sample media.

### TC-UI-007 — 4K Display

Run LumaPlay on the
physical 4K television.

Expected:

The UI scales correctly.

Text remains readable.

Essential controls are
not clipped or distorted.

## 9. Permission Tests

### TC-PER-001 — Permission Granted

Grant the applicable
media-reading permissions.

Expected:

The app can discover
authorized media.

### TC-PER-002 — Permission Denied

Deny the requested permission.

Expected:

The application explains
the limitation and remains usable.

### TC-PER-003 — Permission Revoked

Revoke previously granted
permission through Android settings.

Expected:

The app detects the access
failure and provides recovery.

### TC-PER-004 — SAF Folder Selection

Select an accessible
folder using SAF.

Expected:

LumaPlay can access
authorized media within
the selected scope.

### TC-PER-005 — Persisted SAF Access

Grant persistable access,
restart LumaPlay and
reopen the selected folder.

Expected:

Access persists where
Android permits it.

### TC-PER-006 — Restricted Folder

Attempt to select a folder
restricted by Android.

Expected:

The app handles the
platform restriction
without crashing.

## 10. Media Scanning Tests

### TC-SCAN-001 — Initial Scan

Scan accessible media storage.

Expected:

Supported videos and
audio files are indexed.

### TC-SCAN-002 — Empty Folder

Scan an empty
authorized folder.

Expected:

Scanning completes
and an empty state appears.

### TC-SCAN-003 — Repeat Scan

Scan an unchanged
media collection twice.

Expected:

Duplicate media entries
are not unnecessarily created.

### TC-SCAN-004 — New Media

Add a supported file
to accessible storage.

Run a rescan.

Expected:

The new file appears
in the library.

### TC-SCAN-005 — Modified Media

Modify or replace
an indexed media file.

Run a rescan.

Expected:

The relevant indexed
metadata is updated.

### TC-SCAN-006 — Deleted Media

Delete a previously
indexed file outside LumaPlay.

Run a successful scan.

Expected:

The app reconciles
the missing media appropriately.

### TC-SCAN-007 — Scan Cancellation

Cancel a scan
while it is running.

Expected:

The operation stops safely.

The database remains consistent.

### TC-SCAN-008 — Scan Responsiveness

Navigate the application
during a large scan.

Expected:

The UI remains responsive.

### TC-SCAN-009 — Scan Failure

Cause a controlled
storage-access failure.

Expected:

The scanner reports
the failure without
crashing the application.

## 11. USB Storage Tests

### TC-USB-001 — USB Discovery

Connect a compatible
USB storage device.

Expected:

Android exposes the
storage through an
appropriate supported mechanism.

LumaPlay can access
authorized media.

### TC-USB-002 — USB Media Scan

Scan a user-authorized
USB media folder.

Expected:

Supported files appear
in the library.

### TC-USB-003 — USB Removal

Disconnect USB storage
after indexing its media.

Expected:

The app remains responsive.

Affected media is marked
unavailable where appropriate.

### TC-USB-004 — Removal During Scan

Disconnect the USB drive
while scanning.

Expected:

The scan handles the
failure safely.

### TC-USB-005 — Removal During Playback

Disconnect USB storage
while playing a file.

Expected:

Playback fails gracefully.

The app does not crash.

### TC-USB-006 — USB Reconnection

Reconnect the same drive
and rescan.

Expected:

Media availability
is restored where possible.

### TC-USB-007 — Unsupported Filesystem

Connect storage that
the television cannot mount.

Expected:

LumaPlay does not claim
that the drive is accessible.

The app remains functional.

## 12. Video Playback Tests

### TC-VID-001 — H.264 Playback

Play a supported
H.264 video.

Expected:

Video and audio
play correctly.

### TC-VID-002 — HEVC Playback

Play a representative
HEVC video.

Expected:

Playback succeeds
where supported.

Otherwise, an appropriate
error is displayed.

### TC-VID-003 — 4K UHD Playback

Play a compatible
3840 × 2160 video
on the physical TV.

Expected:

Playback works when
the device supports
the file's codec,
profile and bitrate.

### TC-VID-004 — Play and Pause

Use the remote
to play and pause.

Expected:

Playback responds
correctly.

### TC-VID-005 — Seeking

Seek forward and backward.

Expected:

Playback resumes at
the requested valid position.

### TC-VID-006 — Resume

Play part of a video,
exit and reopen it.

Expected:

The saved playback
position is restored.

### TC-VID-007 — Multiple Audio Tracks

Play a video containing
multiple audio tracks.

Expected:

Available tracks
can be selected.

### TC-VID-008 — Unsupported Codec

Attempt playback
with an unsupported codec.

Expected:

A clear error appears.

The application
does not crash.

### TC-VID-009 — Corrupted File

Attempt to play
a corrupted media file.

Expected:

The app handles
the failure gracefully.

### TC-VID-010 — Long Playback

Play a long-duration
video on the physical TV.

Expected:

No reproducible crash
or uncontrolled
resource growth.

## 13. HDR Tests

These tests require
compatible physical hardware
and representative test files.

### TC-HDR-001 — HDR10

Play a compatible
HDR10 sample.

Expected:

Playback succeeds
where the complete
pipeline supports it.

### TC-HDR-002 — HDR10+

Play a compatible
HDR10+ sample.

Expected:

Actual playback behavior
is recorded.

Do not infer support
from the file label alone.

### TC-HDR-003 — HLG

Play a compatible
HLG sample.

Expected:

Playback behavior
is recorded accurately.

### TC-HDR-004 — Dolby Vision

Play a representative
Dolby Vision sample.

Expected:

Playback succeeds
only where the device
supports the relevant
profile and container.

### TC-HDR-005 — Unsupported HDR

Play an HDR format
unsupported by the device.

Expected:

An appropriate fallback
is used if available.

Otherwise, a clear
error is displayed.

## 14. Audio Output Tests

Record the television's
audio output settings
for every test.

### TC-AUD-001 — Stereo PCM

Play a supported
stereo audio file.

Expected:

Audio is heard correctly.

### TC-AUD-002 — Dolby Digital

Play a compatible
AC-3 test file.

Expected:

Playback behavior
is recorded.

Verify actual output
where indicators permit.

### TC-AUD-003 — Dolby Digital Plus

Play a compatible
E-AC-3 test file.

Expected:

Playback succeeds
where the complete
audio path supports it.

### TC-AUD-004 — DD+ Atmos

Play a representative
Dolby Digital Plus
Atmos test file.

Expected:

Verify actual output
using available TV
and soundbar indicators.

Do not claim Atmos
solely from source metadata.

### TC-AUD-005 — TrueHD Atmos

Attempt playback
of a TrueHD Atmos file.

Expected:

Record whether the
device can decode
or otherwise handle
the audio track.

Do not expect lossless
TrueHD Atmos passthrough
over conventional HDMI ARC.

### TC-AUD-006 — DTS

Play a representative
DTS audio track.

Expected:

Record actual decoding
or passthrough behavior.

Do not assume support.

### TC-AUD-007 — Audio Track Switching

Switch between available
audio tracks during playback.

Expected:

Supported tracks
are selectable.

Playback remains stable.

### TC-AUD-008 — Unsupported Audio

Play media with an
unsupported audio track.

Expected:

The app reports the
limitation and offers
another available track
where appropriate.

## 15. Subtitle Tests

### TC-SUB-001 — Embedded Subtitles

Play a file containing
supported embedded subtitles.

Expected:

Available tracks
can be selected.

### TC-SUB-002 — External SRT

Play a video with
an accessible matching
SRT subtitle file.

Expected:

The subtitle file
loads where supported.

### TC-SUB-003 — Subtitle Disable

Disable active subtitles.

Expected:

Subtitle rendering stops.

### TC-SUB-004 — Subtitle Language

Switch between available
subtitle languages.

Expected:

The selected supported
track is displayed.

### TC-SUB-005 — Invalid Subtitle

Load a malformed
subtitle file.

Expected:

The app handles
the error gracefully.

### TC-SUB-006 — Subtitle Styling

Change supported
subtitle appearance
preferences.

Expected:

The renderer applies
supported settings.

## 16. Music Playback Tests

### TC-MUS-001 — Music Library

Scan supported audio files.

Expected:

Songs appear in
the music library.

### TC-MUS-002 — Song Playback

Select a supported song.

Expected:

Playback starts.

### TC-MUS-003 — Playback Controls

Test play, pause,
previous and next.

Expected:

Controls behave correctly.

### TC-MUS-004 — Shuffle

Enable shuffle.

Expected:

Queue traversal follows
the implemented
shuffle behavior.

### TC-MUS-005 — Repeat

Test repeat-one
and repeat-all.

Expected:

Playback follows
the selected mode.

### TC-MUS-006 — Missing Artwork

Play a song without
embedded artwork.

Expected:

Appropriate placeholder
artwork is displayed.

### TC-MUS-007 — Screen Navigation

Navigate to another
screen during music playback.

Expected:

Playback continues
according to the
approved lifecycle behavior.

## 17. Search Tests

### TC-SEARCH-001 — Video Search

Search for an indexed
video title.

Expected:

Matching videos appear.

### TC-SEARCH-002 — Music Search

Search for a song,
artist or album.

Expected:

Relevant results appear.

### TC-SEARCH-003 — Folder Search

Search for an
indexed folder name.

Expected:

Matching folders appear.

### TC-SEARCH-004 — Partial Match

Enter part of
a known media title.

Expected:

Matching indexed
media is returned.

### TC-SEARCH-005 — No Results

Enter a query with
no matching media.

Expected:

An appropriate
no-results state appears.

### TC-SEARCH-006 — Offline Search

Disconnect internet access.

Perform a text search.

Expected:

Local indexed media
remains searchable.

### TC-SEARCH-007 — Rapid Input

Enter and modify
queries rapidly.

Expected:

The UI remains responsive.

Obsolete results do not
overwrite newer results.

## 18. Voice Search Tests

### TC-VOICE-001 — Permission Granted

Grant microphone permission.

Start voice recognition.

Expected:

A compatible installed
service starts listening.

### TC-VOICE-002 — Permission Denied

Deny microphone permission.

Expected:

Voice search is unavailable.

Text search still works.

### TC-VOICE-003 — Recognition Success

Speak a recognizable
media search query.

Expected:

Recognized text populates
the search field.

Matching local results
are displayed.

### TC-VOICE-004 — Recognition Failure

Trigger or simulate
a recognition failure.

Expected:

A clear error appears.

Keyboard search
remains available.

### TC-VOICE-005 — Cancellation

Start recognition,
then press Back.

Expected:

Recognition stops
and the overlay closes.

### TC-VOICE-006 — No Service

Test on a device without
a compatible recognition service.

Expected:

The app handles
the limitation gracefully.

## 19. Settings Tests

### TC-SET-001 — Preference Persistence

Change an implemented setting.

Restart the app.

Expected:

The preference persists.

### TC-SET-002 — Library Rescan

Start a scan
from Settings.

Expected:

The actual scanner
is activated.

### TC-SET-003 — Subtitle Preference

Change a supported
subtitle preference.

Expected:

The preference persists
and is applied
where supported.

### TC-SET-004 — Audio Preference

Select a supported
audio preference.

Expected:

The setting behaves
according to actual
platform capabilities.

### TC-SET-005 — Device Diagnostics

Open device capabilities.

Expected:

Reported information
comes from real
platform queries.

### TC-SET-006 — Unsupported Setting

Attempt to access
an unsupported option.

Expected:

The option is disabled
or clearly explained.

## 20. Performance Testing

Measure performance
on the physical TV
where possible.

Test scenarios:

- Cold application launch
- Large-library scanning
- Large video library scrolling
- Artwork loading
- Search during scanning
- 4K playback
- Long-duration playback
- Switching between screens
- Repeated player opening
- USB disconnection

Record:

- Observed responsiveness
- Memory consumption
- CPU usage where available
- Playback interruptions
- Reproducible crashes
- Relevant Logcat errors

Establish quantitative
performance targets after
collecting baseline measurements.

Do not invent performance
results or claim that
untested targets were met.

## 21. Stability Testing

Perform repeated
real-world workflows.

Examples:

- Launch and close the app repeatedly.
- Open and exit video playback repeatedly.
- Switch between video and music.
- Scan and rescan large libraries.
- Connect and disconnect USB storage.
- Open and dismiss dialogs repeatedly.
- Resume playback after app restart.
- Use the app for an extended session.

Investigate reproducible
crashes, memory leaks
and unresponsive UI states.

## 22. Regression Testing

After each implementation
milestone, rerun tests
for previously completed
critical functionality.

Minimum regression areas:

- App launch
- Primary navigation
- Storage permissions
- Media scanning
- Video playback
- Music playback
- Playback history
- Search
- Settings persistence

A newly implemented feature
must not silently break
a previously verified feature.

## 23. Automated Test Strategy

Automate suitable tests
for deterministic behavior.

Recommended areas:

- Media sorting
- Search matching
- Resume logic
- Scan reconciliation
- Room DAO operations
- DataStore persistence
- ViewModel state transitions
- Basic Compose navigation

Hardware-dependent tests
must remain explicitly
identified as physical tests.

Do not fabricate automated
results for tests that
require a real television
or soundbar.

## 24. Defect Reporting

Each defect report
shall contain:

- Defect ID
- Summary
- Affected feature
- Environment
- Application build or commit
- Reproduction steps
- Expected behavior
- Actual behavior
- Relevant logs
- Screenshots or recordings
- Severity
- Current status

Severity levels:

CRITICAL:
Data loss, security issue,
persistent crash or
fundamental app failure.

HIGH:
Major feature unusable
without a practical workaround.

MEDIUM:
Important behavior incorrect
but a workaround exists.

LOW:
Minor visual or usability
issue with limited impact.

Severity must reflect
the observed impact,
not assumptions.

## 25. Release-Blocking Defects

Do not release a build
with known critical defects.

Examples include:

- App fails to launch
- Common navigation is unusable
- Ordinary scanning modifies
  or deletes original media
- Database corruption
- Reproducible crashes
  during essential workflows
- Serious permission
  or privacy violations

High-severity defects
must be reviewed before
release approval.

## 26. Milestone Test Gates

### Gate 1 — UI Foundation

Verify:

- App builds and launches
- Theme loads correctly
- Primary navigation works
- D-pad focus is visible
- Basic screens render

### Gate 2 — Storage and Library

Verify:

- Permissions work
- Accessible media is indexed
- Scanning is asynchronous
- Database persistence works
- USB failures are handled

### Gate 3 — Core Playback

Verify:

- Supported videos play
- Supported music plays
- Remote controls work
- Playback progress persists
- Playback errors are handled

### Gate 4 — Advanced Playback

Verify:

- Audio-track selection
- Subtitle selection
- External subtitles where supported
- Playback speed
- Technical overlay
- Representative 4K and HDR files

### Gate 5 — Search and Settings

Verify:

- Offline text search
- Voice-search fallback
- Preference persistence
- Device diagnostics
- Settings navigation

### Gate 6 — Physical TV Validation

Verify:

- Actual 4K TV UI
- Physical remote
- Compatible USB storage
- Real 4K playback
- Available HDR formats
- HDMI ARC audio behavior
- Long-duration stability

### Gate 7 — Release Candidate

Verify:

- Required tests completed
- Critical defects resolved
- Relevant regression tests pass
- Application version updated
- Release build produced
- Final installation tested

## 27. Release Readiness

LumaPlay is ready
for its initial release
when:

1. All approved mandatory
   features are implemented.

2. Critical tests pass
   in the appropriate
   test environments.

3. No known critical
   release-blocking
   defects remain.

4. Physical-device-dependent
   claims have been verified
   on appropriate hardware.

5. Original media files
   remain protected
   during normal operation.

6. Core local-media features
   function offline.

7. The application is
   navigable using a
   standard TV remote.

8. Known limitations
   are documented.

9. The final release build
   has been installed
   and tested.

## 28. Test Execution Record

Maintain a separate
test execution record.

Suggested fields:

| Test ID | Environment | Status | Build | Evidence |
|---|---|---|---|---|
| TC-APP-001 | Emulator | NOT RUN | — | — |
| TC-UI-001 | Emulator | NOT RUN | — | — |
| TC-USB-001 | Physical TV | NOT RUN | — | — |
| TC-VID-003 | Physical TV | NOT RUN | — | — |
| TC-AUD-004 | TV + Soundbar | NOT RUN | — | — |

Add the remaining test cases
as development progresses.

Do not mark a test as PASS
until it has actually
been executed successfully.

## 29. Documentation and Evidence

Preserve appropriate evidence
for completed milestones.

Evidence may include:

- Test execution records
- Relevant Logcat output
- Screenshots
- Screen recordings
- Build reports
- Unit test reports
- Device configuration
- Representative media
  characteristics

Do not commit copyrighted
commercial media files
to the project repository.

Use appropriately licensed
or personally created
test media.

## 30. Safety Requirements

Testing must not
risk unrelated files
or development projects.

Before destructive
testing or database resets:

- Review the exact operation.
- Confirm its scope.
- Preserve important data.
- Obtain explicit approval
  when required.

Use isolated test media
for file-modification
or removal scenarios.

Do not test file deletion
against irreplaceable
personal media.

Do not approve broad
agent terminal permissions
by default.
