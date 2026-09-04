package ch.tichu.counter.feature.groups.picker

import androidx.compose.runtime.Immutable
import ch.tichu.counter.core.model.GroupId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class GroupUi(
    val id: GroupId,
    val name: String,
    val memberCount: Int,
    val gameCount: Int,
    val isActive: Boolean,
)

@Immutable
data class GroupPickerUiState(
    val groups: ImmutableList<GroupUi> = persistentListOf(),
    val isFirstStart: Boolean = false,
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val newGroupName: String = "",
    val nameError: Boolean = false,
)

sealed interface GroupPickerUiEvent {
    data class GroupSelected(val groupId: GroupId) : GroupPickerUiEvent

    data class EditGroup(val groupId: GroupId) : GroupPickerUiEvent

    data object CreateGroupClicked : GroupPickerUiEvent

    data class NewGroupNameChanged(val name: String) : GroupPickerUiEvent

    data object ConfirmCreateGroup : GroupPickerUiEvent

    data object CancelCreateGroup : GroupPickerUiEvent

    data object JustPlayClicked : GroupPickerUiEvent
}

sealed interface GroupPickerUiEffect {
    data object NavigateToHome : GroupPickerUiEffect

    data class NavigateToGroupEdit(val groupId: GroupId?) : GroupPickerUiEffect

    data object NavigateToSetup : GroupPickerUiEffect

    data object Dismiss : GroupPickerUiEffect
}
