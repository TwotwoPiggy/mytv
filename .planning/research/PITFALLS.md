# Pitfalls Research

**Domain:** Android TV Media Player
**Researched:** 2026-06-20
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: TV Remote Key Ghost Clicks (Double Dispatch)

**What goes wrong:**
DPAD Center / Enter button triggers action twice (e.g. opens and immediately closes menu).

**Why it happens:**
Older TV boxes fire both `KeyEvent.ACTION_DOWN` and `ACTION_UP`, or duplicate inputs due to slow UI rendering main-thread blocks.

**How to avoid:**
Implement an explicit time-based debounce gate (e.g. 300ms interval) for all navigation clicks.

**Warning signs:**
Double logs of TV selection in Logcat.

**Phase to address:**
Phase 1 (Key & Gesture Optimization).

---

### Pitfall 2: Memory Leak in Webview / ExoPlayer context switches

**What goes wrong:**
Switching between Webview streams and ExoPlayer causes OutOfMemory (OOM) crash after 10-15 channel changes.

**Why it happens:**
Webview and ExoPlayer retain references to Context/Activity if not properly released.

**How to avoid:**
Explicitly destroy the WebView container and call `player.release()` before initializing a different stream type.

**Phase to address:**
Phase 1 (Verification Removal & Playback Safety).

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Monolithic MainActivity | Rapid prototyping. | Highly fragile, key events hard to debug. | Acceptable in MVP, should be refactored early. |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Hard-to-see Focus State | Users get lost on where the cursor is on TV. | Add prominent scale and highlight (glow effect) on focus. |

---
*Pitfalls research for: Android TV Media Player*
*Researched: 2026-06-20*
