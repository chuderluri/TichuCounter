package ch.tichu.counter.core.data.repository

import ch.tichu.counter.core.common.IdGenerator
import ch.tichu.counter.core.common.TimeProvider
import ch.tichu.counter.core.data.codec.GameEventPayloadCodec
import ch.tichu.counter.core.database.TichuDatabase
import ch.tichu.counter.core.database.dao.GameDao
import ch.tichu.counter.core.database.dao.GameEventDao
import ch.tichu.counter.core.database.dao.PersonDao
import ch.tichu.counter.core.database.entity.GameEntity
import ch.tichu.counter.core.domain.scoring.GameReducer
import ch.tichu.counter.core.domain.scoring.ScoringEngine
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.SyncState
import ch.tichu.counter.core.testing.Fixtures
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameRepositoryImplTest {

    private val database = mockk<TichuDatabase>(relaxed = true)
    private val gameDao = mockk<GameDao>()
    private val eventDao = mockk<GameEventDao>()
    private val personDao = mockk<PersonDao>()
    private val codec = GameEventPayloadCodec()
    private val idGenerator: IdGenerator = mockk()
    private val timeProvider: TimeProvider = mockk()

    private fun repository() = GameRepositoryImpl(
        database = database,
        gameDao = gameDao,
        eventDao = eventDao,
        personDao = personDao,
        codec = codec,
        reducer = GameReducer(ScoringEngine()),
        projector = RoundFactProjector(),
        idGenerator = idGenerator,
        timeProvider = timeProvider,
    )

    private fun guestEntity() = GameEntity(
        id = Fixtures.gameId.value,
        groupId = null,
        status = GameStatus.IN_PROGRESS.name,
        scoreA = 0,
        scoreB = 0,
        roundCount = 0,
        winner = null,
        lineUpJson = codec.encodeLineUp(LineUp.allGuests()),
        createdAt = Fixtures.t0.toEpochMilliseconds(),
        updatedAt = Fixtures.t0.toEpochMilliseconds(),
        finishedAt = null,
        syncState = SyncState.LOCAL_ONLY.name,
    )

    @Test
    fun `observeCurrentGame emits summary for all-guest game without querying persons`() = runTest {
        val entity = guestEntity()
        every { gameDao.observeCurrentGame() } returns flowOf(entity)
        coEvery { gameDao.participantIds(entity.id) } returns emptyList()

        val summary = repository().observeCurrentGame().first()

        assertNotNull(summary)
        assertEquals(Fixtures.gameId, summary!!.game.id)
        assertTrue(summary.currentLineUp.isAllGuests())
        verify(exactly = 0) { personDao.observePersons(any()) }
    }
}
