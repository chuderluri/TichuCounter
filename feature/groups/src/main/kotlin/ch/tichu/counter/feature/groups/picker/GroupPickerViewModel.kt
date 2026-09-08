package ch.tichu.counter.feature.groups.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.usecase.game.ObserveCurrentGameUseCase
import ch.tichu.counter.core.domain.usecase.game.StartGameUseCase
import ch.tichu.counter.core.domain.usecase.group.ClearActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.CreateGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveGroupsUseCase
import ch.tichu.counter.core.domain.usecase.group.SetActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.preferences.ObservePreferencesUseCase
import ch.tichu.counter.core.model.LineUp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupPickerViewModel @Inject constructor(
    observeGroups: ObserveGroupsUseCase,
    observeActiveGroup: ObserveActiveGroupUseCase,
    private val observeCurrentGame: ObserveCurrentGameUseCase,
    private val setActiveGroup: SetActiveGroupUseCase,
    private val clearActiveGroup: ClearActiveGroupUseCase,
    private val createGroup: CreateGroupUseCase,
    private val observePreferences: ObservePreferencesUseCase,
    private val startGame: StartGameUseCase,
) : ViewModel() {

    private data class Draft(val isCreating: Boolean = false, val name: String = "", val error: Boolean = false)

    private val draft = MutableStateFlow(Draft())
    private val showAbandonConfirmation = MutableStateFlow(false)
    private val _effects = Channel<GroupPickerUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val state: StateFlow<GroupPickerUiState> = combine(
        observeGroups(),
        observeActiveGroup(),
        draft,
        showAbandonConfirmation,
    ) { groups, active, draft, showAbandon ->
        GroupPickerUiState(
            groups = groups.map {
                GroupUi(
                    id = it.group.id,
                    name = it.group.name,
                    memberCount = it.memberCount,
                    gameCount = it.gameCount,
                    isActive = it.group.id == active.group?.id,
                )
            }.toImmutableList(),
            isFirstStart = active.needsOnboarding,
            isLoading = false,
            isCreating = draft.isCreating || (active.needsOnboarding && groups.isEmpty() && draft.isCreating),
            newGroupName = draft.name,
            nameError = draft.error,
            showAbandonConfirmation = showAbandon,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupPickerUiState())

    fun onEvent(event: GroupPickerUiEvent) {
        when (event) {
            is GroupPickerUiEvent.GroupSelected -> viewModelScope.launch {
                setActiveGroup(event.groupId)
                _effects.send(GroupPickerUiEffect.NavigateToHome)
            }
            is GroupPickerUiEvent.EditGroup -> viewModelScope.launch {
                _effects.send(GroupPickerUiEffect.NavigateToGroupEdit(event.groupId))
            }
            GroupPickerUiEvent.CreateGroupClicked -> draft.update { it.copy(isCreating = true, error = false) }
            is GroupPickerUiEvent.NewGroupNameChanged -> draft.update { it.copy(name = event.name, error = false) }
            GroupPickerUiEvent.CancelCreateGroup -> draft.value = Draft()
            GroupPickerUiEvent.ConfirmCreateGroup -> viewModelScope.launch {
                when (val result = createGroup(draft.value.name)) {
                    is Result.Success -> {
                        setActiveGroup(result.value.id)
                        draft.value = Draft()
                        _effects.send(GroupPickerUiEffect.NavigateToGroupEdit(result.value.id))
                    }
                    is Result.Failure -> draft.update { it.copy(error = true) }
                }
            }
            GroupPickerUiEvent.QuickPlayClicked -> viewModelScope.launch {
                val hasCurrentGame = observeCurrentGame().first() != null
                if (hasCurrentGame) {
                    showAbandonConfirmation.value = true
                } else {
                    startQuickPlay(abandonCurrent = false)
                }
            }
            GroupPickerUiEvent.AbandonConfirmed -> viewModelScope.launch {
                showAbandonConfirmation.value = false
                startQuickPlay(abandonCurrent = true)
            }
            GroupPickerUiEvent.AbandonDismissed -> showAbandonConfirmation.value = false
        }
    }

    private suspend fun startQuickPlay(abandonCurrent: Boolean) {
        clearActiveGroup()
        val ruleSet = observePreferences().first().defaultRuleSet()
        when (val result = startGame(null, LineUp.allGuests(), ruleSet, abandonCurrent)) {
            is Result.Success -> _effects.send(GroupPickerUiEffect.NavigateToScoring(result.value))
            is Result.Failure -> _effects.send(GroupPickerUiEffect.ShowQuickPlayError)
        }
    }
}
