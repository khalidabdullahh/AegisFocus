# 🌐 Google Play Store — Publishing & Billing Setup Guide

If you decide to publish on Google Play Store (via standard account or GitHub Student Developer Pack credits), follow these instructions to pass app review on your first attempt.

---

## 🚀 Step 1: Create Google Play Console Account
1. Visit: [https://play.google.com/console](https://play.google.com/console)
2. Sign in with your Google account.
3. Complete registration and identity verification.

---

## 🛡️ Step 2: Critical Permission Declarations (To Avoid Rejection)
Google Play requires strict declaration for `AccessibilityService` and `PACKAGE_USAGE_STATS`.

### A. Accessibility Service Declaration
* **Declaration Question**: Why does your app need the Accessibility API?
* **Copy-paste Answer**:
  > "ASAR is a digital wellbeing and focus discipline application. It uses the AccessibilityService API solely to detect when a user attempts to open an application they have explicitly added to their blocked focus list during an active focus session. When detected, ASAR presents a mindful pause overlay to help the user maintain their focus commitment. The service does not collect, transmit, or monitor keystrokes, personal messages, or user credentials."

### B. Usage Stats Permission Declaration
* **Declaration Question**: Why does your app request `PACKAGE_USAGE_STATS`?
* **Copy-paste Answer**:
  > "ASAR uses PACKAGE_USAGE_STATS strictly on-device to calculate daily screen time, session duration, and focus statistics. No usage analytics are uploaded to external third-party servers."

---

## 💳 Step 3: Google Play In-App Billing Configuration
1. In Google Play Console -> **Monetize with Play > In-app products / Subscriptions**:
2. **Subscriptions**:
   - Product ID: `asr_pro_monthly` -> Base Plan: Auto-renewing, $2.99 / month.
   - Product ID: `asr_pro_yearly` -> Base Plan: Auto-renewing, $19.99 / year.
   - Product ID: `asr_student_yearly` -> Base Plan: Auto-renewing, $9.99 / year.
3. **One-Time In-App Product**:
   - Product ID: `asr_pro_lifetime` -> Price: $49.99 (Non-consumable).

---

## 📦 Step 4: App Bundle / APK Upload
1. Build signed Android App Bundle (.aab):
   `./gradlew bundleRelease`
2. Upload to **Production / Closed Testing Track**.
3. Fill Store Listing details from `store_distribution/STORE_METADATA_AND_DESCRIPTIONS.md`.
4. Submit for review.
