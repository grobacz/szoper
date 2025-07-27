package com.szopper.di

import android.content.Context
import com.szopper.data.repository.SettingsRepositoryImpl
import com.szopper.domain.repository.SettingsRepository
import com.szopper.domain.usecase.GetHapticFeedbackSettingUseCase
import com.szopper.domain.usecase.SetHapticFeedbackSettingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideGetHapticFeedbackSettingUseCase(settingsRepository: SettingsRepository): GetHapticFeedbackSettingUseCase {
        return GetHapticFeedbackSettingUseCase(settingsRepository)
    }

    @Provides
    @Singleton
    fun provideSetHapticFeedbackSettingUseCase(settingsRepository: SettingsRepository): SetHapticFeedbackSettingUseCase {
        return SetHapticFeedbackSettingUseCase(settingsRepository)
    }
}
