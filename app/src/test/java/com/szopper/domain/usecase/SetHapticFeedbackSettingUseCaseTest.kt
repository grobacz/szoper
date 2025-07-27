package com.szopper.domain.usecase

import com.szopper.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SetHapticFeedbackSettingUseCaseTest {

    private lateinit var repository: SettingsRepository
    private lateinit var useCase: SetHapticFeedbackSettingUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = SetHapticFeedbackSettingUseCase(repository)
    }

    @Test
    fun `invoke should call repository`() {
        useCase.invoke(true)

        verify(repository).setHapticFeedbackEnabled(true)
    }
}
