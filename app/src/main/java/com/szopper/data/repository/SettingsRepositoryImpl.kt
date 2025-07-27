package com.szopper.data.repository

import android.content.Context
import com.szopper.domain.repository.SettingsRepository

class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override fun isHapticFeedbackEnabled(): Boolean {
        return sharedPreferences.getBoolean("haptic_feedback", true)
    }

    override fun setHapticFeedbackEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("haptic_feedback", enabled).apply()
    }
}
