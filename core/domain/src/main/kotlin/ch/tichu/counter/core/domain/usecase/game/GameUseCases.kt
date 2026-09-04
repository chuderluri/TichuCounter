package ch.tichu.counter.core.domain.usecase.game

import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.common.failure
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.repository.GameRepository
import ch.tichu.counter.core.domain.repository.GroupRepository
import ch.tichu.counter.core.model.GameEventPayload
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameState
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.GameSummary
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.SeatOccupant
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGameStateUseCase @Inject constructor(private val repository: GameRepository) {
    operator fun invoke(gameId: GameId): Flow<GameState?> = repository.observeGameState(gameId)
}

class ObserveCurrentGameUseCase @Inject constructor(private val repository: GameRepository) {
    operator fun invoke(): Flow<GameSummary?> = repository.observeCurrentGame()
}

class ObserveGamesUseCase @Inject constructor(private val repository: GameRepository) {
    operator fun invoke(groupId: GroupId?, statuses: Set<GameStatus> = GameStatus.entries.toSet()): Flow<List<GameSummary>> = repository.observeGames(groupId, statuses)
}

class ObserveGameSummaryUseCase @Inject constructor(private val repository: GameRepository) {
    operator fun invoke(gameId: GameId): Flow<GameSummary?> = repository.observeGameSummary(gameId)
}

class StartGameUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId?,
        lineUp: LineUp,
        ruleSet: RuleSet,
        abandonCurrent: Boolean,
    ): Result<GameId, DomainError> {
        if (groupId == null && !lineUp.isAllGuests()) {
            return DomainError.NotAMember.failure()
        }
        if (groupId != null) {
            for (personId in lineUp.registeredPersons()) {
                if (!groupRepository.isMember(groupId, personId)) return DomainError.NotAMember.failure()
            }
        }
        return gameRepository.startGame(groupId, lineUp, ruleSet, abandonCurrent)
    }
}

class SwapPlayerUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val groupRepository: GroupRepository,
) {
    suspend operator fun invoke(gameId: GameId, seat: Seat, newOccupant: SeatOccupant): Result<Unit, DomainError> {
        val state = gameRepository.getGameState(gameId) ?: return DomainError.GameNotFound.failure()
        if (state.status != GameStatus.IN_PROGRESS) return DomainError.GameNotInProgress.failure()
        val personId = newOccupant.personIdOrNull
        if (personId != null) {
            val groupId = state.groupId ?: return DomainError.NotAMember.failure()
            if (!groupRepository.isMember(groupId, personId)) return DomainError.NotAMember.failure()
            val existing = state.lineUp.seatOf(personId)
            if (existing != null && existing != seat) return DomainError.Unexpected("Person already seated").failure()
        }
        val previous = state.lineUp.occupantAt(seat)
        if (previous == newOccupant) return Result.Success(Unit)
        return gameRepository.appendEvent(gameId, GameEventPayload.PlayerSwapped(seat, previous, newOccupant))
    }
}

class AbandonGameUseCase @Inject constructor(private val repository: GameRepository) {
    suspend operator fun invoke(gameId: GameId, reason: String? = null): Result<Unit, DomainError> {
        val state = repository.getGameState(gameId) ?: return DomainError.GameNotFound.failure()
        if (state.status != GameStatus.IN_PROGRESS) return DomainError.GameNotInProgress.failure()
        return repository.appendEvent(gameId, GameEventPayload.GameAbandoned(reason))
    }
}

class DeleteGameUseCase @Inject constructor(private val repository: GameRepository) {
    suspend operator fun invoke(gameId: GameId): Result<Unit, DomainError> = repository.deleteGame(gameId)
}

class UndoLastEventUseCase @Inject constructor(private val repository: GameRepository) {
    suspend operator fun invoke(gameId: GameId): Result<Unit, DomainError> = repository.undo(gameId)
}

class RedoLastEventUseCase @Inject constructor(private val repository: GameRepository) {
    suspend operator fun invoke(gameId: GameId): Result<Unit, DomainError> = repository.redo(gameId)
}
