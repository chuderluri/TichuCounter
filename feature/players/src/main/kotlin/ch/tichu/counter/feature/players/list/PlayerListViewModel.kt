package ch.tichu.counter.feature.players.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.person.ObserveGroupMembersUseCase
import ch.tichu.counter.core.model.PersonSummary
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
class PlayerListViewModel @Inject constructor(
    observeActiveGroup: ObserveActiveGroupUseCase,
    observeMembers: ObserveGroupMembersUseCase,
) : ViewModel() {

    private data class Filter(val query: String = "", val showArchived: Boolean = false)

    private val filter = MutableStateFlow(Filter())
    private val _effects = Channel<PlayerListUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val activeGroup = observeActiveGroup()

    private val members = activeGroup.flatMapLatest { active ->
        val group = active.group
        if (group == null) flowOf(emptyList()) else observeMembers(group.id, includeArchived = true)
    }

    val state: StateFlow<PlayerListUiState> = combine(activeGroup, members, filter) { active, members, filter ->
        val filtered = members
            .filter { filter.showArchived || !it.person.isArchived }
            .filter { filter.query.isBlank() || it.person.name.contains(filter.query, ignoreCase = true) }
        PlayerListUiState(
            groupName = active.group?.name,
            isQuickPlay = active.isQuickPlay,
            players = filtered.map { it.toRow() }.toImmutableList(),
            archivedCount = members.count { it.person.isArchived },
            showArchived = filter.showArchived,
            query = filter.query,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerListUiState())

    fun onEvent(event: PlayerListUiEvent) {
        when (event) {
            is PlayerListUiEvent.QueryChanged -> filter.update { it.copy(query = event.query) }
            PlayerListUiEvent.ToggleArchived -> filter.update { it.copy(showArchived = !it.showArchived) }
            is PlayerListUiEvent.PlayerClicked -> send(PlayerListUiEffect.NavigateToEdit(event.personId))
            PlayerListUiEvent.AddClicked -> send(PlayerListUiEffect.NavigateToEdit(null))
            PlayerListUiEvent.SwitchGroupClicked -> send(PlayerListUiEffect.OpenGroupPicker)
        }
    }

    private fun send(effect: PlayerListUiEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun PersonSummary.toRow() = PlayerRowUi(
        id = person.id,
        name = person.name,
        color = person.avatarColor,
        gamesPlayed = gamesPlayed,
        lastPlayedAt = lastPlayedAt,
        isArchived = person.isArchived,
        isInCurrentGame = isInCurrentGame,
    )
}
