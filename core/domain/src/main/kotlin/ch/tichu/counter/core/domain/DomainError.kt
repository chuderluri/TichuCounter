package ch.tichu.counter.core.domain

import ch.tichu.counter.core.domain.scoring.ScoringError

sealed interface DomainError {
    data object GameNotFound : DomainError

    data object GameAlreadyInProgress : DomainError

    data object GameNotInProgress : DomainError

    data object NothingToUndo : DomainError

    data object NothingToRedo : DomainError

    data object PersonNotFound : DomainError

    data object GroupNotFound : DomainError

    data object PersonInActiveGame : DomainError

    data object PersonHasGames : DomainError

    data object NotAMember : DomainError

    data object LastGroupMembership : DomainError

    data class InvalidName(val reason: NameProblem) : DomainError

    data class InvalidRound(val errors: List<ScoringError>) : DomainError

    data class Unexpected(val message: String) : DomainError
}

enum class NameProblem {
    BLANK,
    TOO_LONG,
    DUPLICATE,
}
