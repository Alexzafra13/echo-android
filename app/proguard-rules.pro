# Add project specific ProGuard rules here.

# ========================================
# Kotlinx Serialization
# ========================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes
-keep,includedescriptorclasses class com.echo.**$$serializer { *; }
-keepclassmembers class com.echo.** {
    *** Companion;
}
-keepclasseswithmembers class com.echo.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes (DTOs and domain models)
-keep class com.echo.**.data.dto.** { *; }
-keep class com.echo.**.domain.model.** { *; }
-keep class com.echo.core.datastore.preferences.SessionData { *; }
-keep class com.echo.core.datastore.preferences.SessionPreferences$SavedCredentials { *; }

# ========================================
# Retrofit
# ========================================
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep Retrofit service methods
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep generic signature of Call, Response (R8 full mode strips signatures)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ========================================
# OkHttp
# ========================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ========================================
# Room
# ========================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ========================================
# Hilt
# ========================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# ========================================
# Coroutines
# ========================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ========================================
# Media3 / ExoPlayer
# ========================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ========================================
# Coil
# ========================================
-dontwarn coil.**

# ========================================
# Compose
# ========================================
-dontwarn androidx.compose.**

# ========================================
# Security Crypto
# ========================================
-keep class androidx.security.crypto.** { *; }

# ========================================
# General Android
# ========================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
