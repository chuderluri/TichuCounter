package ch.tichu.counter.core.data.di

import ch.tichu.counter.core.common.DefaultDispatcherProvider
import ch.tichu.counter.core.common.DispatcherProvider
import ch.tichu.counter.core.common.IdGenerator
import ch.tichu.counter.core.common.SystemTimeProvider
import ch.tichu.counter.core.common.TimeProvider
import ch.tichu.counter.core.common.UuidIdGenerator
import ch.tichu.counter.core.data.repository.GameRepositoryImpl
import ch.tichu.counter.core.data.repository.GroupRepositoryImpl
import ch.tichu.counter.core.data.repository.PersonRepositoryImpl
import ch.tichu.counter.core.domain.repository.GameRepository
import ch.tichu.counter.core.domain.repository.GroupRepository
import ch.tichu.counter.core.domain.repository.PersonRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository

    @Binds
    abstract fun bindPersonRepository(impl: PersonRepositoryImpl): PersonRepository

    @Binds
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = UuidIdGenerator()

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}
