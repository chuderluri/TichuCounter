package ch.tichu.counter.feature.game.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.ui.component.OccupantDot
import ch.tichu.counter.core.ui.component.TeamColumnDivider
import ch.tichu.counter.core.ui.theme.TichuThemeDefaults
import ch.tichu.counter.core.ui.theme.toColor
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.core.ui.util.relativeDateText
import ch.tichu.counter.feature.game.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScoring: (GameId) -> Unit,
    onOpenGroupPicker: () -> Unit,
    viewModel: GameSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val startError = stringResource(R.string.feature_game_start_error)
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is GameSetupUiEffect.NavigateToScoring -> onNavigateToScoring(effect.gameId)
            GameSetupUiEffect.ShowStartError -> snackbar.showSnackbar(startError)
            GameSetupUiEffect.OpenGroupPicker -> onOpenGroupPicker()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.feature_game_setup_title) + " · " +
                            (state.groupName ?: stringResource(R.string.feature_game_quick_play)),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_game_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        GameSetupContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@Composable
fun GameSetupContent(
    state: GameSetupUiState,
    onEvent: (GameSetupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) return
    Column(modifier.fillMaxSize()) {
        SlotsSection(state, onEvent)
        HorizontalDivider()
        if (state.isQuickPlay) {
            QuickPlayHint(onEvent, Modifier.weight(1f))
        } else {
            MemberPicker(state, onEvent, Modifier.weight(1f))
        }
        HorizontalDivider()
        BottomBar(state, onEvent)
    }
    if (state.renamingSeat != null) {
        RenameDialog(state, onEvent)
    }
    if (state.showNewPerson) {
        NewPersonDialog(state, onEvent)
    }
}

@Composable
private fun SlotsSection(state: GameSetupUiState, onEvent: (GameSetupUiEvent) -> Unit) {
    val colors = TichuThemeDefaults.colors
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 12.dp),
    ) {
        TeamColumn(Team.A, stringResource(R.string.feature_game_team_a).uppercase(), colors.teamA, state, onEvent, Modifier.weight(1f))
        TeamColumnDivider(Modifier.fillMaxHeight())
        Box(Modifier.width(8.dp))
        TeamColumnDivider(Modifier.fillMaxHeight())
        TeamColumn(Team.B, stringResource(R.string.feature_game_team_b).uppercase(), colors.teamB, state, onEvent, Modifier.weight(1f))
    }
    if (!state.isQuickPlay && state.slots.values.any { it != null }) {
        TextButton(
            onClick = { onEvent(GameSetupUiEvent.ClearAllPlayers) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.feature_game_clear_all_players))
        }
    }
}

@Composable
private fun TeamColumn(
    team: Team,
    label: String,
    color: Color,
    state: GameSetupUiState,
    onEvent: (GameSetupUiEvent) -> Unit,
    modifier: Modifier,
) {
    Column(modifier.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = color)
        Spacer(Modifier.height(8.dp))
        Seat.forTeam(team).forEach { seat ->
            SlotCard(seat, state.slots[seat], state.selectedSeat == seat, color, onEvent)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SlotCard(seat: Seat, slot: SlotUi?, selected: Boolean, teamColor: Color, onEvent: (GameSetupUiEvent) -> Unit) {
    val border = when {
        selected -> BorderStroke(2.dp, teamColor)
        slot == null -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    OutlinedCard(
        onClick = {
            if (slot is SlotUi.Guest) onEvent(GameSetupUiEvent.RenameGuestStarted(seat)) else onEvent(GameSetupUiEvent.SeatSelected(seat))
        },
        border = border,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp)
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (slot) {
                null -> {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.feature_game_add_player),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
                is SlotUi.Person -> {
                    OccupantDot(slot.color.toColor())
                    Spacer(Modifier.width(8.dp))
                    Text(slot.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { onEvent(GameSetupUiEvent.ClearSeat(seat)) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.feature_game_clear_slot))
                    }
                }
                is SlotUi.Guest -> {
                    OccupantDot(null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        slot.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onEvent(GameSetupUiEvent.RenameGuestStarted(seat)) },
                        maxLines = 1,
                    )
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.feature_game_rename_guest))
                }
            }
        }
    }
}

@Composable
private fun MemberPicker(state: GameSetupUiState, onEvent: (GameSetupUiEvent) -> Unit, modifier: Modifier) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onEvent(GameSetupUiEvent.QueryChanged(it)) },
                placeholder = { Text(stringResource(R.string.feature_game_search_member)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { onEvent(GameSetupUiEvent.NewPersonClicked) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.feature_game_new_person))
            }
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            item(key = "guest") {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.feature_game_guest_player), fontStyle = FontStyle.Italic) },
                    supportingContent = { Text(stringResource(R.string.feature_game_guest_hint)) },
                    leadingContent = { OccupantDot(null, size = 16.dp) },
                    modifier = Modifier.clickableIf(state.selectedSeat != null) { onEvent(GameSetupUiEvent.GuestPicked) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            items(state.members, key = { it.id.value }) { member ->
                val last = member.lastPlayedAt
                ListItem(
                    headlineContent = { Text(member.name) },
                    supportingContent = {
                        Text(
                            if (last == null) {
                                stringResource(R.string.feature_game_never_played)
                            } else {
                                stringResource(R.string.feature_game_last_played, last.relativeDateText())
                            },
                        )
                    },
                    leadingContent = { OccupantDot(member.color.toColor(), size = 16.dp) },
                    modifier = Modifier
                        .alpha(if (member.isSeated) 0.4f else 1f)
                        .clickableIf(!member.isSeated && state.selectedSeat != null) {
                            onEvent(GameSetupUiEvent.PersonPicked(member.id))
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun QuickPlayHint(onEvent: (GameSetupUiEvent) -> Unit, modifier: Modifier) {
    Box(modifier.padding(16.dp), contentAlignment = Alignment.TopCenter) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.feature_game_quick_play_hint), style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { onEvent(GameSetupUiEvent.CreateGroupInstead) }) {
                    Text(stringResource(R.string.feature_game_create_group_instead))
                }
            }
        }
    }
}

@Composable
private fun BottomBar(state: GameSetupUiState, onEvent: (GameSetupUiEvent) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.feature_game_target_score), modifier = Modifier.weight(1f))
            TargetScoreDropdown(state, onEvent)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onEvent(GameSetupUiEvent.StartGame) },
            enabled = state.canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.feature_game_start_game).uppercase(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TargetScoreDropdown(state: GameSetupUiState, onEvent: (GameSetupUiEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(state.targetScore.toString())
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.targetScoreOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        expanded = false
                        onEvent(GameSetupUiEvent.TargetScoreChanged(option))
                    },
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(state: GameSetupUiState, onEvent: (GameSetupUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(GameSetupUiEvent.RenameCancelled) },
        title = { Text(stringResource(R.string.feature_game_guest_name)) },
        text = {
            OutlinedTextField(
                value = state.renameDraft,
                onValueChange = { onEvent(GameSetupUiEvent.RenameDraftChanged(it)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onEvent(GameSetupUiEvent.RenameConfirmed) }) { Text(stringResource(R.string.feature_game_ok)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(GameSetupUiEvent.RenameCancelled) }) { Text(stringResource(R.string.feature_game_cancel)) }
        },
    )
}

@Composable
private fun NewPersonDialog(state: GameSetupUiState, onEvent: (GameSetupUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(GameSetupUiEvent.NewPersonDismissed) },
        title = { Text(stringResource(R.string.feature_game_new_person)) },
        text = {
            OutlinedTextField(
                value = state.newPersonName,
                onValueChange = { onEvent(GameSetupUiEvent.NewPersonNameChanged(it)) },
                label = { Text(stringResource(R.string.feature_game_new_person_name)) },
                isError = state.newPersonError,
                supportingText = if (state.newPersonError) {
                    { Text(stringResource(R.string.feature_game_name_error)) }
                } else {
                    null
                },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(GameSetupUiEvent.NewPersonConfirmed) },
                enabled = state.newPersonName.isNotBlank(),
            ) { Text(stringResource(R.string.feature_game_ok)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(GameSetupUiEvent.NewPersonDismissed) }) { Text(stringResource(R.string.feature_game_cancel)) }
        },
    )
}

private fun Modifier.clickableIf(enabled: Boolean, onClick: () -> Unit): Modifier = if (enabled) this.clickable(onClick = onClick) else this
