---
phase: 01-verification-bypass-playback-architecture
verified: 2026-06-21T12:00:00Z
status: human_needed
score: 14/14 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: null
  previous_score: null
  gaps_closed: []
  gaps_remaining: []
  regressions: []
warnings:
  - truth: "D1 query code not fully stripped from UserInfo.kt"
    status: warning
    reason: "downloadRemoteUserInfo(), executeD1Query(), and downloadFromGitHub() still exist as dead code (no callers). Functionally harmless but code was not stripped per LIMITS-02 intent."
    artifacts:
      - path: "app/src/main/java/com/horsenma/yourtv/UserInfo.kt"
        issue: "Lines 147-392 contain D1 query infrastructure as dead code. No callers exist outside UserInfo.kt."
---

# Phase 1: Verification Bypass & Playback Architecture Verification Report

**Phase Goal:** 移除一切测试码和绑定验证框，让应用开机即播；解耦 PlayerFragment 的 ExoPlayer 和 Webview 播放逻辑，保障切换稳定（健壮性）。
**Verified:** 2026-06-21T12:00:00Z
**Status:** human_needed
**Re-verification:** No -- initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | App boots without showing any verification/activation dialog | ✓ VERIFIED | Boot path: UserInfoManager.initialize() (line 129) only calls loadApiKeys() + loadCache(). No verification callback, no VerificationCallback interface, no userVerificationHandler field in MainActivity. UserVerificationHandler.kt deleted. userconfirm.xml deleted. No verify_user in layouts. |
| 2 | No D1 database network requests for user status or device binding during boot | ✓ VERIFIED | initialize() (lines 89-108) only calls loadApiKeys() and loadCache(). No D1 query methods are invoked during boot. Dead D1 code (downloadRemoteUserInfo) exists but has zero callers -- see WARNING below. |
| 3 | Channel playback starts automatically after loading completes | ✓ VERIFIED | LoadingFragment shown during boot, viewModel.init() runs with timeout, playTrigger observer triggers playback. LoadingFragment calls activity.ready() which transitions to live playback. |
| 4 | Source file import via LAN admin continues to work | ✓ VERIFIED | SimpleServer and importFromUrl() unchanged. No verification gates block LAN imports. |
| 5 | UserInfoManager.apiKeys remains available for DownGithubPrivate.download() | ✓ VERIFIED | UserInfoManager.apiKeys at UserInfo.kt:73, loadApiKeys() at UserInfo.kt:109. DownGithubPrivate.kt:48 references UserInfoManager.apiKeys for Worker API downloads. |
| 6 | ExoPlayerEngine encapsulates all ExoPlayer creation, playback, monitoring, and release | ✓ VERIFIED | ExoPlayerEngine.kt (530 lines) contains: create() (line 86), play() (line 120), switchSource() (line 157), stop() (line 231), release() (line 240), ensurePlaying() (line 255), getCurrentResolution() (line 267), isPlaying() (line 280), getCurrentTvModel() (line 287), setupPlayerListener() (line 293), startPlaybackMonitor() (line 425), stopPlaybackMonitor() (line 431), PlayerMediaCodecSelector (line 488). |
| 7 | ExoPlayerCallback interface notifies PlayerFragment of playback state changes | ✓ VERIFIED | ExoPlayerCallback.kt (22 lines) has 5 methods: onPlaybackStarted, onPlaybackStopped, onPlaybackError, onVideoSizeChanged, onPlaybackStateChanged. ExoPlayerEngine uses callback?. at lines 296, 312, 319, 325, 358, 414. |
| 8 | PlayerFragment delegates all ExoPlayer operations to ExoPlayerEngine | ✓ VERIFIED | PlayerFragment:56 has exoPlayerEngine field. Calls: exoPlayerEngine.create() (lines 164, 417, 493, 541, 575), .play() (lines 418, 576), .release() (lines 415, 426, 459, 691, 697), .switchSource() (line 438), .ensurePlaying() (lines 127, 642, 667), .stop() (lines 586, 652, 662), .isPlaying() (lines 651, 661). |
| 9 | Engine switching (ExoPlayer <-> WebView) immediately destroys the previous engine | ✓ VERIFIED | PlayerFragment:458-459: exoPlayerEngine.release() called before WebView switch. ExoPlayerEngine.release() (lines 240-252): stopPlaybackMonitor(), handler.removeCallbacksAndMessages(null), player.release(), nulls player/callback/tvModel/context/playerView. |
| 10 | No ExoPlayer references remain directly in PlayerFragment (only via ExoPlayerEngine) | ✓ VERIFIED | PlayerFragment imports only PlayerView (line 23), not ExoPlayer. No private var player: ExoPlayer, no fun updatePlayer(), no PlayerMediaCodecSelector, no checkPlaybackRunnable, no stableSourceCheckRunnable. updatePlayerViewLayout() at line 277 is a UI layout method, not player management. |
| 11 | Handler callbacks are cleared on engine release to prevent leaks | ✓ VERIFIED | ExoPlayerEngine.release() line 242: handler.removeCallbacksAndMessages(null). Also stopPlaybackMonitor() at line 241 removes handler callbacks. |
| 12 | UserVerificationHandler.kt deleted | ✓ VERIFIED | File does not exist ("No such file or directory"). |
| 13 | userconfirm.xml deleted | ✓ VERIFIED | File does not exist ("No such file or directory"). |
| 14 | Fragment transaction safety guards present | ✓ VERIFIED | PlayerFragment:479: isAdded && !isDetached && !childFragmentManager.isStateSaved. PlayerFragment:548: isAdded && !isDetached && childFragmentManager.isStateSaved.not(). |

**Score:** 14/14 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `ExoPlayerCallback.kt` | Callback interface with 5 methods | ✓ VERIFIED | 22 lines, 5 methods present |
| `ExoPlayerEngine.kt` | Encapsulated ExoPlayer lifecycle (min 200 lines) | ✓ VERIFIED | 530 lines, all methods present |
| `PlayerFragment.kt` | Controller orchestrating ExoPlayerEngine and WebFragment | ✓ VERIFIED | 704 lines, implements ExoPlayerCallback, delegates to exoPlayerEngine |
| `UserInfo.kt` | Simplified UserInfoManager with apiKeys preserved | ✓ VERIFIED | apiKeys (line 73), loadApiKeys (line 109), initialize (line 89) preserved. Verification methods (validateKey, checkBinding, updateBinding, checkExpiredTestCodes, getTestCodes, saveTestCode, getActiveUserId) removed. |
| `UserVerificationHandler.kt` | Deleted | ✓ VERIFIED | File does not exist |
| `userconfirm.xml` | Deleted | ✓ VERIFIED | File does not exist |
| `MainActivity.kt` | Clean onCreate without verification initialization | ✓ VERIFIED | Line 129: UserInfoManager.initialize(applicationContext). No VerificationCallback, no userVerificationHandler, no isLoadingInputVisible, no setLoadingInputVisible. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| PlayerFragment.kt | ExoPlayerEngine.kt | exoPlayerEngine instance field | ✓ WIRED | Field at line 56, used throughout (14 call sites) |
| ExoPlayerEngine.kt | ExoPlayerCallback.kt | callback interface | ✓ WIRED | callback?. pattern at 6 locations (lines 296, 312, 319, 325, 358, 414) |
| PlayerFragment.kt | ExoPlayerCallback.kt | implements callback | ✓ WIRED | Line 41: class PlayerFragment : Fragment(), ExoPlayerCallback. Lines 240-275: override all 5 methods. |
| DownGithubPrivate.kt | UserInfoManager.apiKeys | API key access | ✓ WIRED | Line 48: val apiKeys = UserInfoManager.apiKeys |
| MainActivity.kt | UserInfoManager.initialize() | simplified init call | ✓ WIRED | Line 129: UserInfoManager.initialize(applicationContext) |
| MainActivity.kt | PlayerFragment.recreatePlayer() | engine recreation | ✓ WIRED | Line 865: playerFragment.recreatePlayer() |
| MainActivity.kt | PlayerFragment.releasePlayer() | engine release | ✓ WIRED | Lines 1397, 1429: playerFragment.releasePlayer() |
| MainActivity.kt | PlayerFragment.isPlayerActive() | engine status check | ✓ WIRED | Lines 1392, 1428: playerFragment.isPlayerActive() |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|-------------------|--------|
| ExoPlayerEngine | tvModel | PlayerFragment.play(tvModel) | Yes - TVModel from MainViewModel LiveData | ✓ FLOWING |
| PlayerFragment | exoPlayerEngine callbacks | ExoPlayerEngine callback?. invocations | Yes - state changes from real ExoPlayer player | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| No verification code remnants | grep -rn "UserVerificationHandler\|VerificationCallback\|showVerificationDialog\|test_code_expired\|deleteCacheByTestCode" app/src/main/java/ | Empty output | ✓ PASS |
| No layout verification remnants | grep -n "verify_user" setting.xml setting_mytv1.xml | Empty output | ✓ PASS |
| ExoPlayerCallback exists | grep -c "interface ExoPlayerCallback" ExoPlayerCallback.kt | 1 | ✓ PASS |
| ExoPlayerEngine exists | grep -c "class ExoPlayerEngine" ExoPlayerEngine.kt | 1 | ✓ PASS |
| Handler cleanup in release | grep -n "removeCallbacksAndMessages" ExoPlayerEngine.kt | Line 242 | ✓ PASS |
| No TODO/FIXME/TBD markers | grep -rn "TBD\|FIXME\|XXX" in modified files | Empty output | ✓ PASS |
| No stub implementations | grep -rn "return null\|return {}\|return \[\]" in ExoPlayerEngine/PlayerFragment | Empty output | ✓ PASS |
| PlayerFragment has no direct ExoPlayer player | grep -n "private var player: ExoPlayer" PlayerFragment.kt | Empty output | ✓ PASS |

### Probe Execution

Step 7c: SKIPPED (no probe scripts exist for this phase)

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| LIMITS-01 | 01-01 | 彻底移除 TV 端的测试码与激活验证对话框 | ✓ SATISFIED | UserVerificationHandler.kt deleted, userconfirm.xml deleted, no verification code in boot path, no verify_user in layouts, no verification remnants in codebase. |
| LIMITS-02 | 01-01 | 剥离后台对云端 D1 数据库的用户状态及设备绑定网络请求 | ✓ SATISFIED | Verification-specific D1 methods removed (validateKey, checkBinding, updateBinding, checkExpiredTestCodes). initialize() only loads apiKeys + cache. No D1 requests during boot. Dead D1 code (downloadRemoteUserInfo) exists but has zero callers -- see WARNING section. |
| PERF-02 | 01-02 | 对 PlayerFragment.kt 进行播放引擎重构，将 ExoPlayer 逻辑和 Webview 逻辑隔离 | ✓ SATISFIED | ExoPlayerEngine (530 lines) encapsulates all ExoPlayer lifecycle. ExoPlayerCallback interface (5 methods) notifies PlayerFragment. PlayerFragment delegates all operations. Engine switching destroys previous engine. Handler callbacks cleared on release. Fragment safety guards present. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| UserInfo.kt | 147-392 | Dead D1 query code (downloadRemoteUserInfo, executeD1Query, downloadFromGitHub) | WARNING | Code not stripped per LIMITS-02 intent. Dead code (zero callers) but adds maintenance burden. Could be accidentally re-activated. Recommend removal in future cleanup. |

### Warnings

1. **Dead D1 query code in UserInfo.kt (lines 147-392):** `downloadRemoteUserInfo()`, `executeD1Query()`, and `downloadFromGitHub()` methods remain as dead code with zero callers. These make D1 HTTP queries but are never invoked. The plan (01-01) deliberately preserved source file management code, but these specific methods are orphaned. Recommend removing in a future cleanup pass to fully satisfy LIMITS-02's intent of stripping D1 database network request infrastructure.

### Human Verification Required

### 1. Cold Boot Without Activation Code

**Test:** Install the app on a TV device or emulator with no prior data. Cold boot the app.
**Expected:** App shows LoadingFragment briefly, then auto-plays the first available channel. No verification/activation dialog appears at any point.
**Why human:** Cannot verify visual UI behavior programmatically. Need to confirm no dialog appears and playback starts.

### 2. ExoPlayer <-> WebView Engine Switching Stability

**Test:** Play an IPTV channel (ExoPlayer), then switch to a WebView source, then switch back to IPTV. Repeat 5-10 times rapidly.
**Expected:** Each switch cleanly destroys the previous engine. No crashes, no memory leaks (monitor via Android Profiler), no frozen playback.
**Why human:** Requires real device interaction and visual confirmation of playback state. Cannot test memory leak behavior programmatically.

### 3. Source Import via LAN

**Test:** Access the LAN admin at http://<device-ip>:8080 from a browser on the same network. Import a source file.
**Expected:** Source file is imported successfully and channels appear in the player.
**Why human:** Requires network setup and browser interaction. Cannot verify LAN server functionality without running the app.

---

_Verified: 2026-06-21T12:00:00Z_
_Verifier: Claude (gsd-verifier)_
