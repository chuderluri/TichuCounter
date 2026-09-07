package ch.tichu.counter.feature.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.GameStatus
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.component.RoundHistoryTable
import ch.tichu.counter.core.ui.component.TeamHeader
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.feature.history.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScoring: (GameId) -> Unit,
    onBugReport: () -> Unit,
    viewModel: GameDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is GameDetailUiEffect.NavigateToScoring -> onNavigateToScoring(effect.gameId)
            GameDetailUiEffect.NavigateBack -> onNavigateBack()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_history_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(GameDetailUiEvent.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (state.status == GameStatus.IN_PROGRESS) {
                        IconButton(onClick = { viewModel.onEvent(GameDetailUiEvent.Undo) }, enabled = state.canUndo) {
                            Icon(Icons.Default.Undo, contentDescription = null)
                        }
                        IconButton(onClick = { viewModel.onEvent(GameDetailUiEvent.Redo) }, enabled = state.canRedo) {
                            Icon(Icons.Default.Redo, contentDescription = null)
                        }
                    }
                    BugReportActionButton(onBugReport)
                },
            )
        },
    ) { padding ->
        GameDetailContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@Composable
fun GameDetailContent(state: GameDetailUiState, onEvent: (GameDetailUiEvent) -> Unit, modifier: Modifier = Modifier) {
    if (state.isLoading) return
    Column(modifier.fillMaxSize()) {
        TeamHeader(state.scoreA, state.scoreB, winner = state.winner, modifier = Modifier.padding(horizontal = 8.dp))
        Text(
            state.teamANames + "     " + state.teamBNames,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        RoundHistoryTable(state.history, modifier = Modifier.weight(1f))
        if (state.status == GameStatus.IN_PROGRESS || state.status == GameStatus.ABANDONED) {
            Button(
                onClick = { onEvent(GameDetailUiEvent.Continue) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text(stringResource(R.string.feature_history_continue)) }
        }
    }
}
