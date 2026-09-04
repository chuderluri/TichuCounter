package ch.tichu.counter.feature.groups.edit

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.PersonId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class MemberUi(
    val id: PersonId,
    val name: String,
    val color: AvatarColor,
    val isInCurrentGame: Boolean,
)

@Immutable
data class CandidateUi(
    val id: PersonId,
    val name: String,
    val color: AvatarColor,
)

@Immutable
data class GroupEditUiState(
    val groupId: GroupId? = null,
    val name: String = "",
    val originalName: String = "",
    val isArchived: Boolean = false,
    val members: ImmutableList<MemberUi> = persistentListOf(),
    val candidates: ImmutableList<CandidateUi> = persistentListOf(),
    val nameError: Boolean = false,
    val isLoading: Boolean = true,
    val showAddExisting: Boolean = false,
    val showAddNew: Boolean = false,
    val newPersonName: String = "",
    val newPersonError: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && name != originalName
}

sealed interface GroupEditUiEvent {
    data class NameChanged(val name: String) : GroupEditUiEvent

    data object Save : GroupEditUiEvent

    data class EditMember(val personId: PersonId) : GroupEditUiEvent

    data class RemoveMember(val personId: PersonId) : GroupEditUiEvent

    data object AddExistingClicked : GroupEditUiEvent

    data class AddExisting(val personId: PersonId) : GroupEditUiEvent

    data object AddNewClicked : GroupEditUiEvent

    data class NewPersonNameChanged(val name: String) : GroupEditUiEvent

    data object ConfirmNewPerson : GroupEditUiEvent

    data object DismissDialogs : GroupEditUiEvent

    data object ToggleArchive : GroupEditUiEvent
}

sealed interface GroupEditUiEffect {
    data class NavigateToPlayerEdit(val personId: PersonId) : GroupEditUiEffect

    data class ShowMessage(val message: Message) : GroupEditUiEffect

    data object NavigateBack : GroupEditUiEffect

    enum class Message { PERSON_IN_GAME, SAVED }
}
