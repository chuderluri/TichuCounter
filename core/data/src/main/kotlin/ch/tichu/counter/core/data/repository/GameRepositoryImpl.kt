package ch.tichu.counter.core.data.repository

import androidx.room.withTransaction
import ch.tichu.counter.core.common.IdGenerator
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.common.TimeProvider
import ch.tichu.counter.core.common.failure
import ch.tichu.counter.core.common.success
import ch.tichu.counter.core.data.codec.GameEventPayloadCodec
import ch.tichu.counter.core.data.mapper.toDomain
import ch.tichu.counter.core.data.mapper.toEntity
import ch.tichu.counter.core.data.mapper.toEpochMillis
import ch.tichu.counter.core.database.TichuDatabase
import ch.tichu.counter.core.database.dao.GameDao
import ch.tichu.counter.core.database.dao.GameEventDao
import ch.tichu.counter.core.database.dao.PersonDao
import ch.tichu.counter.core.database.entity.GameEntity
import ch.tichu.counter.core.database.entity.GameParticipantEntity
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.repository.GameRepository
import ch.tichu.counter.core.domain.scoring.GameReducer
import ch.tichu.counter.core.model.EventId
import ch.tichu.counter.core.model.GameEvent
import ch.tichu.counter.core.model.GameEventPayload
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameState
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.GameSummary
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor(
    private val database: TichuDatabase,
    private val gameDao: GameDao,
    private val eventDao: GameEventDao,
    private val personDao: PersonDao,
    private val codec: GameEventPayloadCodec,
    private val reducer: GameReducer,
    private val projector: RoundFactProjector,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
) : GameRepository {

    override fun observeGameState(gameId: GameId): Flow<GameState?> = eventDao.observeEvents(gameId.value).map { entities ->
        if (entities.isEmpty()) null else reducer.reduce(entities.map { it.toDomain(codec) })
    }

    override fun observeGameSummary(gameId: GameId): Flow<GameSummary?> = gameDao.observeGame(gameId.value).flatMapLatest { entity ->
        if (entity == null) flowOf(null) else summaryFlow(entity)
    }

    override fun observeCurrentGame(): Flow<GameSummary?> = gameDao.observeCurrentGame().flatMapLatest { entity ->
        if (entity == null) flowOf(null) else summaryFlow(entity)
    }

    override fun observeGames(groupId: GroupId?, statuses: Set<GameStatus>): Flow<List<GameSummary>> = gameDao.observeGames(groupId?.value, statuses.map { it.name }).flatMapLatest { entities ->
        if (entities.isEmpty()) {
            flowOf(emptyList())
        } else {
            val lineUps = entities.associate { it.id to codec.decodeLineUp(it.lineUpJson) }
            val personIds = lineUps.values.flatMap { it.registeredPersons() }.map { it.value }.distinct()
            personDao.observePersons(personIds).map { persons ->
                val byId = persons.associate { PersonId(it.id) to it.toDomain() }
                entities.map { entity -> entity.toSummary(lineUps.getValue(entity.id), byId) }
            }
        }
    }

    override suspend fun getGameState(gameId: GameId): GameState? {
        val entities = eventDao.getEvents(gameId.value)
        return if (entities.isEmpty()) null else reducer.reduce(entities.map { it.toDomain(codec) })
    }

    override suspend fun startGame(
        groupId: GroupId?,
        lineUp: LineUp,
        ruleSet: RuleSet,
        abandonCurrent: Boolean,
    ): Result<GameId, DomainError> = database.withTransaction {
        val current = gameDao.getCurrentGame()
        if (current != null) {
            if (!abandonCurrent) return@withTransaction DomainError.GameAlreadyInProgress.failure()
            appendInTransaction(GameId(current.id), GameEventPayload.GameAbandoned("Replaced by a new game"))
        }
        val now = timeProvider.now()
        val gameId = GameId(idGenerator.newId())
        gameDao.insert(
            GameEntity(
                id = gameId.value,
                groupId = groupId?.value,
                status = GameStatus.IN_PROGRESS.name,
                scoreA = 0,
                scoreB = 0,
                roundCount = 0,
                winner = null,
                lineUpJson = codec.encodeLineUp(lineUp),
                createdAt = now.toEpochMillis(),
                updatedAt = now.toEpochMillis(),
                finishedAt = null,
                syncState = SyncState.LOCAL_ONLY.name,
            ),
        )
        val started = GameEvent(
            id = EventId(idGenerator.newId()),
            gameId = gameId,
            sequence = 1,
            occurredAt = now,
            isUndone = false,
            payload = GameEventPayload.GameStarted(groupId, lineUp, ruleSet),
        )
        eventDao.insert(started.toEntity(codec))
        refreshProjections(gameId)
        gameId.success()
    }

    override suspend fun appendEvent(gameId: GameId, payload: GameEventPayload): Result<Unit, DomainError> = database.withTransaction { appendInTransaction(gameId, payload) }

    override suspend fun undo(gameId: GameId): Result<Unit, DomainError> = database.withTransaction {
        val last = eventDao.lastActiveEvent(gameId.value) ?: return@withTransaction DomainError.NothingToUndo.failure()
        eventDao.setUndone(last.id, true, timeProvider.now().toEpochMillis(), SyncState.LOCAL_ONLY.name)
        refreshProjections(gameId)
        Unit.success()
    }

    override suspend fun redo(gameId: GameId): Result<Unit, DomainError> = database.withTransaction {
        val first = eventDao.firstUndoneEvent(gameId.value) ?: return@withTransaction DomainError.NothingToRedo.failure()
        eventDao.setUndone(first.id, false, null, SyncState.LOCAL_ONLY.name)
        refreshProjections(gameId)
        Unit.success()
    }

    override suspend fun deleteGame(gameId: GameId): Result<Unit, DomainError> = database.withTransaction {
        gameDao.getGame(gameId.value) ?: return@withTransaction DomainError.GameNotFound.failure()
        gameDao.delete(gameId.value)
        Unit.success()
    }

    private suspend fun appendInTransaction(gameId: GameId, payload: GameEventPayload): Result<Unit, DomainError> {
        gameDao.getGame(gameId.value) ?: return DomainError.GameNotFound.failure()
        eventDao.deleteUndone(gameId.value)
        val sequence = (eventDao.maxSequence(gameId.value) ?: 0) + 1
        val event = GameEvent(
            id = EventId(idGenerator.newId()),
            gameId = gameId,
            sequence = sequence,
            occurredAt = timeProvider.now(),
            isUndone = false,
            payload = payload,
        )
        eventDao.insert(event.toEntity(codec))
        refreshProjections(gameId)
        return Unit.success()
    }

    private suspend fun refreshProjections(gameId: GameId) {
        val entities = eventDao.getEvents(gameId.value)
        if (entities.isEmpty()) return
        val events = entities.map { it.toDomain(codec) }
        val state = reducer.reduce(events)
        val game = gameDao.getGame(gameId.value) ?: return

        gameDao.update(
            game.copy(
                status = state.status.name,
                scoreA = state.scoreA,
                scoreB = state.scoreB,
                roundCount = state.rounds.size,
                winner = state.winner?.name,
                lineUpJson = codec.encodeLineUp(state.lineUp),
                updatedAt = timeProvider.now().toEpochMillis(),
                finishedAt = if (state.status == GameStatus.FINISHED) state.lastEventAt.toEpochMillis() else null,
            ),
        )

        val participants = mutableSetOf<PersonId>()
        events.filter { !it.isUndone }.forEach { event ->
            when (val payload = event.payload) {
                is GameEventPayload.GameStarted -> participants += payload.lineUp.registeredPersons()
                is GameEventPayload.PlayerSwapped -> payload.next.personIdOrNull?.let { participants += it }
                else -> Unit
            }
        }
        gameDao.deleteParticipants(gameId.value)
        gameDao.insertParticipants(participants.map { GameParticipantEntity(gameId.value, it.value) })

        eventDao.deleteRoundFactsForGame(gameId.value)
        val roundEvents = events.filter { !it.isUndone && it.payload is GameEventPayload.RoundScored }
        val facts = roundEvents.zip(state.rounds).flatMap { (event, result) ->
            projector.project(event.id, gameId, state.groupId, result, event.occurredAt)
        }
        if (facts.isNotEmpty()) eventDao.insertRoundFacts(facts)
    }

    private fun summaryFlow(entity: GameEntity): Flow<GameSummary> {
        val lineUp = codec.decodeLineUp(entity.lineUpJson)
        return kotlinx.coroutines.flow.flow {
            val participantIds = gameDao.participantIds(entity.id)
            val currentIds = lineUp.registeredPersons().map { it.value }
            emit((participantIds + currentIds).distinct())
        }.flatMapLatest { personIds ->
            personDao.observePersons(personIds).map { persons ->
                entity.toSummary(lineUp, persons.associate { PersonId(it.id) to it.toDomain() })
            }
        }
    }

    private fun GameEntity.toSummary(lineUp: LineUp, persons: Map<PersonId, ch.tichu.counter.core.model.Person>) = GameSummary(
        game = toDomain(),
        scoreA = scoreA,
        scoreB = scoreB,
        roundCount = roundCount,
        winner = winner?.let { ch.tichu.counter.core.model.Team.valueOf(it) },
        currentLineUp = lineUp,
        persons = persons,
    )
}
