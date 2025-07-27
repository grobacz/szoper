package com.szopper.domain.usecase

import com.szopper.domain.repository.SettingsRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SetHapticFeedbackSettingUseCaseTest {

    @Test
    fun `invoke should call repository`() {
        val repository = mockk<SettingsRepository>(relaxed = true)
        val useCase = SetHapticFeedbackSettingUseCase(repository)

        useCase.invoke(true)

        verify { repository.setHapticFeedbackEnabled(true) }
    }
}
