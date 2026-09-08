# Aegis Focus — Digital Discipline & Attention Shield

**Aegis Focus** is a native, local-first Android application designed to help users deliberately control distracting apps and build digital discipline. Built with modern Android architecture, Jetpack Compose, Material 3, Room, Kotlin Coroutines, Flow, and WorkManager.

---

## 🛡️ Core Highlights

- **Obsidian Dark Calm UI**: Minimalist, calm, dark-first interface crafted with Jetpack Compose & Material 3.
- **Persistent Focus Sessions**: Sessions survive app kills, swipe aways, and system reboots via epoch-timestamp persistence in local Room SQLite DB.
- **Modular App Blocking Engine**: Multi-layer detection using `AccessibilityService` for instant foreground intervention, backed by `UsageStatsManager` fallback monitoring.
- **Tamper-Resistant Strict Mode**: When active, session parameters and blocked app lists are strictly locked. Normal cancellation is disabled.
- **Intentional Emergency Exit**: Multi-step friction mechanism (calm reflection, configurable cooldown timer, mindful phrase confirmation) with exit audit logging.
- **Local Analytics & Screen Time Tracking**: 100% on-device analytics, discipline streak calculation, and distraction breakdown.
- **Recurring Focus Schedules**: Exact alarms and WorkManager routines for daily habits (e.g. Study 09:00–13:00, Sleep 23:00–07:00).
- **100% On-Device Privacy Guarantee**: Zero cloud servers, zero analytics trackers, zero accounts. All data resides securely in your local Room database.

---

## 📁 Architecture Overview

Aegis Focus follows Clean Architecture and MVVM patterns:

```
com.aegisfocus.app/
├── core/
│   ├── di/               # AppContainer & ServiceLocator (clean dependency injection)
│   ├── theme/            # Obsidian palette, typography, custom shapes, calm colors
│   ├── navigation/       # Type-safe Screen definitions & AegisNavHost bottom navigation
│   ├── common/           # TimeUtils, Epoch Math, Result wrappers
│   └── components/       # Reusable Design System (FocusTimerDisplay, ModeSelector, StatCard...)
├── data/
│   ├── local/            # Room Database (6 entities, 6 DAOs, type converters)
│   └── repository/       # Repository implementations bridging Room & platform managers
├── domain/
│   ├── model/            # Core models: FocusSession, FocusProfile, Schedule, AppInfo...
│   ├── repository/       # Domain repository contracts
│   └── usecase/          # Business logic: StartSession, EndSession, EmergencyExit, RestoreSession...
├── engine/               # Modular Blocking Engine (FocusSessionManager, Matcher, Monitor, Intervention)
├── service/              # AccessibilityService, ForegroundTimerService, BootReceiver, AlarmReceiver, Workers
└── presentation/         # Jetpack Compose UI: Home, Focus, Apps, Stats, Settings, Onboarding, Blocker
```

---

## 🚀 Local Setup & Development Instructions

### 1. Opening the Project
1. Open **Android Studio** (Koala / Ladybug or newer recommended).
2. Select **File → Open...**
3. Select the `aegis-focus` folder in this workspace:
   ```
   /Users/khalidabdullah/AntiGravity/aegis-focus
   ```

### 2. Syncing Gradle
1. Android Studio will automatically prompt to sync Gradle.
2. If it does not sync automatically, click **File → Sync Project with Gradle Files** or click the Elephant Sync icon in the top toolbar.
3. Make sure your JDK in Android Studio is set to **Java 17** or higher (**Settings/Preferences → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**).

### 3. Running on Android Emulator
1. Open **Device Manager** in Android Studio.
2. Create or select an Android Virtual Device (AVD) running **Android 14 (API 34)** or **Android 15 (API 35)** with Google Play Services.
3. Click the green **Run ▶** button in Android Studio (or press `Shift + F10`).
4. Alternatively from terminal:
   ```bash
   ./gradlew installDebug
   ```

### 4. Running on a Physical Android Device
1. Enable **Developer Options** and **USB Debugging** on your Android phone.
2. Connect your device via USB cable and allow USB debugging when prompted on the screen.
3. Select your device in the Android Studio target dropdown and click **Run ▶**.

---

## 🔐 Required Permissions Guide

Aegis Focus relies solely on legitimate Android platform capabilities. For full functionality, enable the following in the app:

| Permission | Why It Is Needed | How to Enable |
|---|---|---|
| **Usage Access** | To query daily screen time, distraction breakdown, and streak metrics. | Go to **Settings tab → Usage Access → Grant** (Settings → Apps → Special app access → Usage access → Aegis Focus → Allow). |
| **Accessibility Service** | Detects when a shielded distracting app is opened in real time and displays the calm pause intervention. | Go to **Settings tab → Accessibility Blocker Service → Configure** (Settings → Accessibility → Downloaded apps → Aegis Focus → Turn ON). |
| **Notifications** | Shows the ongoing countdown timer and prevents background process interruption. | Prompted on first launch or enabled via **Settings tab → Persistent Notifications**. |
| **Display Over Other Apps** *(Optional)* | Allows calm intervention dialogs over blocked apps. | **Settings tab → Display Over Apps → Allow**. |
| **Exact Alarms** *(Optional)* | Ensures scheduled routines start exactly on the minute. | **Settings tab → Exact Alarms → Allow**. |

---

## 🧪 Testing Features Step-by-Step

### 1. Testing App Blocking
1. Open the **Apps** tab in Aegis Focus.
2. Select one or more installed apps you want to block (e.g., YouTube, Chrome, Instagram, or any sample app).
3. Go to the **Focus** tab.
4. Set a duration (e.g. 5 or 25 minutes), keep **Normal** or **Strict** mode, and tap **START FOCUS SESSION**.
5. Return to the home screen of your phone and tap on the blocked application.
6. **Result**: The calm **Aegis Discipline Shield** intervention appears ("Take a Deep Breath. [App] is paused during your Focus Session") preventing compulsive access and allowing you to return to focus.

### 2. Testing Strict Mode & Emergency Exit
1. In the **Focus** tab, select **Strict Mode**.
2. Tap **START FOCUS SESSION**.
3. Notice that:
   - The app list in the **Apps** tab is locked (cannot be edited).
   - Session duration cannot be reduced.
   - The normal "End Session" button is replaced with "Strict Discipline Active (Locked)".
4. Tap **Emergency Exit**.
5. The **Mindful Reflection Dialog** will appear:
   - Enforces a 5-second mindfulness cooldown.
   - Requires typing *"I choose to break focus"*.
   - Once confirmed, session ends with an audit record in local analytics.

### 3. Testing Persistent Session Recovery
1. Start an active focus session in Aegis Focus.
2. Swipe Aegis Focus away from the Android Recent Apps / multitasking list (killing the app process).
3. Re-open Aegis Focus.
4. **Result**: Aegis Focus immediately recalculates the exact remaining epoch time from the local Room database, resumes the live countdown timer, and restores active blocking without losing state.

### 4. Testing Scheduled Routines
1. Go to the **Settings** tab.
2. Tap **+ Add Schedule** and create a routine (e.g. Current Hour/Minute + 1 minute).
3. Enable the routine toggle.
4. When the scheduled time arrives, `ScheduleAlarmReceiver` and `ScheduleCheckWorker` automatically activate the focus session.

---

## 🧩 Running Automated Tests

Run all unit tests verifying session expiration math, strict mode state machine, emergency exit logic, session restoration, blocked app matcher, and schedule bitmask calculations:

```bash
cd aegis-focus
./gradlew test
```

Unit test suites included:
- `SessionExpirationTest`: Validates boundary conditions and progress fraction calculations.
- `StrictModeStateMachineTest`: Ensures strict mode rule immutability and blocks illegal cancellations.
- `EmergencyExitLogicTest`: Tests multi-friction exit recording and penalty logging.
- `SessionRestorationTest`: Validates background recovery across process termination and reboots.
- `BlockedAppMatcherTest`: Tests thread-safe package matching and critical system app whitelisting.
- `ScheduleCalculatorTest`: Tests days-of-week bitmasks and midnight-crossing schedule calculations.

---

## 🛠️ Troubleshooting Common Issues

1. **Accessibility Service keeps turning off on OEM devices (Xiaomi / Samsung / OnePlus):**
   - OEM battery optimizations may put background services to sleep. Go to *Settings → Battery → Battery Optimization / App Battery Usage → Aegis Focus → Set to "Unrestricted"*.
2. **Blocked App does not trigger intervention on Emulator:**
   - Ensure the Accessibility Service is enabled for Aegis Focus in *Settings → Accessibility → Downloaded apps → Aegis Focus*.
   - If running on a minimal Android Emulator without third-party apps, select Chrome or any installed app from the **Apps** tab.
3. **Gradle Sync Error / Java Version Mismatch:**
   - Ensure your Android Studio is using JDK 17 (`Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK: Java 17`).

---

## 📄 License & Originality Notice

Aegis Focus is an original, production-quality digital discipline application built with custom architecture, clean styling, and privacy-respecting local APIs. No third-party proprietary assets or branding are used.
