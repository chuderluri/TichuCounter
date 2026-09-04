package ch.tichu.counter.core.domain.scoring

import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TichuType
import ch.tichu.counter.core.model.TimelineEntry
import ch.tichu.counter.core.testing.Fixtures
import ch.tichu.counter.core.testing.Fixtures.call
import ch.tichu.counter.core.testing.events
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameReducerTest {

    private val reducer = GameReducer(ScoringEngine())

    @Test
    fun `fresh game has zero scores and no rounds`() {
        val state = reducer.reduce(events { start() })
        assertEquals(0, state.scoreA)
        assertEquals(0, state.scoreB)
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertTrue(state.rounds.isEmpty())
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
        assertEquals(1, state.roundNumber)
    }

    @Test
    fun `rounds accumulate running scores like the UI sketch`() {
        val state = reducer.reduce(
            events {
                start()
                round(60, call(Seat.A1, TichuType.SMALL, true))
                round(30, call(Seat.B1, TichuType.GRAND, true))
                doubleWin(Team.A)
            },
        )
        assertEquals(390, state.scoreA)
        assertEquals(310, state.scoreB)
        assertEquals(3, state.rounds.size)
        assertEquals(listOf(160, 190, 390), state.rounds.map { it.runningScoreA })
        assertEquals(listOf(40, 310, 310), state.rounds.map { it.runningScoreB })
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test
    fun `undone events are skipped and enable redo`() {
        val state = reducer.reduce(
            events {
                start()
                round(60)
                round(70)
                undoLast()
            },
        )
        assertEquals(60, state.scoreA)
        assertEquals(1, state.rounds.size)
        assertTrue(state.canUndo)
        assertTrue(state.canRedo)
    }

    @Test
    fun `undoing everything leaves only the start`() {
        val state = reducer.reduce(
            events {
                start()
                round(60)
                undoLast()
            },
        )
        assertEquals(0, state.scoreA)
        assertFalse(state.canUndo)
        assertTrue(state.canRedo)
    }

    @Test
    fun `swap changes line up for later rounds only`() {
        val state = reducer.reduce(
            events {
                start()
                round(60)
                swap(Seat.B2, Fixtures.registered(Fixtures.eva))
                round(50)
            },
        )
        assertEquals(Fixtures.registered(Fixtures.dan), state.rounds[0].lineUp.occupantAt(Seat.B2))
        assertEquals(Fixtures.registered(Fixtures.eva), state.rounds[1].lineUp.occupantAt(Seat.B2))
        assertEquals(Fixtures.registered(Fixtures.eva), state.lineUp.occupantAt(Seat.B2))
        assertEquals(3, state.timeline.size)
        assertIs<TimelineEntry.Swap>(state.timeline[1])
    }

    @Test
    fun `swap to guest and back is allowed`() {
        val state = reducer.reduce(
            events {
                start()
                swap(Seat.A1, Fixtures.guest("Uncle Joe"))
                round(60)
                swap(Seat.A1, Fixtures.registered(Fixtures.anna))
            },
        )
        assertEquals(Fixtures.guest("Uncle Joe"), state.rounds[0].lineUp.occupantAt(Seat.A1))
        assertEquals(Fixtures.registered(Fixtures.anna), state.lineUp.occupantAt(Seat.A1))
    }

    @Test
    fun `reaching target finishes the game`() {
        val state = reducer.reduce(
            events {
                start(ruleSet = RuleSet.DEFAULT.copy(targetScore = 200))
                round(100)
                round(100)
            },
        )
        assertEquals(GameStatus.FINISHED, state.status)
        assertEquals(Team.A, state.winner)
    }

    @Test
    fun `undoing the final round reopens the game`() {
        val state = reducer.reduce(
            events {
                start(ruleSet = RuleSet.DEFAULT.copy(targetScore = 200))
                round(100)
                round(100)
                undoLast()
            },
        )
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertNull(state.winner)
    }

    @Test
    fun `tie at target continues with default rules`() {
        val state = reducer.reduce(
            events {
                start(ruleSet = RuleSet.DEFAULT.copy(targetScore = 100))
                round(50)
                round(50)
            },
        )
        assertEquals(GameStatus.IN_PROGRESS, state.status)
        assertEquals(100, state.scoreA)
        assertEquals(100, state.scoreB)
    }

    @Test
    fun `abandoned game has status abandoned`() {
        val state = reducer.reduce(
            events {
                start()
                round(60)
                abandon()
            },
        )
        assertEquals(GameStatus.ABANDONED, state.status)
        assertTrue(state.canUndo)
    }

    @Test
    fun `quick play game keeps null group`() {
        val state = reducer.reduce(events { start(lineUp = ch.tichu.counter.core.model.LineUp.allGuests(), groupId = null) })
        assertNull(state.groupId)
        assertTrue(state.lineUp.isAllGuests())
    }

    @Test
    fun `empty list is rejected`() {
        assertThrows<IllegalArgumentException> { reducer.reduce(emptyList()) }
    }

    @Test
    fun `events are ordered by sequence regardless of list order`() {
        val ordered = events {
            start()
            round(60)
            round(30)
        }
        val shuffled = ordered.reversed()
        assertEquals(reducer.reduce(ordered), reducer.reduce(shuffled))
    }
}
