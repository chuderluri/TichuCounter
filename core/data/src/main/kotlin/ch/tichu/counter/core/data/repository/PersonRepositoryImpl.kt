package ch.tichu.counter.core.data.repository

import androidx.room.withTransaction
import ch.tichu.counter.core.common.IdGenerator
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.common.TimeProvider
import ch.tichu.counter.core.common.failure
import ch.tichu.counter.core.common.success
import ch.tichu.counter.core.data.codec.GameEventPayloadCodec
import ch.tichu.counter.core.data.mapper.normalizeName
import ch.tichu.counter.core.data.mapper.toDomain
import ch.tichu.counter.core.data.mapper.toEpochMillis
import ch.tichu.counter.core.database.TichuDatabase
import ch.tichu.counter.core.database.dao.GameDao
import ch.tichu.counter.core.database.dao.GroupDao
import ch.tichu.counter.core.database.dao.PersonDao
import ch.tichu.counter.core.database.entity.GroupMemberEntity
import ch.tichu.counter.core.database.entity.PersonEntity
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.repository.PersonRepository
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.Person
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.PersonSummary
import ch.tichu.counter.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepositoryImpl @Inject constructor(
    private val database: TichuDatabase,
    private val personDao: PersonDao,
    private val groupDao: GroupDao,
    private val gameDao: GameDao,
    private val codec: GameEventPayloadCodec,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
) : PersonRepository {

    override fun observeAll(includeArchived: Boolean): Flow<List<Person>> =
        personDao.observeAll(includeArchived).map { list -> list.map { it.toDomain() } }

    override fun observeMembers(groupId: GroupId, includeArchived: Boolean): Flow<List<PersonSummary>> =
        combine(
            personDao.observeMembers(groupId.value, includeArchived),
            gameDao.observeCurrentGame(),
        ) { members, current ->
            val seated = current?.let { codec.decodeLineUp(it.lineUpJson).registeredPersons() } ?: emptySet()
            members.map { it.toDomain(isInCurrentGame = PersonId(it.id) in seated) }
        }

    override fun observePerson(personId: PersonId): Flow<Person?> =
        personDao.observePerson(personId.value).map { it?.toDomain() }

    override suspend fun getPerson(personId: PersonId): Person? = personDao.getPerson(personId.value)?.toDomain()

    override suspend fun findByName(name: String): Person? =
        personDao.findActiveByNormalizedName(name.normalizeName())?.toDomain()

    override suspend fun create(name: String, avatarColor: AvatarColor, groupId: GroupId?): Result<Person, DomainError> =
        database.withTransaction {
            val now = timeProvider.now().toEpochMillis()
            val entity = PersonEntity(
                id = idGenerator.newId(),
                name = name.trim(),
                nameNormalized = name.normalizeName(),
                avatarColor = avatarColor.name,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.LOCAL_ONLY.name,
            )
            personDao.insert(entity)
            if (groupId != null) {
                groupDao.getGroup(groupId.value) ?: return@withTransaction DomainError.GroupNotFound.failure()
                groupDao.insertMember(
                    GroupMemberEntity(groupId.value, entity.id, now, SyncState.LOCAL_ONLY.name),
                )
            }
            entity.toDomain().success()
        }

    override suspend fun update(personId: PersonId, name: String, avatarColor: AvatarColor): Result<Unit, DomainError> {
        val existing = personDao.getPerson(personId.value) ?: return DomainError.PersonNotFound.failure()
        personDao.update(
            existing.copy(
                name = name.trim(),
                nameNormalized = name.normalizeName(),
                avatarColor = avatarColor.name,
                updatedAt = timeProvider.now().toEpochMillis(),
                syncState = pendingIfSynced(existing.syncState),
            ),
        )
        return Unit.success()
    }

    override suspend fun setArchived(personId: PersonId, archived: Boolean): Result<Unit, DomainError> {
        val existing = personDao.getPerson(personId.value) ?: return DomainError.PersonNotFound.failure()
        personDao.update(
            existing.copy(
                isArchived = archived,
                updatedAt = timeProvider.now().toEpochMillis(),
                syncState = pendingIfSynced(existing.syncState),
            ),
        )
        return Unit.success()
    }

    override suspend fun delete(personId: PersonId): Result<Unit, DomainError> {
        personDao.getPerson(personId.value) ?: return DomainError.PersonNotFound.failure()
        if (personDao.hasGames(personId.value)) return DomainError.PersonHasGames.failure()
        personDao.delete(personId.value)
        return Unit.success()
    }

    override suspend fun hasGames(personId: PersonId): Boolean = personDao.hasGames(personId.value)

    private fun pendingIfSynced(current: String): String =
        if (current == SyncState.SYNCED.name) SyncState.PENDING_UPLOAD.name else current
}
