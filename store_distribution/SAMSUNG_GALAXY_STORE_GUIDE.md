# 📱 Samsung Galaxy Store — Step-by-Step Publishing & IAP Guide

Samsung Galaxy Store is pre-installed on over **1 Billion+** Samsung smartphones, tablets, and Galaxy devices worldwide. Registration and publishing are **100% FREE** with **NO annual developer fees**.

---

## 🚀 Step 1: Create Samsung Developer Account
1. Visit: [https://developer.samsung.com/galaxy-store](https://developer.samsung.com/galaxy-store)
2. Click **Join Now** / **Sign In** with your Samsung Account.
3. Complete the free developer registration (select **Individual Developer**).
4. Go to **Samsung Seller Portal**: [https://seller.samsungapps.com](https://seller.samsungapps.com).

---

## 📦 Step 2: Add New Application
1. On Seller Portal dashboard, click **Add New Application**.
2. Choose **Android** -> Set Default Language: **English (US)**.
3. **Application Title**: `ASAR — Turn Time Into Progress`
4. **Package Name**: `com.aegisfocus.app`

---

## 🎨 Step 3: Upload Graphic Assets & Description
1. **App Icon**: 512 x 512 px PNG (located at `assets/` or `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`).
2. **Cover Image / Feature Graphic**: 1024 x 500 px PNG (located at `feature_graphic.png`).
3. **Screenshots**: Minimum 3 screenshots (1080 x 1920 px or 1080 x 2400 px PNG).
4. **Description**: Copy & paste from `store_distribution/STORE_METADATA_AND_DESCRIPTIONS.md`.
5. **Category**: `Applications > Productivity` or `Health/Fitness`.
6. **Age Rating**: Set to **4+ / All Ages**.
7. **Privacy Policy URL**: `https://bproductive.vercel.app/privacy.html`

---

## ⚙️ Step 4: Binary (.APK / .AAB) Upload
1. In the **Binary** tab, click **Add Binary**.
2. Select your signed APK (`app/build/outputs/apk/debug/app-debug.apk` or signed release APK).
3. Supported OS: Android 8.0 (Oreo) to Android 15 (API 26 to 35).
4. Target Devices: Samsung Galaxy Smartphones and Tablets.

---

## 💳 Step 5: In-App Purchases (IAP) on Samsung Galaxy Store
Samsung Galaxy Store provides two flexible options:
1. **Samsung IAP Integration**:
   - In Seller Portal, go to **In-App Purchase** tab -> **Add Item**.
   - Item IDs:
     - `asr_pro_monthly` ($2.99 / Subscription)
     - `asr_pro_yearly` ($19.99 / Subscription)
     - `asr_pro_lifetime` ($49.99 / Non-Consumable Item)
2. **Web / Direct License Key & Local Mobile Checkout**:
   - For users outside Samsung Pay or in regions like Bangladesh, ASAR includes the direct **bKash / Nagad / Web License Key Redemption** flow inside the app.
   - Users simply enter their Transaction ID or License Code, and the backend verifies and activates Pro automatically.

---

## 🛡️ Step 6: Samsung Policy Declarations
When prompted about permissions:
- **Accessibility Service**: Answer *"Used solely for on-device app blocking during focus sessions and showing the mindful pause screen."*
- **Usage Access**: Answer *"Used locally on device to calculate screen time statistics."*

Click **Submit for Review**. Samsung review typically takes **24 to 72 hours**.
