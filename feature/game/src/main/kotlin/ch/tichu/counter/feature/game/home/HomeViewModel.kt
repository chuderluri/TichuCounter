package ch.tichu.counter.feature.game.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.usecase.game.ObserveCurrentGameUseCase
import ch.tichu.counter.core.domain.usecase.game.StartGameUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveGroupsUseCase
import ch.tichu.counter.core.domain.usecase.group.SetActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.preferences.ObservePreferencesUseCase
import ch.tichu.counter.core.model.GameSummary
import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.Team
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    observeGroups: ObserveGroupsUseCase,
    private val setActiveGroup: SetActiveGroupUseCase,
    private val observePreferences: ObservePreferencesUseCase,
    private val startGame: StartGameUseCase,
) : ViewModel() {

    private val showAbandon = MutableStateFlow(false)
    private val pendingAction = MutableStateFlow<PendingNewGameAction?>(null)
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
        observeGroups(),
        currentGameWithGroup,
        showAbandon,
    ) { active, groups, (summary, gameGroupName), showAbandon ->
        val groupGame = summary
            ?.takeIf { it.game.groupId != null }
            ?.toUi(gameGroupName, isOtherGroup = summary.game.groupId != active.group?.id)
        val quickPlayGame = summary
            ?.takeIf { it.game.groupId == null }
            ?.toUi(groupName = null, isOtherGroup = false)
        HomeUiState(
            groupName = active.group?.name,
            groups = groups.map {
                GroupUi(id = it.group.id, name = it.group.name, isActive = it.group.id == active.group?.id)
            }.toImmutableList(),
            isQuickPlay = active.isQuickPlay,
            groupGame = groupGame,
            quickPlayGame = quickPlayGame,
            gameInProgress = summary != null,
            showAbandonConfirmation = showAbandon,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.NewGameClicked -> {
                if (state.value.gameInProgress) {
                    pendingAction.value = PendingNewGameAction.SETUP
                    showAbandon.value = true
                } else {
                    send(HomeUiEffect.NavigateToSetup(abandonCurrent = false))
                }
            }
            HomeUiEvent.QuickPlayClicked -> {
                if (state.value.gameInProgress) {
                    pendingAction.value = PendingNewGameAction.QUICK_PLAY
                    showAbandon.value = true
                } else {
                    startQuickPlay(abandonCurrent = false)
                }
            }
            HomeUiEvent.ResumeGame -> state.value.groupGame?.let { send(HomeUiEffect.NavigateToScoring(it.gameId)) }
            HomeUiEvent.ResumeQuickPlay -> state.value.quickPlayGame?.let { send(HomeUiEffect.NavigateToScoring(it.gameId)) }
            HomeUiEvent.AbandonAndStartConfirmed -> {
                showAbandon.value = false
                when (pendingAction.value) {
                    PendingNewGameAction.SETUP -> send(HomeUiEffect.NavigateToSetup(abandonCurrent = true))
                    PendingNewGameAction.QUICK_PLAY -> startQuickPlay(abandonCurrent = true)
                    null -> Unit
                }
                pendingAction.value = null
            }
            HomeUiEvent.AbandonDismissed -> {
                showAbandon.value = false
                pendingAction.value = null
            }
            is HomeUiEvent.GroupSelected -> viewModelScope.launch {
                setActiveGroup(event.groupId)
            }
            HomeUiEvent.CreateGroupClicked -> send(HomeUiEffect.NavigateToGroupCreate)
            HomeUiEvent.SettingsClicked -> send(HomeUiEffect.OpenSettings)
        }
    }

    private fun startQuickPlay(abandonCurrent: Boolean) {
        viewModelScope.launch {
            val ruleSet = observePreferences().first().defaultRuleSet()
            when (val result = startGame(null, LineUp.allGuests(), ruleSet, abandonCurrent)) {
                is Result.Success -> send(HomeUiEffect.NavigateToScoring(result.value))
                is Result.Failure -> Unit
            }
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
