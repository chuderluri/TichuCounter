package ch.tichu.counter.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ch.tichu.counter.core.database.entity.GameEntity
import ch.tichu.counter.core.database.entity.GameParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeGame(id: String): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGame(id: String): GameEntity?

    @Query("SELECT * FROM games WHERE status = 'IN_PROGRESS' ORDER BY updated_at DESC LIMIT 1")
    fun observeCurrentGame(): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE status = 'IN_PROGRESS' ORDER BY updated_at DESC LIMIT 1")
    suspend fun getCurrentGame(): GameEntity?

    @Query(
        """
        SELECT * FROM games
        WHERE ((:groupId IS NULL AND group_id IS NULL) OR group_id = :groupId)
          AND status IN (:statuses)
        ORDER BY updated_at DESC
        """,
    )
    fun observeGames(groupId: String?, statuses: List<String>): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(game: GameEntity)

    @Update
    suspend fun update(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun delete(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertParticipants(participants: List<GameParticipantEntity>)

    @Query("DELETE FROM game_participants WHERE game_id = :gameId")
    suspend fun deleteParticipants(gameId: String)

    @Query("SELECT person_id FROM game_participants WHERE game_id = :gameId")
    suspend fun participantIds(gameId: String): List<String>
}
