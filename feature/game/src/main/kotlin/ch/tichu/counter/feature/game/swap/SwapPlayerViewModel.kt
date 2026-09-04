package ch.tichu.counter.feature.game.swap

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.usecase.game.ObserveGameStateUseCase
import ch.tichu.counter.core.domain.usecase.game.SwapPlayerUseCase
import ch.tichu.counter.core.domain.usecase.person.CreatePersonUseCase
import ch.tichu.counter.core.domain.usecase.person.ObserveGroupMembersUseCase
import ch.tichu.counter.core.domain.repository.PersonRepository
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.SeatOccupant
import ch.tichu.counter.core.ui.navigation.SwapPlayerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SwapCandidateUi(val id: PersonId, val name: String, val color: AvatarColor)

@Immutable
data class SwapPlayerUiState(
    val seat: Seat,
    val currentName: String = "",
    val isQuickPlay: Boolean = false,
    val candidates: ImmutableList<SwapCandidateUi> = persistentListOf(),
    val query: String = "",
    val guestName: String = "",
    val showGuestField: Boolean = false,
    val showNewPerson: Boolean = false,
    val newPersonName: String = "",
    val newPersonError: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface SwapPlayerUiEvent {
    data class QueryChanged(val query: String) : SwapPlayerUiEvent

    data class PersonPicked(val personId: PersonId) : SwapPlayerUiEvent

    data object GuestClicked : SwapPlayerUiEvent

    data class GuestNameChanged(val name: String) : SwapPlayerUiEvent

    data object GuestConfirmed : SwapPlayerUiEvent

    data object NewPersonClicked : SwapPlayerUiEvent

    data class NewPersonNameChanged(val name: String) : SwapPlayerUiEvent

    data object NewPersonConfirmed : SwapPlayerUiEvent

    data object Dismiss : SwapPlayerUiEvent
}

sealed interface SwapPlayerUiEffect {
    data object Dismiss : SwapPlayerUiEffect

    data object ShowError : SwapPlayerUiEffect
}

@HiltViewModel
class SwapPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGameState: ObserveGameStateUseCase,
    observeMembers: ObserveGroupMembersUseCase,
    private val personRepository: PersonRepository,
    private val createPerson: CreatePersonUseCase,
    private val swapPlayer: SwapPlayerUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SwapPlayerRoute>()
    private val gameId = GameId(route.gameId)
    private val seat = Seat.valueOf(route.seat)

    private data class Draft(
        val query: String = "",
        val guestName: String = "",
        val showGuestField: Boolean = false,
        val showNewPerson: Boolean = false,
        val newPersonName: String = "",
        val newPersonError: Boolean = false,
    )

    private val draft = MutableStateFlow(Draft(guestName = SeatOccupant.defaultGuestName(seat)))
    private val _effects = Channel<SwapPlayerUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val gameState = observeGameState(gameId)
    private val members = gameState.flatMapLatest { state ->
        val groupId = state?.groupId
        if (groupId == null) flowOf(emptyList()) else observeMembers(groupId)
    }

    val state: StateFlow<SwapPlayerUiState> = combine(gameState, members, draft) { game, members, draft ->
        if (game == null) return@combine SwapPlayerUiState(seat = seat)
        val seated = game.lineUp.registeredPersons()
        val current = game.lineUp.occupantAt(seat)
        val currentName = when (current) {
            is SeatOccupant.Guest -> current.name
            is SeatOccupant.Registered -> members.firstOrNull { it.person.id == current.personId }?.person?.name
                ?: personRepository.getPerson(current.personId)?.name ?: "?"
        }
        SwapPlayerUiState(
            seat = seat,
            currentName = currentName,
            isQuickPlay = game.groupId == null,
            candidates = members
                .filter { it.person.id !in seated }
                .filter { draft.query.isBlank() || it.person.name.contains(draft.query, ignoreCase = true) }
                .map { SwapCandidateUi(it.person.id, it.person.name, it.person.avatarColor) }
                .toImmutableList(),
            query = draft.query,
            guestName = draft.guestName,
            showGuestField = draft.showGuestField || game.groupId == null,
            showNewPerson = draft.showNewPerson,
            newPersonName = draft.newPersonName,
            newPersonError = draft.newPersonError,
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SwapPlayerUiState(seat = seat))

    fun onEvent(event: SwapPlayerUiEvent) {
        when (event) {
            is SwapPlayerUiEvent.QueryChanged -> draft.update { it.copy(query = event.query) }
            is SwapPlayerUiEvent.PersonPicked -> swap(SeatOccupant.Registered(event.personId))
            SwapPlayerUiEvent.GuestClicked -> draft.update { it.copy(showGuestField = true) }
            is SwapPlayerUiEvent.GuestNameChanged -> draft.update { it.copy(guestName = event.name) }
            SwapPlayerUiEvent.GuestConfirmed -> {
                val name = draft.value.guestName.trim().ifBlank { SeatOccupant.defaultGuestName(seat) }
                swap(SeatOccupant.Guest(name))
            }
            SwapPlayerUiEvent.NewPersonClicked -> draft.update { it.copy(showNewPerson = true, newPersonName = it.query, newPersonError = false) }
            is SwapPlayerUiEvent.NewPersonNameChanged -> draft.update { it.copy(newPersonName = event.name, newPersonError = false) }
            SwapPlayerUiEvent.NewPersonConfirmed -> viewModelScope.launch {
                val groupId = gameState.first()?.groupId ?: return@launch
                when (val result = createPerson(draft.value.newPersonName, AvatarColor.forIndex(state.value.candidates.size), groupId)) {
                    is Result.Success -> swap(SeatOccupant.Registered(result.value.id))
                    is Result.Failure -> draft.update { it.copy(newPersonError = true) }
                }
            }
            SwapPlayerUiEvent.Dismiss -> viewModelScope.launch { _effects.send(SwapPlayerUiEffect.Dismiss) }
        }
    }

    private fun swap(occupant: SeatOccupant) {
        viewModelScope.launch {
            when (swapPlayer(gameId, seat, occupant)) {
                is Result.Success -> _effects.send(SwapPlayerUiEffect.Dismiss)
                is Result.Failure -> _effects.send(SwapPlayerUiEffect.ShowError)
            }
        }
    }
}
