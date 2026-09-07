package ch.tichu.counter.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SeatOccupant {
    @Serializable
    @SerialName("registered")
    data class Registered(val personId: PersonId) : SeatOccupant

    @Serializable
    @SerialName("guest")
    data class Guest(val name: String) : SeatOccupant

    val personIdOrNull: PersonId?
        get() = (this as? Registered)?.personId

    val isGuest: Boolean
        get() = this is Guest

    companion object {
        fun defaultGuestName(seat: Seat): String = "Guest ${seat.displayNumber}"
    }
}
