package ch.tichu.counter.feature.game.home

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GroupId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
data class GroupUi(
    val id: GroupId,
    val name: String,
    val isActive: Boolean,
)

@Immutable
data class HomeUiState(
    val groupName: String? = null,
    val groups: ImmutableList<GroupUi> = persistentListOf(),
    val isQuickPlay: Boolean = false,
    val needsOnboarding: Boolean = false,
    val groupGame: CurrentGameUi? = null,
    val quickPlayGame: CurrentGameUi? = null,
    val gameInProgress: Boolean = false,
    val showAbandonConfirmation: Boolean = false,
    val isLoading: Boolean = true,
)

enum class PendingNewGameAction { SETUP, QUICK_PLAY }

sealed interface HomeUiEvent {
    data object NewGameClicked : HomeUiEvent

    data object QuickPlayClicked : HomeUiEvent

    data object ResumeGame : HomeUiEvent

    data object ResumeQuickPlay : HomeUiEvent

    data object AbandonAndStartConfirmed : HomeUiEvent

    data object AbandonDismissed : HomeUiEvent

    data class GroupSelected(val groupId: GroupId) : HomeUiEvent

    data object CreateGroupClicked : HomeUiEvent

    data object SettingsClicked : HomeUiEvent
}

sealed interface HomeUiEffect {
    data class NavigateToSetup(val abandonCurrent: Boolean) : HomeUiEffect

    data class NavigateToScoring(val gameId: GameId) : HomeUiEffect

    data object OpenGroupPicker : HomeUiEffect

    data object NavigateToGroupCreate : HomeUiEffect

    data object OpenSettings : HomeUiEffect
}
