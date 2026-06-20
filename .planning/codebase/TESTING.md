# Testing Patterns

**Analysis Date:** 2026-06-20

## Test Framework

**Runner & Assertion Library:**
- **None:** The codebase does not currently contain any automated unit, integration, or end-to-end testing frameworks (such as JUnit or Espresso).

**Run Commands:**
- No test execution command is available.

## Verification Approach

Since automated testing is absent, all features are validated via manual integration tests.

### Build and Deployment
To compile and deploy the APK for manual verification:

```bash
# Build the debug APK
./gradlew assembleDebug

# Deploy to target Android device / emulator via ADB
adb install -r app/build/outputs/apk/debug/yourtv_v1.8.5.apk
```
*Note: Make sure your target device/emulator (minSdk 23) is running and connected via `adb devices`.*

### Core Verification Checklist

**1. Live Video Stream Playback:**
- Launch the application on a target device.
- Verify ExoPlayer loads and streams default HLS channels successfully.
- Trigger channels requiring webview (e.g. `webview://`) and verify Tencent X5 WebView initializes and renders the player controls.

**2. Key Events & Menu Interception:**
- Use DPAD up/down buttons to cycle TV channels.
- Press DPAD Center / Enter once to toggle the group menu (`MenuFragment`).
- Double-tap DPAD Center / Enter within 500ms to open the advanced Settings (`SettingFragment`).
- Double-press BACK to exit the app.

**3. LAN Web Config Server:**
- Launch the app and check the displayed local IP address.
- Open a web browser on a PC within the same network and navigate to `http://<Device_IP>:8080`.
- Verify the management UI renders. Modify a setting or source line and submit.
- Verify the TV app updates its active settings/channels list in real-time.

---

*Testing analysis: 2026-06-20*
*Update when automated testing is introduced*
