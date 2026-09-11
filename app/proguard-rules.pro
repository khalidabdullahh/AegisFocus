# ASR Focus & Discipline Proguard Rules

# ─── General Annotations & Attributes ──────────────────────────────────────────
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# ─── Room Database Entities & DAOs ───────────────────────────────────────────
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.TypeConverter *;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ─── Google Play Billing Client ───────────────────────────────────────────────
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# ─── Kotlin Coroutines ────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── Jetpack Compose ──────────────────────────────────────────────────────────
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**
