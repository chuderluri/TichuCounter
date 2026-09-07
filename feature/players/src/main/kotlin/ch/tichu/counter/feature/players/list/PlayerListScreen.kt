package ch.tichu.counter.feature.players.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.component.EmptyState
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.core.ui.util.relativeDateText
import ch.tichu.counter.feature.players.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerListScreen(
    onNavigateToEdit: (PersonId?) -> Unit,
    onOpenGroupPicker: () -> Unit,
    onBugReport: () -> Unit,
    viewModel: PlayerListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is PlayerListUiEffect.NavigateToEdit -> onNavigateToEdit(effect.personId)
            PlayerListUiEffect.OpenGroupPicker -> onOpenGroupPicker()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = { viewModel.onEvent(PlayerListUiEvent.SwitchGroupClicked) }) {
                        Text(
                            stringResource(R.string.feature_players_title) + " · " +
                                (state.groupName ?: stringResource(R.string.feature_players_quick_play)),
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
        floatingActionButton = {
            if (!state.isQuickPlay && !state.isLoading) {
                FloatingActionButton(onClick = { viewModel.onEvent(PlayerListUiEvent.AddClicked) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.feature_players_add))
                }
            }
        },
    ) { padding ->
        PlayerListContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun PlayerListContent(
    state: PlayerListUiState,
    onEvent: (PlayerListUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) return
    if (state.isQuickPlay) {
        EmptyState(
            icon = Icons.Default.Group,
            title = stringResource(R.string.feature_players_quick_play_title),
            description = stringResource(R.string.feature_players_quick_play_description),
            modifier = modifier,
            action = {
                Button(onClick = { onEvent(PlayerListUiEvent.SwitchGroupClicked) }) {
                    Text(stringResource(R.string.feature_players_create_group))
                }
            },
        )
        return
    }
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { onEvent(PlayerListUiEvent.QueryChanged(it)) },
            placeholder = { Text(stringResource(R.string.feature_players_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (state.players.isEmpty() && state.query.isBlank()) {
            EmptyState(
                icon = Icons.Default.Group,
                title = stringResource(R.string.feature_players_empty_title),
                description = stringResource(R.string.feature_players_empty_description),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(state.players, key = { it.id.value }) { player ->
                    PlayerRow(player, onClick = { onEvent(PlayerListUiEvent.PlayerClicked(player.id)) })
                }
            }
        }
        if (state.archivedCount > 0) {
            TextButton(
                onClick = { onEvent(PlayerListUiEvent.ToggleArchived) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Text(
                    if (state.showArchived) {
                        stringResource(R.string.feature_players_hide_archived)
                    } else {
                        stringResource(R.string.feature_players_show_archived, state.archivedCount)
                    },
                )
            }
        }
    }
}

@Composable
private fun PlayerRow(player: PlayerRowUi, onClick: () -> Unit) {
    val supporting = buildString {
        append(stringResource(R.string.feature_players_games_played, player.gamesPlayed))
        append(" · ")
        val last = player.lastPlayedAt
        append(
            if (last == null) {
                stringResource(R.string.feature_players_never_played)
            } else {
                stringResource(R.string.feature_players_last_played, last.relativeDateText())
            },
        )
        if (player.isArchived) append(" · ").append(stringResource(R.string.feature_players_archived_label))
    }
    ListItem(
        headlineContent = { Text(player.name) },
        supportingContent = { Text(supporting) },
        trailingContent = if (player.isInCurrentGame) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SportsEsports,
                        contentDescription = stringResource(R.string.feature_players_in_game),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else {
            null
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .alpha(if (player.isArchived) 0.5f else 1f),
    )
}
