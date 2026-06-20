# Architecture

**Analysis Date:** 2026-06-20

## Pattern Overview

**Overall:** Single-Activity MVVM (Model-View-ViewModel) Android Application.
Comprises a rich UI layer made of Fragments, a shared `MainViewModel` that binds them together, a background server layer (`SimpleServer`) for LAN control, and a cryptographic data utilities layer.

**Key Characteristics:**
- **Single Activity Host:** `MainActivity.kt` hosts all sub-features via full-screen overlay Fragments.
- **ViewModel-driven Communication:** View states (active TV, channel groups, EPG info) are hosted in `MainViewModel.kt` using LiveData/StateFlow.
- **Embedded Web Server:** Multi-threaded local server (`SimpleServer.kt`) running on a background socket to receive configuration updates dynamically.
- **KeyEvent-driven Control:** Custom remote control (DPAD) button intercepting optimized for TV devices.

## Layers

**UI / View Layer:**
- Purpose: Render video player, menus, settings, and handle user inputs (remote controllers, touch gestures).
- Contains: `MainActivity.kt`, `PlayerFragment.kt`, `MenuFragment.kt`, `SettingFragment.kt`, `ChannelFragment.kt`, `InfoFragment.kt`.
- Depends on: ViewModel Layer (`MainViewModel.kt`).

**ViewModel Layer:**
- Purpose: Bridge the UI with business logic and manage reactive state.
- Contains: `MainViewModel.kt`.
- Location: `app/src/main/java/com/horsenma/yourtv/MainViewModel.kt`.
- Depends on: Data Layer / Models / Encryption Utilities.
- Used by: All Fragments and `MainActivity.kt`.

**Service Layer:**
- Purpose: Background network operations, LAN configuration, and lifecycle updates.
- Contains: `SimpleServer.kt` (LAN Admin Web), `UpdateManager.kt` (OTA updates), `DownGithubPrivate.kt` (private repo synchronization).
- Depends on: Data Layer & SharedPreferences (`SP.kt`).

**Data / Domain Layer:**
- Purpose: Represent TV structures, parse EPG (XMLTV), load lists, and decrypt sources.
- Contains: `models/` (parsing), `requests/` (networking), `SP.kt` (persistence), `SourceDecoder.kt` (decryption).

## Data Flow

### 1. Live Stream Playback Flow
1. User presses UP/DOWN on TV remote or clicks a channel in `ChannelFragment.kt`.
2. `MainActivity.kt` intercepts the KeyEvent and calls `viewModel.selectTV(...)`.
3. `MainViewModel.kt` updates the active TV LiveData.
4. `PlayerFragment.kt` observes the active TV change:
   - If the URL prefix is `webview://`, it loads the Tencent TBS X5 WebView.
   - Otherwise, it initializes the ExoPlayer instance with HLS/RTSP/RTMP datasource.
5. `InfoFragment.kt` simultaneously observes the change and fetches EPG info to render the current program title.

### 2. LAN Remote Configuration Flow
1. User opens a browser on their computer pointing to `http://<TV_IP>:8080`.
2. `SimpleServer.kt` (NanoHTTPD) receives the request and returns the control web page.
3. User edits a video source or configuration option and clicks Save.
4. Browser issues a POST request to `/api/settings` or `/api/sources`.
5. `SimpleServer.kt` processes the request, parses the JSON payload, and writes to `SP.kt`.
6. Server alerts `MainViewModel.kt` or restarts the Activity to apply updates immediately.

### State Management:
- **Persistent State:** Android `SharedPreferences` managed via `SP.kt`.
- **In-Memory State:** Managed inside `MainViewModel.kt` using LiveData/MutableStateFlow to enable cross-Fragment reactivity.

## Key Abstractions

**SourceDecoder:**
- Purpose: Decrypt TV source configurations, cloudflare configs, and github private keys.
- Examples: `SourceDecoder.kt`, `SourceEncoder.kt`.
- Pattern: Cryptographic utility utilizing AES and Base64.

**SimpleServer:**
- Purpose: Web Server daemon.
- Examples: `SimpleServer.kt`.
- Pattern: Singleton service hosted within `MainActivity.kt` lifetime.

**UserInfoManager / UserVerificationHandler:**
- Purpose: Validating client credentials and key updates.
- Examples: `UserInfo.kt`, `UserVerificationHandler.kt`.
- Pattern: Security Controller.

## Entry Points

**Application Class:**
- Location: `YourTVApplication.kt`
- Responsibilities: Globally configures Tencent X5 SDK loading, exception handling, and context provisioning.

**Host Activity:**
- Location: `MainActivity.kt`
- Responsibilities: Main window creation, setting full-screen flags, boot-up updates, initializing the local HTTP server, and remote controller event dispatching.

**System Boot Receiver:**
- Location: `BootReceiver.kt`
- Responsibilities: Listens to system boot broadcasts to autostart `MainActivity` if configured.

## Error Handling

**Strategy:** Global crash safety interceptor coupled with visual Error Fragments.
- **Global Interceptor:** `YourTVExceptionHandler.kt` catches uncaught Java/Kotlin runtime exceptions, writes stack traces to local logs, and restarts the app gracefully.
- **UI Error:** `ErrorFragment.kt` presents error messages (e.g. "Source Load Fail", "Decoder Error") directly onto the screen.

## Cross-Cutting Concerns

**Logging:**
- `Logger.kt` manages custom file-based and Logcat logging. Enabled/disabled dynamically via `ENABLE_LOG` build config.

**Cryptography:**
- AES decryption for external configurations (`cloudflare.txt`, `sources.txt`, `github_private.txt`). Web compatibility is preserved via a custom cryptographic web app.

---

*Architecture analysis: 2026-06-20*
*Update when major patterns change*
