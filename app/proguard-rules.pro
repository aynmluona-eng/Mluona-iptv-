# ProGuard rules for Mluona IPTV

# Preserve IPTV models
-keep class com.example.data.model.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
