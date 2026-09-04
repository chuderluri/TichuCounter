package ch.tichu.counter.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "games",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["group_id", "status", "updated_at"]),
        Index(value = ["status"]),
    ],
)
data class GameEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String?,
    val status: String,
    @ColumnInfo(name = "score_a") val scoreA: Int,
    @ColumnInfo(name = "score_b") val scoreB: Int,
    @ColumnInfo(name = "round_count") val roundCount: Int,
    val winner: String?,
    @ColumnInfo(name = "line_up_json") val lineUpJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long?,
    @ColumnInfo(name = "sync_state") val syncState: String,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
)
