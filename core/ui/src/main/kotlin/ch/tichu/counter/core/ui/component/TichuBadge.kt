package ch.tichu.counter.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.tichu.counter.core.model.TichuType
import ch.tichu.counter.core.ui.theme.TichuThemeDefaults

@Composable
fun TichuBadge(
    type: TichuType,
    success: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = TichuThemeDefaults.colors
    val color = if (success) colors.success else colors.failure
    val label = (if (type == TichuType.SMALL) "T" else "GT") + if (success) "\u2713" else "\u2717"
    Text(
        text = label,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun DoubleWinBadge(modifier: Modifier = Modifier) {
    Text(
        text = "DW",
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}
