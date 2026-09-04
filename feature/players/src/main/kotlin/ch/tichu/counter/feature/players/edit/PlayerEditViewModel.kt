package ch.tichu.counter.feature.players.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveGroupsOfPersonUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveGroupsUseCase
import ch.tichu.counter.core.domain.usecase.person.ArchivePersonUseCase
import ch.tichu.counter.core.domain.usecase.person.CreatePersonUseCase
import ch.tichu.counter.core.domain.usecase.person.DeletePersonUseCase
import ch.tichu.counter.core.domain.usecase.person.IsPersonInCurrentGameUseCase
import ch.tichu.counter.core.domain.usecase.person.ObservePersonUseCase
import ch.tichu.counter.core.domain.usecase.person.SetPersonGroupMembershipUseCase
import ch.tichu.counter.core.domain.usecase.person.UpdatePersonUseCase
import ch.tichu.counter.core.domain.repository.PersonRepository
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.ui.navigation.PlayerEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePerson: ObservePersonUseCase,
    observeGroups: ObserveGroupsUseCase,
    observeGroupsOfPerson: ObserveGroupsOfPersonUseCase,
    private val observeActiveGroup: ObserveActiveGroupUseCase,
    private val isPersonInCurrentGame: IsPersonInCurrentGameUseCase,
    private val personRepository: PersonRepository,
    private val createPerson: CreatePersonUseCase,
    private val updatePerson: UpdatePersonUseCase,
    private val archivePerson: ArchivePersonUseCase,
    private val deletePerson: DeletePersonUseCase,
    private val setMembership: SetPersonGroupMembershipUseCase,
) : ViewModel() {

    private val personId: PersonId? = savedStateHandle.toRoute<PlayerEditRoute>().personId?.let(::PersonId)

    private data class Draft(
        val name: String? = null,
        val color: AvatarColor? = null,
        val nameError: Boolean = false,
    )

    private val draft = MutableStateFlow(Draft())
    private val _effects = Channel<PlayerEditUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val personFlow = if (personId == null) flowOf(null) else observePerson(personId)
    private val membershipsFlow = if (personId == null) flowOf(emptyList()) else observeGroupsOfPerson(personId)
    private val lockFlow = flow { emit(personId != null && isPersonInCurrentGame(personId)) }
    private val canDeleteFlow = flow { emit(personId != null && !personRepository.hasGames(personId)) }

    val state: StateFlow<PlayerEditUiState> = combine(
        personFlow,
        observeGroups(),
        membershipsFlow,
        lockFlow,
        canDeleteFlow,
        draft,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val person = values[0] as ch.tichu.counter.core.model.Person?
        val groups = values[1] as List<ch.tichu.counter.core.model.GroupSummary>
        val memberships = values[2] as List<ch.tichu.counter.core.model.Group>
        val locked = values[3] as Boolean
        val canDelete = values[4] as Boolean
        val draft = values[5] as Draft
        val memberIds = memberships.map { it.id }.toSet()
        PlayerEditUiState(
            personId = person?.id,
            name = draft.name ?: person?.name ?: "",
            color = draft.color ?: person?.avatarColor ?: AvatarColor.forIndex((0..9).random()),
            isArchived = person?.isArchived ?: false,
            groups = groups.map { GroupMembershipUi(it.group.id, it.group.name, it.group.id in memberIds) }
                .toImmutableList(),
            isLocked = locked,
            canDelete = canDelete && !locked,
            nameError = draft.nameError,
            isLoading = personId != null && person == null,
            isDirty = draft.name != null || draft.color != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerEditUiState(isLoading = personId != null))

    fun onEvent(event: PlayerEditUiEvent) {
        when (event) {
            is PlayerEditUiEvent.NameChanged -> draft.update { it.copy(name = event.name, nameError = false) }
            is PlayerEditUiEvent.ColorPicked -> draft.update { it.copy(color = event.color) }
            is PlayerEditUiEvent.GroupToggled -> toggleGroup(event.groupId)
            PlayerEditUiEvent.Save -> save()
            PlayerEditUiEvent.ToggleArchive -> viewModelScope.launch {
                val id = personId ?: return@launch
                when (val result = archivePerson(id, !state.value.isArchived)) {
                    is Result.Success -> _effects.send(PlayerEditUiEffect.NavigateBack)
                    is Result.Failure -> if (result.error == DomainError.PersonInActiveGame) {
                        _effects.send(PlayerEditUiEffect.ShowMessage(PlayerEditUiEffect.Message.LOCKED))
                    }
                }
            }
            PlayerEditUiEvent.Delete -> viewModelScope.launch {
                val id = personId ?: return@launch
                if (deletePerson(id) is Result.Success) _effects.send(PlayerEditUiEffect.NavigateBack)
            }
        }
    }

    private fun save() {
        viewModelScope.launch {
            val current = state.value
            val result = if (personId == null) {
                val activeGroupId = observeActiveGroup().first().group?.id
                createPerson(current.name, current.color, activeGroupId)
            } else {
                updatePerson(personId, current.name, current.color)
            }
            when (result) {
                is Result.Success -> {
                    draft.value = Draft()
                    _effects.send(PlayerEditUiEffect.ShowMessage(PlayerEditUiEffect.Message.SAVED))
                    if (personId == null) _effects.send(PlayerEditUiEffect.NavigateBack)
                }
                is Result.Failure -> when (result.error) {
                    DomainError.PersonInActiveGame ->
                        _effects.send(PlayerEditUiEffect.ShowMessage(PlayerEditUiEffect.Message.LOCKED))
                    else -> draft.update { it.copy(nameError = true) }
                }
            }
        }
    }

    private fun toggleGroup(groupId: GroupId) {
        val id = personId ?: return
        val current = state.value.groups.firstOrNull { it.groupId == groupId } ?: return
        viewModelScope.launch {
            when (val result = setMembership(id, groupId, !current.isMember)) {
                is Result.Success -> Unit
                is Result.Failure -> when (result.error) {
                    DomainError.LastGroupMembership ->
                        _effects.send(PlayerEditUiEffect.ShowMessage(PlayerEditUiEffect.Message.LAST_GROUP))
                    DomainError.PersonInActiveGame ->
                        _effects.send(PlayerEditUiEffect.ShowMessage(PlayerEditUiEffect.Message.LOCKED))
                    else -> Unit
                }
            }
        }
    }
}
