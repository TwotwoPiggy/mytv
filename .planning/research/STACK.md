# Stack Research

**Domain:** Android TV Media Player
**Researched:** 2026-06-20
**Confidence:** HIGH

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Android SDK | compileSdk 35 | Platform API | Ensures compatibility with modern Android 14 APIs while preserving TV traits. |
| Kotlin | 2.1.0 | App Programming | Modern, null-safe, coroutine-native language for Android development. |
| Jetpack Media3 ExoPlayer | 1.5.1 | Media Playback | Google's standard player framework for high performance live streaming. |
| Tencent TBS (X5 Webview) | 44286 | Web Live Streams | Specifically optimized for Chinese network environments to parse and play web-based streams. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| AndroidX Security Crypto | 1.0.0 | Secret Decryption | Decrypt local asset files like `sources.txt` without raw key leakage. |
| NanoHTTPD | 2.3.1 | LAN Admin Server | Enable local port 8080 administration on the TV box from PC browser. |
| AndroidX Dynamic Animation | 1.0.0 | Physics-based Animation | Create smooth bounce animations on TV remote focus changes. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Android Studio | IDE | Standard IDE for compilation, memory profiling, and TV emulator debugging. |
| ADB (Android Debug Bridge) | Device Control | Fast installation of build APKs on remote TV boxes using `adb install`. |

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Tencent TBS X5 Webview | Standard Android System Webkit | If the TV box resides outside mainland China or TBS servers are unreachable. |
| Jetpack Media3 ExoPlayer | IJKPlayer (Bilibili) | If there are legacy RTSP streams that ExoPlayer fails to decode (not needed here). |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Lottie Animation | Severe frame drops and memory usage on legacy Mstar/Amlogic TV chips. | Simple View properties / ValueAnimator. |
| Jetpack Compose UI | High composition overhead on low-end dual-core TV boxes; can cause input lag. | Classic View / XML with ViewBinding. |

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| `androidx.media3:media3-exoplayer@1.5.1` | `com.squareup.okhttp3:okhttp@4.12.0` | OkHttp datasource resolution compatibility tested. |

## Sources

- Android official developers guide — Android TV inputs & key interception.
- Tencent TBS SDK release notes — Webview X5 integration.

---
*Stack research for: Android TV Media Player*
*Researched: 2026-06-20*
