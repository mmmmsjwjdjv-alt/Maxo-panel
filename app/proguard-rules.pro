# Proguard & R8 Obfuscation Rules for DRAGON
-repackageclasses ''
-allowaccessmodification

# Keep Compose Runtime internals
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep data models used in JSON parsing
-keepclassmembers class com.example.data.** { *; }
-keep class com.example.data.api.** { *; }

# Obfuscate utilities and logic
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
