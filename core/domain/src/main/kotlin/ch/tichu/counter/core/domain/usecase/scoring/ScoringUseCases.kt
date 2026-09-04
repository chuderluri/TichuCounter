package ch.tichu.counter.core.domain.usecase.scoring

import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.common.failure
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.repository.GameRepository
import ch.tichu.counter.core.domain.scoring.ScoringEngine
import ch.tichu.counter.core.domain.scoring.ValidationResult
import ch.tichu.counter.core.model.GameEventPayload
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.RoundInput
import ch.tichu.counter.core.model.RoundResult
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.LineUp
import javax.inject.Inject

class ValidateRoundUseCase @Inject constructor(private val engine: ScoringEngine) {
    operator fun invoke(input: RoundInput, rules: RuleSet): ValidationResult = engine.validate(input, rules)
}

class PreviewRoundUseCase @Inject constructor(private val engine: ScoringEngine) {
    operator fun invoke(
        roundNumber: Int,
        lineUp: LineUp,
        input: RoundInput,
        previousA: Int,
        previousB: Int,
        rules: RuleSet,
    ): RoundResult = engine.score(roundNumber, lineUp, input, previousA, previousB, rules)
}

class RecordRoundUseCase @Inject constructor(
    private val repository: GameRepository,
    private val engine: ScoringEngine,
) {
    suspend operator fun invoke(gameId: GameId, input: RoundInput): Result<Unit, DomainError> {
        val state = repository.getGameState(gameId) ?: return DomainError.GameNotFound.failure()
        if (state.status != GameStatus.IN_PROGRESS) return DomainError.GameNotInProgress.failure()
        return when (val validation = engine.validate(input, state.ruleSet)) {
            is ValidationResult.Invalid -> DomainError.InvalidRound(validation.errors).failure()
            ValidationResult.Valid ->
                repository.appendEvent(gameId, GameEventPayload.RoundScored(input.outcome, input.tichuCalls))
        }
    }
}
