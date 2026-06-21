# 🛡️ AuraShield AI: Real-Time Mobile Deepfake & Voice Clone Transaction Shield

## 🛑 The Core Problem
Modern financial fraud has evolved. Threat actors leverage real-time generative AI voice cloning (such as synthetic speech models) to hijack active calls. By impersonating family members, banking representatives, or emergency personnel, attackers induce immediate panic, manipulating victims into authorizing irreversible peer-to-peer financial transfers (via platforms like Google Pay) while actively remaining on the phone line.

## 📱 The Technical Solution
AuraShield AI acts as an out-of-band, localized security interceptor on Android. It maps low-level telecommunication states directly to transaction security layers, executing local edge inference to intercept unauthorized asset migration the exact millisecond a vulnerability window opens.

---

## 🏗️ Architectural Core Engine

1. ⏱️ **Background Package Tracking Loop:**
   An active `BackgroundMonitorService` runs an optimized 500ms background execution loop utilizing `UsageStatsManager` to identify when protected financial registries (e.g., Google Pay: `com.google.android.apps.nbu.paisa.user`) are pulled into the system foreground framework.

2. 📊 **Dynamic Forensic Call Registry:**
   Utilizes a native Android `ContentResolver` pipeline to safely read incoming phone events directly from `CallLog.Calls`. Threat assessment metrics are bound directly to active device logs dynamically and rendered via a reactive Jetpack Compose interface.

3. 🛑 **WindowManager Intercept Shield:**
   When a threat threshold is crossed, the service injects a critical, system-level overlay directly into the WindowManager layer using high-priority layout flags (`TYPE_APPLICATION_OVERLAY`). This completely freezes user interaction within the target payment application.

4. 🔑 **Biometric Escape Engine (Anti-Recursion):**
   Features a thread-safe 30-second escape velocity state machine (`isCoolingOffActive`). Upon verified fingerprint biometric evaluation, the system suppresses overlay re-triggering for 30 seconds, enabling frictionless regular interaction without compromising ambient security loops.

5. 🧪 **Auditor Testing Sandbox:**
   Includes an integrated "Simulate Voice Attack Trigger" console configuration matrix. Due to Android kernel-level sandboxing restrictions which isolate standard carrier mic arrays to the native system dialer app, this toggle provides a deterministic testing sandbox to reliably demonstrate downstream threat intercept vectors during carrier network calls.

---

## 🛠️ Build & Installation Guide

### 📋 Prerequisites
* Android Studio (Ladybug or newer)
* Android SDK 34+ (Android 14)
* Gradle 8.0+
* A physical Android testing device connected via USB with Developer Options and USB Debugging activated.

### 💻 Automated Deployment
Open your system terminal workspace and execute:
```bash
# Clone the repository framework
git clone <repository-url>
cd aurashield-ai

# Compile Kotlin targets and verify syntax trees
./gradlew compileDebugKotlin

# Deploy package binary target directly to connected USB handset
./gradlew installDebug
```

### ⚠️ Essential Android Permission Configuration
To achieve systemic stability, ensure the following runtime permission profiles are authorized upon initial application bootstrap initialization:

* **Call Logs Access (READ_CALL_LOG):** Compiles live incoming device caller identities into the Forensic Detail view blocks.
* **App Usage Tracking (UsageStatsManager):** Grants the background window tracking system the authority to calculate financial target visibility matrices.
* **Display Over Other Apps (SYSTEM_ALERT_WINDOW):** Grants the core WindowManager layout engine permission to deploy the emergency coral transaction shield.

---

## 👥 Team Identity
Developed with 💻 by Team CodeHers:

👑 **Anisha Sadhukhan** (Team Leader)

🧠 **Deepali Kumari**

🛡️ **Pranavi Pathak**

---

## 📄 License & Copyright
⚖️ © 2026 CodeHers. All rights reserved. Developed exclusively for hackathon evaluation and prototype deployment. 🛡️
