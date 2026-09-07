package ch.tichu.counter.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.component.EmptyState
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.core.ui.util.shortDateText
import ch.tichu.counter.feature.history.R

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
            description = stringResource(R.string.feature_history_empty_description),
            modifier = modifier,
        )
    } else {
        LazyColumn(modifier.fillMaxSize()) {
            GameStatus.entries.forEach { status ->
                val games = state.games.filter { it.status == status }
                if (games.isNotEmpty()) {
                    item(key = "header-$status") {
                        Text(
                            text = stringResource(status.titleRes()).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(games, key = { it.id.value }) { game ->
                        GameListRow(
                            game = game,
                            onClick = { onEvent(GameListUiEvent.GameClicked(game.id)) },
                            onDelete = { onEvent(GameListUiEvent.DeleteRequested(game.id)) },
                        )
                    }
                }
            }
        }
    }
    val pending = state.pendingDelete
    if (pending != null) {
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
private fun GameListRow(game: GameListRowUi, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = {
            Row(Modifier.fillMaxWidth()) {
                Text(game.teamANames, modifier = Modifier.weight(1f), fontStyle = if (game.hasGuests) FontStyle.Italic else FontStyle.Normal)
                Text("${game.scoreA} : ${game.scoreB}", style = MaterialTheme.typography.titleMedium)
                Text(game.teamBNames, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontStyle = if (game.hasGuests) FontStyle.Italic else FontStyle.Normal)
            }
        },
        supportingContent = {
            Text("${game.updatedAt.shortDateText()} · ${stringResource(R.string.feature_history_rounds, game.roundCount)}")
        },
        trailingContent = {
            Row {
                if (game.winner != null) Text("🏆")
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.feature_history_delete))
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}

private fun GameStatus.titleRes(): Int = when (this) {
    GameStatus.IN_PROGRESS -> R.string.feature_history_in_progress
    GameStatus.FINISHED -> R.string.feature_history_finished
    GameStatus.ABANDONED -> R.string.feature_history_abandoned
}
