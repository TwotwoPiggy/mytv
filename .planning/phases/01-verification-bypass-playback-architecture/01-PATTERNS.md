# Phase 1: Verification Bypass & Playback Architecture - Pattern Map

**Mapped:** 2026-06-21
**Files analyzed:** 14
**Analogs found:** 8 / 14

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `ExoPlayerCallback.kt` (NEW) | interface | event-driven | `mytv1/WebFragmentCallback.kt` | exact |
| `ExoPlayerEngine.kt` (NEW) | engine/service | streaming | `PlayerFragment.kt` (lines 303-639) | role-match |
| `PlayerFragment.kt` (MODIFY) | controller | streaming | `PlayerFragment.kt` (existing) | self-reference |
| `MainActivity.kt` (MODIFY) | controller | request-response | `MainActivity.kt` (existing) | self-reference |
| `SettingFragment.kt` (MODIFY) | component | request-response | `SettingFragment.kt` (existing) | self-reference |
| `MenuFragment.kt` (MODIFY) | component | event-driven | `MenuFragment.kt` (existing) | self-reference |
| `MainViewModel.kt` (MODIFY) | store | transform | `MainViewModel.kt` (existing) | self-reference |
| `UserInfo.kt` (MODIFY) | model | transform | `UserInfo.kt` (existing) | self-reference |
| `setting.xml` (MODIFY) | config | - | `setting.xml` (existing) | self-reference |
| `setting_mytv1.xml` (MODIFY) | config | - | `setting_mytv1.xml` (existing) | self-reference |
| `UserVerificationHandler.kt` (DELETE) | service | request-response | N/A | delete target |
| `userconfirm.xml` (DELETE) | config | - | N/A | delete target |
| `LoadingFragment.kt` (REFERENCE) | component | event-driven | `LoadingFragment.kt` | loading pattern |
| `WebFragment.kt` (REFERENCE) | engine/service | streaming | `WebFragment.kt` | engine pattern |

## Pattern Assignments

### `ExoPlayerCallback.kt` (interface, event-driven) -- NEW FILE

**Analog:** `app/src/main/java/com/horsenma/mytv1/WebFragmentCallback.kt`

**Complete file to copy** (lines 1-7):
```kotlin
package com.horsenma.mytv1

interface WebFragmentCallback {
    fun onPlaybackStarted()
    fun onPlaybackStopped()
    fun onPlaybackError(error: String)
}
```

**Pattern to follow:** Define a minimal callback interface with playback lifecycle events. The new `ExoPlayerCallback` should extend this pattern with additional ExoPlayer-specific events:

```kotlin
package com.horsenma.yourtv

interface ExoPlayerCallback {
    fun onPlaybackStarted()
    fun onPlaybackStopped()
    fun onPlaybackError(error: String)
    fun onVideoSizeChanged(width: Int, height: Int)
    fun onPlaybackStateChanged(state: Int)
}
```

**Key convention:** Callback interfaces live in the same package as the engine class. Methods are named `on` + event name. No default implementations.

---

### `ExoPlayerEngine.kt` (engine/service, streaming) -- NEW FILE

**Analog:** `PlayerFragment.kt` lines 303-639 (ExoPlayer creation, listeners, source switching)

**Imports pattern** (from PlayerFragment lines 1-55):
```kotlin
package com.horsenma.yourtv

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import android.os.Build
import com.horsenma.yourtv.data.PlayerType
import com.horsenma.yourtv.models.TVModel
```

**ExoPlayer creation pattern** (from PlayerFragment lines 302-326):
```kotlin
@OptIn(UnstableApi::class)
fun create(context: Context, playerView: PlayerView) {
    val renderersFactory = DefaultRenderersFactory(context)
    val playerMediaCodecSelector = PlayerMediaCodecSelector()
    renderersFactory.setMediaCodecSelector(playerMediaCodecSelector)
    renderersFactory.setExtensionRendererMode(
        if (SP.softDecode) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
    )

    player = ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .build()
    player?.repeatMode = REPEAT_MODE_ALL
    player?.playWhenReady = true
    playerView.player = player
}
```

**Player listener pattern** (from PlayerFragment lines 327-628):
```kotlin
player?.addListener(object : Player.Listener {
    override fun onVideoSizeChanged(videoSize: VideoSize) {
        callback?.onVideoSizeChanged(videoSize.width, videoSize.height)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            callback?.onPlaybackStarted()
        } else {
            callback?.onPlaybackStopped()
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        callback?.onPlaybackStateChanged(state)
        // Source switching logic for buffering/timeout detection
    }

    override fun onPlayerError(error: PlaybackException) {
        callback?.onPlaybackError(error.message ?: "Unknown error")
    }
})
```

**Source switching pattern** (from PlayerFragment lines 854-937):
```kotlin
@OptIn(UnstableApi::class)
fun switchSource(tvModel: TVModel) {
    val actualUrl = tvModel.getVideoUrl() ?: return
    player?.run {
        val mediaItem = tvModel.getMediaItem() ?: return
        stop()
        clearMediaItems()
        val mediaSource = tvModel.getMediaSource()
        val hlsMediaSource = if (mediaSource != null && actualUrl.endsWith(".m3u8")) {
            HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(3))
                .createMediaSource(mediaItem)
        } else {
            mediaSource
        }
        if (hlsMediaSource != null) {
            setMediaSource(hlsMediaSource)
        } else {
            setMediaItem(mediaItem)
        }
        prepare()
        playWhenReady = true
    }
}
```

**Release/cleanup pattern** (from PlayerFragment lines 1330-1341, WebFragment lines 941-954):
```kotlin
fun release() {
    handler.removeCallbacksAndMessages(null)  // Critical: clear all pending runnables
    player?.release()
    player = null
    callback = null
    tvModel = null
}
```

**Handler pattern for periodic checks** (from PlayerFragment lines 70-100, 715-778):
```kotlin
private val handler = Handler(Looper.myLooper()!!)
private val checkPlaybackInterval = 15_000L

// In release(), must clear all:
handler.removeCallbacksAndMessages(null)
```

**PlayerMediaCodecSelector inner class** (from PlayerFragment lines 1179-1220):
```kotlin
class PlayerMediaCodecSelector : MediaCodecSelector {
    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean
    ): MutableList<MediaCodecInfo> {
        val infos = MediaCodecUtil.getDecoderInfos(
            mimeType, requiresSecureDecoder, requiresTunnelingDecoder
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            val softwareCodecs = infos.filter { !it.hardwareAccelerated }
            if (softwareCodecs.isNotEmpty()) return softwareCodecs.toMutableList()
        }
        if (SP.softDecode) {
            val softwareCodecs = infos.filter { !it.hardwareAccelerated }
            if (softwareCodecs.isNotEmpty()) return softwareCodecs.toMutableList()
        } else if (mimeType.startsWith("audio/")) {
            val softwareCodecs = infos.filter { !it.hardwareAccelerated }
            if (softwareCodecs.isNotEmpty()) return softwareCodecs.toMutableList()
        }
        return infos
    }
}
```

---

### `PlayerFragment.kt` (controller, streaming) -- MODIFY

**Self-reference:** Remove ExoPlayer inline code, delegate to ExoPlayerEngine.

**What to remove:**
- `player: ExoPlayer?` field (line 66) -- replaced by ExoPlayerEngine
- `updatePlayer()` method (lines 302-639) -- moved to ExoPlayerEngine.create()
- `switchSource()` method (lines 854-937) -- moved to ExoPlayerEngine.switchSource()
- `ensurePlaying()` method (lines 844-851) -- moved to ExoPlayerEngine
- `getCurrentResolution()` method (lines 1222-1231) -- moved to ExoPlayerEngine
- `PlayerMediaCodecSelector` inner class (lines 1179-1220) -- moved to ExoPlayerEngine
- `checkPlaybackRunnable` (lines 715-778) -- moved to ExoPlayerEngine
- `stableSourceCheckRunnable` (lines 93-101) -- moved to ExoPlayerEngine
- `play()` IPTV branch (lines 1061-1156) -- delegated to ExoPlayerEngine.play()

**What to keep:**
- UI layout management (playerView vs webView visibility)
- WebView fragment transaction management
- Gesture handling delegation
- Source button management
- `enterPictureInPictureMode()` / `exitPictureInPictureMode()` (lines 104-198)

**Callback integration pattern** (from WebFragment usage in PlayerFragment lines 1001-1022):
```kotlin
// ExoPlayerEngine callback setup
exoPlayerEngine.setCallback(object : ExoPlayerCallback {
    override fun onPlaybackStarted() {
        playbackStartTime = System.currentTimeMillis()
        bufferingCount = 0
        tvModel.retryTimes = 0
        lastStopTime = 0L
    }
    override fun onPlaybackStopped() {
        isStable = false
        playbackStartTime = 0L
        lastStopTime = System.currentTimeMillis()
    }
    override fun onPlaybackError(error: String) {
        isStable = false
        playbackStartTime = 0L
        lastStopTime = System.currentTimeMillis()
        tvModel.setErrInfo(error)
    }
    override fun onVideoSizeChanged(width: Int, height: Int) {
        if (!isInPictureInPictureMode) updatePlayerViewLayout()
    }
    override fun onPlaybackStateChanged(state: Int) {
        // Buffering detection logic stays in PlayerFragment
    }
})
```

**Engine switching protocol** (from PlayerFragment lines 961-1060):
```kotlin
// IPTV -> WebView: release ExoPlayerEngine, create WebFragment
exoPlayerEngine.release()
binding.playerView.player = null
binding.playerView.visibility = View.GONE
binding.webView.visibility = View.VISIBLE
// ... create WebFragment

// WebView -> IPTV: remove WebFragment, create ExoPlayerEngine
webFragment.stopPlayback()
childFragmentManager.beginTransaction().remove(webFragment).commit()
exoPlayerEngine.create(requireContext(), binding.playerView)
exoPlayerEngine.play(tvModel)
```

---

### `MainActivity.kt` (controller, request-response) -- MODIFY

**Self-reference:** Remove verification initialization and key handling.

**Lines to remove:**
- `userVerificationHandler` field (line 128)
- `dialog` field (line 129)
- `VerificationCallback` interface (lines 135-139)
- `verificationCallback` init (lines 155-169)
- `isLoadingInputVisible` field (line 119)
- `setLoadingInputVisible()` method (lines 124-126)
- Verification key handling in `onKey()` (lines 1253-1256, 1268-1271, 1285-1306, 1336-1348, 1357-1370)

**What to keep:** `UserInfoManager.initialize()` call, but simplified (see UserInfo.kt below).

---

### `SettingFragment.kt` (component, request-response) -- MODIFY

**Self-reference:** Remove verification entry point.

**Lines to remove:**
- `binding.verifyUser.setOnClickListener` (lines 319-321)
- `showVerificationDialog()` method (lines 678-736)
- `binding.verifyUser` references in layout iteration (line 354)

---

### `MenuFragment.kt` (component, event-driven) -- MODIFY

**Self-reference:** Remove test_code_expired broadcast.

**Lines to remove:**
- `test_code_expired` broadcast receiver (lines 159-181)

---

### `MainViewModel.kt` (store, transform) -- MODIFY

**Self-reference:** Remove verification cache method.

**Lines to remove:**
- `deleteCacheByTestCode()` method (lines 1120-1153)

---

### `UserInfo.kt` (model, transform) -- MODIFY

**Self-reference:** Remove verification methods, KEEP apiKeys.

**Critical constraint:** `UserInfoManager.apiKeys` and `loadApiKeys()` MUST be preserved -- used by `DownGithubPrivate.download()` (line 48).

**Lines to remove:**
- `validateKey()` method
- `checkBinding()` method
- `updateBinding()` method
- `checkExpiredTestCodes()` method
- `getTestCodes()` method
- `saveTestCode()` method
- `getActiveUserId()` method
- Constants: `KEY_TEST_CODES`, `KEY_ACTIVE_USER_ID`, `KEY_LAST_CHECK_TIME`
- `checkExpiredTestCodes` coroutine block in `initialize()` (lines 115-129)

**Lines to KEEP:**
- `UserInfoManager` object declaration
- `apiKeys` field
- `loadApiKeys()` method
- `initialize()` skeleton (simplified to only call `loadApiKeys()`)
- `loadCache()` method

---

### `setting.xml` (config) -- MODIFY

**Lines to remove:**
- `verify_user` button element (lines 151-163)
- `nextFocusDown/Right/Left` references to `verify_user` (lines 110, 145, 174)

### `setting_mytv1.xml` (config) -- MODIFY

**Same changes as setting.xml**

---

### `LoadingFragment.kt` (component, event-driven) -- REFERENCE

**Pattern for loading transition** (lines 1-82):

**Callback interface pattern** (line 11-13):
```kotlin
interface LoadingFragmentCallback {
    fun onLoadingCompleted()
}
```

**Fragment removal pattern** (lines 61-70):
```kotlin
private fun removeFragment() {
    try {
        requireActivity().supportFragmentManager.beginTransaction()
            .remove(this)
            .commitAllowingStateLoss()
        callback?.onLoadingCompleted()
    } catch (e: Exception) {
        Log.e(TAG, "removeFragment error: ${e.message}", e)
    }
}
```

**Pattern for boot-to-play flow:**
1. Show LoadingFragment in `onCreateView()`
2. Call `activity.ready()` to signal readiness
3. On data loaded, hide LoadingFragment which triggers `onHiddenChanged()`
4. `onHiddenChanged()` calls `stopMusicAndSwitchToLive()` to start playback

---

## Shared Patterns

### Callback Interface Pattern
**Source:** `mytv1/WebFragmentCallback.kt` (7 lines)
**Apply to:** `ExoPlayerCallback.kt` (new file)
```kotlin
interface WebFragmentCallback {
    fun onPlaybackStarted()
    fun onPlaybackStopped()
    fun onPlaybackError(error: String)
}
```
Convention: Minimal interface, events named `on` + action, no default implementations.

### Engine Lifecycle Pattern
**Source:** `mytv1/WebFragment.kt` lines 941-954
**Apply to:** `ExoPlayerEngine.release()`
```kotlin
override fun onDestroyView() {
    super.onDestroyView()
    handler.removeCallbacksAndMessages(null)  // Critical: clear all pending runnables
    webView?.let {
        val parent = it.parent as? ViewGroup
        parent?.removeView(it)
        when (it) {
            is X5WebView -> it.destroy()
            is AndroidWebView -> it.destroy()
        }
    }
    webView = null
    callback = null
}
```
Convention: Clear handler, release resources, null out references in onDestroy/release.

### Handler Cleanup Pattern
**Source:** `PlayerFragment.kt` lines 1336-1341
**Apply to:** `ExoPlayerEngine.release()` and `PlayerFragment.onDestroyView()`
```kotlin
override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
    handler.removeCallbacks(checkPlaybackRunnable)
    handler.removeCallbacks(stableSourceCheckRunnable)
}
```
Convention: Always remove all handler callbacks in reverse order of registration.

### Fragment Transaction Safety Pattern
**Source:** `PlayerFragment.kt` lines 983-989
**Apply to:** All fragment transactions
```kotlin
if (isAdded && !isDetached && !childFragmentManager.isStateSaved) {
    childFragmentManager.beginTransaction()
        .replace(R.id.web_view, webFragment)
        .commitNow()
} else {
    Log.w(TAG, "Skipped WebFragment loading: isAdded=$isAdded, isDetached=$isDetached, isStateSaved=${childFragmentManager.isStateSaved}")
    // Fallback to error state
}
```
Convention: Always check `isAdded`, `isDetached`, and `isStateSaved` before fragment transactions.

### Logging Pattern
**Source:** Throughout codebase
**Apply to:** All new code
```kotlin
private const val TAG = "ClassName"
Log.d(TAG, "Action description: ${tvModel.tv.title}")
Log.e(TAG, "Error description: ${e.message}", e)
Log.w(TAG, "Warning description")
```
Convention: One TAG constant per class, use `Log.d/e/w`, include context (title, state) in messages.

---

## No Analog Found

Files with no close match in the codebase (planner should use RESEARCH.md patterns instead):

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `ExoPlayerCallback.kt` | interface | event-driven | Exact analog exists (WebFragmentCallback.kt) |
| `ExoPlayerEngine.kt` | engine/service | streaming | Code exists inline in PlayerFragment.kt, extracting to class |

## Metadata

**Analog search scope:** `app/src/main/java/com/horsenma/yourtv/`, `app/src/main/java/com/horsenma/mytv1/`
**Files scanned:** 14 (7 core files, 4 modification targets, 3 reference files)
**Pattern extraction date:** 2026-06-21
