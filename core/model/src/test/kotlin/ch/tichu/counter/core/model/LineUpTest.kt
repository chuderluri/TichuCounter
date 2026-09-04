package ch.tichu.counter.core.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LineUpTest {

    private val anna = SeatOccupant.Registered(PersonId("anna"))
    private val ben = SeatOccupant.Registered(PersonId("ben"))
    private val cleo = SeatOccupant.Registered(PersonId("cleo"))
    private val dan = SeatOccupant.Registered(PersonId("dan"))

    @Test
    fun `same person twice is rejected`() {
        assertThrows<IllegalArgumentException> { LineUp.of(anna, cleo, anna, dan) }
    }

    @Test
    fun `guests with the same name are allowed`() {
        val lineUp = LineUp.of(SeatOccupant.Guest("Guest"), cleo, SeatOccupant.Guest("Guest"), dan)
        assertEquals(2, lineUp.registeredPersons().size)
    }

    @Test
    fun `seat and team lookups`() {
        val lineUp = LineUp.of(anna, cleo, ben, dan)
        assertEquals(Seat.A2, lineUp.seatOf(ben.personId))
        assertEquals(Team.B, lineUp.teamOf(dan.personId))
        assertNull(lineUp.seatOf(PersonId("nobody")))
        assertEquals(listOf(anna, ben), lineUp.members(Team.A))
    }

    @Test
    fun `swap rejects already seated person on another seat`() {
        val lineUp = LineUp.of(anna, cleo, ben, dan)
        assertThrows<IllegalArgumentException> { lineUp.swap(Seat.A1, ben) }
    }

    @Test
    fun `swap replaces occupant`() {
        val lineUp = LineUp.of(anna, cleo, ben, dan).swap(Seat.B1, SeatOccupant.Guest("Eva"))
        assertEquals(SeatOccupant.Guest("Eva"), lineUp.occupantAt(Seat.B1))
        assertEquals(3, lineUp.registeredPersons().size)
    }

    @Test
    fun `all guests default names follow seat numbers`() {
        val lineUp = LineUp.allGuests()
        assertTrue(lineUp.isAllGuests())
        assertEquals(SeatOccupant.Guest("Player 1"), lineUp.occupantAt(Seat.A1))
        assertEquals(SeatOccupant.Guest("Player 2"), lineUp.occupantAt(Seat.A2))
        assertEquals(SeatOccupant.Guest("Player 3"), lineUp.occupantAt(Seat.B1))
        assertEquals(SeatOccupant.Guest("Player 4"), lineUp.occupantAt(Seat.B2))
    }

    @Test
    fun `line up round trips through json`() {
        val json = Json { encodeDefaults = true }
        val lineUp = LineUp.of(anna, SeatOccupant.Guest("Eva"), ben, dan)
        val decoded = json.decodeFromString<LineUp>(json.encodeToString(LineUp.serializer(), lineUp))
        assertEquals(lineUp, decoded)
    }

    @Test
    fun `event payload round trips through json with discriminator`() {
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val payload: GameEventPayload = GameEventPayload.RoundScored(
            RoundOutcome.DoubleWin(Team.B),
            listOf(TichuCall(Seat.B1, TichuType.GRAND, true)),
        )
        val encoded = json.encodeToString(GameEventPayload.serializer(), payload)
        assertTrue(encoded.contains("\"type\":\"roundScored\""))
        assertEquals(payload, json.decodeFromString(GameEventPayload.serializer(), encoded))
    }
}
