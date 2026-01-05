package com.echo.feature.home.di

import com.echo.feature.home.data.api.RadioBrowserApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RadioBrowserClient

@Module
@InstallIn(SingletonComponent::class)
object RadioBrowserModule {

    private const val RADIO_BROWSER_BASE_URL = "https://de1.api.radio-browser.info/json/"

    @Provides
    @Singleton
    @RadioBrowserClient
    fun provideRadioBrowserOkHttpClient(
        @Named("isDebug") isDebug: Boolean
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Add User-Agent header as required by Radio Browser API
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "EchoAndroid/1.0")
                    .build()
                chain.proceed(request)
            }

        if (isDebug) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @RadioBrowserClient
    fun provideRadioBrowserRetrofit(
        @RadioBrowserClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(RADIO_BROWSER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideRadioBrowserApiService(
        @RadioBrowserClient retrofit: Retrofit
    ): RadioBrowserApiService {
        return retrofit.create(RadioBrowserApiService::class.java)
    }
}
