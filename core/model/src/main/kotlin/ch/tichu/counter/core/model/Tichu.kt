package ch.tichu.counter.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TichuType {
    SMALL,
    GRAND,
}

@Serializable
data class TichuCall(
    val seat: Seat,
    val type: TichuType,
    val success: Boolean,
)
