package ch.tichu.counter.feature.game.setup

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.Seat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.datetime.Instant

@Immutable
sealed interface SlotUi {
    @Immutable
    data class Person(val id: PersonId, val name: String, val color: AvatarColor) : SlotUi

    @Immutable
    data class Guest(val name: String) : SlotUi
}

@Immutable
data class MemberCandidateUi(
    val id: PersonId,
    val name: String,
    val color: AvatarColor,
    val lastPlayedAt: Instant?,
    val isSeated: Boolean,
)

@Immutable
data class GameSetupUiState(
    val groupName: String? = null,
    val isQuickPlay: Boolean = false,
    val slots: ImmutableMap<Seat, SlotUi?> = persistentMapOf(),
    val selectedSeat: Seat? = Seat.A1,
    val members: ImmutableList<MemberCandidateUi> = persistentListOf(),
    val query: String = "",
    val targetScore: Int = 1000,
    val targetScoreOptions: ImmutableList<Int> = persistentListOf(500, 1000, 1500, 2000),
    val renamingSeat: Seat? = null,
    val renameDraft: String = "",
    val showNewPerson: Boolean = false,
    val newPersonName: String = "",
    val newPersonError: Boolean = false,
    val isLoading: Boolean = true,
    val isStarting: Boolean = false,
) {
    val canStart: Boolean get() = slots.size == Seat.entries.size && slots.values.all { it != null } && !isStarting
}

sealed interface GameSetupUiEvent {
    data class SeatSelected(val seat: Seat) : GameSetupUiEvent

    data class PersonPicked(val personId: PersonId) : GameSetupUiEvent

    data object GuestPicked : GameSetupUiEvent

    data class ClearSeat(val seat: Seat) : GameSetupUiEvent

    data class RenameGuestStarted(val seat: Seat) : GameSetupUiEvent

    data class RenameDraftChanged(val name: String) : GameSetupUiEvent

    data object RenameConfirmed : GameSetupUiEvent

    data object RenameCancelled : GameSetupUiEvent

    data class QueryChanged(val query: String) : GameSetupUiEvent

    data object NewPersonClicked : GameSetupUiEvent

    data class NewPersonNameChanged(val name: String) : GameSetupUiEvent

    data object NewPersonConfirmed : GameSetupUiEvent

    data object NewPersonDismissed : GameSetupUiEvent

    data class TargetScoreChanged(val score: Int) : GameSetupUiEvent

    data object StartGame : GameSetupUiEvent

    data object CreateGroupInstead : GameSetupUiEvent
}

sealed interface GameSetupUiEffect {
    data class NavigateToScoring(val gameId: GameId) : GameSetupUiEffect

    data object ShowStartError : GameSetupUiEffect

    data object OpenGroupPicker : GameSetupUiEffect
}
