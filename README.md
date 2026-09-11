# ASAR — Turn Time Into Progress

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/asar_logo_dark.png">
    <source media="(prefers-color-scheme: light)" srcset="assets/asar_logo_light.png">
    <img alt="ASAR — Be Productive" src="assets/asar_logo_dark.png" width="280">
  </picture>
</p>

<p align="center">
  <b>Turn Time Into Progress · Be Productive</b><br>
  A native, local-first Android focus and digital discipline application engineered with Kotlin Jetpack Compose, Room SQLite, Supabase Cloud Sync, Google Play Billing, and Device Administrator anti-uninstall protection.
</p>

<p align="center">
  <a href="https://github.com/khalidabdullahh/AegisFocus/actions"><img src="https://img.shields.io/badge/CI%2FCD-GitHub%20Actions%20Passing-brightgreen?logo=github-actions" alt="Build Status" /></a>
  <a href="https://owroghakrlwuqtmsozzf.supabase.co"><img src="https://img.shields.io/badge/Backend-Supabase%20PostgreSQL-3ECF8E?logo=supabase" alt="Supabase" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin" alt="Kotlin" /></a>
  <a href="#"><img src="https://img.shields.io/badge/Android%20SDK-API%2026%20..%2035-3DDC84?logo=android" alt="Android" /></a>
</p>

---

## 🚀 Key Updates & Features

- **Rebranded to ASAR**: Features custom brand lockup artwork with the signature ASR monogram and `ASAR — Turn Time Into Progress` identity.
- **Supabase Cloud Sync & Authentication**:
  - Secure Email/Password registration & login.
  - Native Google 1-Tap Sign-In with Credential Manager.
  - PostgreSQL schema with Row-Level Security (RLS) policies for cross-device stats synchronization.
- **Dual Monetization & License Engine**:
  - **Google Play Billing v7.1.1**: Subscription and in-app purchase lifecycle management.
  - **Offline Web License Redemption**: Cryptographic offline license keys (`ASAR-LIFE-XXXX-XXXX`) supported for direct website payments (e.g., bKash, Nagad, Stripe).
- **Discord Telemetry Dispatch**:
  - Real-time event notifications for new user signups, logins, and license activations sent to `#aegis-focus`.
- **Automated CI/CD APK Pipeline**:
  - GitHub Actions automatically compiles and packages clean debug APKs on every commit without requiring local Android Studio.
- **Strict Mode & Anti-Uninstall**:
  - Android Device Administrator integration preventing unauthorized uninstallation or bypass during active strict focus sessions.
- **Mindful Pause & Adaptive Friction**:
  - Escalating cooldown timers (5s → 10s → 20s → 30s) and intentional mindful intention prompts before app unblocking.
- **Transparent Discipline Score**:
  - Fully transparent 10-100 algorithmic score based on consistency, streaks, daily goals, and distraction penalties.

---

## 🛡️ Free vs. Pro Feature Matrix

| Feature | Free ($0) | Pro ($19.99/yr or $49.99 Lifetime) |
|---|:---:|:---:|
| Core app blocking (Accessibility + UsageStats) | ✅ | ✅ |
| Focus sessions & Timer | ✅ | ✅ |
| Basic schedules (Weekly routines) | ✅ | ✅ |
| Strict Mode (Up to 3 hours) | ✅ | ✅ |
| Mindful pause on intervention | ✅ | ✅ |
| Discipline Score & Streaks | ✅ | ✅ |
| Unlimited Strict Mode (6h–24h) | — | ✅ |
| Device Admin Anti-Uninstall Protection | — | ✅ |
| Advanced schedules & Unlimited profiles | — | ✅ |
| Website / domain blocking | — | ✅ |
| Notification Shield (Block distracting alerts) | — | ✅ |
| Adaptive friction (5s→30s escalation) | — | ✅ |
| Supabase Cloud Backup & Sync | — | ✅ |
| Deep Analytics & Event Logs | — | ✅ |

---

## 🏗️ Clean Architecture Overview

```
com.aegisfocus.app/
├── core/
│   ├── di/               # AppContainer & ServiceLocator
│   ├── theme/            # Obsidian dark palette, Typography, Shapes
│   ├── navigation/       # Screen routes & AegisNavHost
│   └── components/       # Reusable Compose design components
├── domain/
│   ├── model/            # FocusSession, FocusProfile, Schedule, AppInfo
│   ├── subscription/     # EntitlementManager & SubscriptionPlan
│   ├── mindfulness/      # MindfulnessEngine & AdaptiveFrictionController
│   ├── analytics/        # DisciplineScoreCalculator
│   ├── motivation/       # XpManager & DisciplineLevel
│   └── usecase/          # StartSession, EndSession, EmergencyExit, StrictModeCommit
├── data/
│   ├── local/            # Room Database (12 entities, 11 DAOs)
│   ├── remote/           # SupabaseConfig, SupabaseAuthService, SupabaseSyncService, DiscordNotifier
│   ├── billing/          # PlayBillingManager
│   └── license/          # LicenseKeyValidator (HMAC SHA-256)
├── engine/               # FocusSessionManager, BlockingRuleRepository, BlockedAppMatcher
├── service/              # AegisAccessibilityBlockerService, AegisForegroundTimerService,
│                         # NotificationShieldService, AegisDeviceAdminReceiver, BootReceiver
└── presentation/
    ├── MainActivity.kt
    ├── home/             # HomeScreen (Discipline Score, XP, Streaks)
    ├── focus/            # FocusScreen & ActiveFocusScreen
    ├── apps/             # AppsScreen (App discovery & profile assign)
    ├── stats/            # StatsScreen (Charts & insights)
    ├── auth/             # AuthScreen (Supabase Login / Register / Google 1-Tap)
    ├── paywall/          # PaywallScreen (4 plans & offline license redemption)
    └── blocker/          # BlockInterventionActivity
```

---

## ⚡ GitHub Actions (Automatic APK Downloads)

Whenever code is pushed to `main`, GitHub Actions automatically compiles the project into an Android APK.

### 📥 How to Download the Latest APK:
1. Navigate to the **[GitHub Actions Tab](https://github.com/khalidabdullahh/AegisFocus/actions)**.
2. Select the latest successful workflow run at the top.
3. Scroll down to the **Artifacts** section.
4. Click **`ASAR-Debug-APK`** to download the zip file.
5. Extract the `.apk` file and install it directly on any Android device (or upload to [Appetize.io](https://appetize.io) for in-browser testing).

---

## 💻 Local Development & Testing

### 1. Build via Gradle
```bash
# Clone the repository
git clone https://github.com/khalidabdullahh/AegisFocus.git
cd AegisFocus

# Run unit test suites
./gradlew test

# Assemble debug APK locally
./gradlew assembleDebug
```

### 2. Run the Web Simulator & Marketing Site
No Android environment needed:
```bash
python3 -m http.server 3000
# Open http://localhost:3000 in your browser
```

---

## 📄 License & Privacy

- **100% Privacy Respected**: All focus logs and app usage records remain strictly on-device by default.
- Cloud synchronization is opt-in via Supabase with encrypted transport.
- Private project — All rights reserved.
