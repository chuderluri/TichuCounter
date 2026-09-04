package ch.tichu.counter.feature.groups.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.usecase.group.AddGroupMemberUseCase
import ch.tichu.counter.core.domain.usecase.group.ArchiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.RemoveGroupMemberUseCase
import ch.tichu.counter.core.domain.usecase.group.RenameGroupUseCase
import ch.tichu.counter.core.domain.usecase.person.CreatePersonUseCase
import ch.tichu.counter.core.domain.usecase.person.ObserveAllPersonsUseCase
import ch.tichu.counter.core.domain.usecase.person.ObserveGroupMembersUseCase
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.ui.navigation.GroupEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGroup: ObserveGroupUseCase,
    observeMembers: ObserveGroupMembersUseCase,
    observeAllPersons: ObserveAllPersonsUseCase,
    private val renameGroup: RenameGroupUseCase,
    private val archiveGroup: ArchiveGroupUseCase,
    private val addMember: AddGroupMemberUseCase,
    private val removeMember: RemoveGroupMemberUseCase,
    private val createPerson: CreatePersonUseCase,
) : ViewModel() {

    private val groupId: GroupId = GroupId(
        requireNotNull(savedStateHandle.toRoute<GroupEditRoute>().groupId) { "GroupEdit needs a groupId" },
    )

    private data class Draft(
        val name: String? = null,
        val nameError: Boolean = false,
        val showAddExisting: Boolean = false,
        val showAddNew: Boolean = false,
        val newPersonName: String = "",
        val newPersonError: Boolean = false,
    )

    private val draft = MutableStateFlow(Draft())
    private val _effects = Channel<GroupEditUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val state: StateFlow<GroupEditUiState> = combine(
        observeGroup(groupId),
        observeMembers(groupId, includeArchived = false),
        observeAllPersons(includeArchived = false),
        draft,
    ) { group, members, allPersons, draft ->
        if (group == null) return@combine GroupEditUiState(isLoading = true)
        val memberIds = members.map { it.person.id }.toSet()
        GroupEditUiState(
            groupId = group.id,
            name = draft.name ?: group.name,
            originalName = group.name,
            isArchived = group.isArchived,
            members = members.map {
                MemberUi(it.person.id, it.person.name, it.person.avatarColor, it.isInCurrentGame)
            }.toImmutableList(),
            candidates = allPersons.filter { it.id !in memberIds }
                .map { CandidateUi(it.id, it.name, it.avatarColor) }
                .toImmutableList(),
            nameError = draft.nameError,
            isLoading = false,
            showAddExisting = draft.showAddExisting,
            showAddNew = draft.showAddNew,
            newPersonName = draft.newPersonName,
            newPersonError = draft.newPersonError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupEditUiState())

    fun onEvent(event: GroupEditUiEvent) {
        when (event) {
            is GroupEditUiEvent.NameChanged -> draft.update { it.copy(name = event.name, nameError = false) }
            GroupEditUiEvent.Save -> viewModelScope.launch {
                val name = draft.value.name ?: return@launch
                when (renameGroup(groupId, name)) {
                    is Result.Success -> {
                        draft.update { it.copy(name = null) }
                        _effects.send(GroupEditUiEffect.ShowMessage(GroupEditUiEffect.Message.SAVED))
                    }
                    is Result.Failure -> draft.update { it.copy(nameError = true) }
                }
            }
            is GroupEditUiEvent.EditMember -> viewModelScope.launch {
                _effects.send(GroupEditUiEffect.NavigateToPlayerEdit(event.personId))
            }
            is GroupEditUiEvent.RemoveMember -> viewModelScope.launch {
                val result = removeMember(groupId, event.personId)
                if (result is Result.Failure && result.error == DomainError.PersonInActiveGame) {
                    _effects.send(GroupEditUiEffect.ShowMessage(GroupEditUiEffect.Message.PERSON_IN_GAME))
                }
            }
            GroupEditUiEvent.AddExistingClicked -> draft.update { it.copy(showAddExisting = true) }
            is GroupEditUiEvent.AddExisting -> viewModelScope.launch {
                addMember(groupId, event.personId)
                draft.update { it.copy(showAddExisting = false) }
            }
            GroupEditUiEvent.AddNewClicked -> draft.update { it.copy(showAddNew = true, newPersonName = "", newPersonError = false) }
            is GroupEditUiEvent.NewPersonNameChanged -> draft.update { it.copy(newPersonName = event.name, newPersonError = false) }
            GroupEditUiEvent.ConfirmNewPerson -> viewModelScope.launch {
                val color = AvatarColor.forIndex(state.value.members.size)
                when (createPerson(draft.value.newPersonName, color, groupId)) {
                    is Result.Success -> draft.update { it.copy(showAddNew = false, newPersonName = "") }
                    is Result.Failure -> draft.update { it.copy(newPersonError = true) }
                }
            }
            GroupEditUiEvent.DismissDialogs -> draft.update { it.copy(showAddExisting = false, showAddNew = false) }
            GroupEditUiEvent.ToggleArchive -> viewModelScope.launch {
                archiveGroup(groupId, !state.value.isArchived)
                _effects.send(GroupEditUiEffect.NavigateBack)
            }
        }
    }
}
