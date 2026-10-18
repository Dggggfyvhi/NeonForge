# Add project specific ProGuard rules here.
# Keep OkHttp/Gson default behavior stable.
-keep class com.google.gson.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
