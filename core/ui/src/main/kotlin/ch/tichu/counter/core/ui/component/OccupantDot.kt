package ch.tichu.counter.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.tichu.counter.core.ui.theme.TichuThemeDefaults

@Composable
fun OccupantDot(
    color: Color?,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
) {
    val guestColor = TichuThemeDefaults.colors.guest
    if (color == null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(width = 1.5.dp, color = guestColor, shape = CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}
