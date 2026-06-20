# Architecture Research

**Domain:** Android TV Media Player
**Researched:** 2026-06-20
**Confidence:** HIGH

## Standard Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────┐
│                       UI / View Layer                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────┐  │
│  │ MainActivity     │  │ PlayerFragment   │  │ Menus     │  │
│  └────────┬─────────┘  └────────┬─────────┘  └─────┬─────┘  │
│           │                     │                  │        │
├───────────┼─────────────────────┼──────────────────┼────────┤
│           ▼                     ▼                  ▼        │
│                       ViewModel Layer                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ MainViewModel                                         │  │
│  └──────────────────────────────┬────────────────────────┘  │
├─────────────────────────────────┼───────────────────────────┤
│                                 ▼                           │
│                       Data / Service Layer                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐  │
│  │ Decoders │  │ SP Cache │  │ NanoHTTPD│  │ Verification│  │
│  │          │  │          │  │ Server   │  │ (To Bypass) │  │
│  └──────────┘  └──────────┘  └──────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `MainActivity` | Orchestrate screens, capture KeyEvents. | Android `AppCompatActivity` with key dispatching overrides. |
| `PlayerFragment` | Stream player surface (ExoPlayer & Webview). | Wrapper over Jetpack Media3 and Tencent TBS. |
| `UserVerificationHandler` | Prompt for key activation. | Dialog binding controller (targeted for bypass). |

## Architectural Patterns

### Pattern 1: Event Dispatcher for TV Controllers
- **What:** Intercept DPAD KeyEvents at activity level and route them to sub-components with debounce.
- **When to use:** Crucial for TV devices to prevent double clicks and skip options.
- **Trade-offs:** Can introduce latency if debounce is too high.

## Data Flow

### Key Event Routing Flow

```
[Remote Controller Click]
          ↓
[MainActivity.onKeyDown] → [Debounce filter] → [ViewModel.selectTV]
                                                    ↓
[PlayerFragment] ←─── (Observes playTrigger) ───────┘
```

## Scaling Considerations
- **0-1k users (Single LAN client):** Low footprint, Android client is fully standalone.
- **LAN Web Administration:** Concurrency is usually 1 (single admin), NanoHTTPD handles it using simple thread-pooling.

## Anti-Patterns

### Anti-Pattern 1: Heavy View Creation on Main Thread
- **What people do:** Inflating heavy XMLs (e.g. settings list) synchronously on click.
- **Why it's wrong:** Causes TV screens to freeze or stutter during layouts.
- **Do this instead:** Use ViewStubs or async layout inflation.

---
*Architecture research for: Android TV Media Player*
*Researched: 2026-06-20*
