package ch.tichu.counter.core.domain.scoring

import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.Team

sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(val errors: List<ScoringError>) : ValidationResult

    val isValid: Boolean get() = this is Valid
}

sealed interface ScoringError {
    data class CardPointsOutOfRange(val team: Team, val value: Int) : ScoringError

    data class CardPointsNotMultipleOfStep(val team: Team, val value: Int) : ScoringError

    data class CardPointsSumMismatch(val sum: Int) : ScoringError

    data class DuplicateTichuCall(val seat: Seat) : ScoringError

    data object MultipleTichuSuccesses : ScoringError

    data class TichuSuccessOnLosingTeamInDoubleWin(val seat: Seat) : ScoringError

    data object GameAlreadyFinished : ScoringError
}
