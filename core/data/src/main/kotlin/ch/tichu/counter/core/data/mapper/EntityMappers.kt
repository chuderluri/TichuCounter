package ch.tichu.counter.core.data.mapper

import ch.tichu.counter.core.data.codec.GameEventPayloadCodec
import ch.tichu.counter.core.database.dao.GroupWithCounts
import ch.tichu.counter.core.database.dao.PersonWithStats
import ch.tichu.counter.core.database.entity.GameEntity
import ch.tichu.counter.core.database.entity.GameEventEntity
import ch.tichu.counter.core.database.entity.GroupEntity
import ch.tichu.counter.core.database.entity.PersonEntity
import ch.tichu.counter.core.model.AvatarColor
import ch.tichu.counter.core.model.EventId
import ch.tichu.counter.core.model.Game
import ch.tichu.counter.core.model.GameEvent
import ch.tichu.counter.core.model.GameEventType
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.Group
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.GroupSummary
import ch.tichu.counter.core.model.Person
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.model.PersonSummary
import ch.tichu.counter.core.model.SyncState
import kotlinx.datetime.Instant

fun String.normalizeName(): String = trim().lowercase()

fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

fun Instant.toEpochMillis(): Long = toEpochMilliseconds()

fun GroupEntity.toDomain(): Group = Group(
    id = GroupId(id),
    name = name,
    isArchived = isArchived,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    syncState = SyncState.valueOf(syncState),
)

fun GroupWithCounts.toDomain(): GroupSummary = GroupSummary(
    group = Group(
        id = GroupId(id),
        name = name,
        isArchived = isArchived,
        createdAt = createdAt.toInstant(),
        updatedAt = updatedAt.toInstant(),
        syncState = SyncState.valueOf(syncState),
    ),
    memberCount = memberCount,
    gameCount = gameCount,
)

fun PersonEntity.toDomain(): Person = Person(
    id = PersonId(id),
    name = name,
    avatarColor = runCatching { AvatarColor.valueOf(avatarColor) }.getOrDefault(AvatarColor.BLUE),
    isArchived = isArchived,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    syncState = SyncState.valueOf(syncState),
)

fun PersonWithStats.toDomain(isInCurrentGame: Boolean): PersonSummary = PersonSummary(
    person = Person(
        id = PersonId(id),
        name = name,
        avatarColor = runCatching { AvatarColor.valueOf(avatarColor) }.getOrDefault(AvatarColor.BLUE),
        isArchived = isArchived,
        createdAt = createdAt.toInstant(),
        updatedAt = updatedAt.toInstant(),
        syncState = SyncState.valueOf(syncState),
    ),
    gamesPlayed = gamesPlayed,
    lastPlayedAt = lastPlayedAt?.toInstant(),
    isInCurrentGame = isInCurrentGame,
)

fun GameEntity.toDomain(): Game = Game(
    id = GameId(id),
    groupId = groupId?.let(::GroupId),
    status = GameStatus.valueOf(status),
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    finishedAt = finishedAt?.toInstant(),
    syncState = SyncState.valueOf(syncState),
)

fun GameEventEntity.toDomain(codec: GameEventPayloadCodec): GameEvent = GameEvent(
    id = EventId(id),
    gameId = GameId(gameId),
    sequence = sequence,
    occurredAt = occurredAt.toInstant(),
    isUndone = isUndone,
    payload = codec.decode(payload),
)

fun GameEvent.toEntity(codec: GameEventPayloadCodec, syncState: SyncState = SyncState.LOCAL_ONLY): GameEventEntity =
    GameEventEntity(
        id = id.value,
        gameId = gameId.value,
        sequence = sequence,
        type = GameEventType.of(payload).name,
        payload = codec.encode(payload),
        occurredAt = occurredAt.toEpochMillis(),
        isUndone = isUndone,
        undoneAt = null,
        syncState = syncState.name,
    )
