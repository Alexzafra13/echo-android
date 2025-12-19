package com.echo.core.database.di

import android.content.Context
import androidx.room.Room
import com.echo.core.database.EchoDatabase
import com.echo.core.database.dao.CachedAlbumDao
import com.echo.core.database.dao.CachedPlaylistDao
import com.echo.core.database.dao.CachedTrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): EchoDatabase {
        return Room.databaseBuilder(
            context,
            EchoDatabase::class.java,
            EchoDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development; use proper migrations in production
            .build()
    }

    @Provides
    fun provideCachedTrackDao(database: EchoDatabase): CachedTrackDao {
        return database.cachedTrackDao()
    }

    @Provides
    fun provideCachedAlbumDao(database: EchoDatabase): CachedAlbumDao {
        return database.cachedAlbumDao()
    }

    @Provides
    fun provideCachedPlaylistDao(database: EchoDatabase): CachedPlaylistDao {
        return database.cachedPlaylistDao()
    }
}
