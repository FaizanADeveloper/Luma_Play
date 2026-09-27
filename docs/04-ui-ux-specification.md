# LumaPlay — UI/UX Specification

| Field | Details |
|---|---|
| Project | LumaPlay |
| Platform | Android TV |
| UI Framework | Jetpack Compose for TV |
| Design System | Material 3 for TV |
| Design Reference | Google Stitch exports |
| Reference Canvas | 1920 × 1080 |
| Input Method | TV remote and D-pad |
| Document Version | 1.0 |
| Status | Implementation specification |

## 1. Purpose

This document defines the user interface, interaction
patterns and navigation behavior for LumaPlay.

The approved Google Stitch exports are the primary
visual references for screen composition, spacing,
colors, typography, artwork and component styling.

Design directory:

`designs/stitch/LumaPlay_UI_UX/`

Before implementing a screen, inspect its corresponding
design files and available assets.

If a design is missing, incomplete or inconsistent
with an approved functional requirement, document
the discrepancy rather than inventing a final design.

## 2. Design Principles

LumaPlay shall provide a cinematic, television-first
experience.

The interface must be:

- Optimized for landscape television displays.
- Fully operable with a standard Android TV remote.
- Visually consistent across all screens.
- Responsive to focus changes and user actions.
- Readable from normal television viewing distances.
- Accessible without touch or mouse interaction.
- Responsive during scanning and media loading.
- Honest about actual media and device capabilities.

Core playback and library features must work offline.

## 3. Visual Design System

### 3.1 Colors

Follow the approved Stitch color palette.

The intended visual direction is:

- Dark navy and near-black backgrounds.
- Electric-blue and cyan accents.
- Light foreground text.
- Muted secondary information.
- Distinct error, warning and success states.

Do not introduce a competing color palette without
approval.

Use centralized theme tokens rather than hardcoded
colors throughout individual screens.

### 3.2 Typography

Create a consistent typography hierarchy for:

- Page headings
- Section headings
- Media titles
- Body text
- Metadata
- Button labels
- Technical information

Use scalable text sizes appropriate for TV viewing.

Avoid truncating essential actions or error messages.

Long media titles may use controlled truncation
where the full title remains accessible elsewhere.

### 3.3 Spacing and Layout

Use the approved 1920 × 1080 Stitch canvas as the
visual reference, not as a fixed pixel requirement.

Layouts must adapt to different TV resolutions,
density settings and available display areas.

Respect television-safe margins.

Avoid placing essential controls against screen edges.


### 3.3.1 4K Display Compatibility

The approved Google Stitch designs use a 1920 × 1080
reference canvas with a 16:9 aspect ratio.

LumaPlay must support both Full HD (1920 × 1080)
and 4K UHD (3840 × 2160) televisions.

The UI shall use responsive Jetpack Compose layouts
rather than fixed pixel positioning.

The 1080p design canvas does not impose a 1080p
video playback limit.

Where supported by the device, display and Android
media pipeline, LumaPlay shall support native
4K UHD video playback.

UI rendering resolution and video output resolution
must be treated as separate capabilities.

The interface shall be validated on the 1080p
emulator and the physical 4K television.


### 3.4 Shared Components

Develop reusable native Compose components for:

- Navigation sidebar
- Media cards
- Focusable action buttons
- Section headers
- Metadata badges
- Progress indicators
- Search fields
- Dialogs
- Loading indicators
- Empty-state panels
- Error-state panels
- Technical information rows

Components must share consistent focus styling,
spacing and interaction behavior.

## 4. Global Application Shell

The primary application shell contains:

- Persistent navigation sidebar
- Current screen content
- Appropriate status information
- A predictable focus entry point

Primary navigation destinations:

1. Home
2. Videos
3. Music
4. Folders
5. Search
6. Settings

Use the ordering and exact visual arrangement shown
in the approved Stitch designs.

The sidebar may collapse or expand according to
the approved interaction design.

Immersive video playback shall not be obstructed
by the persistent application sidebar.

## 5. D-pad Navigation

Every essential action must be reachable using
a standard Android TV remote.

### 5.1 Directional Navigation

- Up and Down move between vertically related items.
- Left and Right move between horizontally related items.
- Select activates the currently focused control.
- Back returns to the previous appropriate destination.
- Media keys control playback where supported.

Focus movement must follow the visual layout.

Do not allow focus to disappear behind overlays
or move unpredictably to unrelated components.

### 5.2 Focus Indicators

Every interactive component must provide a clear
visible focus state.

Focus styling may include:

- Highlighted border
- Background change
- Subtle scale animation
- Elevated appearance
- Increased text or icon contrast

Follow the approved Stitch focus treatments.

Focused elements must remain fully visible.

### 5.3 Focus Restoration

When returning from a details screen to a media
library, restore focus to the previously selected
media card where practical.

When closing a dialog, return focus to the control
that opened it.

When opening a new screen, establish a predictable
initial focus target.

### 5.4 Navigation Testing

Test every screen without a mouse or keyboard.

Validate navigation using the Android TV emulator
remote and the physical television remote.

## 6. Screen 01 — Onboarding

### Purpose

Introduce LumaPlay and guide users through the
initial media-access setup.

### Required Content

- LumaPlay branding
- Brief product introduction
- Storage access explanation
- Relevant permission requests
- Folder or storage selection where supported
- Initial media scan action
- Progress, success and error states

### Interaction Requirements

The user must be able to complete onboarding
using only the TV remote.

Do not request unnecessary permissions.

If the user denies access, explain the consequences
and offer an appropriate retry or alternative.

Do not require an internet connection or account.

## 7. Screen 02 — Home

### Purpose

Provide quick access to the user's media and
previous playback activity.

### Required Sections

- Featured or highlighted local media
- Continue Watching
- Recently Added Videos
- Recently Added Music, where shown in the design
- Library statistics
- Storage and scan status
- Manual rescan action

### Data Requirements

All media titles, artwork, progress indicators,
technical badges and library statistics must
come from actual indexed media or stored history.

Do not ship the sample movies or fictional storage
statistics shown in design mockups as real content.

### Interaction Requirements

Selecting a media card opens its appropriate
details screen or resumes playback according
to the approved design.

The Home screen must handle:

- An empty library
- A library scan in progress
- Missing artwork
- Unavailable external storage
- Media removed since the last scan

## 8. Screen 03 — Video Library

### Purpose

Allow users to browse indexed local videos.

### Required Content

- Video grid or approved library layout
- Media artwork or thumbnail
- Video title
- Available duration and technical metadata
- Loading and empty states

Implement any sorting, filtering or grouping
controls that are present in the approved design
and supported by the media repository.

### Interaction Requirements

- Navigate cards with the D-pad.
- Keep focused cards visible.
- Select a card to open Video Details.
- Restore focus when returning from details.
- Handle large libraries using lazy rendering.

## 9. Screen 04 — Video Details

### Purpose

Present information about the selected video
and provide playback actions.

### Required Content

- Title
- Artwork or backdrop, where available
- Duration
- Resolution
- Video and audio codec information
- Available HDR metadata
- File size and accessible location
- Saved playback progress
- Play and Resume actions, as applicable

Only show metadata that is available.

Do not infer actual hardware decoding or
audio passthrough from file metadata.

### Interaction Requirements

The primary action must have an obvious focus state.

Back returns to the previous library or
navigation destination.

## 10. Screen 05 — Video Player

### Purpose

Provide immersive local video playback.

### Required Controls

- Play and pause
- Seek backward and forward
- Playback timeline
- Current position and duration
- Audio-track selection
- Subtitle-track selection
- Playback-speed selection
- Supported aspect-ratio options
- Technical information overlay
- Exit or Back behavior

### Player UI Behavior

Playback controls may hide after an appropriate
period of inactivity.

Remote interaction should reveal them again.

Focus must remain predictable while controls
appear or disappear.

Track-selection dialogs must display only
available tracks and supported options.

### Technical Overlay

Where available, show:

- Source video codec
- Source audio codec
- Resolution
- Frame rate
- HDR metadata
- Selected tracks
- Relevant playback diagnostics

Clearly distinguish source metadata from
confirmed device and output capabilities.

### Error States

Provide appropriate messages for:

- Missing file
- Unsupported format
- Playback failure
- Storage disconnection
- Unavailable audio or subtitle track

## 11. Screen 06 — Music Library

### Purpose

Allow users to discover and browse local music.

### Required Content

- Songs
- Albums
- Artists
- Available album artwork
- Track titles and artist information
- Duration and relevant metadata

Use the approved Stitch organization and layout.

Handle unknown artists, missing artwork and
incomplete metadata gracefully.

## 12. Screen 07 — Music Player

### Purpose

Provide a television-friendly music playback
experience.

### Required Content

- Current track
- Artist and album
- Available artwork
- Playback progress
- Play and pause
- Previous and next
- Queue
- Shuffle
- Repeat

### Interaction Requirements

All controls must be D-pad accessible.

The player must remain synchronized with
the actual Media3 playback state.

Music playback should continue appropriately
when navigating between library screens.

## 13. Screen 08 — Folder Browser

### Purpose

Allow users to browse accessible media folders.

### Required Content

- Current storage source
- Current folder
- Parent navigation
- Child folders
- Playable media files
- Relevant file information

### Interaction Requirements

Selecting a folder opens it.

Selecting a playable media file opens the
appropriate video or music player.

Back returns to the parent folder or previous
application destination as appropriate.

Display clear states for inaccessible folders
and disconnected USB storage.

## 14. Screen 09 — Universal Search

### Purpose

Search the user's indexed local media library.

### Searchable Content

- Videos
- Songs
- Artists
- Albums
- Folders
- Available filenames

### Required UI

- Search field
- Microphone action
- Search results
- Appropriate result categories
- Empty-query state
- No-results state
- Search failure state, where applicable

Search must remain usable with a standard
TV remote.

Text search must function offline.

## 15. Screen 10 — Voice Search

Voice search is an extension of Universal Search,
not a separate media library.

### Required UI States

1. Search screen with microphone action
2. Active listening overlay
3. Recognized query and matching results
4. Permission, unavailable-service or error state

### Interaction Requirements

- Select the microphone action.
- Request microphone permission if required.
- Show an active listening indicator.
- Display recognized text where supported.
- Populate the existing search query.
- Search the local media index.
- Provide keyboard input as a fallback.

The dedicated Assistant button on the physical
TV remote is not assumed to be available to LumaPlay.

Voice recognition may depend on an installed
recognition service and may require internet access.

## 16. Screen 11 — Settings

### Required Categories

- Library and scanning
- Video playback
- Audio preferences
- Subtitle appearance
- Device compatibility and diagnostics
- About LumaPlay

Follow the approved Stitch designs for the
exact presentation of these categories.

### Interaction Requirements

- All controls must be D-pad accessible.
- Changes must be reflected in application state.
- Persist supported preferences using DataStore.
- Explain unavailable device-specific settings.
- Avoid presenting unsupported options as functional.

## 17. Device Compatibility Screen

The compatibility screen shall present detected
device and media capabilities.

Possible information includes:

- Android version
- Available video decoders
- Available audio decoders
- Reported HDR capabilities
- Relevant audio output capabilities

Capability labels must identify whether they refer
to source media, reported hardware support or
verified active playback.

The app must not claim that Dolby Atmos, Dolby Vision,
HDR10+ or a particular passthrough mode is active
without appropriate evidence.

## 18. Media Scanning UI

Provide UI states for:

- Not scanned
- Scan in progress
- Scan completed
- No supported media found
- Permission required
- Storage disconnected
- Scan failed

A scan indicator must reflect the actual scanner state.

If measurable progress is unavailable, display
an indeterminate progress indicator.

The user must be able to navigate the interface
without the scan blocking the main thread.

## 19. Empty and Error States

Design and implement explicit states for:

- Empty video library
- Empty music library
- Empty folder
- No search results
- Missing file
- Unsupported codec
- Permission denied
- USB drive removed
- Playback failure
- Voice recognition unavailable

Each state should explain what happened and,
where possible, provide a relevant recovery action.

Do not show fictional sample content in place
of an empty real library.

## 20. Loading and Animation

Use subtle transitions that support navigation
without distracting from media content.

Appropriate animation targets include:

- Focus changes
- Sidebar expansion
- Screen transitions
- Loading indicators
- Playback-control visibility
- Voice listening indicators

Avoid excessive animations and unnecessary
recomposition.

Respect reduced-motion accessibility settings
where supported.

## 21. Performance Requirements

The UI must remain responsive during:

- Media scanning
- Thumbnail generation
- Metadata extraction
- Search
- Database updates

Use lazy lists and grids for large libraries.

Load artwork asynchronously with bounded caching.

Avoid loading all media artwork into memory.

Optimize for the primary Android TV's limited RAM.

## 22. Accessibility

- Provide readable text contrast.
- Use sufficiently large focusable targets.
- Maintain visible focus.
- Use accessible labels for icon-only actions.
- Avoid relying on color alone to convey state.
- Ensure important dialogs are remote-accessible.
- Keep essential controls within television-safe areas.

## 23. Design-to-Code Implementation Rules

For each screen:

1. Locate the corresponding approved Stitch design.
2. Inspect screenshots and exported assets.
3. Identify reusable visual components.
4. Implement the layout using native Compose for TV.
5. Connect the screen to its ViewModel.
6. Connect the ViewModel to real repositories.
7. Implement loading, empty and error states.
8. Test D-pad navigation.
9. Compare the running screen against the design.
10. Build and test before proceeding.

Do not paste exported HTML or CSS directly
into the native Android application.

Do not introduce a WebView merely to reproduce
the Stitch design.

## 24. UI Acceptance Criteria

A screen is complete only when:

- Its layout follows the approved design.
- All essential actions work using the TV remote.
- Focus behavior is predictable.
- Real application state is displayed correctly.
- Loading, empty and error states are handled.
- Navigation and Back behavior work.
- The screen performs acceptably on the emulator.
- Relevant behavior is tested on the physical TV.

Visual similarity alone does not constitute
functional completion.

## 25. Design Change Control

If implementation requires changing an approved
design, record:

- The affected screen
- The proposed change
- The reason for the change
- Any affected user workflows
- The approval status

Do not silently remove an approved feature
because it is difficult to implement.
