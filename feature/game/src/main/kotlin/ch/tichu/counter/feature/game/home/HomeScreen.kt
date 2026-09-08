package ch.tichu.counter.feature.game.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.ui.component.BugReportActionButton
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
            HomeUiEffect.OpenSettings -> onOpenSettings()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = { viewModel.onEvent(HomeUiEvent.SwitchGroupClicked) }) {
                        Text(
                            state.groupName ?: stringResource(R.string.feature_game_quick_play),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(HomeUiEvent.SettingsClicked) }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.feature_game_settings))
                    }
                    BugReportActionButton(onBugReport)
                },
            )
        },
        floatingActionButton = {
            if (state.currentGame != null) {
                FloatingActionButton(onClick = { viewModel.onEvent(HomeUiEvent.NewGameClicked) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.feature_game_new_game))
                }
            }
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
    val game = state.currentGame
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (game == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.feature_game_no_game),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { onEvent(HomeUiEvent.NewGameClicked) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.feature_game_start_new_game), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        stringResource(R.string.feature_game_current_game).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    CurrentGameCard(game, onClick = { onEvent(HomeUiEvent.ResumeGame) })
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
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
private fun CurrentGameCard(game: CurrentGameUi, onClick: () -> Unit) {
    val colors = TichuThemeDefaults.colors
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
