package ch.tichu.counter.feature.history

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.Team
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant

@Immutable
data class GameMemberUi(val name: String, val isGuest: Boolean)

@Immutable
data class GameListRowUi(
    val id: GameId,
    val status: GameStatus,
    val scoreA: Int,
    val scoreB: Int,
    val winner: Team?,
    val teamA: ImmutableList<GameMemberUi>,
    val teamB: ImmutableList<GameMemberUi>,
    val roundCount: Int,
    val updatedAt: Instant,
)

@Immutable
data class GameListUiState(
    val groupName: String? = null,
    val isQuickPlay: Boolean = false,
    val games: ImmutableList<GameListRowUi> = persistentListOf(),
    val pendingDelete: GameListRowUi? = null,
    val isLoading: Boolean = true,
)

sealed interface GameListUiEvent {
    data class GameClicked(val gameId: GameId) : GameListUiEvent

    data class DeleteRequested(val game: GameListRowUi) : GameListUiEvent

    data object DeleteConfirmed : GameListUiEvent

    data object DeleteDismissed : GameListUiEvent

    data object SwitchGroupClicked : GameListUiEvent
}

sealed interface GameListUiEffect {
    data class NavigateToDetail(val gameId: GameId) : GameListUiEffect

    data object OpenGroupPicker : GameListUiEffect
}
