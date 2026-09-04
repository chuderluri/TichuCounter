package ch.tichu.counter.feature.players.edit

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.PersonId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class GroupMembershipUi(
    val groupId: GroupId,
    val name: String,
    val isMember: Boolean,
)

@Immutable
data class PlayerEditUiState(
    val personId: PersonId? = null,
    val name: String = "",
    val color: AvatarColor = AvatarColor.BLUE,
    val isArchived: Boolean = false,
    val groups: ImmutableList<GroupMembershipUi> = persistentListOf(),
    val isLocked: Boolean = false,
    val canDelete: Boolean = false,
    val nameError: Boolean = false,
    val isLoading: Boolean = true,
    val isDirty: Boolean = false,
) {
    val isNew: Boolean get() = personId == null
    val canSave: Boolean get() = !isLocked && name.isNotBlank() && (isNew || isDirty)
}

sealed interface PlayerEditUiEvent {
    data class NameChanged(val name: String) : PlayerEditUiEvent

    data class ColorPicked(val color: AvatarColor) : PlayerEditUiEvent

    data class GroupToggled(val groupId: GroupId) : PlayerEditUiEvent

    data object Save : PlayerEditUiEvent

    data object ToggleArchive : PlayerEditUiEvent

    data object Delete : PlayerEditUiEvent
}

sealed interface PlayerEditUiEffect {
    data object NavigateBack : PlayerEditUiEffect

    data class ShowMessage(val message: Message) : PlayerEditUiEffect

    enum class Message { SAVED, LOCKED, LAST_GROUP, NAME_ERROR }
}
