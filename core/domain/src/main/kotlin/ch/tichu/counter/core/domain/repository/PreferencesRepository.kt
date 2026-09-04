package ch.tichu.counter.core.domain.repository

import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.ThemeMode
import ch.tichu.counter.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setActiveGroup(groupId: GroupId?)

    suspend fun setOnboardingDone(done: Boolean)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDefaultTargetScore(score: Int)

    suspend fun setFinishOnTie(enabled: Boolean)

    suspend fun setKeepScreenOn(enabled: Boolean)

    suspend fun setHapticFeedback(enabled: Boolean)
}
