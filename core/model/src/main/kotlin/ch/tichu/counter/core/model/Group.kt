package ch.tichu.counter.core.model

import kotlinx.datetime.Instant

data class Group(
    val id: GroupId,
    val name: String,
    val isArchived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState,
)

data class GroupMembership(
    val groupId: GroupId,
    val personId: PersonId,
    val joinedAt: Instant,
)

data class GroupSummary(
    val group: Group,
    val memberCount: Int,
    val gameCount: Int,
)
