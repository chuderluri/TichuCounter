package ch.tichu.counter.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class GameEvent(
    val id: EventId,
    val gameId: GameId,
    val sequence: Int,
    val occurredAt: Instant,
    val isUndone: Boolean,
    val payload: GameEventPayload,
)

@Serializable
sealed interface GameEventPayload {
    @Serializable
    @SerialName("gameStarted")
    data class GameStarted(
        val groupId: GroupId?,
        val lineUp: LineUp,
        val ruleSet: RuleSet,
    ) : GameEventPayload

    @Serializable
    @SerialName("roundScored")
    data class RoundScored(
        val outcome: RoundOutcome,
        val tichuCalls: List<TichuCall>,
    ) : GameEventPayload

    @Serializable
    @SerialName("playerSwapped")
    data class PlayerSwapped(
        val seat: Seat,
        val previous: SeatOccupant,
        val next: SeatOccupant,
    ) : GameEventPayload

    @Serializable
    @SerialName("gameAbandoned")
    data class GameAbandoned(
        val reason: String? = null,
    ) : GameEventPayload
}

enum class GameEventType {
    GAME_STARTED,
    ROUND_SCORED,
    PLAYER_SWAPPED,
    GAME_ABANDONED,
    ;

    companion object {
        fun of(payload: GameEventPayload): GameEventType = when (payload) {
            is GameEventPayload.GameStarted -> GAME_STARTED
            is GameEventPayload.RoundScored -> ROUND_SCORED
            is GameEventPayload.PlayerSwapped -> PLAYER_SWAPPED
            is GameEventPayload.GameAbandoned -> GAME_ABANDONED
        }
    }
}
