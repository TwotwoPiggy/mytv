# Walking Skeleton — YourTV

**Phase:** 1
**Generated:** 2026-06-21

## Capability Proven End-to-End

Application boots directly to live TV playback without any verification dialog, and ExoPlayer playback logic is cleanly separated into a dedicated engine class with callback-based communication.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Architecture pattern | Single-Activity MVVM (preserved) | Existing pattern works well; no reason to change |
| Playback engine | ExoPlayerEngine class (extracted from PlayerFragment) | Single responsibility, testable, prevents memory leaks per D-06 |
| Engine communication | ExoPlayerCallback interface (mirrors WebFragmentCallback) | Loose coupling, matches existing pattern per D-08 |
| WebView playback | WebFragment (unchanged, already isolated) | Already properly encapsulated with WebFragmentCallback |
| Boot flow | Simplified MainActivity.onCreate() — no verification, direct loading | Removes network dependency for boot per D-12 |
| State management | MainViewModel LiveData/StateFlow (preserved) | Channel state persists across engine switches per D-11 |
| Verification removal | Surgical file-by-file deletion, preserve apiKeys | DownGithubPrivate.download() depends on UserInfoManager.apiKeys |

## Stack Touched in Phase 1

- [x] Verification removal — delete UserVerificationHandler, verification dialogs, broadcasts, SharedPreferences keys
- [x] UserInfoManager simplification — remove verification methods, keep apiKeys/loadApiKeys
- [x] Boot-to-play flow — LoadingFragment transitions directly to playback without verification gate
- [x] ExoPlayerEngine extraction — dedicated class with create/play/switchSource/release lifecycle
- [x] ExoPlayerCallback interface — callback-based communication between engine and PlayerFragment
- [x] Engine switching protocol — immediate destruction of previous engine, zero-residue switching

## Out of Scope (Deferred to Later Slices)

- Remote controller key debounce and UI aesthetics (Phase 2)
- Traditional-to-Simplified Chinese conversion (Phase 3)
- Jetpack Compose rewrite (v2 Deferred)
- Automated UI testing (Out of Scope per PROJECT.md)

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Phase 2: Remote controller debounce, MenuFragment/SettingFragment glassmorphism UI, EPG streaming parser
- Phase 3: All frontend text converted to Simplified Chinese
