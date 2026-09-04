package ch.tichu.counter.core.domain.repository

import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.model.Group
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.GroupSummary
import ch.tichu.counter.core.model.PersonId
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun observeGroups(includeArchived: Boolean = false): Flow<List<GroupSummary>>

    fun observeGroup(groupId: GroupId): Flow<Group?>

    fun observeGroupsOfPerson(personId: PersonId): Flow<List<Group>>

    suspend fun getGroup(groupId: GroupId): Group?

    suspend fun create(name: String): Result<Group, DomainError>

    suspend fun rename(groupId: GroupId, name: String): Result<Unit, DomainError>

    suspend fun setArchived(groupId: GroupId, archived: Boolean): Result<Unit, DomainError>

    suspend fun addMember(groupId: GroupId, personId: PersonId): Result<Unit, DomainError>

    suspend fun removeMember(groupId: GroupId, personId: PersonId): Result<Unit, DomainError>

    suspend fun isMember(groupId: GroupId, personId: PersonId): Boolean
}
