package com.kostasmavridis.tabataclock.di

import android.content.Context
import com.kostasmavridis.tabataclock.audio.SoundManager
import com.kostasmavridis.tabataclock.data.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun provideSoundManager(@ApplicationContext context: Context): SoundManager =
        SoundManager(context)
}
