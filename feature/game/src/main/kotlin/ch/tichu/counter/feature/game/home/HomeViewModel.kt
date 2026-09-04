package ch.tichu.counter.feature.game.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.tichu.counter.core.domain.usecase.game.ObserveCurrentGameUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveGroupUseCase
import ch.tichu.counter.core.model.GameSummary
import ch.tichu.counter.core.model.Team
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeActiveGroup: ObserveActiveGroupUseCase,
    observeCurrentGame: ObserveCurrentGameUseCase,
    observeGroup: ObserveGroupUseCase,
) : ViewModel() {

    private val showAbandon = MutableStateFlow(false)
    private val _effects = Channel<HomeUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val currentGameWithGroup = observeCurrentGame().flatMapLatest { summary ->
        val groupId = summary?.game?.groupId
        if (summary == null || groupId == null) {
            flowOf(summary to null)
        } else {
            observeGroup(groupId).flatMapLatest { group -> flowOf(summary to group?.name) }
        }
    }

    val state: StateFlow<HomeUiState> = combine(
        observeActiveGroup(),
        currentGameWithGroup,
        showAbandon,
    ) { active, (summary, gameGroupName), showAbandon ->
        HomeUiState(
            groupName = active.group?.name,
            isQuickPlay = active.isQuickPlay,
            currentGame = summary?.toUi(gameGroupName, isOtherGroup = summary.game.groupId != active.group?.id),
            showAbandonConfirmation = showAbandon,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.NewGameClicked -> {
                if (state.value.currentGame != null) {
                    showAbandon.value = true
                } else {
                    send(HomeUiEffect.NavigateToSetup(abandonCurrent = false))
                }
            }
            HomeUiEvent.ResumeGame -> state.value.currentGame?.let { send(HomeUiEffect.NavigateToScoring(it.gameId)) }
            HomeUiEvent.AbandonAndStartConfirmed -> {
                showAbandon.value = false
                send(HomeUiEffect.NavigateToSetup(abandonCurrent = true))
            }
            HomeUiEvent.AbandonDismissed -> showAbandon.value = false
            HomeUiEvent.SwitchGroupClicked -> send(HomeUiEffect.OpenGroupPicker)
            HomeUiEvent.SettingsClicked -> send(HomeUiEffect.OpenSettings)
        }
    }

    private fun send(effect: HomeUiEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun GameSummary.toUi(groupName: String?, isOtherGroup: Boolean) = CurrentGameUi(
        gameId = game.id,
        scoreA = scoreA,
        scoreB = scoreB,
        teamANames = currentLineUp.members(Team.A).joinToString(" · ") { displayName(it) },
        teamBNames = currentLineUp.members(Team.B).joinToString(" · ") { displayName(it) },
        roundNumber = roundCount + 1,
        startedAt = game.createdAt,
        groupName = groupName,
        isOtherGroup = isOtherGroup,
    )
}
