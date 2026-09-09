# Aegis Focus — Digital Discipline & Attention Shield

**Aegis Focus** is a native, local-first Android application designed to help users deliberately control distracting apps and build digital discipline. Built with modern Android architecture: Jetpack Compose, Material 3, Room, Kotlin Coroutines, Flow, WorkManager, and a full Free/Pro entitlement system.

> **Live Web Simulator:** [https://khalidabdullahh.github.io/bProductive/](https://khalidabdullahh.github.io/bProductive/) — Open in your browser for an interactive UI preview without Android Studio.

---

## 🛡️ Core Features

| Feature | Free | Pro |
|---|:---:|:---:|
| Core app blocking | ✅ | ✅ |
| Focus sessions (Normal mode) | ✅ | ✅ |
| Basic schedules | ✅ | ✅ |
| Strict Mode (up to 3 hours) | ✅ | ✅ |
| Mindful pause on block | ✅ | ✅ |
| Discipline Score & Streak | ✅ | ✅ |
| Unlimited Strict Mode (6h–24h) | — | ✅ |
| Advanced schedules | — | ✅ |
| Unlimited profiles | — | ✅ |
| Website / domain blocking | — | ✅ |
| Notification Shield | — | ✅ |
| Adaptive friction (5s→30s escalation) | — | ✅ |
| Deep analytics | — | ✅ |

**Pricing:** Free · Pro Monthly $2.99 · Pro Yearly $19.99 · Lifetime $49.99 · Student $9.99/yr

---

## 🏗️ Architecture Overview

Clean Architecture + MVVM, fully local-first:

```
com.aegisfocus.app/
├── core/
│   ├── di/               # AppContainer & ServiceLocator (manual DI)
│   ├── theme/            # Obsidian dark palette, Typography, Shapes
│   ├── navigation/       # Screen sealed class + AegisNavHost (5-tab bottom nav + Paywall)
│   ├── common/           # TimeUtils, Result, epoch math
│   └── components/       # Reusable Design System (Timer, Cards, Buttons, Dialogs)
├── domain/
│   ├── model/            # FocusSession, FocusProfile, Schedule, AppInfo, AppCategory...
│   ├── subscription/     # EntitlementManager, SubscriptionPlan (Free/Pro caps)
│   ├── mindfulness/      # MindfulnessEngine, AdaptiveFrictionController, MindfulReason
│   ├── analytics/        # DisciplineScoreCalculator (transparent formula)
│   ├── motivation/       # XpManager, DisciplineLevel (NEW_STARTER→IRON_WILL), Achievement
│   ├── repository/       # Domain repository interfaces
│   └── usecase/          # StartSession, EndSession, EmergencyExit, RestoreSession, StrictModeCommit...
├── data/
│   ├── local/
│   │   ├── AegisDatabase.kt      # Room DB v2 (12 entities, 11 DAOs) with Migration(1,2)
│   │   ├── entity/               # v1: Session, Profile, Schedule, App, Snapshot, Exit
│   │   │                         # v2: FocusEvent, DailyGoal, UserXp, Achievement, Subscription, WebsiteRule
│   │   └── dao/                  # Comprehensive query DAOs for all 12 tables
│   └── repository/               # Repository implementations bridging Room & Android APIs
├── engine/               # FocusSessionManager, BlockingRuleRepository, BlockedAppMatcher,
│                         # ForegroundAppMonitor, BlockInterventionController, SessionPersistenceManager
├── service/              # AegisAccessibilityBlockerService, AegisForegroundTimerService,
│                         # NotificationShieldService (Pro), BootReceiver, AlarmReceiver, Workers
└── presentation/
    ├── MainActivity.kt
    ├── home/             # HomeScreen (Discipline Score + XP + streak + Pro nudge), HomeViewModel
    ├── focus/            # FocusScreen, ActiveFocusScreen, FocusViewModel
    ├── apps/             # AppsScreen, AppsViewModel
    ├── stats/            # StatsScreen, StatsViewModel
    ├── settings/         # SettingsScreen, SettingsViewModel
    ├── paywall/          # PaywallScreen (feature comparison, 4 plans, no dark patterns), PaywallViewModel
    ├── onboarding/       # OnboardingScreen, OnboardingViewModel
    └── blocker/          # BlockInterventionActivity
```

---

## 🗄️ Database Schema (v2)

Room database: `aegis_focus.db` — **version 2** with `Migration(1,2)`.

| Table | Purpose |
|---|---|
| `focus_sessions` | All focus sessions (active, completed, cancelled) |
| `focus_profiles` | Named focus profiles (Deep Work, Study, Sleep Calm, Quick Reset + user custom) |
| `schedules` | Recurring timed rules (e.g. Study every Mon–Fri 09:00–13:00) |
| `blocked_apps` | Per-session blocked app package names |
| `usage_snapshots` | Daily usage data snapshots for analytics |
| `emergency_exits` | Audit log of all Strict Mode emergency exits |
| `focus_events` *(v2)* | Analytics events: SESSION_STARTED, COMPLETED, BLOCK_ATTEMPT, MINDFUL_PAUSE, EMERGENCY_EXIT |
| `daily_goals` *(v2)* | User's daily focus targets (minutes + sessions) and completion state |
| `user_xp` *(v2)* | Cumulative XP, current level, streak (singleton row) |
| `achievements` *(v2)* | Earned achievement records |
| `subscription_state` *(v2)* | Active plan type + expiry (singleton row) |
| `website_rules` *(v2)* | Domain/keyword blocking rules (Pro feature) |

---

## 📐 Discipline Score Formula

The Discipline Score is **fully transparent** — no hidden algorithm:

```
Score = Base(80)
      + ConsistencyBonus   (0–10: based on 7-day goal completion rate)
      + GoalBonus          (0–5:  met today's focus goal)
      + StreakBonus        (0–5:  3+ day streak → +2, 7d → +3, 14d → +5)
      − DistractionPenalty (0–20: block attempts × 2, capped)
      − EmergencyExitPenalty (0–20: exits × 4, capped)

Clamped: [10, 100]
```

---

## 🎮 XP Progression System

| Level | XP Required | Emoji | Description |
|---|---|:---:|---|
| New Starter | 0 | 🌱 | Beginning your discipline journey |
| Consistent | 500 | ⚡ | Building reliable focus habits |
| Focused | 1,500 | 🎯 | Mastering intentional attention |
| Disciplined | 3,500 | 🔥 | Disciplined mind, deliberate action |
| Iron Will | 7,500 | 🏆 | Unbreakable. The highest tier. |

**XP Earning:**
- `2 XP/min` (capped at 200 per session)
- `+50 XP` bonus for Strict Mode sessions
- `+25 XP` bonus for zero-block-attempt "Laser Focus" sessions
- `+50 XP` for meeting daily goal
- `+XP` for unlocking achievements

---

## 🔒 Strict Mode — Free vs Pro

| | Free | Pro |
|---|:---:|:---:|
| Maximum duration | **3 hours** | **Unlimited** |
| Presets | 30m, 60m, 2h, 3h | 30m, 1h, 2h, 3h, 6h, 12h + custom |
| Commitment flow | ✅ | ✅ |
| Emergency exit friction | ✅ | ✅ |
| Adaptive friction escalation | — | ✅ (5s→10s→20s→30s) |

---

## 🚀 Local Setup & Development

### Prerequisites
- **Android Studio** Koala / Ladybug or newer
- **JDK 17+**
- Android device or emulator running **Android 8.0+ (API 26+)**

### 1. Open the Project
```bash
# Open in Android Studio → File → Open → select:
/Users/khalidabdullah/AntiGravity/aegis-focus
```

### 2. Sync Gradle
Android Studio will auto-sync. If not: **File → Sync Project with Gradle Files**.
Set Gradle JDK to Java 17: **Settings → Build, Execution, Deployment → Gradle → Gradle JDK**.

### 3. Required Android Permissions
These permissions require manual user grant in Android Settings:

| Permission | Where to Grant | Required For |
|---|---|---|
| Usage Access | Settings → Digital Wellbeing → Usage Access | Screen time analytics |
| Accessibility Service | Settings → Accessibility → Aegis Focus | App blocking (recommended) |
| Display over other apps | Settings → Apps → Aegis Focus → Display over | Block intervention overlay |
| Notification Listener | Settings → Notifications → Notification access | Notification Shield (Pro) |

### 4. Build & Run
```bash
# Run on connected device or emulator:
# Android Studio → Run → 'app' configuration

# Or command line (requires JAVA_HOME set):
cd aegis-focus
./gradlew assembleDebug
```

### 5. Run Unit Tests
```bash
cd aegis-focus
./gradlew test
```

All **10 unit test suites** must pass:
1. `SessionExpirationTest`
2. `StrictModeStateMachineTest`
3. `EmergencyExitLogicTest`
4. `SessionRestorationTest`
5. `BlockedAppMatcherTest`
6. `ScheduleCalculatorTest`
7. `EntitlementManagerTest`
8. `DisciplineScoreCalculatorTest`
9. `MindfulnessEngineTest` (includes `AdaptiveFrictionControllerTest`)
10. `XpMotivationTest` (includes `DisciplineLevelTest`)

---

## 🌐 Web Simulator (No Android Studio Needed)

Open the live simulator in a browser:

**Online:** [https://khalidabdullahh.github.io/bProductive/](https://khalidabdullahh.github.io/bProductive/)

**Locally:**
```bash
cd /Users/khalidabdullah/AntiGravity/aegis-focus
python3 -m http.server 3000
# Then open: http://localhost:3000
```

The simulator includes: app blocking flow, Strict Mode commitment, mindful pause, Discipline Score display, XP progression, Free/Pro toggle, and schedule management.

---

## 🏛️ Key Design Decisions

### 1. No Hilt — Manual DI
`AppContainer` (singleton) wired via `ServiceLocator`. Clean, predictable, no annotation processor overhead.

### 2. Epoch-Based Session Persistence
`endEpochMs` stored in Room. Timer ticks from `System.currentTimeMillis()`. Sessions survive app kills, swipe-aways, and reboots.

### 3. EntitlementManager — Central Capability Queries
Never scatter `isPro` flags. Always query a named capability:
```kotlin
entitlementManager.maxStrictDurationMinutes()   // 180 (Free) or Int.MAX_VALUE (Pro)
entitlementManager.canUseWebsiteBlocking()       // false (Free) or true (Pro)
entitlementManager.canUseNotificationShield()    // false (Free) or true (Pro)
```

### 4. minSdk = 26 (Android 8.0)
Targeting minSdk 26 covers ~95%+ of active Android devices. Keeping this broad is intentional.

### 5. 100% Local-First Privacy
Zero cloud servers. Zero analytics SDKs. Zero mandatory accounts. All data in `aegis_focus.db` on your device.

---

## 📄 License

Private project — all rights reserved.
