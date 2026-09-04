package ch.tichu.counter.core.domain.scoring

import ch.tichu.counter.core.model.RoundInput
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TichuType
import ch.tichu.counter.core.testing.Fixtures
import ch.tichu.counter.core.testing.Fixtures.call
import ch.tichu.counter.core.testing.Fixtures.cards
import ch.tichu.counter.core.testing.Fixtures.doubleWin
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScoringEngineTest {

    private val engine = ScoringEngine()
    private val rules = RuleSet.DEFAULT
    private val lineUp = Fixtures.lineUp()

    private fun score(input: RoundInput, previousA: Int = 0, previousB: Int = 0) = engine.score(1, lineUp, input, previousA, previousB, rules)

    @Nested
    inner class WorkedExamples {

        @Test
        fun `1 - 60 card points no calls`() {
            val result = score(RoundInput(cards(60)))
            assertEquals(60, result.teamA.total)
            assertEquals(40, result.teamB.total)
        }

        @Test
        fun `2 - 60 with A1 small tichu success`() {
            val result = score(RoundInput(cards(60), listOf(call(Seat.A1, TichuType.SMALL, true))))
            assertEquals(160, result.teamA.total)
            assertEquals(40, result.teamB.total)
        }

        @Test
        fun `3 - 60 with B1 grand tichu fail`() {
            val result = score(RoundInput(cards(60), listOf(call(Seat.B1, TichuType.GRAND, false))))
            assertEquals(60, result.teamA.total)
            assertEquals(-160, result.teamB.total)
        }

        @Test
        fun `4 - double win A no calls`() {
            val result = score(RoundInput(doubleWin(Team.A)))
            assertEquals(200, result.teamA.total)
            assertEquals(0, result.teamB.total)
        }

        @Test
        fun `5 - double win A with A2 small success`() {
            val result = score(RoundInput(doubleWin(Team.A), listOf(call(Seat.A2, TichuType.SMALL, true))))
            assertEquals(300, result.teamA.total)
            assertEquals(0, result.teamB.total)
        }

        @Test
        fun `6 - double win A with B1 small fail`() {
            val result = score(RoundInput(doubleWin(Team.A), listOf(call(Seat.B1, TichuType.SMALL, false))))
            assertEquals(200, result.teamA.total)
            assertEquals(-100, result.teamB.total)
        }

        @Test
        fun `7 - phoenix minus 25`() {
            val result = score(RoundInput(cards(-25)))
            assertEquals(-25, result.teamA.total)
            assertEquals(125, result.teamB.total)
        }

        @Test
        fun `8 - two successful tichus is invalid`() {
            val input = RoundInput(cards(60), listOf(call(Seat.A1, success = true), call(Seat.B1, success = true)))
            val result = engine.validate(input, rules)
            assertIs<ValidationResult.Invalid>(result)
            assertTrue(ScoringError.MultipleTichuSuccesses in result.errors)
        }

        @Test
        fun `9 - 63 is not a multiple of step`() {
            val result = engine.validate(RoundInput(cards(63, 37)), rules)
            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.any { it is ScoringError.CardPointsNotMultipleOfStep })
        }

        @Test
        fun `10 - tichu success on losing team in double win is invalid`() {
            val input = RoundInput(doubleWin(Team.A), listOf(call(Seat.B2, TichuType.GRAND, true)))
            val result = engine.validate(input, rules)
            assertIs<ValidationResult.Invalid>(result)
            assertEquals(listOf(ScoringError.TichuSuccessOnLosingTeamInDoubleWin(Seat.B2)), result.errors)
        }

        @Test
        fun `11 - reaching target finishes the game`() {
            val result = score(RoundInput(cards(60)), previousA = 950, previousB = 900)
            assertEquals(1010, result.runningScoreA)
            assertEquals(FinishResult.Finished(Team.A), engine.winner(result.runningScoreA, result.runningScoreB, rules))
        }

        @Test
        fun `12 - tie at target continues by default`() {
            val result = score(RoundInput(cards(50)), previousA = 950, previousB = 950)
            assertEquals(FinishResult.Continue, engine.winner(result.runningScoreA, result.runningScoreB, rules))
        }

        @Test
        fun `12b - tie at target finishes as draw when finishOnTie`() {
            val tieRules = rules.copy(finishOnTie = true)
            assertEquals(FinishResult.Finished(null), engine.winner(1000, 1000, tieRules))
        }
    }

    @Nested
    inner class Validation {

        @ParameterizedTest
        @ValueSource(ints = [-25, 0, 50, 100, 125])
        fun `boundary values are valid`(teamA: Int) {
            assertEquals(ValidationResult.Valid, engine.validate(RoundInput(cards(teamA)), rules))
        }

        @ParameterizedTest
        @ValueSource(ints = [-30, 130])
        fun `out of range is invalid`(teamA: Int) {
            val result = engine.validate(RoundInput(cards(teamA)), rules)
            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.any { it is ScoringError.CardPointsOutOfRange })
        }

        @Test
        fun `sum mismatch is invalid`() {
            val result = engine.validate(RoundInput(cards(60, 50)), rules)
            assertIs<ValidationResult.Invalid>(result)
            assertTrue(ScoringError.CardPointsSumMismatch(110) in result.errors)
        }

        @Test
        fun `duplicate call per seat is invalid`() {
            val input = RoundInput(cards(60), listOf(call(Seat.A1, TichuType.SMALL, false), call(Seat.A1, TichuType.GRAND, false)))
            val result = engine.validate(input, rules)
            assertIs<ValidationResult.Invalid>(result)
            assertTrue(ScoringError.DuplicateTichuCall(Seat.A1) in result.errors)
        }

        @Test
        fun `several failed calls are allowed`() {
            val input = RoundInput(
                cards(60),
                listOf(call(Seat.A1, success = false), call(Seat.A2, success = false), call(Seat.B1, success = false)),
            )
            assertEquals(ValidationResult.Valid, engine.validate(input, rules))
        }

        @Test
        fun `double win with failing tichu on losing team is valid`() {
            val input = RoundInput(doubleWin(Team.B), listOf(call(Seat.A1, success = false)))
            assertEquals(ValidationResult.Valid, engine.validate(input, rules))
        }
    }

    @Nested
    inner class Helpers {

        @Test
        fun `complement fills to 100`() {
            assertEquals(40, engine.complement(60, rules))
            assertEquals(125, engine.complement(-25, rules))
        }

        @Test
        fun `tichu bonus sums per team`() {
            val calls = listOf(
                call(Seat.A1, TichuType.SMALL, true),
                call(Seat.A2, TichuType.GRAND, false),
                call(Seat.B1, TichuType.SMALL, false),
            )
            assertEquals(-100, engine.tichuBonus(calls, Team.A, rules))
            assertEquals(-100, engine.tichuBonus(calls, Team.B, rules))
        }

        @Test
        fun `negative running scores are allowed`() {
            val result = score(RoundInput(cards(0), listOf(call(Seat.A1, TichuType.GRAND, false))), previousA = 100)
            assertEquals(-100, result.runningScoreA)
        }
    }
}
