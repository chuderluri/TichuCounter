package ch.tichu.counter.core.domain.usecase.group

import ch.tichu.counter.core.common.Result
import ch.tichu.counter.core.common.failure
import ch.tichu.counter.core.common.flatMap
import ch.tichu.counter.core.domain.DomainError
import ch.tichu.counter.core.domain.repository.GroupRepository
import ch.tichu.counter.core.domain.repository.PreferencesRepository
import ch.tichu.counter.core.domain.usecase.person.IsPersonInCurrentGameUseCase
import ch.tichu.counter.core.domain.usecase.person.validateName
import ch.tichu.counter.core.model.Group
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.GroupSummary
import ch.tichu.counter.core.model.PersonId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveGroupsUseCase @Inject constructor(private val repository: GroupRepository) {
    operator fun invoke(includeArchived: Boolean = false): Flow<List<GroupSummary>> = repository.observeGroups(includeArchived)
}

class ObserveGroupUseCase @Inject constructor(private val repository: GroupRepository) {
    operator fun invoke(groupId: GroupId): Flow<Group?> = repository.observeGroup(groupId)
}

class ObserveGroupsOfPersonUseCase @Inject constructor(private val repository: GroupRepository) {
    operator fun invoke(personId: PersonId): Flow<List<Group>> = repository.observeGroupsOfPerson(personId)
}

data class ActiveGroup(
    val onboardingDone: Boolean,
    val group: Group?,
) {
    val isQuickPlay: Boolean get() = onboardingDone && group == null
    val needsOnboarding: Boolean get() = !onboardingDone
}

class ObserveActiveGroupUseCase @Inject constructor(
    private val preferences: PreferencesRepository,
    private val groups: GroupRepository,
) {
    operator fun invoke(): Flow<ActiveGroup> = preferences.preferences
        .map { it.onboardingDone to it.activeGroupId }
        .distinctUntilChanged()
        .flatMapLatest { (onboardingDone, groupId) ->
            val groupFlow = if (groupId == null) flowOf(null) else groups.observeGroup(groupId)
            combine(groupFlow, flowOf(onboardingDone)) { group, done -> ActiveGroup(done, group) }
        }
}

class SetActiveGroupUseCase @Inject constructor(private val preferences: PreferencesRepository) {
    suspend operator fun invoke(groupId: GroupId) {
        preferences.setActiveGroup(groupId)
        preferences.setOnboardingDone(true)
    }
}

class ClearActiveGroupUseCase @Inject constructor(private val preferences: PreferencesRepository) {
    suspend operator fun invoke() {
        preferences.setActiveGroup(null)
        preferences.setOnboardingDone(true)
    }
}

class CreateGroupUseCase @Inject constructor(private val repository: GroupRepository) {
    suspend operator fun invoke(name: String): Result<Group, DomainError> = validateName(name).flatMap { repository.create(it) }
}

class RenameGroupUseCase @Inject constructor(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: GroupId, name: String): Result<Unit, DomainError> = validateName(name).flatMap { repository.rename(groupId, it) }
}

class ArchiveGroupUseCase @Inject constructor(
    private val repository: GroupRepository,
    private val preferences: PreferencesRepository,
) {
    suspend operator fun invoke(groupId: GroupId, archived: Boolean): Result<Unit, DomainError> {
        val result = repository.setArchived(groupId, archived)
        if (archived && result is Result.Success) preferences.setActiveGroup(null)
        return result
    }
}

class AddGroupMemberUseCase @Inject constructor(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: GroupId, personId: PersonId): Result<Unit, DomainError> = repository.addMember(groupId, personId)
}

class RemoveGroupMemberUseCase @Inject constructor(
    private val repository: GroupRepository,
    private val isInCurrentGame: IsPersonInCurrentGameUseCase,
) {
    suspend operator fun invoke(groupId: GroupId, personId: PersonId): Result<Unit, DomainError> {
        if (isInCurrentGame(personId)) return DomainError.PersonInActiveGame.failure()
        return repository.removeMember(groupId, personId)
    }
}
