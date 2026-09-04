package ch.tichu.counter.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RuleSet(
    val targetScore: Int = 1000,
    val roundCardPointsTotal: Int = 100,
    val minTeamCardPoints: Int = -25,
    val maxTeamCardPoints: Int = 125,
    val cardPointStep: Int = 5,
    val smallTichuValue: Int = 100,
    val grandTichuValue: Int = 200,
    val doubleWinValue: Int = 200,
    val finishOnTie: Boolean = false,
) {
    fun tichuValue(type: TichuType): Int = when (type) {
        TichuType.SMALL -> smallTichuValue
        TichuType.GRAND -> grandTichuValue
    }

    companion object {
        val DEFAULT = RuleSet()
        val TARGET_SCORE_OPTIONS = listOf(500, 1000, 1500, 2000)
    }
}
