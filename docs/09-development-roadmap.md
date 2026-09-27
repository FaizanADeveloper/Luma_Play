
# LumaPlay — Development Roadmap

| Field | Details |
|---|---|
| Project | LumaPlay |
| Application ID | `com.lumaplay.tv` |
| Platform | Android TV |
| Minimum SDK | Android 11 (API 30) |
| Language | Kotlin |
| UI | Jetpack Compose for TV |
| Architecture | MVVM + Repository |
| Playback | AndroidX Media3 |
| Document Version | 1.0 |
| Status | Implementation roadmap |

## 1. Purpose

This document defines the development sequence,
milestone deliverables, validation requirements
and source-control procedures for LumaPlay.

The application will be implemented incrementally.

Each milestone must produce a coherent,
buildable and testable result.

Do not attempt to implement the entire
application in a single development task.

## 2. Project Baseline

The Android Studio project has already
been created successfully.

Existing configuration:

- Project: LumaPlay
- Application ID: com.lumaplay.tv
- Minimum SDK: API 30
- Language: Kotlin
- Build system: Gradle Kotlin DSL
- Template: Empty Activity — Compose for TV

The initial application has been built
and launched on the Android TV emulator.

Preserve this working baseline.

Do not recreate the project or replace
its Gradle configuration unnecessarily.

## 3. Documentation Sources

Before implementation, review:

- 01-project-overview.md
- 02-requirements-srs.md
- 03-technical-architecture.md
- 04-ui-ux-specification.md
- 05-data-and-storage.md
- 06-playback-specification.md
- 07-search-and-settings.md
- 08-testing-plan.md
- 09-development-roadmap.md
- AGENTS.md
- README.md

Documentation directory:

docs/

Design directory:

designs/stitch/LumaPlay_UI_UX/

The actual approved Stitch exports
must be inspected before implementing
their corresponding screens.

Do not treat sample movie titles,
posters or technical badges in
design mockups as production data.

## 4. Development Principles

The development process shall follow
these principles:

1. Preserve the working project.
2. Implement one milestone at a time.
3. Review changes before accepting them.
4. Use compatible stable dependencies.
5. Keep the application buildable.
6. Test functionality, not only compilation.
7. Avoid unnecessary architecture complexity.
8. Use real media and storage APIs.
9. Protect original media files.
10. Commit verified progress to Git.

Do not silently remove approved
requirements or introduce unrelated features.

## 5. Phase 0 — Project Protection

### Objective

Establish a recoverable baseline before
allowing AI-assisted code modifications.

### Tasks

- Review the existing Android Studio project.
- Verify the application ID and minimum SDK.
- Confirm that the existing project builds.
- Confirm that the emulator launches the app.
- Review the project documentation.
- Verify the approved Stitch assets.
- Initialize Git if necessary.
- Configure an appropriate .gitignore.
- Create the initial baseline commit.
- Establish an independent project backup.

### Git Requirements

Use a private Git repository.

Exclude generated build outputs,
local SDK configuration, credentials
and signing keys.

Do not commit local.properties,
build caches or sensitive files.

### Acceptance Criteria

- Existing project builds successfully.
- The baseline is committed to Git.
- A separate recoverable backup exists.
- Required documentation is accessible.
- Original Stitch exports are preserved.

Do not begin substantial agent-generated
code changes before this phase is complete.

## 6. Phase 1 — UI Foundation

### Objective

Implement the core native Android TV
interface and navigation structure.

### Tasks

- Establish the LumaPlay theme.
- Define reusable design tokens.
- Implement the main application shell.
- Implement the navigation sidebar.
- Establish the Compose navigation graph.
- Implement reusable media cards.
- Implement shared focusable controls.
- Implement screen loading states.
- Implement empty-state components.
- Implement error-state components.
- Establish predictable D-pad focus behavior.

### Initial Screens

Create the basic native structure for:

- Home
- Videos
- Music
- Folders
- Search
- Settings

Use temporary development placeholders
only where real repositories have not
yet been implemented.

Clearly distinguish placeholders
from completed functionality.

Do not hardcode fictional media as
permanent production content.

### Deliverables

- Native television theme
- Working primary navigation
- Shared UI components
- Basic screen layouts
- D-pad focus behavior
- Initial screen-state architecture

### Acceptance Criteria

- Application builds successfully.
- All primary screens can be opened.
- Back navigation behaves predictably.
- Focus remains visible.
- Essential navigation works by remote.
- Layouts follow approved Stitch references.
- UI scales appropriately on the emulator.

### Milestone Gate

Build, test, review and commit
the UI foundation before proceeding.

## 7. Phase 2 — Storage and Media Index

### Objective

Implement real local media discovery
and persistent library storage.

### Tasks

- Implement Android-version-aware permissions.
- Implement MediaStore discovery.
- Implement supported SAF folder selection.
- Implement storage-source management.
- Create the Room database.
- Create media entities and DAOs.
- Implement media repositories.
- Implement asynchronous scanning.
- Implement scan progress reporting.
- Implement incremental database updates.
- Handle unavailable storage.
- Handle permission denial and revocation.

### Deliverables

- Functional media scanner
- Persistent Room media index
- Real video and audio library data
- Storage selection workflow
- Scan progress UI
- Storage error states

### Acceptance Criteria

- Accessible media can be discovered.
- Indexed media persists after app restart.
- Repeat scans avoid unnecessary duplicates.
- Scanning does not block navigation.
- Missing storage is handled safely.
- Original media files remain unchanged.

### Milestone Gate

Test with accessible sample media.

Build, review and commit the
storage subsystem before proceeding.

## 8. Phase 3 — Core Video Playback

### Objective

Implement reliable playback
of supported local video files.

### Tasks

- Add compatible Media3 dependencies.
- Implement the playback controller.
- Implement managed ExoPlayer lifecycle.
- Integrate MediaSession.
- Implement the native video player screen.
- Implement play and pause.
- Implement seeking.
- Implement playback timeline.
- Implement playback-state observation.
- Implement playback error handling.
- Implement Room playback history.
- Implement resume functionality.

### Deliverables

- Working video player
- Remote playback controls
- Playback progress persistence
- Resume functionality
- Clear playback error states

### Acceptance Criteria

- Supported sample videos play.
- Remote controls work.
- Seeking behaves correctly.
- Resume survives app restart.
- Missing files fail gracefully.
- Player resources are managed correctly.

### Milestone Gate

Build, execute playback tests,
review and commit.

Do not proceed solely because
the player screen renders.

## 9. Phase 4 — Music Playback

### Objective

Implement the local music
library and music player.

### Tasks

- Connect indexed audio to the music UI.
- Implement song browsing.
- Implement artist and album browsing.
- Implement the music player screen.
- Integrate Media3 playback.
- Implement queue management.
- Implement previous and next.
- Implement shuffle and repeat.
- Implement suitable MediaSession behavior.
- Implement appropriate background playback.
- Handle missing artwork and metadata.

### Deliverables

- Functional music library
- Native music player
- Playback queue
- Shuffle and repeat
- Appropriate media controls

### Acceptance Criteria

- Supported local music plays.
- Queue navigation works.
- Shuffle and repeat behave consistently.
- Missing metadata is handled.
- Playback lifecycle is reliable.

### Milestone Gate

Build, test, review and commit.

## 10. Phase 5 — Advanced Video Playback

### Objective

Implement advanced playback
controls and technical diagnostics.

### Tasks

- Implement audio-track selection.
- Implement subtitle-track selection.
- Implement supported external subtitles.
- Implement subtitle appearance settings.
- Implement playback-speed controls.
- Implement supported scaling modes.
- Implement the technical information overlay.
- Implement device capability detection.
- Improve playback error recovery.

### 4K and HDR

Test representative media using:

- H.264
- HEVC
- VP9
- AV1
- HDR10
- HDR10+
- HLG
- Dolby Vision

Support depends on the actual
device, codec profile, container
and media pipeline.

Do not claim universal support.

### Audio

Test representative audio formats,
including:

- AAC
- FLAC
- AC-3
- E-AC-3
- Dolby Digital Plus Atmos
- Dolby TrueHD
- DTS-family formats

Actual decoding and passthrough
capabilities must be distinguished.

### Deliverables

- Advanced player controls
- Track selection
- Supported subtitle features
- Technical diagnostics
- Device capability information

### Acceptance Criteria

- Available tracks can be selected.
- Supported subtitles render correctly.
- Technical information is accurate.
- Unsupported formats fail gracefully.
- Device capability claims are evidence-based.

### Milestone Gate

Build, test and commit.

Hardware-dependent functionality
remains unverified until physical testing.

## 11. Phase 6 — Universal Search

### Objective

Implement fast offline search
across indexed local media.

### Tasks

- Implement SearchRepository.
- Create suitable Room queries.
- Add appropriate search indexes.
- Implement SearchViewModel.
- Connect the approved Search UI.
- Implement video results.
- Implement music results.
- Implement artist and album results.
- Implement folder results.
- Implement loading and empty states.
- Implement result navigation.

### Deliverables

- Functional offline text search
- Categorized search results
- Remote-friendly search input
- Appropriate empty and error states

### Acceptance Criteria

- Indexed media is searchable.
- Partial title matching works.
- Search functions offline.
- Results open correct destinations.
- Rapid input does not freeze the UI.

### Milestone Gate

Build, test, review and commit.

## 12. Phase 7 — Voice Search

### Objective

Provide optional voice input
for universal media search.

### Tasks

- Implement microphone permission handling.
- Detect recognition service availability.
- Implement supported speech recognition.
- Implement the listening overlay.
- Display recognized text.
- Connect recognition to local search.
- Implement cancellation.
- Implement timeout and error handling.
- Preserve keyboard search as a fallback.

### Deliverables

- Functional microphone action
- Voice recognition where supported
- Listening UI
- Recognition error handling
- Keyboard fallback

### Acceptance Criteria

- Recognition works with a compatible service.
- Denied permission does not break text search.
- Recognition can be cancelled.
- No installed service is handled gracefully.
- No always-on microphone behavior is introduced.

### Milestone Gate

Build, test, review and commit.

## 13. Phase 8 — Settings and Diagnostics

### Objective

Complete persistent user preferences
and device capability reporting.

### Tasks

- Implement DataStore preferences.
- Complete library settings.
- Implement playback preferences.
- Implement supported audio preferences.
- Implement subtitle preferences.
- Complete device diagnostics.
- Implement the About screen.
- Connect settings to real functionality.
- Verify persistence after app restart.

### Deliverables

- Functional Settings screen
- Persistent preferences
- Library management controls
- Device diagnostics
- Application information

### Acceptance Criteria

- Implemented settings work.
- Preferences persist.
- Unsupported settings are not misrepresented.
- Library actions invoke real operations.
- Device information reflects actual platform data.

### Milestone Gate

Build, test, review and commit.

## 14. Phase 9 — Physical TV Validation

### Objective

Verify real-world behavior
on the primary television.

### Hardware

Primary display:

Redmi Smart TV X50
Android 11
4K UHD

Audio:

Zebronics ZEB-JUKE BAR 9800 DWS PRO
connected through HDMI ARC.

### Test Areas

- Application installation
- Actual remote navigation
- 4K UI appearance
- Compatible USB storage
- Large media libraries
- 4K video playback
- Supported HDR formats
- Available audio decoding
- HDMI ARC passthrough behavior
- Soundbar output indicators
- Long-duration playback
- Memory and performance

### Important Limitations

The 1080p emulator cannot establish
actual 4K or HDR output.

Source metadata cannot establish
actual Dolby Atmos output.

Conventional HDMI ARC cannot carry
lossless Dolby TrueHD Atmos.

Do not claim successful hardware
validation without real testing.

### Deliverables

- Physical test execution records
- Confirmed device capabilities
- Identified compatibility limitations
- Reproducible defect reports
- Relevant performance observations

### Acceptance Criteria

- Core workflows work on the physical TV.
- Supported test media plays correctly.
- Remote navigation is reliable.
- USB access is validated.
- Actual audio behavior is documented.
- Critical hardware-specific defects are addressed.

### Milestone Gate

Complete the required physical
tests before release approval.

## 15. Phase 10 — Optimization

### Objective

Improve performance, reliability
and resource management.

### Tasks

- Profile memory consumption.
- Profile expensive UI operations.
- Optimize media queries.
- Optimize thumbnail caching.
- Review player lifecycle.
- Review scanning concurrency.
- Eliminate unnecessary recomposition.
- Improve large-library scrolling.
- Investigate reproducible playback issues.
- Review application startup performance.

### Deliverables

- Improved responsiveness
- Controlled memory usage
- Stable player lifecycle
- Reduced unnecessary background work
- Documented performance observations

### Acceptance Criteria

- No known critical resource leaks.
- Large libraries remain navigable.
- Scanning does not unnecessarily interrupt playback.
- Repeated playback does not create duplicate players.
- Relevant regression tests pass.

## 16. Phase 11 — Release Preparation

### Objective

Produce a verified
initial LumaPlay release.

### Tasks

- Review all approved requirements.
- Complete mandatory test cases.
- Resolve release-blocking defects.
- Verify permissions and privacy behavior.
- Review application metadata.
- Update the application version.
- Review open-source license obligations.
- Produce a signed release build.
- Install the release build on the physical TV.
- Perform final regression testing.
- Document known limitations.

### Deliverables

- Verified release build
- Test execution report
- Known limitations
- Release notes
- Source-control release tag
- Independent project backup

### Acceptance Criteria

- Mandatory features are implemented.
- Required tests have passed.
- No known critical defects remain.
- Physical-device claims are verified.
- Release build installs successfully.
- Core local-media workflows function offline.

## 17. Definition of Done

A development task is complete only when:

1. The implementation meets its approved requirements.
2. Relevant code has been reviewed.
3. The application builds successfully.
4. Relevant automated tests pass.
5. Required manual tests are executed.
6. Loading and error states are handled.
7. D-pad behavior is verified where applicable.
8. Documentation is updated where necessary.
9. Changes are committed to Git.

A successful compilation alone
does not satisfy the Definition of Done.

## 18. Antigravity Development Workflow

For every milestone, Antigravity shall:

1. Read the relevant documentation.
2. Inspect the existing implementation.
3. Inspect the applicable Stitch designs.
4. Propose a scoped implementation plan.
5. Identify affected files.
6. Identify required dependencies.
7. Identify any terminal commands.
8. Wait for review before significant changes.
9. Implement the approved scope.
10. Build and execute relevant tests.
11. Report results and remaining limitations.
12. Present changes for review.

Do not automatically begin the next
milestone after completing the current one.

## 19. Terminal Command Safety

Agent-generated terminal commands
must be reviewed before execution.

Prefer narrowly scoped commands.

Use one-time approval for
individual commands when appropriate.

Do not grant blanket terminal
permissions by default.

Commands involving the following
require particular scrutiny:

- Recursive deletion
- Directory cleanup
- Project resets
- Force checkout
- Destructive Git operations
- Bulk file replacement
- Filesystem-wide modifications
- System configuration changes

Never run a destructive command
against unrelated directories.

Never assume that a command is safe
merely because it was generated
by a coding assistant.

## 20. Git Workflow

Use a private Git repository.

Maintain a stable working branch
and create feature branches
where useful.

Recommended commit checkpoints:

- Initial project baseline
- UI foundation
- Storage and media index
- Core video playback
- Music playback
- Advanced playback
- Universal search
- Voice search
- Settings and diagnostics
- Physical device fixes
- Release candidate

Commit messages should describe
the actual completed change.

Do not commit broken builds
as completed milestones.

## 21. Backup Strategy

Maintain at least one independent
backup outside the active project directory.

The Git repository is useful
for version control but must
not be the only recovery mechanism.

Preserve:

- Source code
- Documentation
- Approved design exports
- Relevant configuration
- Important test evidence

Do not place credentials
or signing secrets in
ordinary source-control backups.

Before major refactoring,
confirm that a recoverable
checkpoint exists.

## 22. Dependency Management

Use stable dependencies
compatible with the
existing Android project.

Do not upgrade Kotlin,
Gradle or the Android
Gradle Plugin unnecessarily.

Add dependencies only
when required by an
approved feature.

Keep related Media3
component versions aligned.

Document important
dependency changes.

## 23. Change Management

If a proposed implementation
conflicts with an approved
requirement or design:

1. Identify the conflict.
2. Explain the technical reason.
3. Propose available alternatives.
4. Identify affected documents.
5. Obtain approval.
6. Update the relevant specification.
7. Implement the approved decision.

Do not silently remove
difficult features.

Do not introduce casting,
mirroring or unrelated
product functionality.

## 24. Milestone Progress Tracking

Maintain a development
progress record.

Suggested fields:

| Milestone | Status | Build | Tests | Commit |
|---|---|---|---|---|
| Project protection | NOT STARTED | — | — | — |
| UI foundation | NOT STARTED | — | — | — |
| Storage and media index | NOT STARTED | — | — | — |
| Core video playback | NOT STARTED | — | — | — |
| Music playback | NOT STARTED | — | — | — |
| Advanced playback | NOT STARTED | — | — | — |
| Universal search | NOT STARTED | — | — | — |
| Voice search | NOT STARTED | — | — | — |
| Settings and diagnostics | NOT STARTED | — | — | — |
| Physical TV validation | NOT STARTED | — | — | — |
| Optimization | NOT STARTED | — | — | — |
| Release preparation | NOT STARTED | — | — | — |

Update this table
only when milestone
status changes.

Do not mark a milestone
complete without its
required verification.

## 25. Initial Antigravity Task

The first Antigravity task
must be read-only planning.

It shall:

- Inspect the existing Android project.
- Read all project documentation.
- Inspect the available Stitch exports.
- Identify existing dependencies.
- Verify the proposed architecture.
- Identify missing or conflicting requirements.
- Propose the Phase 0 and Phase 1 plans.
- Identify intended file changes.
- Identify any necessary terminal commands.

It must not:

- Recreate the project.
- Delete existing files.
- Replace Gradle configuration.
- Install unnecessary dependencies.
- Execute destructive commands.
- Implement the entire application.
- Begin coding before the plan is reviewed.

The initial output shall be
a clear implementation plan
for user approval.

## 26. Final Project Scope

LumaPlay is an offline-first
local video and music player
for Android TV.

Its approved scope includes:

- Local media discovery
- Video library
- Music library
- Folder browser
- Video playback
- Music playback
- Supported advanced playback
- Universal search
- Optional voice search
- Persistent settings
- Device diagnostics

Casting and screen mirroring
are explicitly excluded.

Those capabilities belong
to the separate future
LumaCast project.

Do not expand LumaPlay's
scope without approval.
