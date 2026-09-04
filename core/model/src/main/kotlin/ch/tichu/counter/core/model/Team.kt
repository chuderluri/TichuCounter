package ch.tichu.counter.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Team {
    A,
    B,
    ;

    val opponent: Team
        get() = if (this == A) B else A
}

@Serializable
enum class Seat(val team: Team, val slotIndex: Int) {
    A1(Team.A, 0),
    B1(Team.B, 0),
    A2(Team.A, 1),
    B2(Team.B, 1),
    ;

    val partner: Seat
        get() = when (this) {
            A1 -> A2
            A2 -> A1
            B1 -> B2
            B2 -> B1
        }

    val displayNumber: Int
        get() = when (this) {
            A1 -> 1
            A2 -> 2
            B1 -> 3
            B2 -> 4
        }

    companion object {
        fun forTeam(team: Team): List<Seat> = entries.filter { it.team == team }
    }
}
