package ch.tichu.counter.core.model

import kotlinx.datetime.Instant

data class Person(
    val id: PersonId,
    val name: String,
    val avatarColor: AvatarColor,
    val isArchived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState,
)

enum class AvatarColor {
    RED,
    ORANGE,
    AMBER,
    GREEN,
    TEAL,
    BLUE,
    INDIGO,
    PURPLE,
    PINK,
    BROWN,
    ;

    companion object {
        fun forIndex(index: Int): AvatarColor = entries[index.mod(entries.size)]
    }
}

data class PersonSummary(
    val person: Person,
    val gamesPlayed: Int,
    val lastPlayedAt: Instant?,
    val isInCurrentGame: Boolean,
)
