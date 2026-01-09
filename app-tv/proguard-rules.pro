# Echo TV ProGuard Rules

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep TV/Leanback classes
-keep class androidx.leanback.** { *; }
-keep class androidx.tv.** { *; }
