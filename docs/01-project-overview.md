# LumaPlay — Project Overview

**Project:** LumaPlay
**Application ID:** com.lumaplay.tv
**Platform:** Android TV
**Minimum Android Version:** Android 11 (API 30)
**Status:** Initial development
**Document Version:** 1.0

## 1. Product Vision

LumaPlay is an offline-first Android TV media player
designed to provide a cinematic experience for watching
locally stored videos and listening to music.

The application will allow users to discover, organize,
search and play media stored on their Android TV or
accessible external USB storage.

LumaPlay will feature a modern, remote-friendly
interface based on the approved Google Stitch designs.

## 2. Project Objectives

- Build a native Android TV application.
- Provide reliable local video and music playback.
- Discover media from accessible internal and USB storage.
- Support navigation using a television remote.
- Provide advanced playback controls and media metadata.
- Support HDR and multichannel audio where the device
  and connected equipment permit them.
- Maintain playback history and resume positions.
- Work without an internet connection for core features.
- Provide an interface optimized for television screens.

## 3. Target Platform

Initial development targets Android TV devices running
Android 11 or newer.

Primary physical test device:
- Redmi Smart TV X50
- Android 11
- 4K UHD display

Audio test configuration:
- Zebronics ZEB-JUKE BAR 9800 DWS PRO
- Connected through HDMI ARC

The Android TV emulator will be used for development
and initial testing. Actual codec, HDR and audio-output
capabilities must be validated on physical hardware.

## 4. Core Features

### 4.1 Home Dashboard
- Featured media
- Continue Watching
- Recently Added Videos
- Recently Added Music
- Library statistics
- Storage and scanning status

### 4.2 Video Library
- Browse indexed videos
- Display thumbnails and available metadata
- Open video details
- Start or resume playback

### 4.3 Video Player
- Play, pause and seek
- Resume playback
- Select available audio and subtitle tracks
- Adjust playback speed
- Display technical playback information
- Support available codecs and HDR formats

### 4.4 Music Library and Player
- Browse songs, albums and artists
- Display available artwork and metadata
- Play local audio
- Manage playback queue
- Shuffle and repeat

### 4.5 Folder Browser
- Browse accessible internal and external media folders
- Open playable files
- Handle disconnected storage safely

### 4.6 Universal Search
- Search videos, songs, artists, albums and folders
- Support remote-friendly text input
- Provide voice search where supported

### 4.7 Settings and Diagnostics
- Library scanning preferences
- Playback preferences
- Subtitle appearance
- Device capability information
- Application information

## 5. Technology Stack

- Kotlin
- Jetpack Compose for TV
- Material 3 for TV
- AndroidX Media3 ExoPlayer
- Media3 MediaSession
- Room
- DataStore
- MediaStore
- Storage Access Framework
- Kotlin Coroutines and Flow

Architecture: MVVM with repositories.

## 6. User Interface

The approved Google Stitch designs are the primary
visual reference.

Design location:
designs/stitch/LumaPlay_UI_UX/

The interface uses a dark cinematic theme, blue/cyan
accents, media artwork, a persistent navigation sidebar
and clear D-pad focus indicators.

All essential functionality must be accessible using
a standard Android TV remote.

## 7. Project Scope

Included:
- Local video and music playback
- Internal and accessible USB media discovery
- Media libraries and folder navigation
- Playback history and resume
- Universal and voice search
- Playback settings and device diagnostics

Excluded:
- Screen mirroring
- AirPlay receiving
- Chromecast receiving
- Laptop or mobile screen casting
- Cloud streaming services

Casting and screen mirroring belong to a separate
future application named LumaCast.

## 8. Development Strategy

Development will proceed through approved milestones.

Each milestone must include implementation, a successful
build, relevant tests, a review of changed files and
a Git commit.

The Android TV emulator will be used throughout
development, with regular testing on the physical TV.

## 9. Success Criteria

LumaPlay is ready for its initial release when its
approved features are implemented, essential remote
navigation works, supported local media plays reliably,
errors are handled appropriately and physical-device
acceptance tests pass.

The application must not claim support for a codec,
HDR format or audio-output mode solely because the
source file contains that format.
