package ch.tichu.counter.core.domain.repository

import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.Person
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.PersonSummary
import kotlinx.coroutines.flow.Flow

interface PersonRepository {
    fun observeAll(includeArchived: Boolean = false): Flow<List<Person>>

    fun observeMembers(groupId: GroupId, includeArchived: Boolean = false): Flow<List<PersonSummary>>

    fun observePerson(personId: PersonId): Flow<Person?>

    suspend fun getPerson(personId: PersonId): Person?

    suspend fun findByName(name: String): Person?

    suspend fun create(name: String, avatarColor: AvatarColor, groupId: GroupId?): Result<Person, DomainError>

    suspend fun update(personId: PersonId, name: String, avatarColor: AvatarColor): Result<Unit, DomainError>

    suspend fun setArchived(personId: PersonId, archived: Boolean): Result<Unit, DomainError>

    suspend fun delete(personId: PersonId): Result<Unit, DomainError>

    suspend fun hasGames(personId: PersonId): Boolean
}
