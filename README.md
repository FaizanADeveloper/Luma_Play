
# LumaPlay

**An offline-first video and music player for Android TV.**

LumaPlay is a native Android TV application designed to discover and play accessible local media from internal storage and compatible USB devices.

The interface follows the approved Google Stitch designs and is optimized for television screens and D-pad remote navigation.

## Project Information

| Property | Value |
|---|---|
| Application ID | `com.lumaplay.tv` |
| Platform | Android TV |
| Minimum SDK | Android 11 (API 30) |
| Language | Kotlin |
| UI | Jetpack Compose for TV |
| Architecture | MVVM + Repository |
| Playback | AndroidX Media3 ExoPlayer |
| Database | Room |
| Preferences | DataStore |
| Storage | MediaStore + Storage Access Framework |

## Features

The planned application includes:

- Home screen with dynamic media sections
- Local video library
- Local music library
- Folder browsing
- Compatible USB media access
- Video and music playback
- Playback resume and history
- Audio and subtitle track selection
- Supported 4K and HDR playback
- Universal offline search
- Optional voice search
- Persistent playback preferences
- Device capability diagnostics

Hardware-dependent media support must be verified on the actual device.

Casting and screen mirroring are outside LumaPlay's scope.

## Development Status

LumaPlay is in the initial development stage.

The Android Studio project has been created and its initial application has successfully launched on the Android TV emulator.

The numbered specification documents define the planned functionality. They do not indicate that those features have already been implemented.

## Project Structure

```text
LumaPlay-Project/
├── app/
├── designs/
│   └── stitch/
│       └── LumaPlay_UI_UX/
├── docs/
│   ├── 01-project-overview.md
│   ├── 02-requirements-srs.md
│   ├── 03-technical-architecture.md
│   ├── 04-ui-ux-specification.md
│   ├── 05-data-and-storage.md
│   ├── 06-playback-specification.md
│   ├── 07-search-and-settings.md
│   ├── 08-testing-plan.md
│   └── 09-development-roadmap.md
├── AGENTS.md
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradlew.bat
```

The Stitch directory is the intended location for the approved design exports. Verify that the files have been copied there before UI implementation.

## Requirements

Development requires:

- Android Studio
- A compatible Android SDK
- The JDK supported by the existing Gradle configuration
- An Android TV emulator or physical Android TV device

Use the Gradle wrapper included in the project.

Avoid unnecessary upgrades to the existing Android Gradle Plugin, Kotlin or Gradle versions.

## Building the Application

Open the project root in Android Studio.

Allow Gradle synchronization to finish.

Select the Android TV emulator or an authorized physical Android TV device.

Build and run the `app` configuration.

On Windows, the project can also be built from its root directory using:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run this command only after verifying the project path and reviewing the existing configuration.

## Testing

The initial development environment uses an Android TV emulator running Android 11 (API 30) at 1920 × 1080.

The primary physical test device is the Redmi Smart TV X50, a 4K UHD television.

Audio testing uses the Zebronics ZEB-JUKE BAR 9800 DWS PRO connected through HDMI ARC.

The emulator is suitable for basic UI, navigation and functional testing. Actual 4K, HDR, USB and audio output capabilities require physical-device testing.

See `docs/08-testing-plan.md` for the test cases and acceptance criteria.

## Documentation

| Document | Purpose |
|---|---|
| `01-project-overview.md` | Product vision and scope |
| `02-requirements-srs.md` | Functional and nonfunctional requirements |
| `03-technical-architecture.md` | Application architecture |
| `04-ui-ux-specification.md` | Screen designs and TV navigation |
| `05-data-and-storage.md` | Media discovery, storage and database |
| `06-playback-specification.md` | Video, music and advanced playback |
| `07-search-and-settings.md` | Search, voice input and preferences |
| `08-testing-plan.md` | Testing strategy and quality assurance |
| `09-development-roadmap.md` | Implementation milestones |

AI development agents must also read `AGENTS.md` before proposing changes.

## Development Workflow

1. Review the documentation and approved designs.
2. Establish a recoverable Git checkpoint and independent backup.
3. Perform a read-only project audit.
4. Propose one scoped implementation milestone.
5. Review the planned file changes and terminal commands.
6. Implement the approved milestone.
7. Build and execute relevant tests.
8. Review and commit verified changes.

Do not attempt to implement the entire application in one operation.

## Privacy and File Safety

Core local media discovery, playback and text search must work offline.

LumaPlay must not upload personal media or library metadata.

Ordinary scanning and playback must not modify or delete original media files.

The application must use Android's supported storage APIs and request only necessary permissions.

## License

A project license has not yet been selected.

Do not assume the project is open source or add a license without approval.
