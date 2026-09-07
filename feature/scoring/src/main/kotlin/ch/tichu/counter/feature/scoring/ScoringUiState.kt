package ch.tichu.counter.feature.scoring

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TichuType
import ch.tichu.counter.core.ui.component.HistoryRowUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SeatUi(
    val seat: Seat,
    val name: String,
    val color: AvatarColor?,
    val isGuest: Boolean,
    val tichu: DraftTichu? = null,
)

@Immutable
data class DraftTichu(
    val type: TichuType,
    val success: Boolean,
)

@Immutable
data class ScoringUiState(
    val gameId: GameId? = null,
    val status: ScoringStatus = ScoringStatus.LOADING,
    val targetScore: Int = 1000,
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val roundNumber: Int = 1,
    val seats: ImmutableList<SeatUi> = persistentListOf(),
    val history: ImmutableList<HistoryRowUi> = persistentListOf(),
    val enteredA: String = "",
    val enteredB: String = "",
    val activeTeam: Team = Team.A,
    val doubleWin: Team? = null,
    val bonusA: Int = 0,
    val bonusB: Int = 0,
    val roundOptionsExpanded: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val validationError: Boolean = false,
    val showAbandonConfirmation: Boolean = false,
    val showFinishedDialog: Boolean = false,
    val winner: Team? = null,
    val durationMinutes: Int = 0,
    val finishedRoundListExpanded: Boolean = false,
) {
    val isKeypadEnabled: Boolean get() = status == ScoringStatus.IN_PROGRESS && doubleWin == null
    val isConfirmEnabled: Boolean
        get() = status == ScoringStatus.IN_PROGRESS && !validationError &&
            (doubleWin != null || enteredA.isNotBlank() || enteredB.isNotBlank())

    fun seat(seat: Seat): SeatUi? = seats.firstOrNull { it.seat == seat }
}

enum class ScoringStatus {
    LOADING,
    IN_PROGRESS,
    FINISHED,
    ABANDONED,
}

sealed interface ScoringUiEvent {
    data class Digit(val value: Int) : ScoringUiEvent

    data object Backspace : ScoringUiEvent

    data object ToggleSign : ScoringUiEvent

    data class SwitchTeam(val team: Team) : ScoringUiEvent

    data class DoubleWin(val team: Team?) : ScoringUiEvent

    data class TichuToggled(val seat: Seat, val type: TichuType) : ScoringUiEvent

    data object ToggleRoundOptions : ScoringUiEvent

    data object ConfirmRound : ScoringUiEvent

    data object Undo : ScoringUiEvent

    data object Redo : ScoringUiEvent

    data class SwapPlayerRequested(val seat: Seat) : ScoringUiEvent

    data class RoundRowClicked(val roundNumber: Int) : ScoringUiEvent

    data object AbandonGameRequested : ScoringUiEvent

    data object AbandonGameConfirmed : ScoringUiEvent

    data object AbandonDismissed : ScoringUiEvent

    data object FinishedDialogDismissed : ScoringUiEvent

    data object ToggleFinishedRoundList : ScoringUiEvent

    data object NewGameClicked : ScoringUiEvent

    data object HomeClicked : ScoringUiEvent
}

sealed interface ScoringUiEffect {
    data class ShowRoundSavedSnackbar(val roundNumber: Int) : ScoringUiEffect

    data object ShowInvalidRound : ScoringUiEffect

    data class NavigateToSwapDialog(val gameId: GameId, val seat: Seat) : ScoringUiEffect

    data class NavigateToSetup(val abandonCurrent: Boolean) : ScoringUiEffect

    data class NavigateToScoring(val gameId: GameId) : ScoringUiEffect

    data object NavigateHome : ScoringUiEffect

    data object NavigateBack : ScoringUiEffect
}
