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
        val showAbandon: Boolean = false,
        val showFinished: Boolean = false,
        val roundsExpanded: Boolean = false,
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
            bonusA = bonusA,
            bonusB = bonusB,
            canUndo = game.canUndo,
            canRedo = game.canRedo,
            validationError = validation is ValidationResult.Invalid,
            showAbandonConfirmation = draft.showAbandon,
            showFinishedDialog = draft.showFinished || game.status == GameStatus.FINISHED,
            winner = game.winner,
            durationMinutes = ((game.lastEventAt.toEpochMilliseconds() - game.startedAt.toEpochMilliseconds()) / 60_000).toInt(),
            finishedRoundListExpanded = draft.roundsExpanded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScoringUiState())

    fun onEvent(event: ScoringUiEvent) {
        when (event) {
            is ScoringUiEvent.Digit -> appendDigit(event.value)
            ScoringUiEvent.Backspace -> backspace()
            ScoringUiEvent.ToggleSign -> toggleSign()
            is ScoringUiEvent.SwitchTeam -> draft.update { it.copy(activeTeam = event.team) }
            is ScoringUiEvent.DoubleWin -> draft.update { it.copy(doubleWin = if (it.doubleWin == event.team) null else event.team) }
            is ScoringUiEvent.TichuToggled -> toggleTichu(event.seat, event.type)
            ScoringUiEvent.ConfirmRound -> confirmRound()
            ScoringUiEvent.Undo -> viewModelScope.launch { undo(gameId) }
            ScoringUiEvent.Redo -> viewModelScope.launch { redo(gameId) }
            is ScoringUiEvent.SwapPlayerRequested -> send(ScoringUiEffect.NavigateToSwapDialog(gameId, event.seat))
            is ScoringUiEvent.RoundRowClicked -> Unit
            ScoringUiEvent.AbandonGameRequested -> draft.update { it.copy(showAbandon = true) }
            ScoringUiEvent.AbandonDismissed -> draft.update { it.copy(showAbandon = false) }
            ScoringUiEvent.AbandonGameConfirmed -> viewModelScope.launch {
                abandonGame(gameId)
                draft.update { it.copy(showAbandon = false) }
                _effects.send(ScoringUiEffect.NavigateHome)
            }
            ScoringUiEvent.FinishedDialogDismissed -> draft.update { it.copy(showFinished = false) }
            ScoringUiEvent.ToggleFinishedRoundList -> draft.update { it.copy(roundsExpanded = !it.roundsExpanded) }
            ScoringUiEvent.RematchClicked, ScoringUiEvent.NewGameClicked -> startNewGameWithSameLineUp()
            ScoringUiEvent.HomeClicked -> send(ScoringUiEffect.NavigateHome)
        }
    }

    private fun appendDigit(digit: Int) {
        if (!state.value.isKeypadEnabled) return
        draft.update { current ->
            val value = if (current.activeTeam == Team.A) current.enteredA else current.enteredB
            val negative = value.startsWith('-')
            val raw = value.removePrefix("-")
            if (raw.length >= 3) return@update current
            val next = (if (negative) "-" else "") + (raw + digit).trimStart('0').ifBlank { "0" }
            if (current.activeTeam == Team.A) {
                current.copy(enteredA = next, enteredB = complementText(next))
            } else {
                current.copy(enteredB = next, enteredA = complementText(next))
            }
        }
    }

    private fun backspace() {
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

    private fun toggleTichu(seat: Seat, type: TichuType) {
        draft.update { current ->
            val old = current.tichus[seat]
            val next = when {
                old == null || old.type != type -> DraftTichu(type, success = true)
                old.success -> DraftTichu(type, success = false)
                else -> null
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
                    val round = game.roundNumber
                    draft.value = Draft()
                    _effects.send(ScoringUiEffect.ShowRoundSavedSnackbar(round))
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
