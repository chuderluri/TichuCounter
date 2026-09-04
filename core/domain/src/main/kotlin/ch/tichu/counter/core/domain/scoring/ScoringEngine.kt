package ch.tichu.counter.core.domain.scoring

import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.RoundInput
import ch.tichu.counter.core.model.RoundOutcome
import ch.tichu.counter.core.model.RoundResult
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TeamRoundScore
import ch.tichu.counter.core.model.TichuCall
import javax.inject.Inject

class ScoringEngine @Inject constructor() {

    fun validate(input: RoundInput, rules: RuleSet): ValidationResult {
        val errors = buildList {
            addAll(validateOutcome(input.outcome, rules))
            addAll(validateTichuCalls(input))
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    fun score(
        roundNumber: Int,
        lineUp: LineUp,
        input: RoundInput,
        previousA: Int,
        previousB: Int,
        rules: RuleSet,
    ): RoundResult {
        val (cardsA, cardsB) = cardPoints(input.outcome, rules)
        val bonusA = tichuBonus(input.tichuCalls, Team.A, rules)
        val bonusB = tichuBonus(input.tichuCalls, Team.B, rules)
        val teamA = TeamRoundScore(cardPoints = cardsA, tichuBonus = bonusA)
        val teamB = TeamRoundScore(cardPoints = cardsB, tichuBonus = bonusB)
        return RoundResult(
            roundNumber = roundNumber,
            lineUp = lineUp,
            outcome = input.outcome,
            tichuCalls = input.tichuCalls,
            teamA = teamA,
            teamB = teamB,
            runningScoreA = previousA + teamA.total,
            runningScoreB = previousB + teamB.total,
        )
    }

    fun winner(scoreA: Int, scoreB: Int, rules: RuleSet): FinishResult {
        val reached = scoreA >= rules.targetScore || scoreB >= rules.targetScore
        if (!reached) return FinishResult.Continue
        return when {
            scoreA > scoreB -> FinishResult.Finished(Team.A)
            scoreB > scoreA -> FinishResult.Finished(Team.B)
            rules.finishOnTie -> FinishResult.Finished(null)
            else -> FinishResult.Continue
        }
    }

    fun complement(entered: Int, rules: RuleSet): Int = rules.roundCardPointsTotal - entered

    fun tichuBonus(calls: List<TichuCall>, team: Team, rules: RuleSet): Int = calls.filter { it.seat.team == team }
        .sumOf { call -> if (call.success) rules.tichuValue(call.type) else -rules.tichuValue(call.type) }

    private fun cardPoints(outcome: RoundOutcome, rules: RuleSet): Pair<Int, Int> = when (outcome) {
        is RoundOutcome.CardPoints -> outcome.teamA to outcome.teamB
        is RoundOutcome.DoubleWin ->
            if (outcome.winner == Team.A) rules.doubleWinValue to 0 else 0 to rules.doubleWinValue
    }

    private fun validateOutcome(outcome: RoundOutcome, rules: RuleSet): List<ScoringError> = when (outcome) {
        is RoundOutcome.DoubleWin -> emptyList()
        is RoundOutcome.CardPoints -> buildList {
            validateTeamPoints(Team.A, outcome.teamA, rules)?.let(::addAll)
            validateTeamPoints(Team.B, outcome.teamB, rules)?.let(::addAll)
            val sum = outcome.teamA + outcome.teamB
            if (sum != rules.roundCardPointsTotal) add(ScoringError.CardPointsSumMismatch(sum))
        }
    }

    private fun validateTeamPoints(team: Team, value: Int, rules: RuleSet): List<ScoringError>? {
        val errors = buildList {
            if (value < rules.minTeamCardPoints || value > rules.maxTeamCardPoints) {
                add(ScoringError.CardPointsOutOfRange(team, value))
            }
            if (value.mod(rules.cardPointStep) != 0) {
                add(ScoringError.CardPointsNotMultipleOfStep(team, value))
            }
        }
        return errors.ifEmpty { null }
    }

    private fun validateTichuCalls(input: RoundInput): List<ScoringError> = buildList {
        val calls = input.tichuCalls
        calls.groupBy { it.seat }.filterValues { it.size > 1 }.keys.forEach { add(ScoringError.DuplicateTichuCall(it)) }
        val successes = calls.filter { it.success }
        if (successes.size > 1) add(ScoringError.MultipleTichuSuccesses)
        val outcome = input.outcome
        if (outcome is RoundOutcome.DoubleWin) {
            successes.filter { it.seat.team != outcome.winner }
                .forEach { add(ScoringError.TichuSuccessOnLosingTeamInDoubleWin(it.seat)) }
        }
    }
}

sealed interface FinishResult {
    data object Continue : FinishResult

    data class Finished(val winner: Team?) : FinishResult
}
