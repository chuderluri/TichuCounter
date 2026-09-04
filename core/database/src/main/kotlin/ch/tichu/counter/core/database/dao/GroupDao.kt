package ch.tichu.counter.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.tichu.counter.core.database.entity.GroupEntity
import ch.tichu.counter.core.database.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

data class GroupWithCounts(
    val id: String,
    val name: String,
    @androidx.room.ColumnInfo(name = "name_normalized") val nameNormalized: String,
    @androidx.room.ColumnInfo(name = "is_archived") val isArchived: Boolean,
    @androidx.room.ColumnInfo(name = "created_at") val createdAt: Long,
    @androidx.room.ColumnInfo(name = "updated_at") val updatedAt: Long,
    @androidx.room.ColumnInfo(name = "sync_state") val syncState: String,
    @androidx.room.ColumnInfo(name = "member_count") val memberCount: Int,
    @androidx.room.ColumnInfo(name = "game_count") val gameCount: Int,
)

@Dao
interface GroupDao {

    @Query(
        """
        SELECT g.id, g.name, g.name_normalized, g.is_archived, g.created_at, g.updated_at, g.sync_state,
               (SELECT COUNT(*) FROM group_members gm INNER JOIN persons p ON p.id = gm.person_id
                 WHERE gm.group_id = g.id AND p.is_archived = 0) AS member_count,
               (SELECT COUNT(*) FROM games ga WHERE ga.group_id = g.id) AS game_count
        FROM groups g
        WHERE (:includeArchived OR g.is_archived = 0)
        ORDER BY g.name COLLATE NOCASE
        """,
    )
    fun observeGroupsWithCounts(includeArchived: Boolean): Flow<List<GroupWithCounts>>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun observeGroup(id: String): Flow<GroupEntity?>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroup(id: String): GroupEntity?

    @Query("SELECT * FROM groups WHERE name_normalized = :nameNormalized AND is_archived = 0 LIMIT 1")
    suspend fun findActiveByNormalizedName(nameNormalized: String): GroupEntity?

    @Query(
        """
        SELECT g.* FROM groups g INNER JOIN group_members gm ON gm.group_id = g.id
        WHERE gm.person_id = :personId AND g.is_archived = 0
        ORDER BY g.name COLLATE NOCASE
        """,
    )
    fun observeGroupsOfPerson(personId: String): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(group: GroupEntity)

    @Update
    suspend fun update(group: GroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE group_id = :groupId AND person_id = :personId")
    suspend fun deleteMember(groupId: String, personId: String)

    @Query("SELECT COUNT(*) > 0 FROM group_members WHERE group_id = :groupId AND person_id = :personId")
    suspend fun isMember(groupId: String, personId: String): Boolean

    @Query("SELECT COUNT(*) FROM group_members WHERE person_id = :personId")
    suspend fun membershipCount(personId: String): Int
}
