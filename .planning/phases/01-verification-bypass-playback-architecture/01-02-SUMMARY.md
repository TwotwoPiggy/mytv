---
phase: 01-verification-bypass-playback-architecture
plan: 02
subsystem: exoplayer-engine-extraction
tags: [exoplayer, engine, callback, refactoring, memory-leak-prevention]
dependency_graph:
  requires: [01-01]
  provides: [exoplayer-engine, callback-interface]
  affects: [PlayerFragment, MainActivity]
tech_stack:
  added: []
  patterns: [callback-interface, engine-lifecycle, handler-cleanup]
key_files:
  created:
    - app/src/main/java/com/horsenma/yourtv/ExoPlayerCallback.kt
    - app/src/main/java/com/horsenma/yourtv/ExoPlayerEngine.kt
  modified:
    - app/src/main/java/com/horsenma/yourtv/PlayerFragment.kt
    - app/src/main/java/com/horsenma/yourtv/MainActivity.kt
decisions:
  - "ExoPlayerCallback mirrors WebFragmentCallback pattern with 5 methods"
  - "ExoPlayerEngine owns all ExoPlayer lifecycle, monitoring, and codec selection"
  - "PlayerFragment implements ExoPlayerCallback directly (no separate listener)"
  - "Added recreatePlayer(), releasePlayer(), isPlayerActive() for Activity integration"
metrics:
  duration: ~5 min
  completed: "2026-06-20T19:27:53Z"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 4
  lines_added: 635
  lines_removed: 729
---

# Phase 1 Plan 02: ExoPlayer Engine Extraction Summary

Extracted ExoPlayer logic from the monolithic PlayerFragment into a dedicated ExoPlayerEngine class with callback-based communication, enabling clean engine switching and preventing memory leaks through proper handler cleanup.

## What Was Done

### Task 1: Create ExoPlayerCallback Interface and ExoPlayerEngine Class

**Created `ExoPlayerCallback.kt`** (22 lines):
- Interface with 5 callback methods: `onPlaybackStarted()`, `onPlaybackStopped()`, `onPlaybackError(error)`, `onVideoSizeChanged(width, height)`, `onPlaybackStateChanged(state)`
- Follows existing `WebFragmentCallback` pattern

**Created `ExoPlayerEngine.kt`** (530 lines):
- `create(context, playerView)` — ExoPlayer initialization with renderers factory, PlayerMediaCodecSelector, soft decode support
- `setCallback(callback)` — register ExoPlayerCallback
- `play(tvModel)` — start playing a TVModel with full listener setup
- `switchSource(tvModel)` — switch source with HLS support, debouncing, and toast display
- `stop()` — stop playback without destroying engine
- `release()` — full cleanup: `handler.removeCallbacksAndMessages(null)`, `player.release()`, null out all references
- `ensurePlaying()` — resume if paused
- `getCurrentResolution()` — get current video resolution
- `isPlaying()` — check playback state
- `getCurrentTvModel()` — return current model
- Internal: `setupPlayerListener()`, `startPlaybackMonitor()`, `stopPlaybackMonitor()`
- `PlayerMediaCodecSelector` inner class for API 23 compatibility and soft decode preference
- Playback monitoring with periodic health checks and stable source detection

### Task 2: Refactor PlayerFragment to Use ExoPlayerEngine

**PlayerFragment.kt** reduced from 1345 lines to 623 lines:
- Now implements `ExoPlayerCallback` interface directly
- Added `exoPlayerEngine` field, removed `player: ExoPlayer?` field
- Removed: `updatePlayer()`, `switchSource()` (inline), `ensurePlaying()`, `getCurrentResolution()`, `PlayerMediaCodecSelector`, `checkPlaybackRunnable`, `stableSourceCheckRunnable`, `PlaybackCallback` interface
- Added: `recreatePlayer()`, `releasePlayer()`, `isPlayerActive()` for Activity integration
- All ExoPlayer operations delegated to `exoPlayerEngine.create()`, `.play()`, `.release()`
- Engine switching: IPTV->WebView releases engine first; WebView->IPTV creates new engine

**MainActivity.kt** updated:
- `switchSoftDecode()` calls `playerFragment.recreatePlayer()` instead of `updatePlayer()`
- `onStop()` uses `playerFragment.isPlayerActive()` and `releasePlayer()`
- `handleWebviewTypeSwitch()` uses `playerFragment.releasePlayer()`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking Issue] External references to removed PlayerFragment.player field**
- **Found during:** Task 2 verification
- **Issue:** MainActivity.kt referenced `playerFragment.player`, `playerFragment.updatePlayer()` which no longer exist after refactoring
- **Fix:** Added `recreatePlayer()`, `releasePlayer()`, `isPlayerActive()` methods to PlayerFragment; updated MainActivity to use new API
- **Files modified:** PlayerFragment.kt, MainActivity.kt
- **Commit:** d39ef7d

## Known Stubs

None — all ExoPlayer logic was fully extracted and delegated.

## Threat Flags

No new security-relevant surface introduced. The refactoring only reorganized existing code without adding new endpoints, auth paths, or file access patterns.

## Self-Check: PASSED

- [x] `ExoPlayerCallback.kt` exists with 5 callback methods
- [x] `ExoPlayerEngine.kt` exists with all required methods (create, setCallback, play, switchSource, stop, release, ensurePlaying, getCurrentResolution, isPlaying, getCurrentTvModel)
- [x] `ExoPlayerEngine.release()` calls `handler.removeCallbacksAndMessages(null)` before `player.release()`
- [x] `ExoPlayerEngine` contains `PlayerMediaCodecSelector` inner class
- [x] `PlayerFragment` has `private val exoPlayerEngine = ExoPlayerEngine()` field
- [x] `PlayerFragment` does NOT contain `private var player: ExoPlayer?`
- [x] `PlayerFragment` does NOT contain `fun updatePlayer()`, `class PlayerMediaCodecSelector`, `checkPlaybackRunnable`, `stableSourceCheckRunnable`
- [x] `PlayerFragment` DOES contain `exoPlayerEngine.create(`, `exoPlayerEngine.play(`, `exoPlayerEngine.release(`
- [x] `PlayerFragment` DOES implement `ExoPlayerCallback`
- [x] `PlayerFragment` DOES contain fragment transaction safety guards (`isAdded && !isDetached`)
- [x] `MainActivity` has no remaining references to removed `playerFragment.player` or `playerFragment.updatePlayer()`
- [x] Both commits exist in git log: fc266d0 (Task 1), d39ef7d (Task 2)
