package com.echo.core.media.di

import com.echo.core.media.radio.DefaultMediaItemFactory
import com.echo.core.media.radio.MediaItemFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindMediaItemFactory(
        defaultMediaItemFactory: DefaultMediaItemFactory
    ): MediaItemFactory
}
