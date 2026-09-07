package ch.tichu.counter.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.preferences.ObservePreferencesUseCase
import ch.tichu.counter.core.domain.usecase.preferences.UpdatePreferencesUseCase
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SettingsUiState(
    val activeGroupName: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultTargetScore: Int = 1000,
    val targetScoreOptions: ImmutableList<Int> = RuleSet.TARGET_SCORE_OPTIONS.toImmutableList(),
    val finishOnTie: Boolean = false,
    val keepScreenOn: Boolean = true,
    val hapticFeedback: Boolean = true,
)

sealed interface SettingsUiEvent {
    data object ActiveGroupClicked : SettingsUiEvent

    data object ManageGroupsClicked : SettingsUiEvent

    data class ThemeChanged(val mode: ThemeMode) : SettingsUiEvent

    data class TargetScoreChanged(val score: Int) : SettingsUiEvent

    data class FinishOnTieChanged(val enabled: Boolean) : SettingsUiEvent

    data class KeepScreenOnChanged(val enabled: Boolean) : SettingsUiEvent

    data class HapticChanged(val enabled: Boolean) : SettingsUiEvent

    data object ExportClicked : SettingsUiEvent

    data object ImportClicked : SettingsUiEvent

    data object ReportBugClicked : SettingsUiEvent
}

sealed interface SettingsUiEffect {
    data object OpenGroupPicker : SettingsUiEffect

    data object OpenGroupManagement : SettingsUiEffect

    data object ShowUnavailable : SettingsUiEffect

    data object OpenBugReport : SettingsUiEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeActiveGroup: ObserveActiveGroupUseCase,
    observePreferences: ObservePreferencesUseCase,
    private val updatePreferences: UpdatePreferencesUseCase,
) : ViewModel() {

    private val _effects = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val state: StateFlow<SettingsUiState> = combine(observeActiveGroup(), observePreferences()) { active, prefs ->
        SettingsUiState(
            activeGroupName = active.group?.name,
            themeMode = prefs.themeMode,
            defaultTargetScore = prefs.defaultTargetScore,
            finishOnTie = prefs.finishOnTie,
            keepScreenOn = prefs.keepScreenOn,
            hapticFeedback = prefs.hapticFeedback,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.ActiveGroupClicked -> send(SettingsUiEffect.OpenGroupPicker)
            SettingsUiEvent.ManageGroupsClicked -> send(SettingsUiEffect.OpenGroupManagement)
            is SettingsUiEvent.ThemeChanged -> viewModelScope.launch { updatePreferences.themeMode(event.mode) }
            is SettingsUiEvent.TargetScoreChanged -> viewModelScope.launch { updatePreferences.defaultTargetScore(event.score) }
            is SettingsUiEvent.FinishOnTieChanged -> viewModelScope.launch { updatePreferences.finishOnTie(event.enabled) }
            is SettingsUiEvent.KeepScreenOnChanged -> viewModelScope.launch { updatePreferences.keepScreenOn(event.enabled) }
            is SettingsUiEvent.HapticChanged -> viewModelScope.launch { updatePreferences.hapticFeedback(event.enabled) }
            SettingsUiEvent.ExportClicked, SettingsUiEvent.ImportClicked -> send(SettingsUiEffect.ShowUnavailable)
            SettingsUiEvent.ReportBugClicked -> send(SettingsUiEffect.OpenBugReport)
        }
    }

    private fun send(effect: SettingsUiEffect) = viewModelScope.launch { _effects.send(effect) }
}
