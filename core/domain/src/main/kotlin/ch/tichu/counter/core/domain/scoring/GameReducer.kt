package ch.tichu.counter.core.domain.scoring

import ch.tichu.counter.core.model.GameEvent
import ch.tichu.counter.core.model.GameEventPayload
import ch.tichu.counter.core.model.GameState
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.LineUp
import ch.tichu.counter.core.model.RoundInput
import ch.tichu.counter.core.model.RoundResult
import ch.tichu.counter.core.model.RuleSet
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TimelineEntry
import javax.inject.Inject

class GameReducer @Inject constructor(
    private val scoringEngine: ScoringEngine,
) {

    fun reduce(events: List<GameEvent>): GameState {
        require(events.isNotEmpty()) { "Cannot reduce an empty event list" }
        val ordered = events.sortedBy { it.sequence }
        val first = ordered.first()
        val started = first.payload as? GameEventPayload.GameStarted
            ?: error("First event must be GameStarted, was ${first.payload::class.simpleName}")

        var lineUp: LineUp = started.lineUp
        val ruleSet: RuleSet = started.ruleSet
        var scoreA = 0
        var scoreB = 0
        var status = GameStatus.IN_PROGRESS
        var winner: Team? = null
        val rounds = mutableListOf<RoundResult>()
        val timeline = mutableListOf<TimelineEntry>()
        var lastEventAt = first.occurredAt

        for (event in ordered.drop(1)) {
            if (event.isUndone) continue
            lastEventAt = event.occurredAt
            when (val payload = event.payload) {
                is GameEventPayload.GameStarted -> error("GameStarted must be the first event only")
                is GameEventPayload.RoundScored -> {
                    val result = scoringEngine.score(
                        roundNumber = rounds.size + 1,
                        lineUp = lineUp,
                        input = RoundInput(payload.outcome, payload.tichuCalls),
                        previousA = scoreA,
                        previousB = scoreB,
                        rules = ruleSet,
                    )
                    rounds += result
                    timeline += TimelineEntry.Round(result)
                    scoreA = result.runningScoreA
                    scoreB = result.runningScoreB
                    when (val finish = scoringEngine.winner(scoreA, scoreB, ruleSet)) {
                        FinishResult.Continue -> {
                            status = GameStatus.IN_PROGRESS
                            winner = null
                        }
                        is FinishResult.Finished -> {
                            status = GameStatus.FINISHED
                            winner = finish.winner
                        }
                    }
                }
                is GameEventPayload.PlayerSwapped -> {
                    lineUp = lineUp.swap(payload.seat, payload.next)
                    timeline += TimelineEntry.Swap(payload.seat, payload.previous, payload.next)
                }
                is GameEventPayload.GameAbandoned -> {
                    status = GameStatus.ABANDONED
                    winner = null
                }
            }
        }

        val canUndo = ordered.any { it.sequence > 1 && !it.isUndone }
        val canRedo = ordered.any { it.isUndone }

        return GameState(
            gameId = first.gameId,
            groupId = started.groupId,
            status = status,
            ruleSet = ruleSet,
            lineUp = lineUp,
            scoreA = scoreA,
            scoreB = scoreB,
            rounds = rounds,
            winner = winner,
            canUndo = canUndo,
            canRedo = canRedo,
            startedAt = first.occurredAt,
            lastEventAt = lastEventAt,
            timeline = timeline,
        )
    }
}
