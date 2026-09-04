package ch.tichu.counter.feature.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import ch.tichu.counter.core.domain.usecase.game.ObserveGameStateUseCase
import ch.tichu.counter.core.domain.usecase.game.ObserveGameSummaryUseCase
import ch.tichu.counter.core.domain.usecase.game.RedoLastEventUseCase
import ch.tichu.counter.core.domain.usecase.game.UndoLastEventUseCase
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.RoundOutcome
import ch.tichu.counter.core.model.SeatOccupant
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TimelineEntry
import ch.tichu.counter.core.ui.component.HistoryRowUi
import ch.tichu.counter.core.ui.component.TichuBadgeUi
import ch.tichu.counter.core.ui.navigation.GameDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
data class GameDetailUiState(
    val gameId: GameId? = null,
    val status: GameStatus? = null,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val winner: Team? = null,
    val teamANames: String = "",
    val teamBNames: String = "",
    val history: ImmutableList<HistoryRowUi> = persistentListOf(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface GameDetailUiEvent {
    data object Continue : GameDetailUiEvent

    data object Undo : GameDetailUiEvent

    data object Redo : GameDetailUiEvent

    data object Back : GameDetailUiEvent
}

sealed interface GameDetailUiEffect {
    data class NavigateToScoring(val gameId: GameId) : GameDetailUiEffect

    data object NavigateBack : GameDetailUiEffect
}

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeState: ObserveGameStateUseCase,
    observeSummary: ObserveGameSummaryUseCase,
    private val undo: UndoLastEventUseCase,
    private val redo: RedoLastEventUseCase,
) : ViewModel() {

    private val gameId = GameId(savedStateHandle.toRoute<GameDetailRoute>().gameId)
    private val _effects = Channel<GameDetailUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val state: StateFlow<GameDetailUiState> = combine(observeState(gameId), observeSummary(gameId)) { game, summary ->
        if (game == null || summary == null) return@combine GameDetailUiState()
        GameDetailUiState(
            gameId = gameId,
            status = game.status,
            scoreA = game.scoreA,
            scoreB = game.scoreB,
            winner = game.winner,
            teamANames = summary.currentLineUp.members(Team.A).joinToString(" · ") { summary.displayName(it) },
            teamBNames = summary.currentLineUp.members(Team.B).joinToString(" · ") { summary.displayName(it) },
            history = game.toHistory().toImmutableList(),
            canUndo = game.canUndo,
            canRedo = game.canRedo,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameDetailUiState())

    fun onEvent(event: GameDetailUiEvent) {
        when (event) {
            GameDetailUiEvent.Continue -> send(GameDetailUiEffect.NavigateToScoring(gameId))
            GameDetailUiEvent.Undo -> viewModelScope.launch { undo(gameId) }
            GameDetailUiEvent.Redo -> viewModelScope.launch { redo(gameId) }
            GameDetailUiEvent.Back -> send(GameDetailUiEffect.NavigateBack)
        }
    }

    private fun send(effect: GameDetailUiEffect) = viewModelScope.launch { _effects.send(effect) }

    private fun ch.tichu.counter.core.model.GameState.toHistory(): List<HistoryRowUi> = timeline.mapIndexed { index, item ->
        when (item) {
            is TimelineEntry.Round -> HistoryRowUi.Round(
                roundNumber = item.result.roundNumber,
                totalA = item.result.teamA.total,
                totalB = item.result.teamB.total,
                badgesA = item.result.tichuCalls.filter { it.seat.team == Team.A }
                    .map { TichuBadgeUi(it.type, it.success) }.toImmutableList(),
                badgesB = item.result.tichuCalls.filter { it.seat.team == Team.B }
                    .map { TichuBadgeUi(it.type, it.success) }.toImmutableList(),
                doubleWin = (item.result.outcome as? RoundOutcome.DoubleWin)?.winner,
            )
            is TimelineEntry.Swap -> HistoryRowUi.Swap(
                index = index,
                previousName = item.previous.displayName(),
                nextName = item.next.displayName(),
                previousIsGuest = item.previous is SeatOccupant.Guest,
                nextIsGuest = item.next is SeatOccupant.Guest,
            )
        }
    }

    private fun SeatOccupant.displayName(): String = when (this) {
        is SeatOccupant.Guest -> name
        is SeatOccupant.Registered -> "Player"
    }
}
