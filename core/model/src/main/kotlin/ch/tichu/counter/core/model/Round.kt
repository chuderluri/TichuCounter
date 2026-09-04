package ch.tichu.counter.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RoundOutcome {
    @Serializable
    @SerialName("cardPoints")
    data class CardPoints(val teamA: Int, val teamB: Int) : RoundOutcome

    @Serializable
    @SerialName("doubleWin")
    data class DoubleWin(val winner: Team) : RoundOutcome
}

data class RoundInput(
    val outcome: RoundOutcome,
    val tichuCalls: List<TichuCall> = emptyList(),
)

data class TeamRoundScore(
    val cardPoints: Int,
    val tichuBonus: Int,
) {
    val total: Int get() = cardPoints + tichuBonus
}

data class RoundResult(
    val roundNumber: Int,
    val lineUp: LineUp,
    val outcome: RoundOutcome,
    val tichuCalls: List<TichuCall>,
    val teamA: TeamRoundScore,
    val teamB: TeamRoundScore,
    val runningScoreA: Int,
    val runningScoreB: Int,
) {
    fun score(team: Team): TeamRoundScore = if (team == Team.A) teamA else teamB

    fun runningScore(team: Team): Int = if (team == Team.A) runningScoreA else runningScoreB
}
