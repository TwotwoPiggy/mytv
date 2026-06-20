# Coding Conventions

**Analysis Date:** 2026-06-20

## Naming Patterns

**Files:**
- PascalCase for Kotlin files and components (`PlayerFragment.kt`, `MainActivity.kt`).
- snake_case for XML layouts and drawables (`activity_main.xml`, `appreciate.jpg`).

**Functions:**
- camelCase for all functions (e.g. `sourceUp()`, `showFragment()`).
- Callback event triggers prefixed with `on` (e.g. `onKeyConfirmed()`).

**Variables:**
- camelCase for standard variables (e.g. `menuPressCount`, `lastSwitchTime`).
- UPPER_SNAKE_CASE for constant values (e.g. `BACK_PRESS_INTERVAL`, `WORKER_API_URL`).
- Private properties / fields inside Kotlin files have no special underscore prefix, relying on standard `private` access modifiers.

**Types:**
- PascalCase for interfaces (e.g. `VerificationCallback`).
- PascalCase for Models/Data Classes (e.g. `TVModel`, `ProxyInfo`).

## Code Style

**Formatting & Indentation:**
- Standard Android Kotlin style.
- 4-space indentation for Kotlin code blocks, 4 spaces for XML tags.
- Explicit type declarations preferred for public members, implicit allowed for simple local variable assignments.

**Linting:**
- Checked via standard Android Lint during gradle execution.
- `@Suppress("DEPRECATION")` and `@Suppress("UNUSED_EXPRESSION")` used strategically to manage Legacy TV APIs compatibility.

## Import Organization

**Order:**
1. Standard Java/Kotlin SDK library imports (`java.io.*`, `kotlin.math.*`).
2. Android platform SDK imports (`android.app.*`, `android.os.*`).
3. Androidx / Jetpack library imports (`androidx.appcompat.app.*`, `androidx.lifecycle.*`).
4. Third-party dependencies (Tencent TBS, Glide, NanoHTTPD).
5. Internal package imports (`com.horsenma.yourtv.data.*`, `com.horsenma.yourtv.models.*`).

## Asynchronous Programming

**Patterns:**
- Avoid raw threads. Always use Kotlin Coroutines.
- Use `Dispatchers.IO` for heavy network downloads (e.g., `withContext(Dispatchers.IO)` in `DownGithubPrivate.kt`).
- Launch coroutines bound to View lifetimes using `lifecycleScope.launch` in Activities/Fragments.
- Utilize delay timers via coroutine `delay(ms)` instead of blocking thread sleep.

## Error Handling

**Patterns:**
- Graceful recovery: Use `Result<T>` wrapper for remote fetches and decryption operations (e.g., `Result.success(content)` / `Result.failure(e)`).
- Prevent unexpected crashes on remote TV boxes using the global uncaught exception handler `YourTVExceptionHandler.kt`.
- Silent fail-safe: Catch resource failures (e.g. JSON parse fails, EPG parse fails) locally, fallback to defaults, and display error toast or `ErrorFragment.kt` instead of crashing.

## Logging

**Framework:**
- Encapsulated via `Logger.kt` or custom `Log.d` checks.
- Production logs (e.g., `ENABLE_LOG = false` in release builds) are stripped/disabled using conditional compilation settings inside build configurations.

## Comments

**Guidelines:**
- Document security and cryptographic mechanics (e.g., how the decrypt keys are loaded from Cloudflare/Github).
- Explain remote DPAD key interception rules (which clicks triggers what menus).
- Keep simple operations uncommented, but outline complex state machines (e.g. key double-presses).

---

*Convention analysis: 2026-06-20*
*Update when patterns change*
