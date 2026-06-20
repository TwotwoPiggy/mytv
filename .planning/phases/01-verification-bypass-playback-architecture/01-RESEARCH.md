# Phase 1: Verification Bypass & Playback Architecture - Research

**Researched:** 2026-06-21
**Domain:** Android TV IPTV Player - Verification Removal & ExoPlayer Engine Extraction
**Confidence:** HIGH

## Summary

This phase has two orthogonal workstreams: (1) deleting all verification/authentication code and making the app boot-to-play, and (2) extracting ExoPlayer logic from the monolithic `PlayerFragment.kt` into a dedicated `ExoPlayerEngine` class. The codebase is a Single-Activity MVVM Android app using Kotlin, ExoPlayer (Media3 1.5.1), and Tencent TBS X5 WebView. The verification code is spread across 6 Kotlin files, 2 XML layouts, and has a critical hidden dependency: `UserInfoManager.apiKeys` is used by `DownGithubPrivate.download()` for Worker API source downloads -- this must be preserved even after verification removal.

**Primary recommendation:** Remove verification code surgically file-by-file (not a big-bang delete), then extract ExoPlayer into `ExoPlayerEngine` using a callback interface modeled on the existing `WebFragmentCallback` pattern. Do NOT remove `UserInfoManager` entirely -- only remove verification-specific methods while keeping `apiKeys` loading intact.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| LIMITS-01 | Remove test codes and activation verification dialog completely | Verified: `UserVerificationHandler.kt` (594 lines) is self-contained; removal touchpoints identified in 6 files + 2 layouts |
| LIMITS-02 | Strip backend D1 database user status and device binding network requests | Verified: `UserInfoManager` methods `checkBinding()`, `updateBinding()`, `downloadRemoteUserInfo()`, `validateKey()`, `checkExpiredTestCodes()` are the network callers; `DownGithubPrivate` uses `apiKeys` independently of verification |
| PERF-02 | Refactor PlayerFragment to isolate ExoPlayer and WebView logic | Verified: `PlayerFragment.kt` (1346 lines) contains all ExoPlayer init/listeners/switching mixed with WebView switching; `WebFragment` already isolated with `WebFragmentCallback` pattern |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| ExoPlayer lifecycle & playback | ExoPlayerEngine (new) | PlayerFragment | Engine owns create/release/play; Fragment owns UI binding |
| WebView playback | WebFragment (existing) | PlayerFragment | Already isolated via child fragment |
| Engine switching (ExoPlayer <-> WebView) | PlayerFragment | MainViewModel | Fragment manages UI transition; ViewModel holds channel state |
| Channel state persistence across engine switches | MainViewModel | SP (SharedPreferences) | ViewModel LiveData/StateFlow holds in-memory state |
| Boot-to-play flow | MainActivity | LoadingFragment | Activity orchestrates init; LoadingFragment provides visual feedback |
| Source file download | MainViewModel.importFromUrl() | DownGithubPrivate | ViewModel triggers; DownGithubPrivate executes |
| LAN source management | SimpleServer | MainViewModel | Server receives; ViewModel applies |

## Standard Stack

### Core (No new packages needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jetpack Media3 ExoPlayer | 1.5.1 | Video playback engine | Already in project, current version |
| Kotlin Coroutines | 1.8.1 | Async operations | Already in project, matches conventions |
| AndroidX Lifecycle | 2.8.7 | ViewModel + LiveData | Already in project, architectural foundation |

### No New Dependencies Required

This phase is purely code restructuring (delete verification, extract engine class). No new libraries are needed. The ExoPlayerEngine class will use existing `androidx.media3:media3-exoplayer` which is already imported.

**Installation:** N/A -- no `npm install` or `gradle` dependency changes needed.

## Package Legitimacy Audit

No new packages are being installed. This phase only modifies existing source code.

## Codebase Analysis

### Verification Code Removal -- Complete Dependency Map

**Files to DELETE entirely:**
| File | Lines | Reason |
|------|-------|--------|
| `app/src/main/java/com/horsenma/yourtv/UserVerificationHandler.kt` | 594 | Entire file is verification logic |
| `app/src/main/res/layout/userconfirm.xml` | 131 | Verification dialog layout, only used by UserVerificationHandler |

**Files to MODIFY (remove verification references):**

| File | What to Remove | Lines Affected | Risk |
|------|---------------|----------------|------|
| `MainActivity.kt` | `userVerificationHandler` field (line 128), `dialog` field (line 129), `VerificationCallback` interface (lines 135-139), `verificationCallback` init (lines 155-169), verification-related key handling in `onKey()` (lines 1253-1256, 1268-1271, 1285-1306, 1336-1348, 1357-1370), `isLoadingInputVisible` field (line 119), `setLoadingInputVisible()` (lines 124-126) | ~50 lines | HIGH -- key handling is complex |
| `SettingFragment.kt` | `binding.verifyUser.setOnClickListener` (line 319-321), `showVerificationDialog()` method (lines 678-736), `binding.verifyUser` references in layout iteration (line 354) | ~70 lines | LOW -- isolated button |
| `MenuFragment.kt` | `test_code_expired` broadcast receiver (lines 159-181) | ~25 lines | LOW -- isolated receiver |
| `MainViewModel.kt` | `deleteCacheByTestCode()` method (lines 1120-1153), `test_code_expired` toast string reference (line 1149) | ~35 lines | LOW -- self-contained method |
| `UserInfo.kt` (UserInfoManager) | `validateKey()`, `checkBinding()`, `updateBinding()`, `checkExpiredTestCodes()`, `getTestCodes()`, `saveTestCode()`, `getActiveUserId()`, verification-related constants (`KEY_TEST_CODES`, `KEY_ACTIVE_USER_ID`, `KEY_LAST_CHECK_TIME`), and the `checkExpiredTestCodes` call in `initialize()` (lines 115-129) | ~300 lines | CRITICAL -- must preserve `apiKeys`, `loadApiKeys()`, `initialize()` skeleton |
| `setting.xml` | `verify_user` button element (lines 151-163), `nextFocusDown/Right/Left` references to `verify_user` (lines 110, 145, 174) | ~20 lines | LOW -- layout cleanup |
| `setting_mytv1.xml` | Same as setting.xml | ~20 lines | LOW -- layout cleanup |

**String resources to eventually remove (Phase 3 handles i18n cleanup):**
- `verify_user`, `test_code_expired`, `enter_test_code`, `test_code_invalid`, etc. -- NOT in Phase 1 scope per CONTEXT.md, but string references must be removed from code.

### CRITICAL: UserInfoManager.apiKeys Dependency

`DownGithubPrivate.download()` at line 48 accesses `UserInfoManager.apiKeys`. This is used for downloading source files via the Worker API (when URL doesn't start with `http://` or `https://`). The `apiKeys` are loaded from an encrypted `cloudflare.txt` resource via `UserInfoManager.loadApiKeys()`.

**Constraint:** `UserInfoManager` MUST remain as an object, and `loadApiKeys()` + `apiKeys` field MUST be preserved. Only the verification-specific methods can be removed.

### ExoPlayer in PlayerFragment -- Current State

`PlayerFragment.kt` (1346 lines) currently handles:

**ExoPlayer-specific code to extract:**
| Responsibility | Lines | Notes |
|----------------|-------|-------|
| ExoPlayer creation & configuration | 303-326 | `updatePlayer()` -- renderers factory, codec selector, builder |
| Player listeners (buffering, error, playback state) | 327-628 | ~300 lines of listener callbacks |
| Source switching | 854-937 | `switchSource()` -- stop/clearMediaItems/setMediaSource/prepare |
| Initial play | 940-1156 | `play()` -- massive method handling both IPTV and WebView |
| Playback monitoring | 715-778 | `checkPlaybackRunnable` -- periodic playback health check |
| Stable source saving | 781-841 | `saveStableSource()`, `selectRandomStableSource()` |

**Shared code that must stay in PlayerFragment:**
- UI layout management (playerView vs webView visibility toggling)
- WebView fragment transaction management
- Gesture handling delegation to MainActivity
- Source button management

### WebFragment -- Already Properly Isolated

`WebFragment.kt` (960 lines) is already a self-contained child fragment of `PlayerFragment` with:
- Its own `WebFragmentCallback` interface: `onPlaybackStarted()`, `onPlaybackStopped()`, `onPlaybackError(error: String)`
- WebView lifecycle management in `onDestroyView()`
- `play(tvModel)`, `stopPlayback()`, `updateWebViewLayout()` public methods

### Boot-to-Play Current Flow

Current `MainActivity.onCreate()` (line 141-324):
1. `UserInfoManager.initialize()` -- **THIS triggers verification check on lines 115-129**
2. Fragment initialization -- `LoadingFragment` shown, all fragments added
3. `viewModel.init()` with 5s timeout
4. Stable source check -- tries to resume last channel
5. Channel loading and playback trigger

The verification was never actually blocking boot in the current code (the dialog was triggered separately via `showVerificationDialog()`), but `UserInfoManager.initialize()` runs network checks in background. After removal, `initialize()` should be simplified to only load API keys.

## ExoPlayerEngine Design (D-06 to D-08)

### Callback Interface Pattern

Following the existing `WebFragmentCallback` pattern:

```kotlin
// Reference: app/src/main/java/com/horsenma/mytv1/WebFragmentCallback.kt
interface WebFragmentCallback {
    fun onPlaybackStarted()
    fun onPlaybackStopped()
    fun onPlaybackError(error: String)
}
```

**ExoPlayerEngine should implement:**
```kotlin
interface ExoPlayerCallback {
    fun onPlaybackStarted()
    fun onPlaybackStopped()
    fun onPlaybackError(error: String)
    fun onVideoSizeChanged(width: Int, height: Int)
    fun onPlaybackStateChanged(state: Int)
    // Additional: onBufferingDetected, onSourceSwitchComplete
}
```

### ExoPlayerEngine Class Structure

Based on the extracted code from PlayerFragment:

| Method | Source Lines | Purpose |
|--------|-------------|---------|
| `create(context, playerView)` | 303-326 | Initialize ExoPlayer with renderers factory |
| `play(tvModel)` | 940-960 (IPTV branch) | Start playing a TVModel |
| `switchSource(tvModel)` | 854-937 | Switch to different source URL |
| `release()` | 318-320 | Full cleanup: player.release(), clear references |
| `stop()` | 900-901 | Stop current playback without destroying |
| `ensurePlaying()` | 844-851 | Resume if paused |
| `getCurrentResolution()` | 1222-1231 | Get current video resolution |
| `enterPiP()` / `exitPiP()` | 104-198 | Picture-in-Picture handling |

**Internal state to manage:**
- `player: ExoPlayer?` -- the actual player instance
- `tvModel: TVModel?` -- current playing model
- Buffering/monitoring state (bufferingCount, lastSwitchTime, etc.)
- Handler + Runnables for periodic checks

### Engine Switching Protocol (D-09 to D-11)

Current switching in `PlayerFragment.play()` (lines 940-1156):
- IPTV->WebView: `player?.release()`, then create WebFragment
- WebView->IPTV: Remove WebFragment, create ExoPlayer

**Refactored protocol:**
1. `currentEngine.release()` -- immediate, synchronous
2. UI transition (visibility toggling)
3. `newEngine.create()` + `newEngine.play(tvModel)`
4. ViewModel holds channel state, no data loss during switch

### Memory Management (D-09, D-10)

Current cleanup in `PlayerFragment.onDestroy()` (line 1330-1334):
```kotlin
override fun onDestroy() {
    super.onDestroy()
    player?.release()
    requireActivity().unregisterReceiver(screenReceiver)
}
```

**ExoPlayerEngine.release() must do:**
1. `player?.release()`
2. `player = null`
3. `handler.removeCallbacksAndMessages(null)` -- clear all pending runnables
4. Remove player listener
5. Clear `tvModel` reference
6. Clear callback reference

## Common Pitfalls

### Pitfall 1: UserInfoManager.apiKeys Loss
**What goes wrong:** Deleting `UserInfoManager` entirely breaks `DownGithubPrivate.download()` which needs `apiKeys` for Worker API source downloads.
**Why it happens:** The verification code and the API keys loading are co-located in `UserInfoManager`.
**How to avoid:** Keep `UserInfoManager` object with `initialize()`, `loadApiKeys()`, and `apiKeys` field. Only remove verification methods.
**Warning signs:** If `DownGithubPrivate.download()` fails with "API keys not loaded" after changes.

### Pitfall 2: Fragment Transaction State During Engine Switch
**What goes wrong:** `childFragmentManager.beginTransaction().replace(R.id.web_view, webFragment).commitNow()` can throw `IllegalStateException` if called during `onSaveInstanceState`.
**Why it happens:** PlayerFragment checks `isAdded && !isDetached && !childFragmentManager.isStateSaved` before transactions, but the refactored engine must maintain this guard.
**How to avoid:** ExoPlayerEngine should not own Fragment transactions; PlayerFragment should orchestrate them.

### Pitfall 3: ExoPlayer Listener Memory Leaks
**What goes wrong:** ExoPlayer listeners hold references to PlayerFragment, causing memory leaks if not cleaned up on `release()`.
**Why it happens:** The anonymous `Player.Listener` objects capture `this` (PlayerFragment) and access `tvModel`, `handler`, etc.
**How to avoid:** ExoPlayerEngine should own its listeners; callbacks should be weak references or interface-based.

### Pitfall 4: Handler Callback Leaks in ExoPlayerEngine
**What goes wrong:** `checkPlaybackRunnable` and `stableSourceCheckRunnable` continue firing after engine switch, referencing stale state.
**Why it happens:** Current code has `handler.removeCallbacks()` at some points but not consistently.
**How to avoid:** ExoPlayerEngine.release() must call `handler.removeCallbacksAndMessages(null)`.

### Pitfall 5: Shared Preferences Cleanup
**What goes wrong:** Old verification-related SharedPreferences keys (`VerificationPrefs`, `KEY_TEST_CODES`, `KEY_ACTIVE_USER_ID`, `KEY_LAST_CHECK_TIME`) remain on disk after code removal.
**Why it happens:** SharedPreferences are write-once; deleting code doesn't delete stored values.
**How to avoid:** Optionally clean up on first launch after update. Not blocking, but good hygiene.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| ExoPlayer lifecycle management | Manual create/release scattered in Fragment | ExoPlayerEngine class with clean API | Single responsibility, testable, prevents leak |
| Callback between engine and UI | Direct method calls or global state | Interface-based callbacks (WebFragmentCallback pattern) | Loose coupling, matches existing pattern |
| Source switching state machine | Ad-hoc boolean flags | Encapsulated state in ExoPlayerEngine | Prevents inconsistent state |

## State of the Art

| Approach | Current State | Phase 1 Target | Impact |
|----------|--------------|----------------|--------|
| Verification flow | Multi-step D1 network check + device binding | Completely removed | Boot time improvement |
| ExoPlayer management | Inline in PlayerFragment (1346 lines) | Extracted to ExoPlayerEngine (~400 lines) | PlayerFragment reduces to ~600 lines |
| Engine switching | Inline in PlayerFragment.play() | Delegated to ExoPlayerEngine | Cleaner separation |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `UserInfoManager.apiKeys` is only used by `DownGithubPrivate.download()` for non-HTTP URLs | Common Pitfalls | If other code paths use apiKeys, removing them could break source downloads |
| A2 | No other BroadcastReceiver listens for `TEST_CODE_EXPIRED` besides MenuFragment | Verification Removal | If another receiver exists, it would receive dead broadcasts |
| A3 | The `VerificationPrefs` SharedPreferences (line 457 in UserVerificationHandler) is only used for input rate limiting | Verification Removal | LOW -- only affects verification UI which is being removed |
| A4 | ExoPlayerEngine can be a plain class (not ViewModel or Fragment) | Architecture | LOW -- follows established patterns, PlayerFragment already manages it as a plain field |
| A5 | The `clearCacheChannels()` call in UserVerificationHandler (line 223) is only for verification flow reset | Verification Removal | LOW -- if called elsewhere, removing the verification handler won't affect it |

## Open Questions

1. **Should UserInfoManager.initialize() be simplified or removed?**
   - What we know: Currently called in `MainActivity.onCreate()` line 146. It loads apiKeys, checks expired test codes.
   - What's unclear: Whether any other code path depends on the full initialization sequence.
   - Recommendation: Simplify `initialize()` to only call `loadApiKeys()` and `loadCache()`. Remove the `checkExpiredTestCodes()` coroutine block entirely.

2. **How should ExoPlayerEngine handle the `stableSourceCheckRunnable` and `checkPlaybackRunnable`?**
   - What we know: These run on a Handler with periodic delays, checking playback health and saving stable sources.
   - What's unclear: Whether they should live in the engine or remain in PlayerFragment.
   - Recommendation: Move them into ExoPlayerEngine since they directly monitor player state. PlayerFragment only needs callbacks for UI updates.

3. **Should the `PlayerMediaCodecSelector` inner class move to ExoPlayerEngine?**
   - What we know: It's a custom `MediaCodecSelector` for API 23 compatibility and soft decode preference.
   - What's unclear: Whether it should be an inner class of ExoPlayerEngine or a standalone utility.
   - Recommendation: Move to ExoPlayerEngine as it's only used during player construction.

## Validation Architecture

> nyquist_validation is set to `false` in `.planning/config.json`. Skipping validation section per configuration.

## Sources

### Primary (HIGH confidence)
- Full codebase inspection of all files listed in CONTEXT.md canonical references
- `WebFragmentCallback.kt` interface definition (verified callback pattern)
- `PlayerFragment.kt` line-by-line analysis (1346 lines)
- `UserVerificationHandler.kt` line-by-line analysis (594 lines)
- `UserInfo.kt` / `UserInfoManager` line-by-line analysis (794 lines)
- `DownGithubPrivate.kt` apiKeys dependency (line 48)

### Secondary (MEDIUM confidence)
- Architecture analysis from `.planning/codebase/ARCHITECTURE.md`
- Stack analysis from `.planning/codebase/STACK.md`
- Coding conventions from `.planning/codebase/CONVENTIONS.md`

### Tertiary (LOW confidence)
- None -- all claims verified against actual codebase

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - No new packages; all existing verified
- Architecture: HIGH - Full codebase analysis completed
- Pitfalls: HIGH - Each pitfall traced to specific code locations

**Research date:** 2026-06-21
**Valid until:** 2026-07-21 (stable codebase, low churn)
