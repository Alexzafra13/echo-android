package com.echo.app.di

import com.echo.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Named("isDebug")
    fun provideIsDebug(): Boolean = BuildConfig.DEBUG
}
