# 🛡️ Aegis Focus — Unbreakable Digital Discipline & Focus Shield

<p align="center">
  <img alt="Aegis Focus Logo" src="assets/aegis_logo.png" width="160" style="border-radius: 28px;">
</p>

<p align="center">
  <b>Unbreakable Digital Discipline · Protect Your Attention</b><br>
  <b>অনর্থক স্ক্রলিং থামান, মনোযোগ সুরক্ষিত করুন।</b><br><br>
  A native, 100% local-first Android digital discipline system engineered with Kotlin Jetpack Compose, Room SQLite, cryptographic offline license validation, and unbypassable Device Administrator protection.
</p>

<p align="center">
  <a href="https://github.com/khalidabdullahh/AegisFocus/actions"><img src="https://img.shields.io/badge/CI%2FCD-GitHub%20Actions%20Passing-brightgreen?logo=github-actions" alt="Build Status" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Privacy-100%25%20Offline%20%26%20On--Device-10B981" alt="Privacy" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin" alt="Kotlin" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Android%20SDK-API%2026%20..%2035-3DDC84?logo=android" alt="Android" /></a>
  <a href="https://github.com/khalidabdullahh/AegisFocus/releases"><img src="https://img.shields.io/badge/Releases-Latest%20%26%20Archives-38BDF8?logo=github" alt="Releases" /></a>
</p>

---

## 📥 Direct APK Downloads & Release Archive

All release APKs are signed with production keys and are ready for sideloading or direct installation:

| Version | Release File | Notes & SHA256 Checksum | Direct Link |
|:---|:---|:---|:---:|
| **v1.1.0 (Latest)** | `AegisFocus-Latest.apk` | Rebranded identity, offline license generator, master admin pass `6924923086`, zero telemetry.<br>`SHA256: 315b72ddc8cfa874eccbf74de4a560b0139a48957226f5c71af61b0885138f99` | [Download](https://github.com/khalidabdullahh/AegisFocus/releases/latest/download/AegisFocus-Latest.apk) |
| **v1.1.0 (Archive)** | `releases/AegisFocus-v1.1.0-Release.apk` | Direct immutable release archive for v1.1.0 build.<br>`SHA256: ee75846496d661e1ab222ef11479da554baaeb480137fd7ead28ac812fd2e80f` | [Download](releases/AegisFocus-v1.1.0-Release.apk) |
| **v1.0.0 (Legacy)** | `releases/AegisFocus-v1.0.0-Release.apk` | Initial launch version with core accessibility blocking and discipline analytics.<br>`SHA256: 9051c347337dfd8d02cb2eab3538db1afefeeb6fd6c0d2b3272c2c515146a163` | [Download](releases/AegisFocus-v1.0.0-Release.apk) |

---

## 🌟 Key Features & Philosophy

- **100% Offline & Private (Zero Tracking)**:
  - No mandatory account creation or cloud database tracking.
  - All focus logs, app block rules, and discipline scores live strictly inside your device's local Room database (`aegis_focus.db`).
- **Unbreakable Strict Mode & Anti-Uninstall**:
  - Android Device Administrator lock preventing impulsive app uninstallation or disabling during active focus sessions.
- **Mindful Pause & Adaptive Friction**:
  - Escalating cooldown timers (5s → 10s → 20s → 30s) and intentional mindful prompts before app unblocking.
- **Mandatory Permission Onboarding Gate**:
  - Requires **Usage Access**, **Accessibility Blocker**, and **Notification Access** before entering the dashboard to ensure 100% effective distraction protection.
- **Offline Cryptographic License System**:
  - In-app HMAC SHA-256 license engine supporting lifetime and multi-month offline passes (`AEGIS-12M-XXXX-XXXX`, `AEGIS-LIFETIME-XXXX-XXXX`). Backward compatible with earlier license formats.
- **Built-in Super Admin License Key Generator**:
  - Tap the version card in Settings 5 times ➔ enter master password **`6924923086`** to unlock the embedded license key generator directly from your mobile.

---

## 🛡️ Free vs. Pro Feature Matrix

| Feature | Free ($0 / ৳০) | Pro Lifetime / Annual |
|---|:---:|:---:|
| Core app blocking (Accessibility + UsageStats) | ✅ | ✅ |
| Focus sessions & Timer | ✅ | ✅ |
| Basic schedules (Weekly routines) | ✅ | ✅ |
| Strict Mode (Up to 2 hours) | ✅ | ✅ |
| Mindful pause on intervention | ✅ | ✅ |
| Discipline Score & Streaks | ✅ | ✅ |
| Unlimited Strict Mode (6h–24h) | — | ✅ |
| Device Admin Anti-Uninstall Protection | — | ✅ |
| Advanced schedules & Unlimited profiles | — | ✅ |
| Website / domain blocking | — | ✅ |
| Notification Shield (Block distracting alerts) | — | ✅ |
| Adaptive friction (5s→30s escalation) | — | ✅ |
| 100% Offline Local Storage | ✅ | ✅ |

---

## ⚠️ Google Play Protect Notice (Sideloaded APKs)

When downloading and installing the APK directly without the Google Play Store, Android Play Protect may show a warning:
> *"Unsafe app blocked"* or *"Play Protect doesn't recognise this app's developer"*

### 💡 How to Install (Takes 2 Seconds):
1. On the Play Protect dialog, tap **"More details"** (or **"অতিরিক্ত তথ্য"**).
2. Tap **"Install anyway"** (or **"যেকোনোভাবে ইনস্টল করুন"**).

### 🔍 Why Does This Happen?
1. **Sideloading**: Any APK installed outside Google Play triggers a standard developer verification prompt.
2. **Sensitive Focus Permissions**: Aegis Focus uses Android's native `AccessibilityService` (to block apps in real-time) and `DeviceAdminReceiver` (to prevent cheating or uninstalling during Strict Mode). Google flags these high-level permissions on new unindexed apps until published on the Google Play Store.

---

## 🏗️ Clean Architecture Overview

```
com.aegisfocus.app/
├── core/
│   ├── di/               # AppContainer & ServiceLocator
│   ├── theme/            # Obsidian dark palette, Apple-style Typography, Shapes
│   ├── navigation/       # Screen routes & AegisNavHost
│   └── components/       # Reusable Compose design components
├── domain/
│   ├── model/            # FocusSession, FocusProfile, Schedule, AppInfo, PermissionState
│   ├── subscription/     # EntitlementManager & SubscriptionPlan
│   ├── mindfulness/      # MindfulnessEngine & AdaptiveFrictionController
│   ├── analytics/        # DisciplineScoreCalculator
│   ├── motivation/       # XpManager & DisciplineLevel
│   └── usecase/          # StartSession, EndSession, EmergencyExit, StrictModeCommit
├── data/
│   ├── local/            # Room Database (12 entities, 11 DAOs)
│   ├── billing/          # StoreBillingManager & PlayBillingManager
│   └── license/          # LicenseKeyValidator (HMAC SHA-256 + Super Admin Generator)
├── engine/               # FocusSessionManager, BlockingRuleRepository, BlockedAppMatcher
├── service/              # AegisAccessibilityBlockerService, AegisForegroundTimerService,
│                         # NotificationShieldService, AegisDeviceAdminReceiver, BootReceiver
└── presentation/
    ├── MainActivity.kt
    ├── onboarding/       # OnboardingScreen (Mandatory Permission Gate)
    ├── home/             # HomeScreen (Discipline Score, XP, Streaks)
    ├── focus/            # FocusScreen & ActiveFocusScreen
    ├── apps/             # AppsScreen (App discovery & profile assign)
    ├── stats/            # StatsScreen (Charts & insights)
    ├── settings/         # SettingsScreen (Secured Super Admin Panel)
    ├── paywall/          # PaywallScreen (Offline license key redemption)
    └── blocker/          # BlockInterventionActivity
```

---

## 🔑 Offline License Key Generation

You can generate valid customer license keys in two ways:

### 1. In-App Super Admin Panel (From Phone):
1. Open **Settings (⚙️)** in Aegis Focus.
2. Scroll to the bottom and tap **`Aegis Focus • v1.1.0`** 5 times quickly.
3. Enter Master Password: **`6924923086`**.
4. Select Plan (1 Month, 3 Months, 1 Year, Lifetime) and tap **"GENERATE LICENSE KEY"**.

### 2. Python CLI Generator (From Computer):
```bash
python3 tools/generate_license.py --plan lifetime --count 1
# Output: AEGIS-LIFETIME-XXXX-XXXX
```

---

## 💻 Local Development & Build

```bash
# Clone the repository
git clone https://github.com/khalidabdullahh/AegisFocus.git
cd AegisFocus

# Run unit tests
./gradlew test

# Assemble signed Release APK
./gradlew assembleRelease

# Output is located at:
# AegisFocus-Latest.apk or releases/AegisFocus-Latest.apk
```

---

## 📄 License & Privacy

- **100% Privacy Respected**: All focus logs and app usage records remain strictly on-device. Zero telemetry, zero analytics tracking.
- Developed with ❤️ by **Khalid Abdullah**.
