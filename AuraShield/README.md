# AuraShield — Frontend (Pranavi's Track)

A native Android (Kotlin) client implementing the dark-themed, real-time
voice-clone-defense UI: **Console**, **Live Monitor**, **System Lock**, and
**Risk Log**, navigable from a bottom tab bar.

## Screens

| Screen | File(s) |
|---|---|
| Dashboard / Console | `res/layout/fragment_console.xml`, `ui/console/ConsoleFragment.kt` |
| Lockdown overlay | `res/layout/layout_emergency_lock.xml`, `ui/systemlock/SystemLockFragment.kt` |
| Live scanning | `res/layout/fragment_live_monitor.xml`, `ui/livemonitor/LiveMonitorFragment.kt` |
| Forensic risk log | `res/layout/fragment_risk_log.xml` + `item_risk_log.xml`, `ui/risklog/` |

Custom canvas widgets (`widget/`):
- `CircularProgressView` — animated protection-score ring
- `WaveformView` — live scrolling waveform for the scanning screen
- `MelSpectrogramView` — bar-chart fingerprint for an expanded log entry

## Opening the project

**Recommended: Android Studio** (Hedgehog+) — `File > Open`, point at this
folder, let it sync Gradle, then run on an emulator or device (minSdk 26).

**VS Code** — install the *Kotlin Language*, *Gradle for Java*, and
*Android iDE* (or *vscode-android*) extensions, plus a local Android SDK
(`ANDROID_HOME` set). Open the folder, run `./gradlew assembleDebug` from
the integrated terminal, then `./gradlew installDebug` with a device/emulator
connected. UI layout preview is limited compared to Android Studio, but
editing/build/run all work via the terminal + extensions.

> This sandbox has no network access, so the Gradle wrapper jar and
> dependencies haven't been downloaded/verified here — the first sync on
> your machine will pull them from Google's/Maven's repos as normal.

## Backend integration (Deepali's track)

The frontend already speaks the exact contract described in the AI Backend
spec:

```
POST /predict
→ { "is_deepfake": true, "confidence": 0.92 }
```

- `model/PredictResponse.kt` / `PredictRequest.kt` — JSON DTOs
- `network/ApiService.kt` — Retrofit interface for `/predict`
- `network/RetrofitClient.kt` — Retrofit/OkHttp client, base URL pulled from
  `BuildConfig.API_BASE_URL` (defaults to `http://10.0.2.2:8000/`, which is
  how the Android emulator reaches a FastAPI server running on your laptop
  at `localhost:8000`)
- `repository/VoiceGuardRepository.kt` — single seam to flip from mock data
  to the live call: set `USE_MOCK_DATA = false` once Deepali's FastAPI
  simulator is running, and `analyzeAudio()` will hit the real `/predict`
  endpoint and `toRiskEvent()` will turn the response straight into the
  same `VoiceRiskEvent` model the Risk Log UI renders.

No UI code needs to change when the switch happens — only the repository's
data source.

## Design language

Dark void background (`#06080F`) with a radial navy-to-black gradient,
mint (`#22E6B0`) as the "protected/active" signal color, coral-red
(`#FF5C72`) for clone/lockdown alerts, and amber for the inconclusive
middle state — carried consistently across the score ring, risk pills,
shield glow, and lockdown screen so risk level is always legible at a
glance.
