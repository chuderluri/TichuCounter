package ch.tichu.counter.core.model

import kotlinx.datetime.Instant

enum class GameStatus {
    IN_PROGRESS,
    FINISHED,
    ABANDONED,
}

data class Game(
    val id: GameId,
    val groupId: GroupId?,
    val status: GameStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val finishedAt: Instant?,
    val syncState: SyncState,
) {
    val isQuickPlay: Boolean get() = groupId == null
}

data class GameSummary(
    val game: Game,
    val scoreA: Int,
    val scoreB: Int,
    val roundCount: Int,
    val winner: Team?,
    val currentLineUp: LineUp,
    val persons: Map<PersonId, Person>,
) {
    fun displayName(occupant: SeatOccupant): String = when (occupant) {
        is SeatOccupant.Registered -> persons[occupant.personId]?.name ?: "?"
        is SeatOccupant.Guest -> occupant.name
    }
}

data class GameState(
    val gameId: GameId,
    val groupId: GroupId?,
    val status: GameStatus,
    val ruleSet: RuleSet,
    val lineUp: LineUp,
    val scoreA: Int,
    val scoreB: Int,
    val rounds: List<RoundResult>,
    val winner: Team?,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val startedAt: Instant,
    val lastEventAt: Instant,
    val timeline: List<TimelineEntry>,
) {
    val roundNumber: Int get() = rounds.size + 1

    fun score(team: Team): Int = if (team == Team.A) scoreA else scoreB
}

sealed interface TimelineEntry {
    data class Round(val result: RoundResult) : TimelineEntry

    data class Swap(val seat: Seat, val previous: SeatOccupant, val next: SeatOccupant) : TimelineEntry
}
