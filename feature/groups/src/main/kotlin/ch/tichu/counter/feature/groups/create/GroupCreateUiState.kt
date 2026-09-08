package ch.tichu.counter.feature.groups.create

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.PersonId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class MemberCandidateUi(
    val id: PersonId,
    val name: String,
    val color: AvatarColor,
)

@Immutable
data class GroupCreateUiState(
    val phase: Phase = Phase.NAME,
    val groupId: GroupId? = null,
    val name: String = "",
    val nameError: Boolean = false,
    val members: ImmutableList<MemberCandidateUi> = persistentListOf(),
    val candidates: ImmutableList<MemberCandidateUi> = persistentListOf(),
    val isLoading: Boolean = false,
    val showAddExisting: Boolean = false,
    val showAddNew: Boolean = false,
    val newPersonName: String = "",
    val newPersonError: Boolean = false,
) {
    val canCreate: Boolean get() = name.isNotBlank() && !isLoading

    enum class Phase { NAME, MEMBERS }
}

sealed interface GroupCreateUiEvent {
    data class NameChanged(val name: String) : GroupCreateUiEvent

    data object CreateClicked : GroupCreateUiEvent

    data class AddExisting(val personId: PersonId) : GroupCreateUiEvent

    data object AddExistingClicked : GroupCreateUiEvent

    data object AddNewClicked : GroupCreateUiEvent

    data class NewPersonNameChanged(val name: String) : GroupCreateUiEvent

    data object ConfirmNewPerson : GroupCreateUiEvent

    data object DismissDialogs : GroupCreateUiEvent

    data object DoneClicked : GroupCreateUiEvent
}

sealed interface GroupCreateUiEffect {
    data object NavigateToHome : GroupCreateUiEffect

    data object ShowError : GroupCreateUiEffect
}
