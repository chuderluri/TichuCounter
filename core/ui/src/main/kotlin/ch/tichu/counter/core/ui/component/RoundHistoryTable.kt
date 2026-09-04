package ch.tichu.counter.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TichuType
import ch.tichu.counter.core.ui.theme.TichuThemeDefaults
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class TichuBadgeUi(val type: TichuType, val success: Boolean)

@Immutable
sealed interface HistoryRowUi {
    val key: String

    @Immutable
    data class Round(
        val roundNumber: Int,
        val totalA: Int,
        val totalB: Int,
        val badgesA: ImmutableList<TichuBadgeUi>,
        val badgesB: ImmutableList<TichuBadgeUi>,
        val doubleWin: Team?,
    ) : HistoryRowUi {
        override val key: String get() = "round-$roundNumber"
    }

    @Immutable
    data class Swap(
        val index: Int,
        val previousName: String,
        val nextName: String,
        val previousIsGuest: Boolean,
        val nextIsGuest: Boolean,
    ) : HistoryRowUi {
        override val key: String get() = "swap-$index"
    }
}

val RoundColumnWidth = 28.dp

@Composable
fun TeamColumnDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
fun TeamHeader(
    scoreA: Int,
    scoreB: Int,
    modifier: Modifier = Modifier,
    labelA: String = "TEAM A",
    labelB: String = "TEAM B",
    winner: Team? = null,
) {
    val colors = TichuThemeDefaults.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        TeamHeaderCell(labelA, scoreA, colors.teamA, winner == Team.A, Modifier.weight(1f))
        TeamColumnDivider()
        Box(Modifier.width(RoundColumnWidth))
        TeamColumnDivider()
        TeamHeaderCell(labelB, scoreB, colors.teamB, winner == Team.B, Modifier.weight(1f))
    }
}

@Composable
private fun TeamHeaderCell(label: String, score: Int, color: Color, isWinner: Boolean, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isWinner) "\uD83C\uDFC6 $label" else label,
            style = MaterialTheme.typography.titleLarge,
            color = color,
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = color,
            fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Bold,
        )
    }
}

@Composable
fun RoundHistoryTable(
    rows: ImmutableList<HistoryRowUi>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    autoScrollToEnd: Boolean = true,
    onRoundClick: ((Int) -> Unit)? = null,
) {
    LaunchedEffect(rows.size, autoScrollToEnd) {
        if (autoScrollToEnd && rows.isNotEmpty()) listState.animateScrollToItem(rows.lastIndex)
    }
    LazyColumn(modifier = modifier, state = listState) {
        items(rows, key = { it.key }, contentType = { it::class }) { row ->
            when (row) {
                is HistoryRowUi.Round -> RoundRow(row, onRoundClick)
                is HistoryRowUi.Swap -> SwapRow(row)
            }
        }
    }
}

@Composable
private fun RoundRow(row: HistoryRowUi.Round, onClick: ((Int) -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable { onClick(row.roundNumber) } else Modifier),
    ) {
        TeamCell(
            total = row.totalA,
            badges = row.badgesA,
            doubleWin = row.doubleWin == Team.A,
            alignBadgesStart = true,
            modifier = Modifier.weight(1f),
        )
        TeamColumnDivider()
        Box(Modifier.width(RoundColumnWidth), contentAlignment = Alignment.Center) {
            Text(
                row.roundNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TeamColumnDivider()
        TeamCell(
            total = row.totalB,
            badges = row.badgesB,
            doubleWin = row.doubleWin == Team.B,
            alignBadgesStart = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TeamCell(
    total: Int,
    badges: ImmutableList<TichuBadgeUi>,
    doubleWin: Boolean,
    alignBadgesStart: Boolean,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (alignBadgesStart) {
            Badges(badges, doubleWin)
            Text(
                total.toString(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                total.toString(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Badges(badges, doubleWin)
        }
    }
}

@Composable
private fun Badges(badges: ImmutableList<TichuBadgeUi>, doubleWin: Boolean) {
    if (doubleWin) DoubleWinBadge()
    badges.forEach { TichuBadge(it.type, it.success) }
}

@Composable
private fun SwapRow(row: HistoryRowUi.Swap) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text = "  ${row.previousName} \u2192 ${row.nextName}  ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = if (row.previousIsGuest || row.nextIsGuest) FontStyle.Italic else FontStyle.Normal,
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

val EmptyHistory: ImmutableList<HistoryRowUi> = persistentListOf()
