package ch.tichu.counter.feature.game.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.usecase.game.StartGameUseCase
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.domain.usecase.person.CreatePersonUseCase
import ch.tichu.counter.core.domain.usecase.person.ObserveGroupMembersUseCase
import ch.tichu.counter.core.domain.usecase.preferences.ObservePreferencesUseCase
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.SeatOccupant
import ch.tichu.counter.core.ui.navigation.GameSetupRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
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

@HiltViewModel
class GameSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeActiveGroup: ObserveActiveGroupUseCase,
    observeMembers: ObserveGroupMembersUseCase,
    observePreferences: ObservePreferencesUseCase,
    private val createPerson: CreatePersonUseCase,
    private val startGame: StartGameUseCase,
) : ViewModel() {

    private val abandonCurrent: Boolean = savedStateHandle.toRoute<GameSetupRoute>().abandonCurrent

    private data class Draft(
        val slots: Map<Seat, SeatOccupant?> = Seat.entries.associateWith { null },
        val selectedSeat: Seat? = Seat.A1,
        val query: String = "",
        val targetScore: Int? = null,
        val renamingSeat: Seat? = null,
        val renameDraft: String = "",
        val showNewPerson: Boolean = false,
        val newPersonName: String = "",
        val newPersonError: Boolean = false,
        val isStarting: Boolean = false,
        val initialised: Boolean = false,
    )

    private val draft = MutableStateFlow(Draft())
    private val _effects = Channel<GameSetupUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val activeGroup = observeActiveGroup()
    private val members = activeGroup.flatMapLatest { active ->
        val group = active.group
        if (group == null) flowOf(emptyList()) else observeMembers(group.id)
    }

    init {
        viewModelScope.launch {
            val active = activeGroup.first()
            if (active.group == null && !active.needsOnboarding) {
                draft.update { d ->
                    if (d.initialised) d else d.copy(slots = LineUp.allGuests().seats, selectedSeat = null, initialised = true)
                }
            } else {
                draft.update { it.copy(initialised = true) }
            }
        }
    }

    val state: StateFlow<GameSetupUiState> = combine(activeGroup, members, observePreferences(), draft) { active, members, prefs, draft ->
        val byId = members.associateBy { it.person.id }
        val seated = draft.slots.values.mapNotNull { it?.personIdOrNull }.toSet()
        GameSetupUiState(
            groupName = active.group?.name,
            isQuickPlay = active.group == null,
            slots = draft.slots.mapValues { (_, occupant) ->
                when (occupant) {
                    null -> null
                    is SeatOccupant.Guest -> SlotUi.Guest(occupant.name)
                    is SeatOccupant.Registered -> {
                        val p = byId[occupant.personId]?.person
                        SlotUi.Person(occupant.personId, p?.name ?: "?", p?.avatarColor ?: AvatarColor.BLUE)
                    }
                }
            }.toImmutableMap(),
            selectedSeat = draft.selectedSeat,
            members = members
                .filter { draft.query.isBlank() || it.person.name.contains(draft.query, ignoreCase = true) }
                .map {
                    MemberCandidateUi(
                        id = it.person.id,
                        name = it.person.name,
                        color = it.person.avatarColor,
                        lastPlayedAt = it.lastPlayedAt,
                        isSeated = it.person.id in seated,
                    )
                }.toImmutableList(),
            query = draft.query,
            targetScore = draft.targetScore ?: prefs.defaultTargetScore,
            targetScoreOptions = RuleSet.TARGET_SCORE_OPTIONS.toImmutableList(),
            renamingSeat = draft.renamingSeat,
            renameDraft = draft.renameDraft,
            showNewPerson = draft.showNewPerson,
            newPersonName = draft.newPersonName,
            newPersonError = draft.newPersonError,
            isLoading = !draft.initialised,
            isStarting = draft.isStarting,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameSetupUiState())

    fun onEvent(event: GameSetupUiEvent) {
        when (event) {
            is GameSetupUiEvent.SeatSelected -> draft.update { it.copy(selectedSeat = event.seat) }
            is GameSetupUiEvent.PersonPicked -> fill(SeatOccupant.Registered(event.personId))
            GameSetupUiEvent.GuestPicked -> {
                val seat = draft.value.selectedSeat ?: return
                fill(SeatOccupant.Guest(SeatOccupant.defaultGuestName(seat)))
            }
            is GameSetupUiEvent.ClearSeat -> draft.update {
                it.copy(slots = it.slots + (event.seat to null), selectedSeat = event.seat)
            }
            is GameSetupUiEvent.RenameGuestStarted -> {
                val current = draft.value.slots[event.seat] as? SeatOccupant.Guest ?: return
                draft.update { it.copy(renamingSeat = event.seat, renameDraft = current.name) }
            }
            is GameSetupUiEvent.RenameDraftChanged -> draft.update { it.copy(renameDraft = event.name) }
            GameSetupUiEvent.RenameConfirmed -> draft.update { d ->
                val seat = d.renamingSeat ?: return@update d
                val name = d.renameDraft.trim().ifBlank { SeatOccupant.defaultGuestName(seat) }
                d.copy(slots = d.slots + (seat to SeatOccupant.Guest(name)), renamingSeat = null, renameDraft = "")
            }
            GameSetupUiEvent.RenameCancelled -> draft.update { it.copy(renamingSeat = null, renameDraft = "") }
            is GameSetupUiEvent.QueryChanged -> draft.update { it.copy(query = event.query) }
            GameSetupUiEvent.NewPersonClicked -> draft.update { it.copy(showNewPerson = true, newPersonName = it.query, newPersonError = false) }
            is GameSetupUiEvent.NewPersonNameChanged -> draft.update { it.copy(newPersonName = event.name, newPersonError = false) }
            GameSetupUiEvent.NewPersonConfirmed -> viewModelScope.launch {
                val groupId = activeGroup.first().group?.id
                val color = AvatarColor.forIndex(state.value.members.size)
                when (val result = createPerson(draft.value.newPersonName, color, groupId)) {
                    is Result.Success -> {
                        draft.update { it.copy(showNewPerson = false, newPersonName = "", query = "") }
                        fill(SeatOccupant.Registered(result.value.id))
                    }
                    is Result.Failure -> draft.update { it.copy(newPersonError = true) }
                }
            }
            GameSetupUiEvent.NewPersonDismissed -> draft.update { it.copy(showNewPerson = false) }
            is GameSetupUiEvent.TargetScoreChanged -> draft.update { it.copy(targetScore = event.score) }
            GameSetupUiEvent.StartGame -> start()
            GameSetupUiEvent.CreateGroupInstead -> viewModelScope.launch { _effects.send(GameSetupUiEffect.OpenGroupPicker) }
        }
    }

    private fun fill(occupant: SeatOccupant) {
        draft.update { d ->
            val seat = d.selectedSeat ?: Seat.entries.firstOrNull { d.slots[it] == null } ?: return@update d
            val personId = occupant.personIdOrNull
            val cleaned = if (personId != null) d.slots.mapValues { (_, o) -> if (o?.personIdOrNull == personId) null else o } else d.slots
            val slots = cleaned + (seat to occupant)
            val next = Seat.entries.firstOrNull { slots[it] == null }
            d.copy(slots = slots, selectedSeat = next, query = "")
        }
    }

    private fun start() {
        val current = draft.value
        val occupants = current.slots.mapNotNull { (seat, o) -> o?.let { seat to it } }.toMap()
        if (occupants.size != Seat.entries.size) return
        viewModelScope.launch {
            draft.update { it.copy(isStarting = true) }
            val active = activeGroup.first()
            val prefs = state.value
            val ruleSet = RuleSet.DEFAULT.copy(targetScore = prefs.targetScore)
            val result = startGame(active.group?.id, LineUp(occupants), ruleSet, abandonCurrent)
            when (result) {
                is Result.Success -> _effects.send(GameSetupUiEffect.NavigateToScoring(result.value))
                is Result.Failure -> {
                    draft.update { it.copy(isStarting = false) }
                    _effects.send(GameSetupUiEffect.ShowStartError)
                }
            }
        }
    }
}
