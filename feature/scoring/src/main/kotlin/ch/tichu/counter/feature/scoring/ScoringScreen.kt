package ch.tichu.counter.feature.scoring

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import ch.tichu.counter.core.ui.component.RoundHistoryTable
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
            backspaceEnabled = state.status == ScoringStatus.IN_PROGRESS,
            confirmEnabled = state.isConfirmEnabled,
            confirmLabel = stringResource(R.string.feature_scoring_confirm),
        )
    }
    if (state.showAbandonConfirmation) AbandonDialog(onEvent)
    if (state.showFinishedDialog) FinishedDialog(state, onEvent)
    state.tichuEditor?.let { TichuSheet(it, onEvent) }
}

@Composable
private fun PlayerSection(state: ScoringUiState, onEvent: (ScoringUiEvent) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerRowHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TeamPlayerCells(Team.A, state, onEvent, Modifier.weight(1f))
            TeamPlayerCells(Team.B, state, onEvent, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DoubleWinButton(
                team = Team.A,
                selected = state.doubleWin == Team.A,
                enabled = state.doubleWin == null || state.doubleWin == Team.A,
                onClick = { onEvent(ScoringUiEvent.DoubleWin(Team.A)) },
                modifier = Modifier.weight(1f),
            )
            DoubleWinButton(
                team = Team.B,
                selected = state.doubleWin == Team.B,
                enabled = state.doubleWin == null || state.doubleWin == Team.B,
                onClick = { onEvent(ScoringUiEvent.DoubleWin(Team.B)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TeamPlayerCells(
    team: Team,
    state: ScoringUiState,
    onEvent: (ScoringUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Seat.forTeam(team).forEach { seat ->
            val player = state.seat(seat) ?: return@forEach
            PlayerCell(player = player, onEvent = onEvent, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlayerCell(player: SeatUi, onEvent: (ScoringUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(12.dp)
    val outline = TichuThemeDefaults.teamOutline(player.seat.team)
    val tichu = player.tichu
    val fill = when (tichu?.success) {
        true -> TichuThemeDefaults.colors.success.copy(alpha = 0.18f)
        false -> TichuThemeDefaults.colors.failure.copy(alpha = 0.18f)
        null -> Color.Transparent
    }
    val labelColor = when (tichu?.success) {
        true -> TichuThemeDefaults.colors.success
        false -> TichuThemeDefaults.colors.failure
        null -> Color.Unspecified
    }
    val label = tichu?.let { call ->
        val type = stringResource(if (call.type == TichuType.SMALL) R.string.feature_scoring_tichu_short else R.string.feature_scoring_grand_short)
        "$type ${if (call.success) "✓" else "✗"}"
    }
    val tichuStateDescription = when {
        tichu == null -> stringResource(R.string.feature_scoring_a11y_no_call)
        tichu.success -> stringResource(
            if (tichu.type == TichuType.SMALL) R.string.feature_scoring_a11y_small_made else R.string.feature_scoring_a11y_grand_made,
        )
        else -> stringResource(
            if (tichu.type == TichuType.SMALL) R.string.feature_scoring_a11y_small_lost else R.string.feature_scoring_a11y_grand_lost,
        )
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .border(1.dp, outline, shape)
            .background(fill)
            .combinedClickable(
                onClick = { onEvent(ScoringUiEvent.PlayerClicked(player.seat)) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(ScoringUiEvent.TichuQuickSetRequested(player.seat))
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { this.stateDescription = tichuStateDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                player.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = labelColor,
            )
        }
    }
}

private val PlayerRowHeight = 46.dp

@Composable
private fun DoubleWinButton(
    team: Team,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val teamColor = TichuThemeDefaults.teamColor(team)
    val outline = TichuThemeDefaults.teamOutline(team)
    val label = stringResource(R.string.feature_scoring_double_win)
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(DoubleWinButtonHeight),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) teamColor else outline),
        colors = if (selected) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = teamColor,
                contentColor = TichuThemeDefaults.onTeamColor(team),
            )
        } else {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) {
        Text(if (selected) "✓ $label" else label, maxLines = 1)
    }
}

private val DoubleWinButtonHeight = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TichuSheet(editor: TichuEditorUi, onEvent: (ScoringUiEvent) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onEvent(ScoringUiEvent.TichuEditorDismissed) }) {
        val swapLabel = stringResource(R.string.feature_scoring_swap_player)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(48.dp))
                Text(
                    editor.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = TichuThemeDefaults.teamColor(editor.seat.team),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = { onEvent(ScoringUiEvent.SwapPlayerRequested(editor.seat)) },
                    modifier = Modifier.semantics { this.contentDescription = swapLabel },
                ) {
                    Text("⇄", style = MaterialTheme.typography.titleLarge)
                }
            }
            HorizontalDivider()
            TichuOptionButton(
                label = stringResource(R.string.feature_scoring_no_call),
                selected = editor.current == null,
                enabled = true,
                tone = TichuOptionTone.NEUTRAL,
                onClick = { onEvent(ScoringUiEvent.TichuOptionSelected(editor.seat, null)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TichuOptionButton(
                    label = stringResource(R.string.feature_scoring_small_made),
                    selected = editor.current == DraftTichu(TichuType.SMALL, success = true),
                    enabled = editor.madeAllowed,
                    tone = TichuOptionTone.MADE,
                    onClick = { onEvent(ScoringUiEvent.TichuOptionSelected(editor.seat, DraftTichu(TichuType.SMALL, success = true))) },
                    modifier = Modifier.weight(1f),
                )
                TichuOptionButton(
                    label = stringResource(R.string.feature_scoring_small_lost),
                    selected = editor.current == DraftTichu(TichuType.SMALL, success = false),
                    enabled = true,
                    tone = TichuOptionTone.LOST,
                    onClick = { onEvent(ScoringUiEvent.TichuOptionSelected(editor.seat, DraftTichu(TichuType.SMALL, success = false))) },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TichuOptionButton(
                    label = stringResource(R.string.feature_scoring_grand_made),
                    selected = editor.current == DraftTichu(TichuType.GRAND, success = true),
                    enabled = editor.madeAllowed,
                    tone = TichuOptionTone.MADE,
                    onClick = { onEvent(ScoringUiEvent.TichuOptionSelected(editor.seat, DraftTichu(TichuType.GRAND, success = true))) },
                    modifier = Modifier.weight(1f),
                )
                TichuOptionButton(
                    label = stringResource(R.string.feature_scoring_grand_lost),
                    selected = editor.current == DraftTichu(TichuType.GRAND, success = false),
                    enabled = true,
                    tone = TichuOptionTone.LOST,
                    onClick = { onEvent(ScoringUiEvent.TichuOptionSelected(editor.seat, DraftTichu(TichuType.GRAND, success = false))) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private enum class TichuOptionTone { NEUTRAL, MADE, LOST }

@Composable
private fun TichuOptionButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    tone: TichuOptionTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TichuThemeDefaults.colors
    val container = when (tone) {
        TichuOptionTone.MADE -> colors.success.copy(alpha = 0.18f)
        TichuOptionTone.LOST -> colors.failure.copy(alpha = 0.18f)
        TichuOptionTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (tone) {
        TichuOptionTone.MADE -> colors.success
        TichuOptionTone.LOST -> colors.failure
        TichuOptionTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(TichuOptionButtonHeight)
            .semantics { this.selected = selected },
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = container, contentColor = content),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val TichuOptionButtonHeight = 56.dp

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
    Box(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, color),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Row(
                modifier = Modifier
                    .widthIn(min = ScoreFieldMinWidth)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
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
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(BonusSlotWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (bonus != 0) {
                Text(
                    bonus.signed(),
                    color = if (bonus > 0) TichuThemeDefaults.colors.success else TichuThemeDefaults.colors.failure,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

private val ScoreFieldMinWidth = 64.dp
private val BonusSlotWidth = 40.dp

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
