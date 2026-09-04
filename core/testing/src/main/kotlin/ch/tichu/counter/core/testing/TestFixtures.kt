package ch.tichu.counter.core.testing

import ch.tichu.counter.core.common.IdGenerator
import ch.tichu.counter.core.common.TimeProvider
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.EventId
import ch.tichu.counter.core.model.GameEvent
import ch.tichu.counter.core.model.GameEventPayload
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.Person
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.RoundOutcome
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.SeatOccupant
import ch.tichu.counter.core.model.SyncState
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TichuCall
import ch.tichu.counter.core.model.TichuType
import kotlinx.datetime.Instant

class FakeTimeProvider(start: Instant = Instant.fromEpochMilliseconds(1_700_000_000_000)) : TimeProvider {
    var current: Instant = start

    override fun now(): Instant = current

    fun advanceSeconds(seconds: Long) {
        current = Instant.fromEpochMilliseconds(current.toEpochMilliseconds() + seconds * 1000)
    }
}

class SequentialIdGenerator(private val prefix: String = "id") : IdGenerator {
    private var counter = 0

    override fun newId(): String = "$prefix-${++counter}"
}

object Fixtures {
    val anna = PersonId("anna")
    val ben = PersonId("ben")
    val cleo = PersonId("cleo")
    val dan = PersonId("dan")
    val eva = PersonId("eva")
    val groupId = GroupId("group-1")
    val gameId = GameId("game-1")
    val t0: Instant = Instant.fromEpochMilliseconds(1_700_000_000_000)

    fun person(id: PersonId, name: String = id.value.replaceFirstChar { it.uppercase() }): Person = Person(
        id = id,
        name = name,
        avatarColor = AvatarColor.BLUE,
        isArchived = false,
        createdAt = t0,
        updatedAt = t0,
        syncState = SyncState.LOCAL_ONLY,
    )

    fun lineUp(
        a1: SeatOccupant = SeatOccupant.Registered(anna),
        b1: SeatOccupant = SeatOccupant.Registered(cleo),
        a2: SeatOccupant = SeatOccupant.Registered(ben),
        b2: SeatOccupant = SeatOccupant.Registered(dan),
    ): LineUp = LineUp.of(a1, b1, a2, b2)

    fun guest(name: String): SeatOccupant = SeatOccupant.Guest(name)

    fun registered(id: PersonId): SeatOccupant = SeatOccupant.Registered(id)

    fun call(seat: Seat, type: TichuType = TichuType.SMALL, success: Boolean = true): TichuCall = TichuCall(seat, type, success)

    fun cards(teamA: Int, teamB: Int = 100 - teamA): RoundOutcome = RoundOutcome.CardPoints(teamA, teamB)

    fun doubleWin(team: Team): RoundOutcome = RoundOutcome.DoubleWin(team)
}

class EventLogBuilder(
    private val gameId: GameId = Fixtures.gameId,
    private val time: FakeTimeProvider = FakeTimeProvider(),
) {
    private val events = mutableListOf<GameEvent>()

    fun start(
        lineUp: LineUp = Fixtures.lineUp(),
        ruleSet: RuleSet = RuleSet.DEFAULT,
        groupId: GroupId? = Fixtures.groupId,
    ): EventLogBuilder = add(GameEventPayload.GameStarted(groupId, lineUp, ruleSet))

    fun round(teamA: Int, vararg calls: TichuCall): EventLogBuilder = add(GameEventPayload.RoundScored(Fixtures.cards(teamA), calls.toList()))

    fun round(outcome: RoundOutcome, vararg calls: TichuCall): EventLogBuilder = add(GameEventPayload.RoundScored(outcome, calls.toList()))

    fun doubleWin(team: Team, vararg calls: TichuCall): EventLogBuilder = add(GameEventPayload.RoundScored(Fixtures.doubleWin(team), calls.toList()))

    fun swap(seat: Seat, next: SeatOccupant): EventLogBuilder {
        val current = currentLineUp().occupantAt(seat)
        return add(GameEventPayload.PlayerSwapped(seat, current, next))
    }

    fun abandon(): EventLogBuilder = add(GameEventPayload.GameAbandoned())

    fun undoLast(): EventLogBuilder {
        val index = events.indexOfLast { it.sequence > 1 && !it.isUndone }
        require(index >= 0) { "Nothing to undo" }
        events[index] = events[index].copy(isUndone = true)
        return this
    }

    fun build(): List<GameEvent> = events.toList()

    private fun add(payload: GameEventPayload): EventLogBuilder {
        time.advanceSeconds(60)
        events += GameEvent(
            id = EventId("evt-${events.size + 1}"),
            gameId = gameId,
            sequence = events.size + 1,
            occurredAt = time.now(),
            isUndone = false,
            payload = payload,
        )
        return this
    }

    private fun currentLineUp(): LineUp {
        var lineUp = (events.first().payload as GameEventPayload.GameStarted).lineUp
        events.drop(1).filter { !it.isUndone }.forEach { event ->
            val payload = event.payload
            if (payload is GameEventPayload.PlayerSwapped) lineUp = lineUp.swap(payload.seat, payload.next)
        }
        return lineUp
    }
}

fun events(block: EventLogBuilder.() -> Unit): List<GameEvent> = EventLogBuilder().apply(block).build()
