package ch.tichu.counter.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_events",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["game_id", "sequence"], unique = true),
        Index(value = ["game_id", "is_undone"]),
    ],
)
data class GameEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "game_id") val gameId: String,
    val sequence: Int,
    val type: String,
    val payload: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "is_undone") val isUndone: Boolean,
    @ColumnInfo(name = "undone_at") val undoneAt: Long?,
    @ColumnInfo(name = "sync_state") val syncState: String,
)
