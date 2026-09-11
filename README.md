# ASR — Turn Time Into Progress

**ASR** is a native, local-first Android application and digital discipline system designed to help users deliberately protect their attention, control distracting apps, and turn focused time into meaningful progress.

> **Live Web Simulator & Landing Page:** [https://khalidabdullahh.github.io/ASR-TurnTimeIntoProgress/](https://khalidabdullahh.github.io/ASR-TurnTimeIntoProgress/) — Open in your browser for an interactive preview without Android Studio.

---

## 🛡️ Core Highlights

- **ASR Focus Engine**: Minimalist, calm, dark-first interface crafted with Jetpack Compose & Material 3.
- **Persistent Focus Sessions**: Sessions survive app kills, swipe aways, and system reboots via epoch-timestamp persistence in local Room SQLite DB.
- **Modular App Blocking Engine**: Multi-layer detection using `AccessibilityService` for instant foreground intervention, backed by `UsageStatsManager` fallback monitoring.
- **Tamper-Resistant Strict Mode**: When active, session parameters and blocked app lists are strictly locked. Normal cancellation is disabled. (Free: up to 3h; Pro: unlimited).
- **Intentional Emergency Exit**: Multi-step friction mechanism (calm reflection, configurable cooldown timer, mindful phrase confirmation) with exit audit logging.
- **Discipline Score & Analytics**: 100% on-device transparent discipline formula (Base 80 + consistency/streak/goal bonuses − distraction/exit penalties).
- **XP Progression & Levels**: 5 tiers (🌱 New Starter → ⚡ Consistent → 🎯 Focused → 🔥 Disciplined → 🏆 Iron Will).
- **100% On-Device Privacy Guarantee**: Zero cloud servers, zero analytics trackers, zero accounts. All data resides securely in your local Room database.

---

## 🛡️ Free vs. Pro Feature Matrix

| Feature | Free ($0) | Pro ($19.99/yr) |
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
│   ├── di/               # AppContainer & ServiceLocator (clean dependency injection)
│   ├── theme/            # Obsidian dark palette, Typography, Shapes
│   ├── navigation/       # Screen sealed class + AegisNavHost (5-tab bottom nav + Paywall)
│   ├── common/           # TimeUtils, Result, epoch math
│   └── components/       # Reusable Design System (Timer, Cards, Buttons, Dialogs)
├── domain/
│   ├── model/            # FocusSession, FocusProfile, Schedule, AppInfo, AppCategory...
│   ├── subscription/     # EntitlementManager, SubscriptionPlan (Free/Pro capability gates)
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
| `focus_profiles` | Named focus profiles (Deep Work, Study, Sleep Calm, Quick Reset + custom) |
| `schedules` | Recurring timed rules (e.g. Study Mon–Fri 09:00–13:00) |
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

The Discipline Score is **fully transparent** — no hidden math:

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

---

## 🚀 Local Setup & Development

### 1. Open the Project
```bash
# Open in Android Studio → File → Open → select:
/Users/khalidabdullah/AntiGravity/aegis-focus
```

### 2. Run Unit Tests
```bash
cd aegis-focus
./gradlew test
```

All **10 unit test suites** cover:
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

**Online:** [https://khalidabdullahh.github.io/ASR-TurnTimeIntoProgress/](https://khalidabdullahh.github.io/ASR-TurnTimeIntoProgress/)

**Locally:**
```bash
cd /Users/khalidabdullah/AntiGravity/aegis-focus
python3 -m http.server 3000
# Then open: http://localhost:3000
```

---

## 📄 License

Private project — all rights reserved.
