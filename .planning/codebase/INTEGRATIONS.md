# External Integrations

**Analysis Date:** 2026-06-20

## APIs & External Services

**Cloudflare Workers:**
- File download proxy (`https://yourtv.horsenma.top`) to bypass network constraints.
  - SDK/Client: REST API via raw `HttpURLConnection` / `HttpsURLConnection`.
  - Auth: API key (`worker_api_key`) read from custom encrypted configuration (`UserInfoManager.apiKeys`).

**GitHub API:**
- Private GitHub repository access (`https://api.github.com/repos/*`) to dynamically download private video sources or remote configurations.
  - SDK/Client: REST API calls via `DownGithubPrivate.kt`.
  - Auth: Personal Access Token (PAT) configured in the app's encrypted resources.
  - Endpoints used: `/contents/{path}?ref={branch}`.

**GitHub Raw Content:**
- Directly downloads public/fallback files from `https://raw.githubusercontent.com/horsenmail/yourtv/*`.
  - Client: `DownGithubPrivate.kt` / `HttpClient.kt`.

**IPTV & EPG Sources:**
- User-defined or default external live video sources (M3U channels) and Electronic Program Guide (EPG) XML sources.
  - Client: OkHttp (`HttpClient.kt`) with DNS Cache custom resolver (`DnsCache.kt`).

**Tencent TBS Service:**
- Integrates with Tencent TBS (Tencent Business System) server for X5 Webview core downloading, updating, and initialization.
  - SDK/Client: `com.tencent.tbs:tbssdk`.

## Data Storage

**Shared Preferences:**
- Primary local data store for app state, settings, window-in-window, channel selections, and remote source URLs.
  - Implementation: Android `SharedPreferences` wrapped in helper `SP.kt`.
  - Storage location: `/data/data/com.horsenma.yourtv/shared_prefs/`.

**Local File Cache:**
- Temporary and persistent caching of downloaded channel sources and parsed XML/JSON EPG data.
  - Location: App internal storage `context.cacheDir` and `context.filesDir`.

## Local HTTP Server

**NanoHTTPD LAN Config Server:**
- Embedded web server (`SimpleServer.kt`) running on the device, allowing users to modify configurations and TV sources from a browser in the same LAN.
  - Port: Default `8080`.
  - Endpoints:
    - `/` (serves the HTML/JS configuration interface).
    - `/api/settings` (POST to update configuration).
    - `/api/sources` (POST/GET to manage TV channels source).

## Authentication & Identity

**User Verification & Activation:**
- Activates and verifies the app against custom server endpoints.
  - Implementation: `UserVerificationHandler.kt` handles dialog-based activation key input.
  - Model: `UserInfo.kt` parses user validity, subscription, and key states.

## Environment Configuration

**Asset Secrets:**
- Encrypted asset files placed in `app/src/main/assets/` containing critical external integration tokens:
  - `cloudflare.txt` (Worker API access configuration).
  - `github_private.txt` (Private Github repos and personal access tokens).
  - `sources.txt` (Default IPTV/WebView video sources).
  - Web crypto tool: `https://yourtvcrypto.horsenma.net` is used to encrypt these files before bundling.

---

*Integration audit: 2026-06-20*
*Update when adding/removing external services*
