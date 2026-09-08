# Aegis Focus Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
}
