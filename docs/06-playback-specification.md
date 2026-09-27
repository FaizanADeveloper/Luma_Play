
# LumaPlay — Playback Specification

| Field | Details |
|---|---|
| Project | LumaPlay |
| Application ID | `com.lumaplay.tv` |
| Platform | Android TV |
| Minimum SDK | Android 11 (API 30) |
| Playback Engine | AndroidX Media3 ExoPlayer |
| Media Integration | Media3 MediaSession |
| Primary Display | Redmi Smart TV X50, 4K UHD |
| Audio Connection | HDMI ARC |
| Document Version | 1.0 |
| Status | Development specification |

## 1. Purpose

This document defines LumaPlay's video and audio
playback behavior, supported-format strategy,
playback controls, subtitle handling, resume
functionality and device capability diagnostics.

LumaPlay is an offline-first Android TV media player.

Playback must use accessible local media files
discovered through MediaStore or the Storage
Access Framework.

The playback engine must not assume that a file
is playable solely because its extension or
metadata identifies a particular format.

## 2. Playback Objectives

LumaPlay shall:

- Provide reliable local video playback.
- Provide reliable local music playback.
- Support 4K UHD video where hardware permits.
- Support available HDR formats.
- Support multichannel audio where available.
- Support compatible audio passthrough.
- Provide remote-friendly playback controls.
- Allow audio and subtitle track selection.
- Preserve video playback progress.
- Handle unsupported media gracefully.
- Display accurate technical information.
- Manage playback resources efficiently.

The app must remain usable when a particular
file, codec or audio output mode is unsupported.

## 3. Playback Technology

### 3.1 AndroidX Media3 ExoPlayer

Use AndroidX Media3 ExoPlayer as the primary
playback engine.

Media3 shall manage:

- Media loading
- Container parsing
- Track discovery
- Audio and video decoding
- Rendering
- Seeking
- Playback speed
- Track selection
- Playback state
- Playback errors

Use compatible stable Media3 dependencies.

Keep Media3 component versions aligned.

### 3.2 MediaSession

Use Media3 MediaSession for appropriate
Android media integration.

MediaSession responsibilities include:

- Media-button handling
- Playback-state exposure
- Supported transport controls
- System media integration

Use MediaSessionService where appropriate
for background music playback.

The application must not create duplicate
unmanaged players during screen recomposition.

## 4. Playback Architecture

The playback subsystem shall contain:

### PlaybackService

Manages the MediaSession and the player
lifecycle where service-based playback
is required.

### PlaybackController

Provides playback operations to the
presentation layer.

### PlaybackState

Represents observable player state,
including:

- Idle
- Preparing
- Buffering
- Ready
- Playing
- Paused
- Ended
- Error

### PlaybackProgressRepository

Stores and retrieves video playback progress.

### CapabilityDetector

Queries relevant Android media capabilities.

### PlaybackDiagnostics

Collects available technical information
about the active media and playback session.

The Compose UI must not directly own the
ExoPlayer lifecycle.

## 5. Media Loading

Load media using Android content URIs.

Supported sources include:

- MediaStore content URIs
- Authorized SAF document URIs
- Other approved local content providers

Verify that access is still available
before or during playback.

Do not assume a file path is permanently
valid after a USB drive is disconnected
and reconnected.

If media becomes inaccessible, report
the failure and return to a usable state.

## 6. Video Container Support

The initial test set should include
commonly encountered containers:

- MP4
- MKV
- WebM
- MPEG-TS, where supported

Additional containers may be supported
by compatible Media3 extractors.

Container support does not guarantee
support for every codec contained inside.

Do not advertise a container as universally
supported without testing representative files.

## 7. Video Codec Strategy

The initial codec test matrix includes:

| Codec | Implementation strategy |
|---|---|
| H.264 / AVC | Use available Android decoding |
| H.265 / HEVC | Use available Android decoding |
| VP9 | Use available Android decoding |
| AV1 | Use available Android decoding |

Hardware decoding should be preferred
when available and suitable.

Actual decoding support depends on
the Android device and firmware.

Do not promise software decoding
of demanding 4K formats on limited
Android TV hardware.

A codec capability report is not proof
that every profile, level, bit depth
or resolution is supported.

## 8. 4K UHD Playback

LumaPlay shall support 4K UHD video
where the device's decoding and
rendering pipeline permits it.

Target resolution:

3840 × 2160

The approved Google Stitch interface
uses a 1920 × 1080 reference canvas.

This does not impose a 1080p limit
on video playback.

UI rendering resolution and actual
video output resolution are separate.

The player must avoid unnecessary
software processing that prevents
supported high-resolution playback.

Test 4K playback on the physical
Redmi Smart TV X50.

## 9. HDR Playback

The initial HDR test matrix includes:

- HDR10
- HDR10+
- HLG
- Dolby Vision

Playback depends on:

- Source encoding
- Supported container and profile
- Android decoder availability
- Media3 playback support
- Display capabilities
- Device firmware

Do not assume that advertised television
HDR support guarantees playback of
every file containing that HDR format.

Where available, inspect the source's
color metadata and the device's
reported display capabilities.

If HDR playback is unsupported,
use a compatible fallback only
when the playback pipeline provides one.

Do not promise automatic HDR-to-SDR
tone mapping for every device.

## 10. Video Player Controls

The player shall provide:

- Play
- Pause
- Seek backward
- Seek forward
- Playback timeline
- Current position
- Total duration
- Audio-track selection
- Subtitle-track selection
- Playback-speed selection
- Supported aspect-ratio options
- Technical information overlay
- Exit or Back action

Controls must be accessible using
a standard Android TV remote.

The exact visual arrangement shall
follow the approved Stitch design.

## 11. D-pad Playback Behavior

The player shall support predictable
remote navigation.

Required behavior:

- Select activates the focused control.
- Directional buttons navigate visible controls.
- Back dismisses an active overlay first.
- Back exits playback when no dismissible
  overlay remains.
- Supported media keys control playback.

Seek behavior must be consistent
and must not conflict with navigation
inside the visible control panel.

Do not rely on touch gestures.

## 12. Playback Control Visibility

Playback controls may automatically
hide after a suitable period of inactivity.

Remote interaction shall reveal
the controls when appropriate.

The selected control must retain
predictable focus while overlays
appear or disappear.

Dialogs for tracks, speed and
aspect ratio must remain usable
with the television remote.

## 13. Seeking

Provide backward and forward seeking.

The exact seek interval shall be
configurable or finalized during
implementation.

Seeking must respect the media's
seekability and available duration.

Live or unseekable sources are outside
the initial local-media requirements.

The timeline must not falsely imply
that seeking is available when it is not.

## 14. Playback Resume

LumaPlay shall remember video
playback progress.

Save progress:

- Periodically during playback
- When playback pauses
- When leaving the player
- At appropriate lifecycle transitions

Avoid database writes on every
position update.

When reopening a partially watched
video, offer Resume where appropriate.

The completion threshold and
resume behavior near the end
of a video shall be finalized
during implementation.

Playback history must survive
normal application restarts.

## 15. Audio Track Selection

The player shall detect available
audio tracks in the selected media.

Where available, display:

- Language
- Codec
- Channel information
- Track label
- Other useful metadata

Users shall be able to select
a supported audio track.

Do not display nonexistent tracks.

If the selected track cannot play,
provide an understandable error
or suitable fallback.

## 16. Audio Codec Strategy

The initial audio test matrix includes:

- AAC
- MP3
- FLAC
- PCM
- AC-3 / Dolby Digital
- E-AC-3 / Dolby Digital Plus
- Dolby TrueHD
- DTS-family formats

The presence of a format in this
test matrix does not guarantee
decoding or passthrough support.

Support depends on Android,
the available decoders, Media3
and the connected audio equipment.

Unsupported formats must be
reported accurately.

## 17. Dolby Atmos

Dolby Atmos may be carried in
different audio formats.

Relevant examples include:

- Dolby Digital Plus with Atmos
- Dolby TrueHD with Atmos

These formats have different
transport requirements.

LumaPlay must distinguish:

1. Source file contains Atmos metadata.
2. Android reports compatible audio capabilities.
3. Playback uses a supported audio path.
4. Connected equipment actually receives
   an Atmos-compatible signal.

Do not infer item 4 solely from item 1.

## 18. HDMI ARC Audio Configuration

Primary physical test setup:

Redmi Smart TV X50
        |
        | HDMI ARC
        v
Zebronics ZEB-JUKE BAR 9800 DWS PRO

The application shall use Android's
available audio output capabilities.

Dolby Digital Plus Atmos may work
over HDMI ARC when supported
throughout the playback chain.

Conventional HDMI ARC does not
support lossless Dolby TrueHD
Atmos passthrough.

Do not advertise lossless TrueHD
Atmos output for this connection.

The soundbar's displayed audio
format, where available, should
be used during physical testing
to corroborate actual output.

## 19. Audio Passthrough

Provide passthrough behavior only
through supported Android and
Media3 audio output mechanisms.

Where appropriate, the app may
expose an audio output preference.

Possible preference states:

- Automatic
- PCM or decoded output
- Passthrough where supported

The exact available controls
must be determined during
implementation and device testing.

Do not create a setting that
claims to force unsupported
passthrough formats.

If passthrough fails, use an
appropriate supported fallback
where available.

## 20. DTS Support

DTS, DTS-HD and DTS:X must
not be treated as interchangeable.

Decoding and passthrough support
must be evaluated separately.

Do not assume that the television
or connected soundbar supports
every DTS-family format.

If a DTS track is unsupported,
report the limitation and offer
another available audio track
where appropriate.

## 21. Subtitle Support

The player shall support available
embedded subtitle tracks.

The initial external subtitle
target is SubRip (.srt).

Additional formats may be added
after compatibility testing.

Where supported, users shall
be able to:

- Enable subtitles
- Disable subtitles
- Select an available subtitle track
- Choose a subtitle language
- Configure supported appearance options

Subtitle styling must respect
renderer limitations.

Do not promise that every embedded
subtitle format supports arbitrary
font, color or positioning changes.

## 22. External Subtitles

For supported local video files,
LumaPlay may discover compatible
external subtitle files.

Initial matching strategy:

- Same authorized folder
- Matching or related filename
- Supported subtitle extension

External subtitle discovery must
respect Android storage permissions.

Do not assume unrestricted access
to sibling files when a single
document URI is provided.

Handle missing and malformed
subtitle files gracefully.

## 23. Playback Speed

Provide playback-speed controls
where supported by the active
playback configuration.

Potential speeds include:

- 0.5x
- 0.75x
- 1.0x
- 1.25x
- 1.5x
- 2.0x

Only expose values that are
supported and behave acceptably
on the target device.

Default speed:

1.0x

## 24. Aspect Ratio and Scaling

Provide supported video
presentation modes.

Potential modes include:

- Fit
- Fill
- Zoom
- Original aspect ratio,
  where technically meaningful

The exact available modes
depend on the video renderer
and approved player design.

Do not distort video by default.

Avoid unnecessary cropping
unless the user explicitly
selects a cropping mode.

## 25. Music Playback

LumaPlay shall provide a
dedicated music player.

Required functionality:

- Play and pause
- Previous and next
- Playback progress
- Track information
- Available album artwork
- Playback queue
- Shuffle
- Repeat
- MediaSession integration

Music playback should continue
appropriately while navigating
between application screens.

Background playback must comply
with Android's media service
and foreground-service requirements.

## 26. Playback Queue

The music player shall maintain
an ordered playback queue.

Required operations:

- Play selected track
- Advance to next track
- Return to previous track
- Shuffle queue
- Repeat current track
- Repeat queue

The exact queue-editing interface
shall follow the approved design
or require separate approval
if no design exists.

## 27. Technical Information Overlay

The video player shall provide
a technical information overlay.

Display available information
such as:

- Container
- Source video codec
- Source audio codec
- Resolution
- Frame rate
- Available HDR metadata
- Selected audio track
- Selected subtitle track
- Playback position
- Relevant decoder information

Clearly distinguish source metadata
from actual device capabilities.

If active audio output cannot
be verified, show an unknown
or unavailable status.

Do not hardcode Dolby Atmos,
Dolby Vision or HDR badges.

## 28. Playback Error Handling

Handle at least:

- File not found
- Permission revoked
- USB storage disconnected
- Unsupported container
- Unsupported codec
- Decoder initialization failure
- Audio output failure
- Subtitle loading failure
- Corrupted or incomplete media
- Unexpected playback interruption

Errors shall be logged for
development diagnostics.

User-facing messages must be
clear and should offer a
recovery action where possible.

Do not display raw stack traces
to ordinary users.

## 29. Playback Lifecycle

The player must release
unnecessary resources when
they are no longer needed.

Coordinate playback with:

- Activity lifecycle
- MediaSession lifecycle
- Service lifecycle
- Audio focus
- Device sleep and wake behavior

Handle audio focus changes
appropriately.

Do not leak players,
surfaces or Activity references.

## 30. Performance Requirements

Playback must be designed
for Android TV hardware
with limited RAM.

Requirements:

- Prefer suitable hardware decoding.
- Avoid unnecessary frame copies.
- Avoid decoding oversized artwork.
- Use bounded caches.
- Keep player UI recomposition controlled.
- Avoid unnecessary simultaneous players.
- Release unused video surfaces.
- Keep library scans from degrading playback.

Do not promise that every
high-bitrate 4K file will
play smoothly on every device.

## 31. Playback Test Matrix

### PB-001 — H.264 Video

Play a supported H.264
video file.

Expected:
Video and audio play correctly.

### PB-002 — HEVC 4K Video

Play a representative
4K HEVC file.

Expected:
Playback succeeds if supported,
or produces a clear error.

### PB-003 — HDR Video

Play representative HDR10,
HDR10+, HLG and Dolby Vision
files where available.

Expected:
Actual supported formats work.
Unsupported formats fail gracefully.

### PB-004 — Seeking

Seek forward and backward
using the remote.

Expected:
Playback resumes at the
requested valid position.

### PB-005 — Resume

Play part of a video,
exit and reopen it.

Expected:
Saved progress is restored.

### PB-006 — Audio Tracks

Open a file containing
multiple audio tracks.

Expected:
Available tracks are listed
and supported tracks can
be selected.

### PB-007 — Embedded Subtitles

Open a file containing
supported embedded subtitles.

Expected:
Subtitles can be enabled,
disabled and selected.

### PB-008 — External SRT

Open a video with an
accessible matching SRT file.

Expected:
The subtitle file can
be loaded when supported.

### PB-009 — Dolby Digital Plus

Play a compatible E-AC-3
test file through HDMI ARC.

Expected:
Playback succeeds if the
complete output path supports it.

Verify the actual output
using available device
and soundbar indicators.

### PB-010 — Dolby Atmos

Play a representative
Dolby Digital Plus Atmos file.

Expected:
Do not claim Atmos output
without appropriate verification.

### PB-011 — Unsupported Codec

Attempt to play a file
using an unsupported codec.

Expected:
A clear error is displayed
without crashing the app.

### PB-012 — USB Disconnection

Disconnect external storage
during playback.

Expected:
Playback fails gracefully
and the UI remains usable.

### PB-013 — Music Controls

Test play, pause, next,
previous, shuffle and repeat.

Expected:
All supported actions
behave consistently.

### PB-014 — Long Playback

Play a supported long-duration
video on the physical TV.

Expected:
No reproducible crash,
resource leak or uncontrolled
memory growth.

### PB-015 — D-pad Navigation

Navigate all player controls
using the physical TV remote.

Expected:
All essential controls
are reachable.

## 32. Acceptance Criteria

The playback subsystem is
considered complete when:

1. Supported local videos play reliably.

2. Supported local music plays reliably.

3. Playback controls work using
   the television remote.

4. Video resume positions persist.

5. Available audio and subtitle
   tracks can be selected.

6. Supported 4K and HDR content
   is tested on physical hardware.

7. HDMI ARC audio behavior
   is tested on the connected
   soundbar.

8. Unsupported formats and
   disconnected storage are
   handled gracefully.

9. The technical overlay reports
   actual available information.

10. Relevant automated and
    physical-device tests pass.

## 33. Implementation Boundaries

LumaPlay does not include:

- Screen mirroring
- AirPlay receiving
- Chromecast receiving
- Cloud streaming services
- DRM bypass
- Unsupported codec guarantees

Casting and screen mirroring
belong to the separate future
LumaCast project.

Do not add these features
during LumaPlay development.

## 34. Open Implementation Decisions

The following decisions
require validation or approval:

- Exact seek intervals
- Playback completion threshold
- Final external subtitle formats
- Available aspect-ratio modes
- Audio passthrough settings
- Detailed music queue editing
- Device-specific HDR behavior
- Verified soundbar audio output

Do not silently invent
product requirements.

Document approved decisions
before implementing them.
