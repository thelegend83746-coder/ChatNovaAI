# Proguard rules for ChatNova AI
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.chatnova.ai.data.remote.dto.** { *; }
-keep class com.chatnova.ai.data.local.entity.** { *; }
