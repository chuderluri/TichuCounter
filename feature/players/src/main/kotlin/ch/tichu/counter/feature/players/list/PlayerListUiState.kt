package ch.tichu.counter.feature.players.list

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.PersonId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Instant

@Immutable
data class PlayerRowUi(
    val id: PersonId,
    val name: String,
    val color: AvatarColor,
    val gamesPlayed: Int,
    val lastPlayedAt: Instant?,
    val isArchived: Boolean,
    val isInCurrentGame: Boolean,
)

@Immutable
data class PlayerListUiState(
    val groupName: String? = null,
    val isQuickPlay: Boolean = false,
    val players: ImmutableList<PlayerRowUi> = persistentListOf(),
    val archivedCount: Int = 0,
    val showArchived: Boolean = false,
    val query: String = "",
    val isLoading: Boolean = true,
)

sealed interface PlayerListUiEvent {
    data class QueryChanged(val query: String) : PlayerListUiEvent

    data object ToggleArchived : PlayerListUiEvent

    data class PlayerClicked(val personId: PersonId) : PlayerListUiEvent

    data object AddClicked : PlayerListUiEvent

    data object SwitchGroupClicked : PlayerListUiEvent
}

sealed interface PlayerListUiEffect {
    data class NavigateToEdit(val personId: PersonId?) : PlayerListUiEffect

    data object OpenGroupPicker : PlayerListUiEffect
}
