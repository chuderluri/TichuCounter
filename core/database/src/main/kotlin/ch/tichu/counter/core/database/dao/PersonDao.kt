package ch.tichu.counter.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.tichu.counter.core.database.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

data class PersonWithStats(
    val id: String,
    val name: String,
    @ColumnInfo(name = "name_normalized") val nameNormalized: String,
    @ColumnInfo(name = "avatar_color") val avatarColor: String,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_state") val syncState: String,
    @ColumnInfo(name = "games_played") val gamesPlayed: Int,
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long?,
)

@Dao
interface PersonDao {

    @Query(
        """
        SELECT * FROM persons
        WHERE (:includeArchived OR is_archived = 0)
        ORDER BY name COLLATE NOCASE
        """,
    )
    fun observeAll(includeArchived: Boolean): Flow<List<PersonEntity>>

    @Query(
        """
        SELECT p.id, p.name, p.name_normalized, p.avatar_color, p.is_archived, p.created_at, p.updated_at, p.sync_state,
               (SELECT COUNT(*) FROM game_participants gp INNER JOIN games g ON g.id = gp.game_id
                 WHERE gp.person_id = p.id AND g.group_id = :groupId) AS games_played,
               (SELECT MAX(g.updated_at) FROM game_participants gp INNER JOIN games g ON g.id = gp.game_id
                 WHERE gp.person_id = p.id AND g.group_id = :groupId) AS last_played_at
        FROM persons p INNER JOIN group_members gm ON gm.person_id = p.id
        WHERE gm.group_id = :groupId AND (:includeArchived OR p.is_archived = 0)
        ORDER BY p.name COLLATE NOCASE
        """,
    )
    fun observeMembers(groupId: String, includeArchived: Boolean): Flow<List<PersonWithStats>>

    @Query("SELECT * FROM persons WHERE id = :id")
    fun observePerson(id: String): Flow<PersonEntity?>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getPerson(id: String): PersonEntity?

    @Query("SELECT * FROM persons WHERE id IN (:ids)")
    suspend fun getPersons(ids: List<String>): List<PersonEntity>

    @Query("SELECT * FROM persons WHERE id IN (:ids)")
    fun observePersons(ids: List<String>): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE name_normalized = :nameNormalized AND is_archived = 0 LIMIT 1")
    suspend fun findActiveByNormalizedName(nameNormalized: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: PersonEntity)

    @Update
    suspend fun update(person: PersonEntity)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) > 0 FROM game_participants WHERE person_id = :personId")
    suspend fun hasGames(personId: String): Boolean
}
