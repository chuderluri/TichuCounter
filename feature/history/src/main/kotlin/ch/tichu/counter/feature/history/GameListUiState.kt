package ch.tichu.counter.feature.history

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.Team
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant

@Immutable
data class GameListRowUi(
    val id: GameId,
    val status: GameStatus,
    val scoreA: Int,
    val scoreB: Int,
    val winner: Team?,
    val teamANames: String,
    val teamBNames: String,
    val roundCount: Int,
    val updatedAt: Instant,
    val hasGuests: Boolean,
)

@Immutable
data class GameListUiState(
    val groupName: String? = null,
    val isQuickPlay: Boolean = false,
    val games: ImmutableList<GameListRowUi> = persistentListOf(),
    val pendingDelete: GameId? = null,
    val isLoading: Boolean = true,
)

sealed interface GameListUiEvent {
    data class GameClicked(val gameId: GameId) : GameListUiEvent

    data class DeleteRequested(val gameId: GameId) : GameListUiEvent

    data object DeleteConfirmed : GameListUiEvent

    data object DeleteDismissed : GameListUiEvent

    data object SwitchGroupClicked : GameListUiEvent
}

sealed interface GameListUiEffect {
    data class NavigateToDetail(val gameId: GameId) : GameListUiEffect

    data object OpenGroupPicker : GameListUiEffect
}
