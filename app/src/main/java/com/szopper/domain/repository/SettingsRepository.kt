package com.szopper.domain.repository

interface SettingsRepository {
    fun isHapticFeedbackEnabled(): Boolean
    fun setHapticFeedbackEnabled(enabled: Boolean)
}
