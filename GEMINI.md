<!-- GSD:project-start source:PROJECT.md -->
## Project

**YourTV (你的電視)**

一个专为安卓电视、电视盒子和手机设计的 IPTV/网页视频流媒体播放应用程序。它集成了 ExoPlayer 直播流解码与腾讯 TBS X5 WebView 网页视频源渲染能力，并提供局域网后台（NanoHTTPD）供用户轻松进行视频源和配置管理。

**Core Value:** 为用户提供一个纯净、无任何激活与授权使用限制、在电视遥控器和手势下拥有流畅交互的高性能直播播放体验。

### Constraints

- **Compatibility**: Android 6.0+ (API 23+) — 需确保新增的 UI 特效和代码在该最低系统版本下正常工作。
- **Interaction Constraint**: 遥控器 DPAD 键为核心导航方式 — 所有前端优化和布局调整均需保证焦点态可被遥控器正确识别。
- **Build Requirement**: 必须保留 TBS 和本地 AES 资源解密所需的依赖项及 Proguard 混淆规则。
<!-- GSD:project-end -->

<!-- GSD:stack-start source:codebase/STACK.md -->
## Technology Stack

## Languages
- Kotlin (2.0.0 / 2.1.0) - All Android application source code.
- Kotlin DSL (build.gradle.kts, settings.gradle.kts) - Gradle build scripts and configuration.
- Java - Library interoperability (e.g., NanoHTTPD and Tencent TBS are written in Java).
## Runtime
- Android 6.0 (API 23) and above - Run environment for the APK.
- JVM 17 (Java target version 17, Kotlin JVM target 17) - Build and compilation target.
- Gradle with Version Catalog (`gradle/libs.versions.toml`).
- Gradle Wrapper (`gradlew`) - Build orchestration.
## Frameworks
- Android Jetpack - Core UI and architecture libraries (AppCompat 1.7.0, ConstraintLayout 2.2.1, LiveData, ViewModel, Lifecycle 2.8.7).
- Jetpack Media3 (ExoPlayer 1.5.1 / 1.1.1) - High performance video playbacks (HLS, RTSP, RTMP, DASH, etc.).
- Tencent TBS (X5 Webview SDK 44286) - High performance Webview core for rendering web-based live sources.
- None - No testing frameworks are configured or utilized in the current codebase.
- Android Gradle Plugin (AGP) 8.9.1.
## Key Dependencies
- `androidx.media3:media3-exoplayer` (1.5.1 / 1.1.1) - Video engine for IPTV stream decoding and playback.
- `com.tencent.tbs:tbssdk` (44286) - Tencent X5 WebView Core for web video playbacks.
- `com.squareup.okhttp3:okhttp` (4.12.0) - Network client for config downloading and API requests.
- `org.nanohttpd:nanohttpd` (2.3.1) - Embedded HTTP web server running on the device for LAN remote configuration.
- `androidx.security:security-crypto` (1.0.0) / `org.bouncycastle:bcprov-jdk15on` (1.70) - Custom data encryption and decryption.
- `com.google.zxing:core` (3.5.3) - QR code generation and decoding utilities.
- `com.github.bumptech.glide:glide` (4.16.0) - Image caching and rendering (e.g. channel logos).
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` (1.8.1) - Coroutines for non-blocking asynchronous calls.
- `com.google.code.gson:gson` (2.11.0) - Serialization and JSON parsing.
## Configuration
- Device Storage (Shared Preferences via `SP.kt`) for persistent local settings.
- Remote configuration urls (Github private raw URLs, cloudflare workers API).
- Private key config assets (e.g. `cloudflare.txt`, `github_private.txt`, `sources.txt` in encrypted format).
- `build.gradle.kts` (root and app level) - Android Gradle build configurations.
- `proguard-rules.pro` - Code obfuscation and shrinking configuration.
## Platform Requirements
- Android Studio / IntelliJ IDEA supporting JDK 17 & Android SDK 35.
- Compatible with Windows, macOS, and Linux.
- Android TV / Android Box / Android Mobile Phone (minSdk 23 / Android 6.0+).
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

## Naming Patterns
- PascalCase for Kotlin files and components (`PlayerFragment.kt`, `MainActivity.kt`).
- snake_case for XML layouts and drawables (`activity_main.xml`, `appreciate.jpg`).
- camelCase for all functions (e.g. `sourceUp()`, `showFragment()`).
- Callback event triggers prefixed with `on` (e.g. `onKeyConfirmed()`).
- camelCase for standard variables (e.g. `menuPressCount`, `lastSwitchTime`).
- UPPER_SNAKE_CASE for constant values (e.g. `BACK_PRESS_INTERVAL`, `WORKER_API_URL`).
- Private properties / fields inside Kotlin files have no special underscore prefix, relying on standard `private` access modifiers.
- PascalCase for interfaces (e.g. `VerificationCallback`).
- PascalCase for Models/Data Classes (e.g. `TVModel`, `ProxyInfo`).
## Code Style
- Standard Android Kotlin style.
- 4-space indentation for Kotlin code blocks, 4 spaces for XML tags.
- Explicit type declarations preferred for public members, implicit allowed for simple local variable assignments.
- Checked via standard Android Lint during gradle execution.
- `@Suppress("DEPRECATION")` and `@Suppress("UNUSED_EXPRESSION")` used strategically to manage Legacy TV APIs compatibility.
## Import Organization
## Asynchronous Programming
- Avoid raw threads. Always use Kotlin Coroutines.
- Use `Dispatchers.IO` for heavy network downloads (e.g., `withContext(Dispatchers.IO)` in `DownGithubPrivate.kt`).
- Launch coroutines bound to View lifetimes using `lifecycleScope.launch` in Activities/Fragments.
- Utilize delay timers via coroutine `delay(ms)` instead of blocking thread sleep.
## Error Handling
- Graceful recovery: Use `Result<T>` wrapper for remote fetches and decryption operations (e.g., `Result.success(content)` / `Result.failure(e)`).
- Prevent unexpected crashes on remote TV boxes using the global uncaught exception handler `YourTVExceptionHandler.kt`.
- Silent fail-safe: Catch resource failures (e.g. JSON parse fails, EPG parse fails) locally, fallback to defaults, and display error toast or `ErrorFragment.kt` instead of crashing.
## Logging
- Encapsulated via `Logger.kt` or custom `Log.d` checks.
- Production logs (e.g., `ENABLE_LOG = false` in release builds) are stripped/disabled using conditional compilation settings inside build configurations.
## Comments
- Document security and cryptographic mechanics (e.g., how the decrypt keys are loaded from Cloudflare/Github).
- Explain remote DPAD key interception rules (which clicks triggers what menus).
- Keep simple operations uncommented, but outline complex state machines (e.g. key double-presses).
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

## Pattern Overview
- **Single Activity Host:** `MainActivity.kt` hosts all sub-features via full-screen overlay Fragments.
- **ViewModel-driven Communication:** View states (active TV, channel groups, EPG info) are hosted in `MainViewModel.kt` using LiveData/StateFlow.
- **Embedded Web Server:** Multi-threaded local server (`SimpleServer.kt`) running on a background socket to receive configuration updates dynamically.
- **KeyEvent-driven Control:** Custom remote control (DPAD) button intercepting optimized for TV devices.
## Layers
- Purpose: Render video player, menus, settings, and handle user inputs (remote controllers, touch gestures).
- Contains: `MainActivity.kt`, `PlayerFragment.kt`, `MenuFragment.kt`, `SettingFragment.kt`, `ChannelFragment.kt`, `InfoFragment.kt`.
- Depends on: ViewModel Layer (`MainViewModel.kt`).
- Purpose: Bridge the UI with business logic and manage reactive state.
- Contains: `MainViewModel.kt`.
- Location: `app/src/main/java/com/horsenma/yourtv/MainViewModel.kt`.
- Depends on: Data Layer / Models / Encryption Utilities.
- Used by: All Fragments and `MainActivity.kt`.
- Purpose: Background network operations, LAN configuration, and lifecycle updates.
- Contains: `SimpleServer.kt` (LAN Admin Web), `UpdateManager.kt` (OTA updates), `DownGithubPrivate.kt` (private repo synchronization).
- Depends on: Data Layer & SharedPreferences (`SP.kt`).
- Purpose: Represent TV structures, parse EPG (XMLTV), load lists, and decrypt sources.
- Contains: `models/` (parsing), `requests/` (networking), `SP.kt` (persistence), `SourceDecoder.kt` (decryption).
## Data Flow
### 1. Live Stream Playback Flow
### 2. LAN Remote Configuration Flow
### State Management:
- **Persistent State:** Android `SharedPreferences` managed via `SP.kt`.
- **In-Memory State:** Managed inside `MainViewModel.kt` using LiveData/MutableStateFlow to enable cross-Fragment reactivity.
## Key Abstractions
- Purpose: Decrypt TV source configurations, cloudflare configs, and github private keys.
- Examples: `SourceDecoder.kt`, `SourceEncoder.kt`.
- Pattern: Cryptographic utility utilizing AES and Base64.
- Purpose: Web Server daemon.
- Examples: `SimpleServer.kt`.
- Pattern: Singleton service hosted within `MainActivity.kt` lifetime.
- Purpose: Validating client credentials and key updates.
- Examples: `UserInfo.kt`, `UserVerificationHandler.kt`.
- Pattern: Security Controller.
## Entry Points
- Location: `YourTVApplication.kt`
- Responsibilities: Globally configures Tencent X5 SDK loading, exception handling, and context provisioning.
- Location: `MainActivity.kt`
- Responsibilities: Main window creation, setting full-screen flags, boot-up updates, initializing the local HTTP server, and remote controller event dispatching.
- Location: `BootReceiver.kt`
- Responsibilities: Listens to system boot broadcasts to autostart `MainActivity` if configured.
## Error Handling
- **Global Interceptor:** `YourTVExceptionHandler.kt` catches uncaught Java/Kotlin runtime exceptions, writes stack traces to local logs, and restarts the app gracefully.
- **UI Error:** `ErrorFragment.kt` presents error messages (e.g. "Source Load Fail", "Decoder Error") directly onto the screen.
## Cross-Cutting Concerns
- `Logger.kt` manages custom file-based and Logcat logging. Enabled/disabled dynamically via `ENABLE_LOG` build config.
- AES decryption for external configurations (`cloudflare.txt`, `sources.txt`, `github_private.txt`). Web compatibility is preserved via a custom cryptographic web app.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
