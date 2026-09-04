package ch.tichu.counter.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import ch.tichu.counter.core.database.TichuDatabase
import ch.tichu.counter.core.database.dao.GameDao
import ch.tichu.counter.core.database.dao.GameEventDao
import ch.tichu.counter.core.database.dao.GroupDao
import ch.tichu.counter.core.database.dao.PersonDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TichuDatabase =
        Room.databaseBuilder(context, TichuDatabase::class.java, TichuDatabase.NAME)
            .addCallback(
                object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE UNIQUE INDEX IF NOT EXISTS index_games_single_in_progress " +
                                "ON games(status) WHERE status = 'IN_PROGRESS'",
                        )
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE UNIQUE INDEX IF NOT EXISTS index_games_single_in_progress " +
                                "ON games(status) WHERE status = 'IN_PROGRESS'",
                        )
                    }
                },
            )
            .build()

    @Provides
    fun provideGroupDao(db: TichuDatabase): GroupDao = db.groupDao()

    @Provides
    fun providePersonDao(db: TichuDatabase): PersonDao = db.personDao()

    @Provides
    fun provideGameDao(db: TichuDatabase): GameDao = db.gameDao()

    @Provides
    fun provideGameEventDao(db: TichuDatabase): GameEventDao = db.gameEventDao()
}
