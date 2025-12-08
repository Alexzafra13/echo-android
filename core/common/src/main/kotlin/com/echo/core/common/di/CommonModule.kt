package com.echo.core.common.di

import com.echo.core.common.util.CoroutineDispatchers
import com.echo.core.common.util.DefaultCoroutineDispatchers
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommonModule {

    @Binds
    @Singleton
    abstract fun bindCoroutineDispatchers(
        impl: DefaultCoroutineDispatchers
    ): CoroutineDispatchers
}
