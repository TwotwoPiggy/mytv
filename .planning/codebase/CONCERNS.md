# Codebase Concerns

**Analysis Date:** 2026-06-20

## Tech Debt

**Monolithic MainActivity:**
- Issue: `MainActivity.kt` is over 1500 lines long, handling remote key intercepting, embedded server hosting, user verification lifecycle, OTA update prompting, and orchestrating 10 distinct UI fragments.
- Impact: Highly fragile; changes to remote key parsing can break layout transitions or crash the background LAN server.
- Fix approach: Delegate remote DPAD key handler to a dedicated controller. Extract update checks and web server lifecycle to background Services.

**ExoPlayer and Webview tightly coupled in PlayerFragment:**
- Issue: `PlayerFragment.kt` mixes ExoPlayer setup with Tencent TBS Webview lifecycle in a single monolithic controller.
- Impact: High complexity when debugging video streams. Hard to swap webview providers or player engines.
- Fix approach: Introduce a `VideoPlayer` abstraction interface with separate implementations for `ExoVideoPlayer` and `WebVideoPlayer`.

**Absence of Automated Testing:**
- Issue: There are zero automated unit or UI tests.
- Impact: Regressions in key event handling or decoders can easily slip into builds. Every change requires manual clicks on a TV box.
- Fix approach: Set up JUnit for decrypt utilities (`SourceDecoder.kt`) and mock Android contexts.

## Known Bugs

**Remote control click debouncing drift:**
- Symptoms: Quick repeated clicks of UP/DOWN/ENTER can cause the menu to lock up or freeze.
- Trigger: Pressing keys faster than the `BACK_PRESS_INTERVAL` / `DEBOUNCE_INTERVAL` on older Android TV boxes.
- File: `MainActivity.kt` (~line 71-113, key click logic).
- Root cause: Handler runnables are cleared and rescheduled, but differences in device driver key repeat rates are not fully normalized.

## Security Considerations

**Hardcoded AES Decryption Keys:**
- Risk: While TV source assets (`cloudflare.txt`, `github_private.txt`) are encrypted, the decryption keys and AES configs are hardcoded inside `SourceDecoder.kt`. If the APK is decompiled, GitHub PAT tokens and Cloudflare API keys can be compromised.
- Current mitigation: Basic AES encryption is used.
- Recommendations: Offload private key retrieval to a secure remote key distribution worker.

**LAN server lacks Authentication:**
- Risk: The local HTTP server (`SimpleServer.kt`) running on port `8080` has no authentication check. Any client in the local network can overwrite settings or TV source files.
- Current mitigation: Relying purely on LAN perimeter security.
- Recommendations: Generate a dynamic PIN on the TV screen on boot-up and require basic token authorization headers in the web UI.

## Performance Bottlenecks

**EPG XML parsing on low-end devices:**
- Problem: Parsed EPG program guides can contain thousands of lines. Parsing this in memory can trigger GC thrashing or OutOfMemoryError.
- File: `models/EPGXmlParser.kt`.
- Cause: Direct DOM-like parsing.
- Improvement path: Migrate to a streaming parser (`XmlPullParser`) to minimize memory footprint.

**Tencent TBS X5 Core initial download latency:**
- Problem: Slow core download from Tencent servers blocks WebView rendering of live sources.
- File: `YourTVApplication.kt`.
- Measurement: 10s-30s initial block time during cold-boot setup.
- Improvement path: Cache X5 core locally or fallback to system Webkit webview if TBS fails to load in 5 seconds.

## Fragile Areas

**DPAD key combination state machine:**
- File: `MainActivity.kt`.
- Why fragile: Uses triple/quadruple click combinations to trigger Settings (`SettingFragment`).
- Common failures: Users struggle to trigger the menu, or trigger it accidentally.
- Safe modification: Document changes to remote control clicks, ensure any new keys do not conflict with playback.

---

*Concerns audit: 2026-06-20*
*Update as issues are fixed or new ones discovered*
