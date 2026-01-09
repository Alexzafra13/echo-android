# Echo Android Auto ProGuard Rules

# Keep MediaBrowserService
-keep class com.echo.automotive.EchoAutoService { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep media classes
-keep class android.support.v4.media.** { *; }
-keep class androidx.media.** { *; }
