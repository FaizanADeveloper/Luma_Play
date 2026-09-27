
# LumaPlay — Software Requirements Specification (SRS)

**Version:** 1.0  
**Status:** Development specification  
**Application ID:** `com.lumaplay.tv`  
**Platform:** Android TV  
**Minimum SDK:** Android 11 (API 30)

## 1. Purpose

This document defines the functional and non-functional
requirements of LumaPlay, organized around the user's
journey through the application.

Each requirement has a unique ID that developers can
reference during implementation, testing and review.

## 2. User Workflow Overview

The primary user journey is:

1. Launch LumaPlay.
2. Complete first-launch setup and grant storage access.
3. Scan accessible internal or external media storage.
4. Browse the Home screen and media libraries.
5. Select and play a video or audio file.
6. Search for media using text or supported voice input.
7. Configure playback, library and subtitle preferences.
8. Resume previously watched media on subsequent visits.

All core local-media workflows must function offline.

---

## 3. Workflow 1 — First Launch and Onboarding

**WF-01:** The application shall display the LumaPlay
brand and load the main interface without requiring
an account or internet connection.

**WF-02:** On first launch, the application shall explain
its purpose and request only the storage permissions
needed for the current Android version.

**WF-03:** The application shall distinguish between
media permissions and user-selected folder access.

**WF-04:** Users shall be able to choose accessible
storage locations and initiate an initial media scan.

**WF-05:** If access is denied, the application shall
explain the limitation and provide a retry or
alternative folder-selection action.

**WF-06:** Onboarding shall be navigable using a
standard Android TV remote.

### Acceptance Criteria

- A new user can complete setup without a mouse.
- Denied permissions do not cause an application crash.
- The application does not require internet access.
- A user can reach the Home screen after setup.

---

## 4. Workflow 2 — Media Discovery and Scanning

**WF-07:** LumaPlay shall discover accessible local
video and audio files using appropriate Android
media and storage APIs.

**WF-08:** The scanner shall support accessible
internal storage and compatible connected USB storage.

**WF-09:** Scanning shall run asynchronously without
blocking the television interface.

**WF-10:** The application shall display scanning
progress or an appropriate indeterminate state.

**WF-11:** Users shall be able to initiate a manual rescan.

**WF-12:** The media index shall update when previously
indexed files are added, modified or removed.

**WF-13:** Unavailable or disconnected storage shall
not crash the application.

**WF-14:** Duplicate media entries shall be avoided
where reliable file identity is available.

**WF-15:** The application shall retain only access
permissions actually granted by Android.

### Acceptance Criteria

- A supported local media file appears after scanning.
- The interface remains responsive during scanning.
- Disconnecting a USB drive is handled gracefully.
- Rescanning does not unnecessarily duplicate entries.

---

## 5. Workflow 3 — Home Screen

**WF-16:** The Home screen shall follow the approved
Google Stitch design, adapted for native Compose for TV.

**WF-17:** The Home screen shall provide navigation
to Videos, Music, Folders, Search and Settings.

**WF-18:** It shall display Continue Watching when
unfinished video playback history exists.

**WF-19:** It shall display recently added media
when relevant items are available.

**WF-20:** It shall provide a way to initiate a
library rescan.

**WF-21:** Media artwork, progress indicators and
technical badges shall reflect actual indexed data.

**WF-22:** Empty library sections shall display
appropriate empty states instead of fictional media.

### Acceptance Criteria

- All primary navigation items are reachable by D-pad.
- Focus is visually apparent at all times.
- Selecting a media card opens the expected destination.
- The Home screen handles an empty library correctly.

---

## 6. Workflow 4 — Browse the Video Library

**WF-23:** Users shall be able to browse indexed
videos in a television-friendly layout.

**WF-24:** Video cards shall display available
artwork, title and relevant media information.

**WF-25:** Users shall be able to open a video
details screen.

**WF-26:** The details screen shall show available
metadata, including duration, resolution, codecs,
HDR information, file size and location.

**WF-27:** Users shall be able to start playback
from the beginning or resume when progress exists.

**WF-28:** Missing metadata shall be represented
as unavailable rather than fabricated.

### Acceptance Criteria

- Video browsing works with the television remote.
- Selecting a video opens the correct details screen.
- Play and Resume actions launch the selected file.
- Unavailable artwork does not break the layout.

---

## 7. Workflow 5 — Video Playback

**WF-29:** LumaPlay shall use AndroidX Media3
ExoPlayer for local video playback.

**WF-30:** The player shall support play, pause,
seek and resume actions.

**WF-31:** Playback controls shall be operable
using a standard Android TV remote.

**WF-32:** Users shall be able to select available
audio and subtitle tracks.

**WF-33:** Users shall be able to adjust playback
speed where supported.

**WF-34:** Users shall be able to select supported
aspect-ratio or scaling modes.

**WF-35:** The application shall save video
playback progress for future resumption.

**WF-36:** The player shall provide a technical
information overlay showing available playback data.

**WF-37:** Unsupported media formats shall produce
an understandable error rather than a crash.

**WF-38:** The player shall integrate with MediaSession
for appropriate media-button handling.

**WF-39:** HDR playback shall be enabled only when
supported by the media pipeline and device.

**WF-40:** Audio passthrough shall depend on actual
Android audio capabilities and the connected output.

### Acceptance Criteria

- Supported sample videos play without crashing.
- Remote play, pause and seeking function correctly.
- Playback progress persists after leaving the player.
- Unsupported files display an appropriate error.
- Source metadata is not misrepresented as actual output.

---

## 8. Workflow 6 — Browse and Play Music

**WF-41:** Users shall be able to browse local songs,
albums and artists using available metadata.

**WF-42:** The music library shall display artwork
where available and suitable placeholders otherwise.

**WF-43:** Selecting a playable song shall start
audio playback.

**WF-44:** The music player shall display current
track information and playback progress.

**WF-45:** Users shall be able to control play,
pause, previous and next.

**WF-46:** The music player shall support a playback
queue, shuffle and repeat.

**WF-47:** Audio playback shall integrate with
MediaSession for compatible remote controls.

**WF-48:** Unavailable audio metadata shall not
prevent playback of an otherwise supported file.

### Acceptance Criteria

- Indexed songs appear in the music library.
- Remote playback controls function.
- Queue, shuffle and repeat behave consistently.
- Missing album artwork is handled gracefully.

---

## 9. Workflow 7 — Browse Media Folders

**WF-49:** Users shall be able to browse media
within accessible internal and external folders.

**WF-50:** Folder navigation shall support
D-pad selection and predictable Back behavior.

**WF-51:** Selecting a playable file shall open
the appropriate video or audio player.

**WF-52:** Inaccessible folders shall display
a permission or access message.

**WF-53:** Removed external storage shall be
handled without crashing the application.

### Acceptance Criteria

- Users can navigate permitted folder hierarchies.
- Supported media files open in the correct player.
- Permission and disconnected-drive states are clear.

---

## 10. Workflow 8 — Universal Search

**WF-54:** Users shall be able to search indexed
videos, songs, artists, albums and folders.

**WF-55:** Search results shall update based on
the entered query.

**WF-56:** Search input and result navigation
shall be usable with a television remote.

**WF-57:** The Search screen shall provide a
microphone action where voice recognition is supported.

**WF-58:** Voice search shall request microphone
permission when required.

**WF-59:** Recognized speech shall populate the
search query and search the local media index.

**WF-60:** Users shall retain keyboard search
when voice recognition is unavailable or fails.

**WF-61:** The application shall not assume it can
intercept a remote's dedicated Assistant button.

### Acceptance Criteria

- A text query returns matching indexed media.
- Search works without internet access.
- Voice-search failures preserve text-search access.
- No-results states provide useful feedback.

---

## 11. Workflow 9 — Settings and Diagnostics

**WF-62:** Users shall be able to access library,
playback, audio and subtitle preferences.

**WF-63:** Supported user preferences shall persist
between application launches.

**WF-64:** Users shall be able to rescan their
accessible media library.

**WF-65:** The application shall display available
device codec and media capabilities using appropriate
Android capability APIs.

**WF-66:** Audio settings shall distinguish source
audio format from confirmed playback or output status.

**WF-67:** Unsupported audio-output preferences
shall be disabled or explained where detectable.

**WF-68:** Settings shall include application
information and version details.

### Acceptance Criteria

- Saved settings remain after restarting the app.
- Capability information reflects detected data.
- The app does not guarantee unsupported HDR or
  Dolby audio output.

---

## 12. Workflow 10 — Return to the Application

**WF-69:** LumaPlay shall retain indexed library
information between normal application launches.

**WF-70:** The Home screen shall restore applicable
Continue Watching entries.

**WF-71:** Resuming a video shall use the last
reliably saved playback position.

**WF-72:** If a previously indexed file is no longer
accessible, the app shall display a recoverable error.

**WF-73:** Application restarts shall not require
repeating onboarding unless access or configuration
must be restored.

### Acceptance Criteria

- Previously indexed accessible media remains discoverable.
- Playback resume survives a normal app restart.
- Missing files do not cause crashes.

---

## 13. Non-Functional Requirements

### 13.1 Performance

**NFR-01:** Library scanning shall not block the UI.

**NFR-02:** Large media collections shall use
pagination or lazy rendering where appropriate.

**NFR-03:** Artwork loading and caching shall
operate within reasonable memory limits.

**NFR-04:** The application shall be designed for
Android TV hardware with limited RAM, including
the primary test television.

### 13.2 Reliability

**NFR-05:** Permission denial, missing files and
USB disconnection shall be handled gracefully.

**NFR-06:** Playback errors shall be recoverable
without requiring a television restart.

**NFR-07:** Long-running operations shall expose
appropriate loading or progress states.

### 13.3 Accessibility and Usability

**NFR-08:** All essential screens and actions shall
support D-pad navigation.

**NFR-09:** Interactive controls shall have
visible focus indicators.

**NFR-10:** Text, controls and spacing shall be
suitable for television viewing distances.

**NFR-11:** Focus shall return to a predictable
location after navigating Back.

### 13.4 Privacy and Offline Operation

**NFR-12:** Core media browsing and playback shall
not require an account or internet connection.

**NFR-13:** LumaPlay shall request only permissions
needed for implemented functionality.

**NFR-14:** The app shall not upload personal
media files or library metadata to a server.

**NFR-15:** Voice recognition may depend on an
installed recognition service; offline recognition
shall be preferred where supported.

### 13.5 Compatibility

**NFR-16:** The application shall support Android
TV devices running Android 11 or newer.

**NFR-17:** Codec, HDR and multichannel audio support
shall be determined by actual device and media
pipeline capabilities.

**NFR-18:** The UI shall scale appropriately
across supported television resolutions.

---

## 14. Explicitly Out of Scope

The following features are not part of LumaPlay:

- Phone or laptop screen mirroring
- AirPlay receiver functionality
- Chromecast receiver functionality
- Wired display capture
- Cloud media streaming services
- Mandatory user accounts

Screen casting and mirroring may be developed
separately under the future LumaCast project.

---

## 15. Requirements Verification

Each functional requirement shall be reviewed
against its acceptance criteria.

Testing shall include:

- Unit tests for applicable business logic
- UI and D-pad navigation tests
- Media scanning and permission tests
- Video and audio playback tests
- Emulator testing
- Physical Android TV testing
- Connected soundbar audio-path testing

Detailed test cases belong in
`docs/08-testing-plan.md`.

## 16. Change Control

Changes to approved requirements must be
documented before implementation.

Antigravity must not silently remove features,
invent device capabilities or substitute mock
data for completed production functionality.
