
# LumaPlay — AI Development Agent Instructions

## 1. Project Identity

- Name: LumaPlay
- Application ID: `com.lumaplay.tv`
- Platform: Android TV
- Minimum SDK: Android 11 (API 30)
- Language: Kotlin
- UI: Jetpack Compose for TV
- Architecture: MVVM with repositories
- Playback engine: AndroidX Media3 ExoPlayer

LumaPlay is an offline-first local video and music player
for Android TV.

The existing Android Studio project has already been
created, built and launched successfully.

Preserve the existing project.

## 2. Mandatory Documentation

Before proposing implementation changes, read:

1. `docs/01-project-overview.md`
2. `docs/02-requirements-srs.md`
3. `docs/03-technical-architecture.md`
4. `docs/04-ui-ux-specification.md`
5. `docs/05-data-and-storage.md`
6. `docs/06-playback-specification.md`
7. `docs/07-search-and-settings.md`
8. `docs/08-testing-plan.md`
9. `docs/09-development-roadmap.md`

Also inspect:

- `README.md`
- `AGENTS.md`
- `designs/stitch/LumaPlay_UI_UX/`

Inspect the actual design exports before implementing
the corresponding screens.

Do not invent missing design details.

If documentation conflicts with existing code or
another specification, report the conflict and
request clarification before making major changes.

## 3. Initial Task — Read-Only Audit

The first development task is a read-only project audit.

Inspect the project structure, source files,
Gradle configuration, documentation and designs.

Produce a report containing:

- Current project structure
- Existing dependencies
- Current implementation status
- Documentation or architecture conflicts
- Missing information
- Proposed Phase 0 and Phase 1 plans
- Files that would need modification
- Dependencies that would need installation
- Terminal commands that would be required
- Risks and recommended safeguards

Do not edit files or execute commands during
the initial audit without explicit approval.

Wait for the user to approve the proposed plan.

## 4. Development Scope

Implement only approved LumaPlay features.

The initial scope includes:

- Local media discovery
- Video and music libraries
- Folder browsing
- Video and music playback
- Supported advanced playback features
- Universal offline search
- Optional voice search
- Settings
- Device diagnostics

Do not implement casting, screen mirroring,
AirPlay receiving, Chromecast receiving
or unrelated cloud features.

Casting and mirroring belong to the
separate future LumaCast project.

## 5. Incremental Implementation

Follow the approved development roadmap.

Work on one milestone at a time.

For each milestone:

1. Inspect the existing implementation.
2. Identify relevant requirements and designs.
3. Propose a scoped plan.
4. Identify affected files and dependencies.
5. Request approval where required.
6. Implement the approved changes.
7. Build the application.
8. Run relevant tests.
9. Report results and limitations.
10. Present changes for review.

Do not automatically proceed to the next
milestone without user approval.

## 6. File Safety

The project and its surrounding directories
may contain important files.

Never delete, move, overwrite or reset
unrelated files or directories.

Do not recreate the Android Studio project.

Do not replace the entire source tree to
implement a single feature.

Do not modify files outside the LumaPlay
project without explicit approval.

Preserve the original Stitch design exports.

Do not modify or delete original user media
during normal scanning or playback.

## 7. Terminal Command Safety

Before proposing a terminal command:

- Explain its purpose.
- Identify its working directory.
- Check the exact command and arguments.
- Identify files it may modify.
- Identify whether it requires elevated privileges.
- Explain any meaningful risk.

Prefer narrowly scoped, non-destructive commands.

Request one-time approval for commands when
the environment requires permission.

Never recommend blanket or "always allow"
terminal permissions by default.

Commands involving recursive deletion,
force checkout, hard reset, bulk replacement,
filesystem cleanup or system changes
require explicit review.

Do not execute destructive commands merely
to resolve a build or dependency problem.

## 8. Git and Backup Safety

Before substantial code modifications,
verify that the project has a recoverable
Git checkpoint and an independent backup.

Do not assume Git alone is sufficient backup.

Do not execute the following without
explicit approval:

- `git reset --hard`
- `git clean -fd`
- Force pushes
- Branch deletion
- Destructive checkout operations

Do not overwrite uncommitted user changes.

Do not commit credentials, signing keys,
local SDK paths or generated build outputs.

## 9. Dependency Management

Preserve the existing working Gradle setup.

Use stable, mutually compatible dependencies.

Do not unnecessarily upgrade:

- Kotlin
- Android Gradle Plugin
- Gradle
- Android SDK configuration

Keep related Media3 dependencies
on compatible versions.

Explain why a new dependency is needed
before adding it.

## 10. Architecture Rules

Use the approved architecture:

- Kotlin
- Jetpack Compose for TV
- MVVM
- Repositories
- Kotlin Coroutines and Flow
- Room
- DataStore
- MediaStore and SAF
- AndroidX Media3

Keep business logic out of Compose screens.

Manage player lifecycle outside ordinary
screen recomposition.

Perform storage scanning and expensive
metadata extraction off the main thread.

Avoid unnecessary multi-module complexity
during initial development.

## 11. UI and Accessibility

Use the approved Stitch exports
as the visual reference.

Implement native Android TV UI.

Do not use WebView or HTML/CSS
as a substitute for Compose screens.

All essential functionality must work
with a standard TV remote.

Provide:

- Visible focus indicators
- Predictable D-pad navigation
- Appropriate focus restoration
- Readable TV typography
- Loading, empty and error states

The 1920 × 1080 design canvas
does not limit video playback to 1080p.

The interface must scale appropriately
on the physical 4K television.

## 12. Storage and Privacy

Use Android's supported storage APIs.

Request only necessary permissions.

Use MediaStore and authorized SAF access.

Do not request unrestricted
all-files access by default.

Do not assume every USB drive
is automatically accessible.

Keep core media discovery and
text search offline.

Do not upload personal media
or library metadata.

## 13. Playback Accuracy

Use AndroidX Media3 ExoPlayer.

Support media formats according to
actual Android and device capabilities.

Do not guarantee universal support
for every codec, profile or container.

Distinguish between:

- Source media metadata
- Reported decoder capabilities
- Actual playback behavior
- Verified audio or video output

Do not hardcode Dolby Atmos,
Dolby Vision or HDR badges.

Conventional HDMI ARC cannot carry
lossless Dolby TrueHD Atmos.

Hardware-dependent claims require
physical-device verification.

## 14. Testing Requirements

A successful build does not prove
that a feature works.

Run appropriate:

- Unit tests
- Integration tests
- Compose UI tests
- Emulator tests
- Physical-device tests

Use `docs/08-testing-plan.md`
as the testing reference.

Never fabricate test results.

Clearly distinguish:

- Implemented
- Build verified
- Emulator tested
- Physical-device tested
- Not tested
- Blocked

Report failed tests and unresolved issues.

## 15. Reporting Format

After each approved implementation task,
provide a concise report containing:

1. Summary of changes
2. Files modified
3. Dependencies added or changed
4. Commands executed
5. Build result
6. Tests executed and results
7. Known limitations
8. Suggested next step

If a command or test was not executed,
state that explicitly.

Do not claim successful verification
without supporting evidence.

## 16. Stop Conditions

Stop and request user guidance when:

- A destructive operation appears necessary.
- Existing user changes could be lost.
- A major dependency upgrade is proposed.
- Requirements conflict materially.
- The project cannot be built without
  substantial configuration changes.
- An implementation would expand
  the approved project scope.
- A proposed action affects files
  outside the project.
- A significant hardware capability
  cannot be verified.

Protect the existing working project
over speculative implementation speed.
