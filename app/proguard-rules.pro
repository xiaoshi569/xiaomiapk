# Add project specific ProGuard rules here.
# You can find more details about customizing this file at
# https://developer.android.com/studio/build/shrink-code

# If you are using reflection, you might need to keep the classes and methods
# that you are accessing via reflection.
-keep class com.google.gson.stream.** { *; }

# For using GSON with obfuscated classes, you need to keep the fields.
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
