package ch.tichu.counter.feature.groups.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.usecase.group.AddGroupMemberUseCase
import ch.tichu.counter.core.domain.usecase.group.CreateGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.SetActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.person.CreatePersonUseCase
import ch.tichu.counter.core.domain.usecase.person.ObserveAllPersonsUseCase
import ch.tichu.counter.core.domain.usecase.person.ObserveGroupMembersUseCase
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupCreateViewModel @Inject constructor(
    private val createGroup: CreateGroupUseCase,
    private val setActiveGroup: SetActiveGroupUseCase,
    observeAllPersons: ObserveAllPersonsUseCase,
    observeMembers: ObserveGroupMembersUseCase,
    private val addMember: AddGroupMemberUseCase,
    private val createPerson: CreatePersonUseCase,
) : ViewModel() {

    private data class Draft(
        val groupId: GroupId? = null,
        val name: String = "",
        val nameError: Boolean = false,
        val phase: GroupCreateUiState.Phase = GroupCreateUiState.Phase.NAME,
        val showAddExisting: Boolean = false,
        val showAddNew: Boolean = false,
        val newPersonName: String = "",
        val newPersonError: Boolean = false,
    )

    private val draft = MutableStateFlow(Draft())
    private val _effects = Channel<GroupCreateUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val allPersons = observeAllPersons(includeArchived = false)

    private val members = draft.flatMapLatest { d ->
        val id = d.groupId ?: return@flatMapLatest flowOf(emptyList())
        observeMembers(id, includeArchived = false)
    }

    val state: StateFlow<GroupCreateUiState> = combine(draft, members, allPersons) { d, members, allPersons ->
        val memberIds = members.map { it.person.id }.toSet()
        GroupCreateUiState(
            phase = d.phase,
            groupId = d.groupId,
            name = d.name,
            nameError = d.nameError,
            members = members.map { MemberCandidateUi(it.person.id, it.person.name, it.person.avatarColor) }.toImmutableList(),
            candidates = allPersons.filter { it.id !in memberIds }
                .map { MemberCandidateUi(it.id, it.name, it.avatarColor) }
                .toImmutableList(),
            isLoading = false,
            showAddExisting = d.showAddExisting,
            showAddNew = d.showAddNew,
            newPersonName = d.newPersonName,
            newPersonError = d.newPersonError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupCreateUiState())

    fun onEvent(event: GroupCreateUiEvent) {
        when (event) {
            is GroupCreateUiEvent.NameChanged -> draft.update { it.copy(name = event.name, nameError = false) }
            GroupCreateUiEvent.CreateClicked -> create()
            GroupCreateUiEvent.AddExistingClicked -> draft.update { it.copy(showAddExisting = true) }
            is GroupCreateUiEvent.AddExisting -> viewModelScope.launch {
                val id = draft.value.groupId ?: return@launch
                addMember(id, event.personId)
                draft.update { it.copy(showAddExisting = false) }
            }
            GroupCreateUiEvent.AddNewClicked -> draft.update { it.copy(showAddNew = true, newPersonName = "", newPersonError = false) }
            is GroupCreateUiEvent.NewPersonNameChanged -> draft.update { it.copy(newPersonName = event.name, newPersonError = false) }
            GroupCreateUiEvent.ConfirmNewPerson -> viewModelScope.launch {
                val id = draft.value.groupId ?: return@launch
                val color = AvatarColor.forIndex(state.value.members.size)
                when (createPerson(draft.value.newPersonName, color, id)) {
                    is Result.Success -> draft.update { it.copy(showAddNew = false, newPersonName = "") }
                    is Result.Failure -> draft.update { it.copy(newPersonError = true) }
                }
            }
            GroupCreateUiEvent.DismissDialogs -> draft.update { it.copy(showAddExisting = false, showAddNew = false) }
            GroupCreateUiEvent.DoneClicked -> viewModelScope.launch { _effects.send(GroupCreateUiEffect.NavigateToHome) }
        }
    }

    private fun create() {
        val name = draft.value.name.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            when (val result = createGroup(name)) {
                is Result.Success -> {
                    setActiveGroup(result.value.id)
                    draft.update { it.copy(groupId = result.value.id, phase = GroupCreateUiState.Phase.MEMBERS) }
                }
                is Result.Failure -> draft.update { it.copy(nameError = true) }
            }
        }
    }
}
