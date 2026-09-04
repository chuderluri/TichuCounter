package ch.tichu.counter.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "round_facts",
    primaryKeys = ["event_id", "seat"],
    foreignKeys = [
        ForeignKey(
            entity = GameEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["person_id", "group_id", "occurred_at"]),
        Index(value = ["game_id", "round_number"]),
    ],
)
data class RoundFactEntity(
    @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "game_id") val gameId: String,
    @ColumnInfo(name = "group_id") val groupId: String?,
    @ColumnInfo(name = "round_number") val roundNumber: Int,
    @ColumnInfo(name = "person_id") val personId: String,
    val seat: String,
    val team: String,
    @ColumnInfo(name = "partner_id") val partnerId: String?,
    @ColumnInfo(name = "team_card_points") val teamCardPoints: Int,
    @ColumnInfo(name = "team_tichu_bonus") val teamTichuBonus: Int,
    @ColumnInfo(name = "team_total") val teamTotal: Int,
    @ColumnInfo(name = "opponent_total") val opponentTotal: Int,
    @ColumnInfo(name = "tichu_type") val tichuType: String?,
    @ColumnInfo(name = "tichu_success") val tichuSuccess: Boolean?,
    @ColumnInfo(name = "double_win") val doubleWin: String?,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
)
