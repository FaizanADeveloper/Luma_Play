
# LumaPlay — Data and Storage Specification

| Field | Details |
|---|---|
| Project | LumaPlay |
| Application ID | `com.lumaplay.tv` |
| Platform | Android TV |
| Minimum SDK | Android 11 (API 30) |
| Database | Room |
| Preferences | DataStore |
| Storage APIs | MediaStore and Storage Access Framework |
| Document Version | 1.0 |
| Status | Development specification |

## 1. Purpose

This document defines how LumaPlay discovers,
indexes, accesses and manages local video and
audio files.

LumaPlay is an offline-first media player.

The application must support accessible internal
storage and compatible external USB storage
without modifying or deleting the user's files.

The storage architecture must account for Android
permissions, removable storage, missing files,
large libraries and limited device memory.

## 2. Storage Architecture

LumaPlay shall use three primary storage systems.

### 2.1 MediaStore

MediaStore is the primary discovery mechanism
for media indexed by Android.

It shall be used to discover accessible:

- Video files
- Audio files
- Relevant media metadata
- Content URIs

MediaStore access depends on Android permissions,
media indexing and device-specific behavior.

Do not assume that every file on a USB drive
automatically appears in MediaStore.

### 2.2 Storage Access Framework (SAF)

The Storage Access Framework provides access to
folders and documents selected by the user.

It shall be used where appropriate for:

- User-selected media folders
- Compatible external USB storage
- Locations not exposed through MediaStore

The application shall use Android's document
provider interfaces rather than assuming that
every selected folder has a conventional
filesystem path.

### 2.3 Room Database

Room shall maintain the application's persistent
media index and playback history.

The database stores metadata and references
to media files.

It must not store entire video or audio files.

## 3. Supported Storage Sources

The initial implementation shall support:

1. Accessible internal media storage.
2. Compatible USB storage exposed by Android.
3. User-selected folders accessible through SAF.

Network storage, SMB, FTP, WebDAV and cloud
media libraries are outside the initial scope.

A future network-storage feature would require
a separate requirements and security review.

## 4. Android Permissions

### 4.1 Android 11 and 12

For Android 11 and 12, request the applicable
READ_EXTERNAL_STORAGE permission when necessary
for MediaStore-based media access.

Permission requests must be triggered at an
appropriate point in the user workflow.

Do not request permissions unnecessarily
when the application first opens.

### 4.2 Android 13 and Later

On Android 13 and later, request applicable
media-specific permissions, including:

- READ_MEDIA_VIDEO
- READ_MEDIA_AUDIO

Request only permissions required by
implemented functionality.

Android version-specific permission handling
must be isolated in a dedicated component.

### 4.3 SAF Permissions

When a user selects a folder through SAF,
use the returned content URI.

Persist read permission when the returned
grant and Android API permit it.

Do not assume that access to one selected
folder grants access to its parent or
unrelated directories.

Some protected Android directories cannot
be selected through SAF.

### 4.4 Restricted Storage Access

Do not request MANAGE_EXTERNAL_STORAGE
by default.

If broad storage access becomes necessary,
document the exact use case and review
Android platform and distribution policies
before implementation.

### 4.5 Permission Denial

If a permission is denied:

- Explain why the feature needs access.
- Allow an appropriate retry.
- Offer SAF folder selection when suitable.
- Keep other accessible app features usable.
- Do not enter an endless permission loop.

## 5. Media Discovery Workflow

The scanning process shall follow this sequence.

### Stage 1 — Identify Sources

Identify accessible MediaStore collections
and user-authorized SAF locations.

Check whether previously selected storage
sources remain available.

### Stage 2 — Enumerate Media

Enumerate candidate video and audio files.

Avoid traversing directories that Android
does not authorize the application to access.

Use content URIs rather than relying on
absolute filesystem paths.

### Stage 3 — Extract Metadata

Extract available information such as:

- Title
- Display name
- MIME type
- Duration
- File size
- Date added
- Date modified
- Video resolution
- Video rotation
- Audio artist
- Audio album
- Available embedded artwork

Technical metadata extraction must be
performed asynchronously.

Expensive metadata extraction should not
block initial library discovery.

### Stage 4 — Update Database

Insert newly discovered media.

Update records whose source metadata
has changed.

Avoid unnecessary writes for unchanged files.

### Stage 5 — Reconcile Existing Records

Identify indexed media that was not found
during a successfully completed scan.

Distinguish between:

- Deleted files
- Temporarily unavailable storage
- Revoked permissions
- Incomplete or failed scans

Never permanently remove library records
solely because a removable drive was absent
during one scan.

### Stage 6 — Publish Results

Publish scan results to the UI.

Include:

- Scan status
- Discovered video count
- Discovered audio count
- Added and updated counts
- Unavailable media count
- Relevant errors

Display measurable progress when available.

Otherwise, display an indeterminate
progress indicator.

## 6. Scanner Requirements

The scanner must:

- Run off the main thread.
- Support cancellation.
- Avoid overlapping full-library scans.
- Process large collections incrementally.
- Avoid loading entire files into memory.
- Handle metadata extraction failures.
- Handle USB disconnection.
- Avoid unnecessary database operations.
- Preserve user files without modification.

The scanner shall expose its state
through Kotlin Flow or StateFlow.

## 7. Media Identity

A filename is not a reliable unique identifier.

Different folders may contain files
with identical names.

The database shall use an internal stable ID
for each indexed media record.

Store source-specific identity information,
such as:

- MediaStore volume and media identifier
- SAF document URI
- Storage source identifier

Where necessary, combine source identity
with available document metadata.

Do not assume that an external drive receives
the same Android mount path after reconnection.

## 8. Proposed Room Database

Database name:

`lumaplay_database`

The following entities represent the initial
logical schema.

The exact Kotlin types, indexes and foreign
keys shall be finalized during implementation.

### 8.1 MediaItemEntity

Stores the common information for indexed media.

Fields:

- id: Long
- sourceId: String
- sourceUri: String
- sourceType: String
- mediaType: String
- displayName: String
- title: String?
- mimeType: String?
- durationMs: Long?
- sizeBytes: Long?
- dateAdded: Long?
- dateModified: Long?
- parentFolderId: String?
- artworkUri: String?
- availabilityStatus: String
- lastIndexedAt: Long

Media types include VIDEO and AUDIO.

Source types distinguish MediaStore
from SAF-discovered content.

Store timestamps using one consistent
time representation.

### 8.2 VideoMetadataEntity

Stores video-specific information.

Fields:

- mediaId: Long
- width: Int?
- height: Int?
- rotationDegrees: Int?
- frameRate: Float?
- videoCodec: String?
- audioCodec: String?
- hdrFormat: String?
- bitrate: Long?

All technical metadata fields are nullable
because Android may not expose every value.

Source metadata must not be presented as
proof of actual decoding or output.

### 8.3 AudioMetadataEntity

Stores music-specific information.

Fields:

- mediaId: Long
- artist: String?
- album: String?
- albumArtist: String?
- trackNumber: Int?
- discNumber: Int?
- genre: String?
- embeddedArtworkUri: String?

Use suitable fallback labels for
unknown artists and albums.

### 8.4 StorageSourceEntity

Stores information about authorized
storage locations.

Fields:

- id: String
- sourceType: String
- displayName: String
- rootUri: String?
- persistedPermission: Boolean
- availabilityStatus: String
- lastSuccessfulScanAt: Long?
- lastAttemptedScanAt: Long?

Do not store a permission flag as proof
that access is still valid.

Verify access when the source is used.

### 8.5 PlaybackProgressEntity

Stores video playback progress.

Fields:

- mediaId: Long
- positionMs: Long
- durationMs: Long?
- lastPlayedAt: Long
- completed: Boolean

Playback progress shall be linked
to the indexed media item.

### 8.6 ScanRecordEntity

Stores scan history and diagnostic information.

Fields:

- id: Long
- sourceId: String?
- startedAt: Long
- completedAt: Long?
- status: String
- videosDiscovered: Int
- audioDiscovered: Int
- itemsAdded: Int
- itemsUpdated: Int
- errorSummary: String?

Do not store excessively large diagnostic
logs inside the database.

## 9. Database Relationships

MediaItemEntity is the central media record.

VideoMetadataEntity and AudioMetadataEntity
reference their corresponding media item.

PlaybackProgressEntity references
the corresponding media item.

StorageSourceEntity identifies the
authorized source associated with media.

Use foreign keys where appropriate.

Create indexes for frequently queried fields,
including media type, source identity,
date added and searchable metadata.

## 10. Repository Interfaces

Provide repositories for the following concerns.

### MediaRepository

Responsibilities:

- Observe video library
- Observe music library
- Retrieve media details
- Retrieve recently added media
- Check media availability
- Reconcile indexed media

### StorageRepository

Responsibilities:

- Discover accessible sources
- Manage selected folder URIs
- Verify source access
- Report source availability

### ScanRepository

Responsibilities:

- Start scanning
- Cancel scanning
- Observe scan state
- Retrieve scan history

### PlaybackHistoryRepository

Responsibilities:

- Save playback position
- Retrieve resume position
- Mark media completed
- Retrieve Continue Watching entries

Keep Android storage APIs out of
Compose screen implementations.

## 11. Library Queries

The media repository must support
efficient queries for:

- All videos
- Recently added videos
- All songs
- Albums
- Artists
- Media within a folder
- Continue Watching
- Media by storage source
- Local search results

Use lazy loading or paging where appropriate.

Do not load an entire large library
into a single UI collection unnecessarily.

## 12. Search Indexing

Searchable fields include:

- Video title
- Audio title
- Display name
- Artist
- Album
- Folder name

Use appropriate database indexes.

Room FTS may be considered when the
library size or search performance
justifies it.

Search must work offline.

Avoid re-extracting file metadata
for every search query.

## 13. Artwork and Thumbnails

Artwork shall be loaded asynchronously.

Use available MediaStore thumbnails,
embedded artwork or appropriately generated
thumbnails where supported.

Provide placeholders when artwork
cannot be retrieved.

Cache artwork using stable media identity
where practical.

Apply bounded memory and disk caches.

Do not decode full-resolution video
frames for every visible media card.

Artwork cache files are disposable.

Deleting an artwork cache must never
delete original media.

## 14. Playback History

Save playback progress periodically
and at appropriate lifecycle events.

Examples include:

- Playback pause
- Playback stop
- Leaving the player
- Application backgrounding

Avoid writing to Room on every
playback position update.

Resume positions shall remain
available after application restarts.

If the media file is unavailable,
retain its history according to
the library reconciliation policy.

## 15. Removable USB Storage

USB storage must be treated
as potentially temporary.

When a USB drive is disconnected:

- Do not crash.
- Stop or fail affected playback gracefully.
- Mark affected media as unavailable.
- Preserve appropriate library metadata.
- Offer a relevant recovery message.

When the drive is reconnected:

- Recheck authorized access.
- Rescan the available source.
- Reconcile existing media identities.
- Restore availability where possible.

Do not promise automatic USB detection
on every television or firmware version.

Provide a manual rescan option.

## 16. File Safety

LumaPlay is initially a read-only
media discovery and playback application.

Ordinary scanning and playback must not:

- Delete original files
- Rename original files
- Move original files
- Rewrite media metadata
- Modify USB drive contents

No media-management feature that
changes original files may be
implemented without separate approval.

## 17. DataStore Preferences

Use DataStore for small application
preferences rather than Room.

Examples:

- Onboarding completed
- Preferred video playback behavior
- Subtitle appearance
- Audio preferences
- Library scanning preferences
- Selected sorting options

Preferences must persist across
normal application restarts.

Do not store large media collections
or binary artwork in DataStore.

## 18. Database Migration

Every database schema change must
increment the Room database version.

Provide explicit migrations
for production schema changes.

Do not enable destructive migration
as the default release behavior.

Development-only database resets
require clear user approval.

## 19. Error Handling

Storage-related errors include:

- Permission denied
- Persisted permission revoked
- Source unavailable
- USB disconnected
- File removed
- Unsupported document provider
- Metadata extraction failure
- Database failure
- Scan cancelled
- Scan partially completed

Each error shall be reported
at the appropriate layer.

The UI must display a useful
message and recovery action
where one exists.

Do not expose raw stack traces
to ordinary users.

## 20. Performance Requirements

The storage system must operate
within the limitations of
the primary Android TV.

Requirements:

- No main-thread full-library scans.
- Incremental processing of large libraries.
- Bounded artwork caches.
- Efficient database queries.
- No unnecessary duplicate scans.
- Controlled metadata extraction.
- Cancellation of obsolete operations.
- Appropriate cleanup of temporary resources.

The application must remain navigable
while media discovery is running.

## 21. Privacy and Security

All core media discovery and
playback operations are local.

LumaPlay shall not upload
personal media or library
metadata to external servers.

Request only necessary permissions.

Use Android content URI access
rather than attempting to bypass
platform storage restrictions.

Avoid logging sensitive
file paths unnecessarily.

Do not expose user media through
unprotected exported components.

## 22. Testing Requirements

### Test DS-001

Grant applicable storage access.

Expected:
Accessible supported media is discovered.

### Test DS-002

Deny storage permission.

Expected:
The application explains the limitation
and remains usable.

### Test DS-003

Scan an empty authorized folder.

Expected:
The scanner completes and displays
an appropriate empty state.

### Test DS-004

Scan a folder containing videos
and audio files.

Expected:
Supported media is indexed correctly.

### Test DS-005

Run a second scan without file changes.

Expected:
Existing media is not unnecessarily
duplicated.

### Test DS-006

Disconnect USB storage during scanning.

Expected:
The scanner handles the failure
without crashing the application.

### Test DS-007

Disconnect USB storage during playback.

Expected:
Playback fails gracefully and
the application remains responsive.

### Test DS-008

Reconnect previously indexed storage.

Expected:
Available media is reconciled
where stable identity permits.

### Test DS-009

Restart LumaPlay after indexing media.

Expected:
The database and authorized
access information persist.

### Test DS-010

Resume a previously watched video.

Expected:
The saved playback position
is restored correctly.

### Test DS-011

Scan a large media collection.

Expected:
The UI remains responsive
and memory usage stays controlled.

### Test DS-012

Revoke a persisted SAF permission.

Expected:
The application detects the
access failure and offers recovery.

## 23. Acceptance Criteria

The data and storage subsystem
is complete when:

1. Accessible internal media
   can be discovered and indexed.

2. Supported user-selected
   folders can be scanned.

3. Compatible USB media can
   be accessed where Android permits.

4. Scanning does not block the UI.

5. Duplicate entries are avoided
   where reliable identity exists.

6. Removed storage is handled safely.

7. Media metadata persists in Room.

8. Playback history persists.

9. Permissions follow the
   applicable Android version.

10. Original media files remain
    untouched by ordinary operations.

11. Relevant tests pass on
    the emulator and physical TV.

## 24. Implementation Notes

The initial development target is
the Android 11 TV emulator.

Physical validation will use
the Redmi Smart TV X50.

Actual USB discovery and
document-provider behavior
must be tested on the TV.

Do not assume that successful
emulator scanning proves that
every physical USB drive
will behave identically.

Changes to the storage model
must be reflected in the
technical architecture document.
