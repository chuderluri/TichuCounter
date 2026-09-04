package ch.tichu.counter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ch.tichu.counter.core.database.dao.GameDao
import ch.tichu.counter.core.database.dao.GameEventDao
import ch.tichu.counter.core.database.dao.GroupDao
import ch.tichu.counter.core.database.dao.PersonDao
import ch.tichu.counter.core.database.entity.GameEntity
import ch.tichu.counter.core.database.entity.GameEventEntity
import ch.tichu.counter.core.database.entity.GameParticipantEntity
import ch.tichu.counter.core.database.entity.GroupEntity
import ch.tichu.counter.core.database.entity.GroupMemberEntity
import ch.tichu.counter.core.database.entity.PersonEntity
import ch.tichu.counter.core.database.entity.RoundFactEntity

@Database(
    entities = [
        GroupEntity::class,
        PersonEntity::class,
        GroupMemberEntity::class,
        GameEntity::class,
        GameEventEntity::class,
        GameParticipantEntity::class,
        RoundFactEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TichuDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao

    abstract fun personDao(): PersonDao

    abstract fun gameDao(): GameDao

    abstract fun gameEventDao(): GameEventDao

    companion object {
        const val NAME = "tichu.db"
    }
}
