package ch.tichu.counter.feature.scoring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.scoring.ValidationResult
import ch.tichu.counter.core.domain.usecase.game.AbandonGameUseCase
import ch.tichu.counter.core.domain.usecase.game.ObserveGameStateUseCase
import ch.tichu.counter.core.domain.usecase.game.ObserveGameSummaryUseCase
import ch.tichu.counter.core.domain.usecase.game.RedoLastEventUseCase
import ch.tichu.counter.core.domain.usecase.game.StartGameUseCase
import ch.tichu.counter.core.domain.usecase.game.UndoLastEventUseCase
import ch.tichu.counter.core.domain.usecase.scoring.RecordRoundUseCase
import ch.tichu.counter.core.domain.usecase.scoring.ValidateRoundUseCase
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameState
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.RoundInput
import ch.tichu.counter.core.model.RoundOutcome
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.SeatOccupant
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TichuCall
import ch.tichu.counter.core.model.TichuType
import ch.tichu.counter.core.model.TimelineEntry
import ch.tichu.counter.core.ui.component.HistoryRowUi
import ch.tichu.counter.core.ui.component.TichuBadgeUi
import ch.tichu.counter.core.ui.navigation.ScoringRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScoringViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeGameState: ObserveGameStateUseCase,
    observeGameSummary: ObserveGameSummaryUseCase,
    private val validateRound: ValidateRoundUseCase,
    private val recordRound: RecordRoundUseCase,
    private val undo: UndoLastEventUseCase,
    private val redo: RedoLastEventUseCase,
    private val abandonGame: AbandonGameUseCase,
    private val startGame: StartGameUseCase,
) : ViewModel() {

    private val gameId = GameId(savedStateHandle.toRoute<ScoringRoute>().gameId)

    private data class Draft(
        val enteredA: String = "",
        val enteredB: String = "",
        val activeTeam: Team = Team.A,
        val doubleWin: Team? = null,
        val tichus: Map<Seat, DraftTichu> = emptyMap(),
        val tichuEditorSeat: Seat? = null,
        val showAbandon: Boolean = false,
        val finishedDialogDismissed: Boolean = false,
    )

    private val draft = MutableStateFlow(Draft())
    private val _effects = Channel<ScoringUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val gameState = observeGameState(gameId)
    private val summary = observeGameSummary(gameId)

    val state: StateFlow<ScoringUiState> = combine(gameState, summary, draft) { game, summary, draft ->
        if (game == null) return@combine ScoringUiState()
        val input = draft.toInput(game)
        val validation = if (game.status == GameStatus.IN_PROGRESS && (draft.doubleWin != null || draft.enteredA.isNotBlank() || draft.enteredB.isNotBlank())) {
            validateRound(input, game.ruleSet)
        } else {
            ValidationResult.Valid
        }
        val names = summary?.persons.orEmpty()
        val seats = Seat.entries.map { seat ->
            val occupant = game.lineUp.occupantAt(seat)
            when (occupant) {
                is SeatOccupant.Registered -> SeatUi(
                    seat = seat,
                    name = names[occupant.personId]?.name ?: "?",
                    color = names[occupant.personId]?.avatarColor,
                    isGuest = false,
                    tichu = draft.tichus[seat],
                )
                is SeatOccupant.Guest -> SeatUi(seat, occupant.name, null, true, draft.tichus[seat])
            }
        }.toImmutableList()
        val bonusA = input.tichuCalls.filter { it.seat.team == Team.A }.sumOf { if (it.success) game.ruleSet.tichuValue(it.type) else -game.ruleSet.tichuValue(it.type) }
        val bonusB = input.tichuCalls.filter { it.seat.team == Team.B }.sumOf { if (it.success) game.ruleSet.tichuValue(it.type) else -game.ruleSet.tichuValue(it.type) }
        val tichuEditor = draft.tichuEditorSeat?.let { seat ->
            seats.firstOrNull { it.seat == seat }?.let { player ->
                TichuEditorUi(
                    seat = seat,
                    name = player.name,
                    current = player.tichu,
                    madeAllowed = draft.doubleWin == null || draft.doubleWin == seat.team,
                )
            }
        }
        ScoringUiState(
            gameId = game.gameId,
            status = game.status.toUiStatus(),
            targetScore = game.ruleSet.targetScore,
            scoreA = game.scoreA,
            scoreB = game.scoreB,
            roundNumber = game.roundNumber,
            seats = seats,
            history = game.toHistory(names).toImmutableList(),
            enteredA = if (draft.doubleWin != null) if (draft.doubleWin == Team.A) "200" else "0" else draft.enteredA,
            enteredB = if (draft.doubleWin != null) if (draft.doubleWin == Team.B) "200" else "0" else draft.enteredB,
            activeTeam = draft.activeTeam,
            doubleWin = draft.doubleWin,
            doubleWinValue = game.ruleSet.doubleWinValue,
            bonusA = bonusA,
            bonusB = bonusB,
            tichuEditor = tichuEditor,
            canUndo = game.canUndo,
            canRedo = game.canRedo,
            validationError = validation is ValidationResult.Invalid,
            showAbandonConfirmation = draft.showAbandon,
            showFinishedDialog = game.status == GameStatus.FINISHED && !draft.finishedDialogDismissed,
            winner = game.winner,
            durationMinutes = ((game.lastEventAt.toEpochMilliseconds() - game.startedAt.toEpochMilliseconds()) / 60_000).toInt(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScoringUiState())

    fun onEvent(event: ScoringUiEvent) {
        when (event) {
            is ScoringUiEvent.Digit -> appendDigit(event.value)
            ScoringUiEvent.Backspace -> backspace()
            ScoringUiEvent.ToggleSign -> toggleSign()
            is ScoringUiEvent.SwitchTeam -> draft.update { it.copy(activeTeam = event.team) }
            is ScoringUiEvent.DoubleWin -> setDoubleWin(event.team)
            is ScoringUiEvent.PlayerClicked -> draft.update { it.copy(tichuEditorSeat = event.seat) }
            is ScoringUiEvent.TichuOptionSelected -> {
                selectTichu(event.seat, event.call)
                draft.update { it.copy(tichuEditorSeat = null) }
            }
            is ScoringUiEvent.TichuQuickSetRequested -> quickSetTichu(event.seat)
            ScoringUiEvent.TichuEditorDismissed -> draft.update { it.copy(tichuEditorSeat = null) }
            ScoringUiEvent.ConfirmRound -> confirmRound()
            ScoringUiEvent.Undo -> viewModelScope.launch { undo(gameId) }
            ScoringUiEvent.Redo -> viewModelScope.launch { redo(gameId) }
            is ScoringUiEvent.SwapPlayerRequested -> {
                draft.update { it.copy(tichuEditorSeat = null) }
                send(ScoringUiEffect.NavigateToSwapDialog(gameId, event.seat))
            }
            is ScoringUiEvent.RoundRowClicked -> Unit
            ScoringUiEvent.AbandonGameRequested -> draft.update { it.copy(showAbandon = true) }
            ScoringUiEvent.AbandonDismissed -> draft.update { it.copy(showAbandon = false) }
            ScoringUiEvent.AbandonGameConfirmed -> viewModelScope.launch {
                abandonGame(gameId)
                draft.update { it.copy(showAbandon = false) }
                _effects.send(ScoringUiEffect.NavigateHome)
            }
            ScoringUiEvent.FinishedDialogDismissed -> draft.update { it.copy(finishedDialogDismissed = true) }
            ScoringUiEvent.NewGameClicked -> startNewGameWithSameLineUp()
            ScoringUiEvent.HomeClicked -> send(ScoringUiEffect.NavigateHome)
        }
    }

    private fun appendDigit(digit: Int) {
        if (!state.value.isKeypadEnabled) return
        val doubleWinValue = state.value.doubleWinValue
        draft.update { current ->
            val value = if (current.activeTeam == Team.A) current.enteredA else current.enteredB
            val negative = value.startsWith('-')
            val raw = value.removePrefix("-")
            if (raw.length >= 3) return@update current
            val next = (if (negative) "-" else "") + (raw + digit).trimStart('0').ifBlank { "0" }
            when {
                // Typing the double-win value is a shortcut for the double win of the active team.
                next.toIntOrNull() == doubleWinValue -> current.copy(doubleWin = current.activeTeam)
                current.activeTeam == Team.A -> current.copy(enteredA = next, enteredB = complementText(next))
                else -> current.copy(enteredB = next, enteredA = complementText(next))
            }
        }
    }

    private fun backspace() {
        if (draft.value.doubleWin != null) {
            draft.update { it.copy(doubleWin = null) }
            return
        }
        if (!state.value.isKeypadEnabled) return
        draft.update { current ->
            val value = if (current.activeTeam == Team.A) current.enteredA else current.enteredB
            val next = value.dropLast(1).takeIf { it != "-" }.orEmpty()
            if (current.activeTeam == Team.A) {
                current.copy(enteredA = next, enteredB = complementText(next))
            } else {
                current.copy(enteredB = next, enteredA = complementText(next))
            }
        }
    }

    private fun toggleSign() {
        if (!state.value.isKeypadEnabled) return
        draft.update { current ->
            val value = if (current.activeTeam == Team.A) current.enteredA else current.enteredB
            val next = when {
                value.isBlank() -> "-"
                value.startsWith('-') -> value.removePrefix("-")
                else -> "-$value"
            }
            if (current.activeTeam == Team.A) {
                current.copy(enteredA = next, enteredB = complementText(next))
            } else {
                current.copy(enteredB = next, enteredA = complementText(next))
            }
        }
    }

    private fun setDoubleWin(team: Team?) {
        draft.update { current ->
            val next = if (current.doubleWin == team) null else team
            current.copy(
                doubleWin = next,
                // Leaving the double win clears the entered card points.
                enteredA = if (next == null) "" else current.enteredA,
                enteredB = if (next == null) "" else current.enteredB,
                // A successful call can only belong to the winning team.
                tichus = if (next == null) {
                    current.tichus
                } else {
                    current.tichus.filterNot { (seat, call) -> seat.team != next && call.success }
                },
            )
        }
    }

    private fun selectTichu(seat: Seat, call: DraftTichu?) {
        draft.update { current ->
            current.copy(
                tichus = current.tichus.toMutableMap().apply {
                    if (call == null) remove(seat) else put(seat, call)
                },
            )
        }
    }

    private fun quickSetTichu(seat: Seat) {
        draft.update { current ->
            val madeAllowed = current.doubleWin == null || current.doubleWin == seat.team
            val startOfCycle = if (madeAllowed) DraftTichu(TichuType.SMALL, success = true) else DraftTichu(TichuType.SMALL, success = false)
            val next = when (val old = current.tichus[seat]) {
                null -> startOfCycle
                else -> when {
                    old.type != TichuType.SMALL -> startOfCycle
                    old.success -> DraftTichu(TichuType.SMALL, success = false)
                    else -> null
                }
            }
            current.copy(
                tichus = current.tichus.toMutableMap().apply {
                    if (next == null) remove(seat) else put(seat, next)
                },
            )
        }
    }

    private fun confirmRound() {
        viewModelScope.launch {
            val game = gameState.first() ?: return@launch
            val input = draft.value.toInput(game)
            when (recordRound(gameId, input)) {
                is Result.Success -> {
                    draft.value = Draft()
                }
                is Result.Failure -> _effects.send(ScoringUiEffect.ShowInvalidRound)
            }
        }
    }

    private fun Draft.toInput(game: GameState): RoundInput {
        val outcome = doubleWin?.let { RoundOutcome.DoubleWin(it) } ?: run {
            val a = enteredA.toIntOrNull()
            val b = enteredB.toIntOrNull()
            when {
                a != null && b != null -> RoundOutcome.CardPoints(a, b)
                a != null -> RoundOutcome.CardPoints(a, game.ruleSet.roundCardPointsTotal - a)
                b != null -> RoundOutcome.CardPoints(game.ruleSet.roundCardPointsTotal - b, b)
                else -> RoundOutcome.CardPoints(0, game.ruleSet.roundCardPointsTotal)
            }
        }
        return RoundInput(outcome, tichus.map { (seat, t) -> TichuCall(seat, t.type, t.success) })
    }

    private fun complementText(value: String): String {
        val parsed = value.toIntOrNull() ?: return ""
        return (100 - parsed).takeIf { it in -25..125 && it.mod(5) == 0 }?.toString().orEmpty()
    }

    private fun startNewGameWithSameLineUp() {
        viewModelScope.launch {
            val game = gameState.first() ?: return@launch
            when (val result = startGame(game.groupId, game.lineUp, game.ruleSet, abandonCurrent = false)) {
                is Result.Success -> _effects.send(ScoringUiEffect.NavigateToScoring(result.value))
                is Result.Failure -> _effects.send(ScoringUiEffect.ShowInvalidRound)
            }
        }
    }

    private fun GameStatus.toUiStatus(): ScoringStatus = when (this) {
        GameStatus.IN_PROGRESS -> ScoringStatus.IN_PROGRESS
        GameStatus.FINISHED -> ScoringStatus.FINISHED
        GameStatus.ABANDONED -> ScoringStatus.ABANDONED
    }

    private fun GameState.toHistory(persons: Map<ch.tichu.counter.core.model.PersonId, ch.tichu.counter.core.model.Person>): List<HistoryRowUi> = timeline.mapIndexed { index, item ->
        when (item) {
            is TimelineEntry.Round -> {
                val result = item.result
                HistoryRowUi.Round(
                    roundNumber = result.roundNumber,
                    totalA = result.teamA.total,
                    totalB = result.teamB.total,
                    badgesA = result.tichuCalls.filter { it.seat.team == Team.A }
                        .map { TichuBadgeUi(it.type, it.success) }.toImmutableList(),
                    badgesB = result.tichuCalls.filter { it.seat.team == Team.B }
                        .map { TichuBadgeUi(it.type, it.success) }.toImmutableList(),
                    doubleWin = (result.outcome as? RoundOutcome.DoubleWin)?.winner,
                )
            }
            is TimelineEntry.Swap -> HistoryRowUi.Swap(
                index = index,
                previousName = item.previous.displayName(persons),
                nextName = item.next.displayName(persons),
                previousIsGuest = item.previous is SeatOccupant.Guest,
                nextIsGuest = item.next is SeatOccupant.Guest,
            )
        }
    }

    private fun SeatOccupant.displayName(persons: Map<ch.tichu.counter.core.model.PersonId, ch.tichu.counter.core.model.Person>): String = when (this) {
        is SeatOccupant.Guest -> name
        is SeatOccupant.Registered -> persons[personId]?.name ?: "Player"
    }

    private fun send(effect: ScoringUiEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
