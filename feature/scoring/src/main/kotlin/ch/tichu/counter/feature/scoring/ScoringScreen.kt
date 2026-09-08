package ch.tichu.counter.feature.scoring

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalDensity
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
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.component.Keypad
import ch.tichu.counter.core.ui.component.KeypadKey
import ch.tichu.counter.core.ui.component.RoundColumnWidth
import ch.tichu.counter.core.ui.component.RoundHistoryTable
import ch.tichu.counter.core.ui.component.TeamColumnDivider
import ch.tichu.counter.core.ui.component.TeamHeader
import ch.tichu.counter.core.ui.theme.TichuThemeDefaults
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
    onBugReport: () -> Unit,
    viewModel: ScoringViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val invalidMessage = stringResource(R.string.feature_scoring_error)
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
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
        topBar = { ScoringTopBar(state, viewModel::onEvent, onNavigateBack, onBugReport) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        ScoringContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoringTopBar(
    state: ScoringUiState,
    onEvent: (ScoringUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onBugReport: () -> Unit,
) {
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
            BugReportActionButton(onBugReport)
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
    if (state.roundOptionsExpanded) {
        ExpandedRoundOptions(state, onEvent)
    } else {
        CompactPlayerSummary(state)
    }
    RoundOptionsButton(state, onEvent)
}

@Composable
private fun CompactPlayerSummary(state: ScoringUiState) {
    if (!hasSummaryContent(Team.A, state) && !hasSummaryContent(Team.B, state)) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerSummaryNames(Team.A, state, Modifier.weight(1f))
        TeamColumnDivider()
        Box(Modifier.width(RoundColumnWidth))
        TeamColumnDivider()
        PlayerSummaryNames(Team.B, state, Modifier.weight(1f))
    }
}

private fun hasSummaryContent(team: Team, state: ScoringUiState): Boolean = Seat.forTeam(team).any { seat -> state.seat(seat)?.tichu != null }

@Composable
private fun RoundOptionsButton(state: ScoringUiState, onEvent: (ScoringUiEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DoubleWinButton(
            team = Team.A,
            selected = state.doubleWin == Team.A,
            onClick = { onEvent(ScoringUiEvent.DoubleWin(Team.A)) },
            modifier = Modifier.weight(1.2f),
        )
        TextButton(
            onClick = { onEvent(ScoringUiEvent.ToggleRoundOptions) },
            modifier = Modifier.weight(0.6f),
        ) {
            Text(
                stringResource(R.string.feature_scoring_tichu) +
                    if (state.roundOptionsExpanded) " ▴" else " ▾",
            )
        }
        DoubleWinButton(
            team = Team.B,
            selected = state.doubleWin == Team.B,
            onClick = { onEvent(ScoringUiEvent.DoubleWin(Team.B)) },
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun DoubleWinButton(
    team: Team,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = TichuThemeDefaults.teamColor(team)),
        ) {
            Text(
                stringResource(if (team == Team.A) R.string.feature_scoring_double_win_a else R.string.feature_scoring_double_win_b),
                maxLines = 1,
            )
        }
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Text(
                stringResource(if (team == Team.A) R.string.feature_scoring_double_win_a else R.string.feature_scoring_double_win_b),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlayerSummaryNames(team: Team, state: ScoringUiState, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val holders = Seat.forTeam(team).mapNotNull { seat ->
            val player = state.seat(seat) ?: return@mapNotNull null
            player.tichu?.let { it to player }
        }
        holders.forEach { (tichu, player) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(player.name, fontStyle = if (player.isGuest) FontStyle.Italic else FontStyle.Normal)
                Text(
                    " ${tichu.shortLabel()}",
                    color = if (tichu.success) TichuThemeDefaults.colors.success else TichuThemeDefaults.colors.failure,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun DraftTichu.shortLabel(): String = (if (type == TichuType.SMALL) "T" else "GT") + if (success) "✓" else "✗"

@Composable
private fun ExpandedRoundOptions(state: ScoringUiState, onEvent: (ScoringUiEvent) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        TichuTableHeader()
        (Seat.forTeam(Team.A) + Seat.forTeam(Team.B)).forEach { seat ->
            val player = state.seat(seat) ?: return@forEach
            TichuTableRow(seat, player, onEvent)
        }
    }
}

@Composable
private fun TichuTableHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.feature_scoring_player),
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.feature_scoring_small_tichu),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.feature_scoring_grand_tichu),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TichuTableRow(seat: Seat, player: SeatUi, onEvent: (ScoringUiEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { onEvent(ScoringUiEvent.SwapPlayerRequested(seat)) },
            )
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            player.name,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodyMedium,
            color = TichuThemeDefaults.teamColor(seat.team),
            fontStyle = if (player.isGuest) FontStyle.Italic else FontStyle.Normal,
            maxLines = 1,
        )
        TichuCell(player.tichu?.takeIf { it.type == TichuType.SMALL }, Modifier.weight(1f)) {
            onEvent(ScoringUiEvent.TichuToggled(seat, TichuType.SMALL))
        }
        TichuCell(player.tichu?.takeIf { it.type == TichuType.GRAND }, Modifier.weight(1f)) {
            onEvent(ScoringUiEvent.TichuToggled(seat, TichuType.GRAND))
        }
    }
}

@Composable
private fun TichuCell(state: DraftTichu?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val (symbol, color) = when (state?.success) {
        true -> "✓" to TichuThemeDefaults.colors.success
        false -> "✗" to TichuThemeDefaults.colors.failure
        null -> "·" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .height(TichuCellHeight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val TichuCellHeight = 32.dp

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
            keypadEnabled = state.isKeypadEnabled,
            color = colors.teamA,
            modifier = Modifier.weight(1f),
            onClick = { onEvent(ScoringUiEvent.SwitchTeam(Team.A)) },
        )
        Text(":", style = MaterialTheme.typography.titleLarge)
        ScoreInputCell(
            value = state.enteredB,
            bonus = state.bonusB,
            active = state.activeTeam == Team.B,
            keypadEnabled = state.isKeypadEnabled,
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
    keypadEnabled: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val showCursor = active && keypadEnabled
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    val cursorHeight = with(LocalDensity.current) { MaterialTheme.typography.titleLarge.lineHeight.toDp() }
    Row(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = {}),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Card(
            border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, color),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!showCursor) {
                    Text(
                        value.ifBlank { "—" },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    if (value.isNotBlank()) {
                        Text(value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Spacer(Modifier.width(2.dp))
                    }
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(cursorHeight)
                            .background(color.copy(alpha = cursorAlpha)),
                    )
                }
            }
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
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { onEvent(ScoringUiEvent.NewGameClicked) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.feature_scoring_new_game))
                }
                TextButton(
                    onClick = { onEvent(ScoringUiEvent.HomeClicked) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.feature_scoring_home)) }
                TextButton(
                    onClick = { onEvent(ScoringUiEvent.FinishedDialogDismissed) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.feature_scoring_close))
                }
            }
        },
    )
}
