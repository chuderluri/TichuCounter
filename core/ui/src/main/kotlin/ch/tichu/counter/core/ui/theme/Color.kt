package ch.tichu.counter.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ch.tichu.counter.core.model.AvatarColor

@Immutable
data class TichuColors(
    val teamA: Color,
    val onTeamA: Color,
    val teamAContainer: Color,
    val teamB: Color,
    val onTeamB: Color,
    val teamBContainer: Color,
    val success: Color,
    val failure: Color,
    val guest: Color,
)

val LightTichuColors = TichuColors(
    teamA = Color(0xFF1565C0),
    onTeamA = Color.White,
    teamAContainer = Color(0xFFD6E4FF),
    teamB = Color(0xFFC62828),
    onTeamB = Color.White,
    teamBContainer = Color(0xFFFFDAD6),
    success = Color(0xFF2E7D32),
    failure = Color(0xFFC62828),
    guest = Color(0xFF757575),
)

val DarkTichuColors = TichuColors(
    teamA = Color(0xFF8AB4F8),
    onTeamA = Color(0xFF002F65),
    teamAContainer = Color(0xFF0D3C7A),
    teamB = Color(0xFFFF8A80),
    onTeamB = Color(0xFF5A0000),
    teamBContainer = Color(0xFF7A1F1F),
    success = Color(0xFF81C784),
    failure = Color(0xFFFF8A80),
    guest = Color(0xFFBDBDBD),
)

val LocalTichuColors = staticCompositionLocalOf { LightTichuColors }

fun AvatarColor.toColor(): Color = when (this) {
    AvatarColor.RED -> Color(0xFFE53935)
    AvatarColor.ORANGE -> Color(0xFFFB8C00)
    AvatarColor.AMBER -> Color(0xFFFFB300)
    AvatarColor.GREEN -> Color(0xFF43A047)
    AvatarColor.TEAL -> Color(0xFF00897B)
    AvatarColor.BLUE -> Color(0xFF1E88E5)
    AvatarColor.INDIGO -> Color(0xFF3949AB)
    AvatarColor.PURPLE -> Color(0xFF8E24AA)
    AvatarColor.PINK -> Color(0xFFD81B60)
    AvatarColor.BROWN -> Color(0xFF6D4C41)
}
