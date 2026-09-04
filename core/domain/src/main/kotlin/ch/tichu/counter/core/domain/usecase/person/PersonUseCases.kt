package ch.tichu.counter.core.domain.usecase.person

import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.common.failure
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.NameProblem
import ch.tichu.counter.core.domain.repository.GameRepository
import ch.tichu.counter.core.domain.repository.GroupRepository
import ch.tichu.counter.core.domain.repository.PersonRepository
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.Person
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.PersonSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

const val MAX_NAME_LENGTH = 30

fun validateName(raw: String): Result<String, DomainError> {
    val name = raw.trim()
    return when {
        name.isBlank() -> DomainError.InvalidName(NameProblem.BLANK).failure()
        name.length > MAX_NAME_LENGTH -> DomainError.InvalidName(NameProblem.TOO_LONG).failure()
        else -> Result.Success(name)
    }
}

class ObserveGroupMembersUseCase @Inject constructor(private val repository: PersonRepository) {
    operator fun invoke(groupId: GroupId, includeArchived: Boolean = false): Flow<List<PersonSummary>> = repository.observeMembers(groupId, includeArchived)
}

class ObserveAllPersonsUseCase @Inject constructor(private val repository: PersonRepository) {
    operator fun invoke(includeArchived: Boolean = false): Flow<List<Person>> = repository.observeAll(includeArchived)
}

class ObservePersonUseCase @Inject constructor(private val repository: PersonRepository) {
    operator fun invoke(personId: PersonId): Flow<Person?> = repository.observePerson(personId)
}

class CreatePersonUseCase @Inject constructor(private val repository: PersonRepository) {
    suspend operator fun invoke(name: String, avatarColor: AvatarColor, groupId: GroupId?): Result<Person, DomainError> {
        val valid = validateName(name)
        if (valid is Result.Failure) return valid
        val trimmed = (valid as Result.Success).value
        if (repository.findByName(trimmed) != null) return DomainError.InvalidName(NameProblem.DUPLICATE).failure()
        return repository.create(trimmed, avatarColor, groupId)
    }
}

class IsPersonInCurrentGameUseCase @Inject constructor(private val gameRepository: GameRepository) {
    suspend operator fun invoke(personId: PersonId): Boolean {
        val current = gameRepository.observeCurrentGame().first() ?: return false
        return current.currentLineUp.seatOf(personId) != null
    }
}

class UpdatePersonUseCase @Inject constructor(
    private val repository: PersonRepository,
    private val isInCurrentGame: IsPersonInCurrentGameUseCase,
) {
    suspend operator fun invoke(personId: PersonId, name: String, avatarColor: AvatarColor): Result<Unit, DomainError> {
        if (isInCurrentGame(personId)) return DomainError.PersonInActiveGame.failure()
        val valid = validateName(name)
        if (valid is Result.Failure) return valid
        val trimmed = (valid as Result.Success).value
        val existing = repository.findByName(trimmed)
        if (existing != null && existing.id != personId) return DomainError.InvalidName(NameProblem.DUPLICATE).failure()
        return repository.update(personId, trimmed, avatarColor)
    }
}

class ArchivePersonUseCase @Inject constructor(
    private val repository: PersonRepository,
    private val isInCurrentGame: IsPersonInCurrentGameUseCase,
) {
    suspend operator fun invoke(personId: PersonId, archived: Boolean): Result<Unit, DomainError> {
        if (archived && isInCurrentGame(personId)) return DomainError.PersonInActiveGame.failure()
        return repository.setArchived(personId, archived)
    }
}

class DeletePersonUseCase @Inject constructor(
    private val repository: PersonRepository,
    private val isInCurrentGame: IsPersonInCurrentGameUseCase,
) {
    suspend operator fun invoke(personId: PersonId): Result<Unit, DomainError> {
        if (isInCurrentGame(personId)) return DomainError.PersonInActiveGame.failure()
        if (repository.hasGames(personId)) return DomainError.PersonHasGames.failure()
        return repository.delete(personId)
    }
}

class SetPersonGroupMembershipUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val isInCurrentGame: IsPersonInCurrentGameUseCase,
) {
    suspend operator fun invoke(personId: PersonId, groupId: GroupId, member: Boolean): Result<Unit, DomainError> {
        if (!member && isInCurrentGame(personId)) return DomainError.PersonInActiveGame.failure()
        return if (member) {
            groupRepository.addMember(groupId, personId)
        } else {
            val groups = groupRepository.observeGroupsOfPerson(personId).first()
            if (groups.size <= 1) return DomainError.LastGroupMembership.failure()
            groupRepository.removeMember(groupId, personId)
        }
    }
}
