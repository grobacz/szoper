package com.szopper.domain.usecase

import com.szopper.domain.repository.SettingsRepository

class SetHapticFeedbackSettingUseCase(private val settingsRepository: SettingsRepository) {
    operator fun invoke(enabled: Boolean) = settingsRepository.setHapticFeedbackEnabled(enabled)
}
