package ch.tichu.counter.core.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class GroupId(val value: String)

@JvmInline
@Serializable
value class PersonId(val value: String)

@JvmInline
@Serializable
value class GameId(val value: String)

@JvmInline
@Serializable
value class EventId(val value: String)
