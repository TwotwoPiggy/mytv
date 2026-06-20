# Codebase Structure

**Analysis Date:** 2026-06-20

## Directory Layout

```
yourtv/
├── .planning/                  # Project planning, roadmaps, and codebase mapping docs
├── app/                        # Main Android application module
│   ├── libs/                   # Local library binary dependencies (.aar, .jar files)
│   └── src/
│       └── main/
│           ├── assets/         # Cryptographically encrypted source files (sources.txt, etc.)
│           ├── java/           # Kotlin source code package base
│           │   └── com/horsenma/yourtv/
│           │       ├── data/   # Global constants, schemas, response objects
│           │       ├── models/ # EPG parsers and television layout models
│           │       └── requests/ # Custom DNS resolving, OkHttp handlers
│           └── res/            # Android UI layouts (XML), drawables, themes, and values
├── gradle/                     # Gradle build configurations and dependency catalogs
│   ├── wrapper/                # Gradle wrapper jar and properties
│   └── libs.versions.toml      # Dependency Version Catalog
├── screenshots/                # Application UI screenshots and preview images
├── Makefile                    # Target shortcuts for developer actions (clean, adb install)
├── build.gradle.kts            # Root level Gradle build script
├── settings.gradle.kts         # Root level Gradle project composition script
└── gradle.properties           # Global Gradle properties and parameters
```

## Directory Purposes

**app/src/main/java/com/horsenma/yourtv:**
- Purpose: Core application package containing all feature fragments, view models, and orchestration utilities.
- Contains: Kotlin (`*.kt`) files.
- Key files:
  - `MainActivity.kt` - Main orchestrator and layout container.
  - `MainViewModel.kt` - Reactive view model sharing data state between players and menus.
  - `PlayerFragment.kt` - Implements ExoPlayer and Tencent X5 WebView video players.
  - `SimpleServer.kt` - Hosts the local NanoHTTPD server for LAN admin.
  - `UserInfo.kt` / `UserVerificationHandler.kt` - Core authentication.
  - `SP.kt` - Core settings persistence.
  - `SourceDecoder.kt` - AES decryption logic.

**app/src/main/java/com/horsenma/yourtv/data:**
- Purpose: Constants and data structures.
- Contains: `Global.kt` (globals), `Source.kt` (channel source schemas).

**app/src/main/java/com/horsenma/yourtv/models:**
- Purpose: Data models and XML EPG parsers.
- Contains: `TVModel.kt` (TV object), `EPGXmlParser.kt` (XMLTV parser).

**app/src/main/java/com/horsenma/yourtv/requests:**
- Purpose: Net services configurations.
- Contains: `HttpClient.kt` (OkHttp provider), `DnsCache.kt` (custom DNS cache lookup).

**app/src/main/assets:**
- Purpose: Encrypted TV channel sources and private configuration keys.
- Contains: `sources.txt`, `github_private.txt`, `cloudflare.txt` (stored in encrypted format).

**gradle/libs.versions.toml:**
- Purpose: Version catalog to centralize gradle dependency versions.

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/horsenma/yourtv/YourTVApplication.kt` - Application initialization.
- `app/src/main/java/com/horsenma/yourtv/MainActivity.kt` - Launcher activity.
- `app/src/main/java/com/horsenma/yourtv/BootReceiver.kt` - System boot-up hook.

**Configuration:**
- `gradle/libs.versions.toml` - Dependencies catalog.
- `app/src/main/java/com/horsenma/yourtv/SP.kt` - Application shared settings schema.
- `app/build.gradle.kts` - Application compilation options, build configurations, and variant naming.

**Core Logic:**
- `app/src/main/java/com/horsenma/yourtv/PlayerFragment.kt` - Media3 ExoPlayer instantiation, TBS loading.
- `app/src/main/java/com/horsenma/yourtv/SimpleServer.kt` - Embedded HTTP server serving administration pages.
- `app/src/main/java/com/horsenma/yourtv/SourceDecoder.kt` - AES decryption for configs.

## Naming Conventions

**Files:**
- `PascalCase.kt` for Kotlin classes, fragments, activities, and view models (e.g. `PlayerFragment.kt`).
- `camelCase.xml` for Android layout resources, strings, and drawables (e.g. `activity_main.xml`).
- `*.pro` for Proguard configurations (e.g. `proguard-rules.pro`).
- `build.gradle.kts` for Gradle scripts.

**Directories:**
- Package names must be lowercase (`com/horsenma/yourtv/models`).
- Generic gradle configuration directories (`gradle`, `gradle/wrapper`).

## Where to Add New Code

**New TV/Playback Feature:**
- View changes: Add a new custom view or Fragment in `app/src/main/java/com/horsenma/yourtv/` and associate with layout in `app/src/main/res/layout/`.
- State changes: Add properties and observers in `MainViewModel.kt`.

**New Network Integration:**
- Add client helper in `app/src/main/java/com/horsenma/yourtv/requests/HttpClient.kt` or create specialized downloader inside `app/src/main/java/com/horsenma/yourtv/requests/`.

**New TV source format:**
- Add parsing/handling inside `SourceDecoder.kt` and `SourceEncoder.kt`.

---

*Structure analysis: 2026-06-20*
*Update when directory structure changes*
