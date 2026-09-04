package ch.tichu.counter.core.data.repository

import ch.tichu.counter.core.common.IdGenerator
import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.common.TimeProvider
import ch.tichu.counter.core.common.failure
import ch.tichu.counter.core.common.success
import ch.tichu.counter.core.data.mapper.normalizeName
import ch.tichu.counter.core.data.mapper.toDomain
import ch.tichu.counter.core.data.mapper.toEpochMillis
import ch.tichu.counter.core.database.dao.GroupDao
import ch.tichu.counter.core.database.dao.PersonDao
import ch.tichu.counter.core.database.entity.GroupEntity
import ch.tichu.counter.core.database.entity.GroupMemberEntity
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.NameProblem
import ch.tichu.counter.core.domain.repository.GroupRepository
import ch.tichu.counter.core.model.Group
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.GroupSummary
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val personDao: PersonDao,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider,
) : GroupRepository {

    override fun observeGroups(includeArchived: Boolean): Flow<List<GroupSummary>> = groupDao.observeGroupsWithCounts(includeArchived).map { list -> list.map { it.toDomain() } }

    override fun observeGroup(groupId: GroupId): Flow<Group?> = groupDao.observeGroup(groupId.value).map { it?.toDomain() }

    override fun observeGroupsOfPerson(personId: PersonId): Flow<List<Group>> = groupDao.observeGroupsOfPerson(personId.value).map { list -> list.map { it.toDomain() } }

    override suspend fun getGroup(groupId: GroupId): Group? = groupDao.getGroup(groupId.value)?.toDomain()

    override suspend fun create(name: String): Result<Group, DomainError> {
        val normalized = name.normalizeName()
        if (groupDao.findActiveByNormalizedName(normalized) != null) {
            return DomainError.InvalidName(NameProblem.DUPLICATE).failure()
        }
        val now = timeProvider.now().toEpochMillis()
        val entity = GroupEntity(
            id = idGenerator.newId(),
            name = name.trim(),
            nameNormalized = normalized,
            isArchived = false,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.LOCAL_ONLY.name,
        )
        groupDao.insert(entity)
        return entity.toDomain().success()
    }

    override suspend fun rename(groupId: GroupId, name: String): Result<Unit, DomainError> {
        val existing = groupDao.getGroup(groupId.value) ?: return DomainError.GroupNotFound.failure()
        val normalized = name.normalizeName()
        val duplicate = groupDao.findActiveByNormalizedName(normalized)
        if (duplicate != null && duplicate.id != groupId.value) {
            return DomainError.InvalidName(NameProblem.DUPLICATE).failure()
        }
        groupDao.update(
            existing.copy(
                name = name.trim(),
                nameNormalized = normalized,
                updatedAt = timeProvider.now().toEpochMillis(),
            ),
        )
        return Unit.success()
    }

    override suspend fun setArchived(groupId: GroupId, archived: Boolean): Result<Unit, DomainError> {
        val existing = groupDao.getGroup(groupId.value) ?: return DomainError.GroupNotFound.failure()
        groupDao.update(existing.copy(isArchived = archived, updatedAt = timeProvider.now().toEpochMillis()))
        return Unit.success()
    }

    override suspend fun addMember(groupId: GroupId, personId: PersonId): Result<Unit, DomainError> {
        groupDao.getGroup(groupId.value) ?: return DomainError.GroupNotFound.failure()
        personDao.getPerson(personId.value) ?: return DomainError.PersonNotFound.failure()
        groupDao.insertMember(
            GroupMemberEntity(groupId.value, personId.value, timeProvider.now().toEpochMillis(), SyncState.LOCAL_ONLY.name),
        )
        return Unit.success()
    }

    override suspend fun removeMember(groupId: GroupId, personId: PersonId): Result<Unit, DomainError> {
        if (!groupDao.isMember(groupId.value, personId.value)) return DomainError.NotAMember.failure()
        groupDao.deleteMember(groupId.value, personId.value)
        return Unit.success()
    }

    override suspend fun isMember(groupId: GroupId, personId: PersonId): Boolean = groupDao.isMember(groupId.value, personId.value)
}
