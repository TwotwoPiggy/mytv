# Technology Stack

**Analysis Date:** 2026-06-20

## Languages

**Primary:**
- Kotlin (2.0.0 / 2.1.0) - All Android application source code.

**Secondary:**
- Kotlin DSL (build.gradle.kts, settings.gradle.kts) - Gradle build scripts and configuration.
- Java - Library interoperability (e.g., NanoHTTPD and Tencent TBS are written in Java).

## Runtime

**Environment:**
- Android 6.0 (API 23) and above - Run environment for the APK.
- JVM 17 (Java target version 17, Kotlin JVM target 17) - Build and compilation target.

**Package Manager:**
- Gradle with Version Catalog (`gradle/libs.versions.toml`).
- Gradle Wrapper (`gradlew`) - Build orchestration.

## Frameworks

**Core:**
- Android Jetpack - Core UI and architecture libraries (AppCompat 1.7.0, ConstraintLayout 2.2.1, LiveData, ViewModel, Lifecycle 2.8.7).
- Jetpack Media3 (ExoPlayer 1.5.1 / 1.1.1) - High performance video playbacks (HLS, RTSP, RTMP, DASH, etc.).
- Tencent TBS (X5 Webview SDK 44286) - High performance Webview core for rendering web-based live sources.

**Testing:**
- None - No testing frameworks are configured or utilized in the current codebase.

**Build/Dev:**
- Android Gradle Plugin (AGP) 8.9.1.

## Key Dependencies

**Critical:**
- `androidx.media3:media3-exoplayer` (1.5.1 / 1.1.1) - Video engine for IPTV stream decoding and playback.
- `com.tencent.tbs:tbssdk` (44286) - Tencent X5 WebView Core for web video playbacks.
- `com.squareup.okhttp3:okhttp` (4.12.0) - Network client for config downloading and API requests.
- `org.nanohttpd:nanohttpd` (2.3.1) - Embedded HTTP web server running on the device for LAN remote configuration.
- `androidx.security:security-crypto` (1.0.0) / `org.bouncycastle:bcprov-jdk15on` (1.70) - Custom data encryption and decryption.
- `com.google.zxing:core` (3.5.3) - QR code generation and decoding utilities.
- `com.github.bumptech.glide:glide` (4.16.0) - Image caching and rendering (e.g. channel logos).

**Infrastructure:**
- `org.jetbrains.kotlinx:kotlinx-coroutines-android` (1.8.1) - Coroutines for non-blocking asynchronous calls.
- `com.google.code.gson:gson` (2.11.0) - Serialization and JSON parsing.

## Configuration

**Environment:**
- Device Storage (Shared Preferences via `SP.kt`) for persistent local settings.
- Remote configuration urls (Github private raw URLs, cloudflare workers API).
- Private key config assets (e.g. `cloudflare.txt`, `github_private.txt`, `sources.txt` in encrypted format).

**Build:**
- `build.gradle.kts` (root and app level) - Android Gradle build configurations.
- `proguard-rules.pro` - Code obfuscation and shrinking configuration.

## Platform Requirements

**Development:**
- Android Studio / IntelliJ IDEA supporting JDK 17 & Android SDK 35.
- Compatible with Windows, macOS, and Linux.

**Production:**
- Android TV / Android Box / Android Mobile Phone (minSdk 23 / Android 6.0+).

---

*Stack analysis: 2026-06-20*
*Update after major dependency changes*
