package com.kostasmavridis.tabataclock.di

import android.app.Application
import android.content.Context
import com.kostasmavridis.tabataclock.audio.ISoundManager
import com.kostasmavridis.tabataclock.audio.SoundManager
import com.kostasmavridis.tabataclock.data.ISettingsRepository
import com.kostasmavridis.tabataclock.data.SettingsRepository
import com.kostasmavridis.tabataclock.service.IntentServiceNotifier
import com.kostasmavridis.tabataclock.service.ServiceNotifier
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
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): ISettingsRepository = SettingsRepository(context)

    @Provides
    @Singleton
    fun provideSoundManager(
        @ApplicationContext context: Context
    ): ISoundManager = SoundManager(context)

    @Provides
    @Singleton
    fun provideServiceNotifier(
        application: Application
    ): ServiceNotifier = IntentServiceNotifier(application)
}
