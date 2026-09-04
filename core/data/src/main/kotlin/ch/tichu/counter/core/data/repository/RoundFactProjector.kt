package ch.tichu.counter.core.data.repository

import ch.tichu.counter.core.database.entity.RoundFactEntity
import ch.tichu.counter.core.model.EventId
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.RoundOutcome
import ch.tichu.counter.core.model.RoundResult
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.SeatOccupant
import kotlinx.datetime.Instant
import javax.inject.Inject

class RoundFactProjector @Inject constructor() {

    fun project(
        eventId: EventId,
        gameId: GameId,
        groupId: GroupId?,
        result: RoundResult,
        occurredAt: Instant,
    ): List<RoundFactEntity> {
        if (groupId == null) return emptyList()
        return Seat.entries.mapNotNull { seat ->
            val occupant = result.lineUp.occupantAt(seat) as? SeatOccupant.Registered ?: return@mapNotNull null
            val team = seat.team
            val own = result.score(team)
            val other = result.score(team.opponent)
            val call = result.tichuCalls.firstOrNull { it.seat == seat }
            val outcome = result.outcome
            RoundFactEntity(
                eventId = eventId.value,
                gameId = gameId.value,
                groupId = groupId.value,
                roundNumber = result.roundNumber,
                personId = occupant.personId.value,
                seat = seat.name,
                team = team.name,
                partnerId = result.lineUp.occupantAt(seat.partner).personIdOrNull?.value,
                teamCardPoints = own.cardPoints,
                teamTichuBonus = own.tichuBonus,
                teamTotal = own.total,
                opponentTotal = other.total,
                tichuType = call?.type?.name,
                tichuSuccess = call?.success,
                doubleWin = when (outcome) {
                    is RoundOutcome.DoubleWin -> if (outcome.winner == team) "WON" else "LOST"
                    is RoundOutcome.CardPoints -> null
                },
                occurredAt = occurredAt.toEpochMilliseconds(),
            )
        }
    }
}
