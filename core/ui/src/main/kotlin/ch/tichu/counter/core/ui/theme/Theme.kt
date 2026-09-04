package ch.tichu.counter.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00695C),
    secondary = Color(0xFF546E7A),
    tertiary = Color(0xFF8D6E63),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    secondary = Color(0xFF90A4AE),
    tertiary = Color(0xFFBCAAA4),
)

@Composable
fun TichuTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val tichuColors = if (darkTheme) DarkTichuColors else LightTichuColors

    CompositionLocalProvider(LocalTichuColors provides tichuColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TichuTypography,
            content = content,
        )
    }
}

object TichuThemeDefaults {
    val colors: TichuColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTichuColors.current

    @Composable
    @ReadOnlyComposable
    fun teamColor(team: Team): Color = if (team == Team.A) colors.teamA else colors.teamB

    @Composable
    @ReadOnlyComposable
    fun onTeamColor(team: Team): Color = if (team == Team.A) colors.onTeamA else colors.onTeamB

    @Composable
    @ReadOnlyComposable
    fun teamContainer(team: Team): Color = if (team == Team.A) colors.teamAContainer else colors.teamBContainer
}
