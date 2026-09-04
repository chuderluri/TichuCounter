package ch.tichu.counter.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ch.tichu.counter.core.domain.repository.PreferencesRepository
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.ThemeMode
import ch.tichu.counter.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_TARGET_SCORE = intPreferencesKey("default_target_score")
        val FINISH_ON_TIE = booleanPreferencesKey("finish_on_tie")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val ACTIVE_GROUP_ID = stringPreferencesKey("active_group_id")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            defaultTargetScore = prefs[Keys.DEFAULT_TARGET_SCORE] ?: RuleSet.DEFAULT.targetScore,
            finishOnTie = prefs[Keys.FINISH_ON_TIE] ?: RuleSet.DEFAULT.finishOnTie,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            activeGroupId = prefs[Keys.ACTIVE_GROUP_ID]?.let(::GroupId),
        )
    }

    override suspend fun setActiveGroup(groupId: GroupId?) {
        dataStore.edit { prefs ->
            if (groupId == null) prefs.remove(Keys.ACTIVE_GROUP_ID) else prefs[Keys.ACTIVE_GROUP_ID] = groupId.value
        }
    }

    override suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDefaultTargetScore(score: Int) {
        dataStore.edit { it[Keys.DEFAULT_TARGET_SCORE] = score }
    }

    override suspend fun setFinishOnTie(enabled: Boolean) {
        dataStore.edit { it[Keys.FINISH_ON_TIE] = enabled }
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    override suspend fun setHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }
}
