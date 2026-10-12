package ch.tichu.counter.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.tichu.counter.core.domain.usecase.game.DeleteGameUseCase
import ch.tichu.counter.core.domain.usecase.game.ObserveGamesUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.model.GameSummary
import ch.tichu.counter.core.model.SeatOccupant
import ch.tichu.counter.core.model.Team
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameListViewModel @Inject constructor(
    observeActiveGroup: ObserveActiveGroupUseCase,
    observeGames: ObserveGamesUseCase,
    private val deleteGame: DeleteGameUseCase,
) : ViewModel() {

    private val pendingDelete = MutableStateFlow<GameListRowUi?>(null)
    private val _effects = Channel<GameListUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val activeGroup = observeActiveGroup()
    private val games = activeGroup.flatMapLatest { active -> observeGames(active.group?.id) }

    val state: StateFlow<GameListUiState> = combine(activeGroup, games, pendingDelete) { active, games, pending ->
        GameListUiState(
            groupName = active.group?.name,
            isQuickPlay = active.isQuickPlay,
            games = games.map { it.toUi() }.toImmutableList(),
            pendingDelete = pending,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameListUiState())

    fun onEvent(event: GameListUiEvent) {
        when (event) {
            is GameListUiEvent.GameClicked -> send(GameListUiEffect.NavigateToDetail(event.gameId))
            is GameListUiEvent.DeleteRequested -> pendingDelete.value = event.game
            GameListUiEvent.DeleteDismissed -> pendingDelete.value = null
            GameListUiEvent.DeleteConfirmed -> viewModelScope.launch {
                pendingDelete.value?.let { deleteGame(it.id) }
                pendingDelete.value = null
            }
            GameListUiEvent.SwitchGroupClicked -> send(GameListUiEffect.OpenGroupPicker)
        }
    }

    private fun send(effect: GameListUiEffect) = viewModelScope.launch { _effects.send(effect) }

    private fun GameSummary.toUi(): GameListRowUi = GameListRowUi(
        id = game.id,
        status = game.status,
        scoreA = scoreA,
        scoreB = scoreB,
        winner = winner,
        teamA = toMembers(currentLineUp.members(Team.A)),
        teamB = toMembers(currentLineUp.members(Team.B)),
        roundCount = roundCount,
        updatedAt = game.updatedAt,
    )

    private fun GameSummary.toMembers(occupants: List<SeatOccupant>): ImmutableList<GameMemberUi> = occupants.map { GameMemberUi(displayName(it), it is SeatOccupant.Guest) }.toImmutableList()
}
