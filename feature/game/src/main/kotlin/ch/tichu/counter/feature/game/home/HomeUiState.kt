package ch.tichu.counter.feature.game.home

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.GameId
import kotlinx.datetime.Instant

@Immutable
data class CurrentGameUi(
    val gameId: GameId,
    val scoreA: Int,
    val scoreB: Int,
    val teamANames: String,
    val teamBNames: String,
    val roundNumber: Int,
    val startedAt: Instant,
    val groupName: String?,
    val isOtherGroup: Boolean,
)

@Immutable
data class HomeUiState(
    val groupName: String? = null,
    val isQuickPlay: Boolean = false,
    val currentGame: CurrentGameUi? = null,
    val showAbandonConfirmation: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface HomeUiEvent {
    data object NewGameClicked : HomeUiEvent

    data object ResumeGame : HomeUiEvent

    data object AbandonAndStartConfirmed : HomeUiEvent

    data object AbandonDismissed : HomeUiEvent

    data object SwitchGroupClicked : HomeUiEvent

    data object SettingsClicked : HomeUiEvent
}

sealed interface HomeUiEffect {
    data class NavigateToSetup(val abandonCurrent: Boolean) : HomeUiEffect

    data class NavigateToScoring(val gameId: GameId) : HomeUiEffect

    data object OpenGroupPicker : HomeUiEffect

    data object OpenSettings : HomeUiEffect
}
