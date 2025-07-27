package com.szopper.domain.usecase

import com.szopper.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GetHapticFeedbackSettingUseCaseTest {

    private lateinit var repository: SettingsRepository
    private lateinit var useCase: GetHapticFeedbackSettingUseCase

    @Before
    fun setup() {
        repository = mock()
        useCase = GetHapticFeedbackSettingUseCase(repository)
    }

    @Test
    fun `invoke should call repository`() {
        useCase.invoke()

        verify(repository).isHapticFeedbackEnabled()
    }
}
