package ch.tichu.counter.core.domain.repository

import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.model.GameEventPayload
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameState
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.GameSummary
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.RuleSet
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeGameState(gameId: GameId): Flow<GameState?>

    fun observeGameSummary(gameId: GameId): Flow<GameSummary?>

    fun observeCurrentGame(): Flow<GameSummary?>

    fun observeGames(groupId: GroupId?, statuses: Set<GameStatus> = GameStatus.entries.toSet()): Flow<List<GameSummary>>

    suspend fun getGameState(gameId: GameId): GameState?

    suspend fun startGame(
        groupId: GroupId?,
        lineUp: LineUp,
        ruleSet: RuleSet,
        abandonCurrent: Boolean,
    ): Result<GameId, DomainError>

    suspend fun appendEvent(gameId: GameId, payload: GameEventPayload): Result<Unit, DomainError>

    suspend fun undo(gameId: GameId): Result<Unit, DomainError>

    suspend fun redo(gameId: GameId): Result<Unit, DomainError>

    suspend fun deleteGame(gameId: GameId): Result<Unit, DomainError>
}
