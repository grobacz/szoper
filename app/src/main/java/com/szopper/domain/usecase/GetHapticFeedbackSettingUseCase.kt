package com.szopper.domain.usecase

import com.szopper.domain.repository.SettingsRepository

class GetHapticFeedbackSettingUseCase(private val settingsRepository: SettingsRepository) {
    operator fun invoke() = settingsRepository.isHapticFeedbackEnabled()
}
