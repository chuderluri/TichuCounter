package ch.tichu.counter.core.data.codec

import ch.tichu.counter.core.model.GameEventPayload
import ch.tichu.counter.core.model.LineUp
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameEventPayloadCodec @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun encode(payload: GameEventPayload): String = json.encodeToString(GameEventPayload.serializer(), payload)

    fun decode(raw: String): GameEventPayload = json.decodeFromString(GameEventPayload.serializer(), raw)

    fun encodeLineUp(lineUp: LineUp): String = json.encodeToString(LineUp.serializer(), lineUp)

    fun decodeLineUp(raw: String): LineUp = json.decodeFromString(LineUp.serializer(), raw)
}
