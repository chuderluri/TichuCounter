package ch.tichu.counter.core.domain.usecase.preferences

import ch.tichu.counter.core.domain.repository.PreferencesRepository
import ch.tichu.counter.core.model.ThemeMode
import ch.tichu.counter.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePreferencesUseCase @Inject constructor(private val repository: PreferencesRepository) {
    operator fun invoke(): Flow<UserPreferences> = repository.preferences
}

class UpdatePreferencesUseCase @Inject constructor(private val repository: PreferencesRepository) {
    suspend fun themeMode(mode: ThemeMode) = repository.setThemeMode(mode)

    suspend fun defaultTargetScore(score: Int) = repository.setDefaultTargetScore(score)

    suspend fun finishOnTie(enabled: Boolean) = repository.setFinishOnTie(enabled)

    suspend fun keepScreenOn(enabled: Boolean) = repository.setKeepScreenOn(enabled)

    suspend fun hapticFeedback(enabled: Boolean) = repository.setHapticFeedback(enabled)

    suspend fun setOnboardingDone(done: Boolean) = repository.setOnboardingDone(done)
}
