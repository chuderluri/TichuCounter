package ch.tichu.counter.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LineUp(val seats: Map<Seat, SeatOccupant>) {
    init {
        require(seats.size == Seat.entries.size) { "A line-up needs exactly ${Seat.entries.size} seats" }
        val registered = seats.values.mapNotNull { it.personIdOrNull }
        require(registered.size == registered.toSet().size) { "A person cannot occupy two seats" }
    }

    fun occupantAt(seat: Seat): SeatOccupant = seats.getValue(seat)

    fun seatOf(personId: PersonId): Seat? = seats.entries.firstOrNull { it.value.personIdOrNull == personId }?.key

    fun teamOf(personId: PersonId): Team? = seatOf(personId)?.team

    fun members(team: Team): List<SeatOccupant> = Seat.forTeam(team).map { occupantAt(it) }

    fun registeredPersons(): Set<PersonId> = seats.values.mapNotNull { it.personIdOrNull }.toSet()

    fun isAllGuests(): Boolean = seats.values.all { it.isGuest }

    fun swap(seat: Seat, newOccupant: SeatOccupant): LineUp {
        val newPersonId = newOccupant.personIdOrNull
        if (newPersonId != null) {
            val existingSeat = seatOf(newPersonId)
            require(existingSeat == null || existingSeat == seat) { "Person is already seated" }
        }
        return LineUp(seats + (seat to newOccupant))
    }

    companion object {
        fun of(a1: SeatOccupant, b1: SeatOccupant, a2: SeatOccupant, b2: SeatOccupant): LineUp = LineUp(mapOf(Seat.A1 to a1, Seat.B1 to b1, Seat.A2 to a2, Seat.B2 to b2))

        fun allGuests(): LineUp = LineUp(Seat.entries.associateWith { SeatOccupant.Guest(SeatOccupant.defaultGuestName(it)) })
    }
}
