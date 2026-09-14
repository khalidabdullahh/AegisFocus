# 📦 Amazon Appstore — Step-by-Step Publishing & IAP Guide

Amazon Appstore serves millions of users across **Amazon Fire Tablets, Fire OS, and Android devices in 200+ countries**. Creating an Amazon Developer account and publishing is **100% FREE** with **NO registration fee**.

---

## 🚀 Step 1: Create Amazon Developer Account
1. Visit: [https://developer.amazon.com/apps-and-games](https://developer.amazon.com/apps-and-games)
2. Click **Sign In** (use existing Amazon account or create new).
3. Complete the Developer Registration profile (Individual / Company).

---

## 📱 Step 2: Add a New App
1. Go to **Dashboard** -> Click **Add a New App** -> Select **Android**.
2. **App Title**: `ASAR — Turn Time Into Progress`
3. **App SKU**: `ASAR-FOCUS-ANDROID`
4. **App Category**: `Productivity / Utilities`
5. **Customer Support Email**: `khalidabdullah.dev@gmail.com`
6. **Privacy Policy URL**: `https://asr-turntimeintoprogress.vercel.app/privacy.html`

---

## 🎨 Step 3: Images & Multimedia
1. **App Icon**: 512 x 512 px PNG.
2. **Promotional Banner**: 1024 x 500 px PNG.
3. **Screenshots**: Upload 3 to 10 screenshots (1080x1920 or 1920x1080).

---

## ⚙️ Step 4: Upload APK Files
1. Go to the **Upcoming Version > App Files** tab.
2. Select **Upload APK**.
3. Choose `ASAR-Latest.apk` or `app-debug.apk`.
4. **DRM Toggle**: Select **No** (or let Amazon apply DRM).
5. **Target Architecture**: ARM64 / ARMv7 / x86.

---

## 💳 Step 5: In-App Purchasing (IAP) on Amazon
1. In the Amazon Developer Console, open your app and go to the **In-App Items** tab.
2. Create matching In-App items:
   - **Subscription 1**:
     - Title: `ASAR Pro Monthly`
     - SKU: `asr_pro_monthly`
     - Price: `$2.99 / month`
   - **Subscription 2**:
     - Title: `ASAR Pro Yearly`
     - SKU: `asr_pro_yearly`
     - Price: `$19.99 / year`
   - **Entitlement / One-Time**:
     - Title: `ASAR Lifetime Pro`
     - SKU: `asr_pro_lifetime`
     - Price: `$49.99`
3. **Direct / Sideload Payment Fallback**:
   - Because Amazon Fire tablets do not run Google Play Services, ASAR automatically detects this at runtime and enables the direct **bKash / Nagad / Web License Key Redemption** flow so users can still activate Pro seamlessly.

---

## 🛡️ Step 6: Content Rating & Submission
1. **Content Rating**: Fill in questionnaire (No violence, no profanity -> Rating: **All Ages / Everyone**).
2. **Export Compliance**: Select **No** (standard encryption exemption).
3. Click **Submit App**. Amazon review typically completes within **24 hours**.
