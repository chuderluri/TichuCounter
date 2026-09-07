package ch.tichu.counter.feature.scoring

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.model.TichuType
import ch.tichu.counter.core.ui.component.Keypad
import ch.tichu.counter.core.ui.component.KeypadKey
import ch.tichu.counter.core.ui.component.OccupantDot
import ch.tichu.counter.core.ui.component.RoundHistoryTable
import ch.tichu.counter.core.ui.component.TeamHeader
import ch.tichu.counter.core.ui.theme.TichuThemeDefaults
import ch.tichu.counter.core.ui.theme.toColor
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.core.ui.util.signed
import ch.tichu.counter.feature.scoring.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoringScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSwap: (GameId, Seat) -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToScoring: (GameId) -> Unit,
    viewModel: ScoringViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val invalidMessage = stringResource(R.string.feature_scoring_error)
    val roundSavedTemplate = stringResource(R.string.feature_scoring_round_saved, 0)
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ScoringUiEffect.ShowRoundSavedSnackbar -> snackbar.showSnackbar(
                roundSavedTemplate.replace("0", effect.roundNumber.toString()),
            )
            ScoringUiEffect.ShowInvalidRound -> snackbar.showSnackbar(invalidMessage)
            is ScoringUiEffect.NavigateToSwapDialog -> onNavigateToSwap(effect.gameId, effect.seat)
            is ScoringUiEffect.NavigateToSetup -> onNavigateToSetup()
            is ScoringUiEffect.NavigateToScoring -> onNavigateToScoring(effect.gameId)
            ScoringUiEffect.NavigateHome -> onNavigateHome()
            ScoringUiEffect.NavigateBack -> onNavigateBack()
        }
    }
    BackHandler(enabled = state.status == ScoringStatus.IN_PROGRESS) { onNavigateBack() }
    Scaffold(
        topBar = { ScoringTopBar(state, viewModel::onEvent, onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ScoringContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoringTopBar(state: ScoringUiState, onEvent: (ScoringUiEvent) -> Unit, onNavigateBack: () -> Unit) {
    var moreExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.feature_scoring_round, state.roundNumber)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = { onEvent(ScoringUiEvent.Undo) }, enabled = state.canUndo) {
                Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.feature_scoring_undo))
            }
            IconButton(onClick = { onEvent(ScoringUiEvent.Redo) }, enabled = state.canRedo) {
                Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.feature_scoring_redo))
            }
            Box {
                IconButton(onClick = { moreExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.feature_scoring_more))
                }
                DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.feature_scoring_abandon)) },
                        onClick = {
                            moreExpanded = false
                            onEvent(ScoringUiEvent.AbandonGameRequested)
                        },
                    )
                }
            }
        },
    )
}

@Composable
fun ScoringContent(state: ScoringUiState, onEvent: (ScoringUiEvent) -> Unit, modifier: Modifier = Modifier) {
    if (state.status == ScoringStatus.LOADING) return
    Column(modifier.fillMaxSize()) {
        TeamHeader(scoreA = state.scoreA, scoreB = state.scoreB, modifier = Modifier.padding(horizontal = 8.dp), winner = state.winner)
        HorizontalDivider()
        RoundHistoryTable(
            rows = state.history,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onRoundClick = { onEvent(ScoringUiEvent.RoundRowClicked(it)) },
        )
        HorizontalDivider()
        PlayerSection(state, onEvent)
        HorizontalDivider()
        RoundInput(state, onEvent)
        Keypad(
            onKey = { key ->
                when (key) {
                    is KeypadKey.Digit -> onEvent(ScoringUiEvent.Digit(key.value))
                    KeypadKey.Backspace -> onEvent(ScoringUiEvent.Backspace)
                    KeypadKey.ToggleSign -> onEvent(ScoringUiEvent.ToggleSign)
                    KeypadKey.Confirm -> onEvent(ScoringUiEvent.ConfirmRound)
                }
            },
            enabled = state.isKeypadEnabled,
            confirmEnabled = state.isConfirmEnabled,
            confirmLabel = stringResource(R.string.feature_scoring_confirm),
        )
    }
    if (state.showAbandonConfirmation) AbandonDialog(onEvent)
    if (state.showFinishedDialog) FinishedDialog(state, onEvent)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlayerSection(state: ScoringUiState, onEvent: (ScoringUiEvent) -> Unit) {
    val colors = TichuThemeDefaults.colors
    Row(Modifier.fillMaxWidth()) {
        PlayerColumn(Team.A, colors.teamA, state, onEvent, Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        PlayerColumn(Team.B, colors.teamB, state, onEvent, Modifier.weight(1f))
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlayerColumn(
    team: Team,
    teamColor: Color,
    state: ScoringUiState,
    onEvent: (ScoringUiEvent) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
        Seat.forTeam(team).forEach { seat ->
            val player = state.seat(seat) ?: return@forEach
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onEvent(ScoringUiEvent.SwapPlayerRequested(seat)) },
                    )
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OccupantDot(player.color?.toColor(), size = 10.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    player.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = if (player.isGuest) FontStyle.Italic else FontStyle.Normal,
                    maxLines = 1,
                )
                TichuToggle("T", player.tichu?.takeIf { it.type == TichuType.SMALL }, teamColor) {
                    onEvent(ScoringUiEvent.TichuToggled(seat, TichuType.SMALL))
                }
                Spacer(Modifier.width(2.dp))
                TichuToggle("GT", player.tichu?.takeIf { it.type == TichuType.GRAND }, teamColor) {
                    onEvent(ScoringUiEvent.TichuToggled(seat, TichuType.GRAND))
                }
            }
        }
        FilledTonalButton(
            onClick = { onEvent(ScoringUiEvent.DoubleWin(team)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Text(stringResource(if (team == Team.A) R.string.feature_scoring_double_win_a else R.string.feature_scoring_double_win_b))
        }
    }
}

@Composable
private fun TichuToggle(label: String, state: DraftTichu?, teamColor: Color, onClick: () -> Unit) {
    val color = when (state?.success) {
        true -> TichuThemeDefaults.colors.success
        false -> TichuThemeDefaults.colors.failure
        null -> teamColor
    }
    TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)) {
        Text(
            text = label + when (state?.success) {
                true -> "✓"
                false -> "✗"
                null -> ""
            },
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RoundInput(state: ScoringUiState, onEvent: (ScoringUiEvent) -> Unit) {
    val colors = TichuThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScoreInputCell(
            value = state.enteredA,
            bonus = state.bonusA,
            active = state.activeTeam == Team.A,
            color = colors.teamA,
            modifier = Modifier.weight(1f),
            onClick = { onEvent(ScoringUiEvent.SwitchTeam(Team.A)) },
        )
        Text(":", style = MaterialTheme.typography.titleLarge)
        ScoreInputCell(
            value = state.enteredB,
            bonus = state.bonusB,
            active = state.activeTeam == Team.B,
            color = colors.teamB,
            modifier = Modifier.weight(1f),
            onClick = { onEvent(ScoringUiEvent.SwitchTeam(Team.B)) },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ScoreInputCell(
    value: String,
    bonus: Int,
    active: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = {}),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Card(
            border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, color),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Text(
                value.ifBlank { "—" },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
        }
        if (bonus != 0) {
            Spacer(Modifier.width(4.dp))
            Text(
                bonus.signed(),
                color = if (bonus > 0) TichuThemeDefaults.colors.success else TichuThemeDefaults.colors.failure,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun AbandonDialog(onEvent: (ScoringUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(ScoringUiEvent.AbandonDismissed) },
        title = { Text(stringResource(R.string.feature_scoring_abandon_title)) },
        confirmButton = {
            TextButton(onClick = { onEvent(ScoringUiEvent.AbandonGameConfirmed) }) {
                Text(stringResource(R.string.feature_scoring_abandon_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ScoringUiEvent.AbandonDismissed) }) {
                Text(stringResource(R.string.feature_scoring_cancel))
            }
        },
    )
}

@Composable
private fun FinishedDialog(state: ScoringUiState, onEvent: (ScoringUiEvent) -> Unit) {
    val title = when (state.winner) {
        Team.A -> stringResource(R.string.feature_scoring_finished_title_a)
        Team.B -> stringResource(R.string.feature_scoring_finished_title_b)
        null -> stringResource(R.string.feature_scoring_finished_draw)
    }
    val winnerSeats = state.winner?.let { Seat.forTeam(it) }.orEmpty()
    val winners = winnerSeats.mapNotNull { state.seat(it)?.name }.joinToString(" · ")
    AlertDialog(
        onDismissRequest = { onEvent(ScoringUiEvent.FinishedDialogDismissed) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                if (winners.isNotEmpty()) Text(winners, style = MaterialTheme.typography.bodyLarge)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (state.winner == Team.A) Text("🏆", style = MaterialTheme.typography.headlineMedium)
                    Text(state.scoreA.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = if (state.winner == Team.A) FontWeight.Bold else FontWeight.Normal)
                    Text(" : ", style = MaterialTheme.typography.headlineMedium)
                    Text(state.scoreB.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = if (state.winner == Team.B) FontWeight.Bold else FontWeight.Normal)
                    if (state.winner == Team.B) Text("🏆", style = MaterialTheme.typography.headlineMedium)
                }
                Text(stringResource(R.string.feature_scoring_rounds_duration, state.roundNumber - 1, state.durationMinutes))
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { onEvent(ScoringUiEvent.ToggleFinishedRoundList) }) {
                    Text(stringResource(if (state.finishedRoundListExpanded) R.string.feature_scoring_hide_rounds else R.string.feature_scoring_show_rounds))
                }
                if (state.finishedRoundListExpanded) {
                    RoundHistoryTable(state.history, modifier = Modifier.height(180.dp), autoScrollToEnd = false)
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { onEvent(ScoringUiEvent.RematchClicked) }) { Text(stringResource(R.string.feature_scoring_rematch)) }
                TextButton(onClick = { onEvent(ScoringUiEvent.NewGameClicked) }) { Text(stringResource(R.string.feature_scoring_new_game)) }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onEvent(ScoringUiEvent.Undo) }) { Text(stringResource(R.string.feature_scoring_undo_last_round)) }
                TextButton(onClick = { onEvent(ScoringUiEvent.HomeClicked) }) { Text(stringResource(R.string.feature_scoring_home)) }
            }
        },
    )
}
