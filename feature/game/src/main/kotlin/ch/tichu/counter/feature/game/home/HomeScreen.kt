package ch.tichu.counter.feature.game.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.component.GroupSelectorBar
import ch.tichu.counter.core.ui.theme.TichuThemeDefaults
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.core.ui.util.relativeDateText
import ch.tichu.counter.feature.game.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSetup: (abandonCurrent: Boolean) -> Unit,
    onNavigateToScoring: (GameId) -> Unit,
    onOpenGroupPicker: () -> Unit,
    onNavigateToGroupCreate: () -> Unit,
    onOpenSettings: () -> Unit,
    onBugReport: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is HomeUiEffect.NavigateToSetup -> onNavigateToSetup(effect.abandonCurrent)
            is HomeUiEffect.NavigateToScoring -> onNavigateToScoring(effect.gameId)
            HomeUiEffect.OpenGroupPicker -> onOpenGroupPicker()
            HomeUiEffect.NavigateToGroupCreate -> onNavigateToGroupCreate()
            HomeUiEffect.OpenSettings -> onOpenSettings()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_game_play_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(HomeUiEvent.SettingsClicked) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.feature_game_settings))
                    }
                    BugReportActionButton(onBugReport)
                },
            )
        },
    ) { padding ->
        HomeContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) return
    Column(
        modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GroupGameCard(
            state = state,
            groupGame = state.groupGame,
            onEvent = onEvent,
            modifier = if (state.groupGame == null) Modifier.weight(1f) else Modifier,
        )
        QuickPlayCard(
            quickPlayGame = state.quickPlayGame,
            onEvent = onEvent,
            modifier = if (state.quickPlayGame == null) Modifier.weight(1f) else Modifier,
        )
    }

    if (state.showAbandonConfirmation) {
        AlertDialog(
            onDismissRequest = { onEvent(HomeUiEvent.AbandonDismissed) },
            title = { Text(stringResource(R.string.feature_game_abandon_title)) },
            confirmButton = {
                TextButton(onClick = { onEvent(HomeUiEvent.AbandonAndStartConfirmed) }) {
                    Text(stringResource(R.string.feature_game_abandon_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(HomeUiEvent.AbandonDismissed) }) {
                    Text(stringResource(R.string.feature_game_cancel))
                }
            },
        )
    }
}

@Composable
private fun GroupGameCard(
    state: HomeUiState,
    groupGame: CurrentGameUi?,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showGroupSheet by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (groupGame == null) Modifier.fillMaxHeight() else Modifier)
                .padding(16.dp),
        ) {
            Text(
                stringResource(R.string.feature_game_group_game).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            GroupSelectorBar(
                groupName = state.groupName,
                subtitle = if (state.groupName != null) stringResource(R.string.feature_game_group_selector_hint) else null,
                onClick = { showGroupSheet = true },
            )

            if (groupGame == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.groupName != null) {
                            Text(
                                stringResource(R.string.feature_game_no_game),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        if (state.groupName == null) {
                            Button(
                                onClick = { onEvent(HomeUiEvent.CreateGroupClicked) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.feature_game_create_group).uppercase(), style = MaterialTheme.typography.titleMedium)
                            }
                        } else {
                            Button(
                                onClick = { onEvent(HomeUiEvent.NewGameClicked) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.feature_game_start_new_game).uppercase(), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(16.dp))
                CurrentGameCard(groupGame, onClick = { onEvent(HomeUiEvent.ResumeGame) })
            }
        }
    }

    if (showGroupSheet) {
        GroupSwitcherSheet(
            state = state,
            onDismiss = { showGroupSheet = false },
            onGroupSelected = { groupId ->
                showGroupSheet = false
                onEvent(HomeUiEvent.GroupSelected(groupId))
            },
            onCreateGroup = {
                showGroupSheet = false
                onEvent(HomeUiEvent.CreateGroupClicked)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupSwitcherSheet(
    state: HomeUiState,
    onDismiss: () -> Unit,
    onGroupSelected: (GroupId) -> Unit,
    onCreateGroup: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.feature_game_group_selector_hint),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Column(Modifier.fillMaxWidth()) {
            state.groups.forEach { group ->
                ListItem(
                    headlineContent = { Text(group.name) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = if (group.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = if (group.isActive) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                    colors = if (group.isActive) {
                        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    } else {
                        ListItemDefaults.colors(containerColor = Color.Transparent)
                    },
                    modifier = Modifier.clickable { onGroupSelected(group.id) },
                )
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.feature_game_new_group)) },
            leadingContent = {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            modifier = Modifier.clickable { onCreateGroup() },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QuickPlayCard(
    quickPlayGame: CurrentGameUi?,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (quickPlayGame == null) Modifier.fillMaxHeight() else Modifier)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.feature_game_quick_play).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (quickPlayGame == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.feature_game_quick_play_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onEvent(HomeUiEvent.QuickPlayClicked) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.feature_game_quick_play).uppercase(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(16.dp))
                CurrentGameCard(quickPlayGame, onClick = { onEvent(HomeUiEvent.ResumeQuickPlay) })
            }
        }
    }
}

@Composable
private fun CurrentGameCard(game: CurrentGameUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TichuThemeDefaults.colors
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.feature_game_team_a).uppercase(), color = colors.teamA, style = MaterialTheme.typography.labelLarge)
                    Text(game.scoreA.toString(), color = colors.teamA, style = MaterialTheme.typography.displayMedium)
                    Text(game.teamANames, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
                Text(":", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.feature_game_team_b).uppercase(), color = colors.teamB, style = MaterialTheme.typography.labelLarge)
                    Text(game.scoreB.toString(), color = colors.teamB, style = MaterialTheme.typography.displayMedium)
                    Text(game.teamBNames, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.feature_game_round_started, game.roundNumber, game.startedAt.relativeDateText()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (game.isOtherGroup && game.groupName != null) {
                Text(
                    stringResource(R.string.feature_game_game_in_group, game.groupName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = onClick) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.feature_game_continue).uppercase())
                }
            }
        }
    }
}
