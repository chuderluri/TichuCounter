package ch.tichu.counter.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.component.EmptyState
import ch.tichu.counter.core.ui.component.OccupantDot
import ch.tichu.counter.core.ui.theme.TichuThemeDefaults
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.core.ui.util.relativeDateText
import ch.tichu.counter.feature.history.R
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    onNavigateToDetail: (GameId) -> Unit,
    onOpenGroupPicker: () -> Unit,
    onBugReport: () -> Unit,
    viewModel: GameListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is GameListUiEffect.NavigateToDetail -> onNavigateToDetail(effect.gameId)
            GameListUiEffect.OpenGroupPicker -> onOpenGroupPicker()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = { viewModel.onEvent(GameListUiEvent.SwitchGroupClicked) }) {
                        Text(
                            stringResource(R.string.feature_history_title) + " · " +
                                (state.groupName ?: stringResource(R.string.feature_history_quick_play)),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
                actions = {
                    BugReportActionButton(onBugReport)
                },
            )
        },
    ) { padding ->
        GameListContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@Composable
fun GameListContent(state: GameListUiState, onEvent: (GameListUiEvent) -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) return
    if (state.games.isEmpty()) {
        EmptyState(
            icon = Icons.Default.History,
            title = stringResource(R.string.feature_history_empty_title),
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameStatus.entries.forEach { status ->
                val games = state.games.filter { it.status == status }
                if (games.isNotEmpty()) {
                    item(key = "header-$status") {
                        SectionHeader(status, games.size)
                    }
                    items(games, key = { it.id.value }) { game ->
                        SwipeableGameRow(
                            game = game,
                            isDeletePending = state.pendingDelete?.id == game.id,
                            onClick = { onEvent(GameListUiEvent.GameClicked(game.id)) },
                            onDelete = { onEvent(GameListUiEvent.DeleteRequested(game)) },
                        )
                    }
                }
            }
        }
    }
    if (state.pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { onEvent(GameListUiEvent.DeleteDismissed) },
            title = { Text(stringResource(R.string.feature_history_delete_title)) },
            confirmButton = {
                TextButton(onClick = { onEvent(GameListUiEvent.DeleteConfirmed) }) {
                    Text(stringResource(R.string.feature_history_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(GameListUiEvent.DeleteDismissed) }) {
                    Text(stringResource(R.string.feature_history_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(status: GameStatus, count: Int) {
    Text(
        text = stringResource(status.titleRes()).uppercase() + " · " + count,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableGameRow(
    game: GameListRowUi,
    isDeletePending: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val pending by rememberUpdatedState(isDeletePending)
    val requestDelete by rememberUpdatedState(onDelete)
    // Vetoing every value change snaps the row back; only the confirm dialog deletes the game.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && !pending) {
                requestDelete()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = { DeleteSwipeBackground(dismissState.dismissDirection) },
    ) {
        GameRow(game = game, onClick = onClick, onLongClick = onDelete)
    }
}

@Composable
private fun DeleteSwipeBackground(direction: SwipeToDismissBoxValue) {
    val label = stringResource(R.string.feature_history_delete_confirm)
    val contentColor = MaterialTheme.colorScheme.onErrorContainer
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (direction == SwipeToDismissBoxValue.EndToStart) {
            Row(
                modifier = Modifier.padding(end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = contentColor,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameRow(game: GameListRowUi, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val winner = game.winner

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            },
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamColumn(
                    team = Team.A,
                    members = game.teamA,
                    isWinner = winner == Team.A,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${game.scoreA} : ${game.scoreB}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (winner != null) {
                        TichuThemeDefaults.teamColor(winner)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                TeamColumn(
                    team = Team.B,
                    members = game.teamB,
                    isWinner = winner == Team.B,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = game.updatedAt.relativeDateText() +
                        " · " +
                        pluralStringResource(R.plurals.feature_history_rounds, game.roundCount, game.roundCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (game.status == GameStatus.IN_PROGRESS) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = stringResource(R.string.feature_history_continue),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamColumn(
    team: Team,
    members: ImmutableList<GameMemberUi>,
    isWinner: Boolean,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val color = TichuThemeDefaults.teamColor(team)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isWinner && textAlign == TextAlign.Start) {
            WinnerTrophy(team)
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            members.forEach { member ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (member.isGuest && textAlign == TextAlign.Start) {
                        OccupantDot(color = null, size = 10.dp)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = if (member.isGuest) FontStyle.Italic else FontStyle.Normal,
                        color = color,
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (member.isGuest && textAlign == TextAlign.End) {
                        Spacer(Modifier.width(4.dp))
                        OccupantDot(color = null, size = 10.dp)
                    }
                }
            }
        }
        if (isWinner && textAlign == TextAlign.End) {
            Spacer(Modifier.width(4.dp))
            WinnerTrophy(team)
        }
    }
}

@Composable
private fun WinnerTrophy(team: Team) {
    val description = stringResource(
        if (team == Team.A) R.string.feature_history_winner_a else R.string.feature_history_winner_b,
    )
    Text(
        text = "🏆",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { contentDescription = description },
    )
}

private fun GameStatus.titleRes(): Int = when (this) {
    GameStatus.IN_PROGRESS -> R.string.feature_history_in_progress
    GameStatus.FINISHED -> R.string.feature_history_finished
    GameStatus.ABANDONED -> R.string.feature_history_abandoned
}
