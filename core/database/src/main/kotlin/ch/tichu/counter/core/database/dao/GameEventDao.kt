package ch.tichu.counter.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.tichu.counter.core.database.entity.GameEventEntity
import ch.tichu.counter.core.database.entity.RoundFactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameEventDao {

    @Query("SELECT * FROM game_events WHERE game_id = :gameId ORDER BY sequence")
    fun observeEvents(gameId: String): Flow<List<GameEventEntity>>

    @Query("SELECT * FROM game_events WHERE game_id = :gameId ORDER BY sequence")
    suspend fun getEvents(gameId: String): List<GameEventEntity>

    @Query("SELECT MAX(sequence) FROM game_events WHERE game_id = :gameId")
    suspend fun maxSequence(gameId: String): Int?

    @Query("SELECT * FROM game_events WHERE game_id = :gameId AND is_undone = 0 AND sequence > 1 ORDER BY sequence DESC LIMIT 1")
    suspend fun lastActiveEvent(gameId: String): GameEventEntity?

    @Query("SELECT * FROM game_events WHERE game_id = :gameId AND is_undone = 1 ORDER BY sequence ASC LIMIT 1")
    suspend fun firstUndoneEvent(gameId: String): GameEventEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: GameEventEntity)

    @Query(
        """
        UPDATE game_events SET is_undone = :undone, undone_at = :undoneAt, sync_state = :syncState
        WHERE id = :id
        """,
    )
    suspend fun setUndone(id: String, undone: Boolean, undoneAt: Long?, syncState: String)

    @Query("DELETE FROM game_events WHERE game_id = :gameId AND is_undone = 1")
    suspend fun deleteUndone(gameId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoundFacts(facts: List<RoundFactEntity>)

    @Query("DELETE FROM round_facts WHERE event_id = :eventId")
    suspend fun deleteRoundFacts(eventId: String)

    @Query("DELETE FROM round_facts WHERE game_id = :gameId")
    suspend fun deleteRoundFactsForGame(gameId: String)
}
