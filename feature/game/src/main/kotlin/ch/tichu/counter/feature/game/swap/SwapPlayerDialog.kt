package ch.tichu.counter.feature.game.swap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.Team
import ch.tichu.counter.core.ui.component.OccupantDot
import ch.tichu.counter.core.ui.theme.toColor
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.feature.game.R

@Composable
fun SwapPlayerDialog(
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    viewModel: SwapPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorText = stringResource(R.string.feature_game_swap_error)
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SwapPlayerUiEffect.Dismiss -> onDismiss()
            SwapPlayerUiEffect.ShowError -> onError(errorText)
        }
    }
    SwapPlayerContent(state, viewModel::onEvent)
}

@Composable
fun SwapPlayerContent(state: SwapPlayerUiState, onEvent: (SwapPlayerUiEvent) -> Unit) {
    val teamLabel = stringResource(if (state.seat.team == Team.A) R.string.feature_game_team_a else R.string.feature_game_team_b)
    AlertDialog(
        onDismissRequest = { onEvent(SwapPlayerUiEvent.Dismiss) },
        title = { Text(stringResource(R.string.feature_game_swap_title, state.currentName, teamLabel)) },
        text = {
            if (state.isLoading) return@AlertDialog
            Column(Modifier.fillMaxWidth()) {
                if (state.showNewPerson) {
                    OutlinedTextField(
                        value = state.newPersonName,
                        onValueChange = { onEvent(SwapPlayerUiEvent.NewPersonNameChanged(it)) },
                        label = { Text(stringResource(R.string.feature_game_new_person_name)) },
                        isError = state.newPersonError,
                        supportingText = if (state.newPersonError) {
                            { Text(stringResource(R.string.feature_game_name_error)) }
                        } else {
                            null
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    return@Column
                }
                if (state.showGuestField) {
                    OutlinedTextField(
                        value = state.guestName,
                        onValueChange = { onEvent(SwapPlayerUiEvent.GuestNameChanged(it)) },
                        label = { Text(stringResource(R.string.feature_game_guest_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.isQuickPlay) return@Column
                    Spacer(Modifier.height(8.dp))
                }
                if (!state.isQuickPlay) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { onEvent(SwapPlayerUiEvent.QueryChanged(it)) },
                        placeholder = { Text(stringResource(R.string.feature_game_swap_search)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        if (!state.showGuestField) {
                            item(key = "guest") {
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.feature_game_guest_player), fontStyle = FontStyle.Italic) },
                                    leadingContent = { OccupantDot(null, size = 16.dp) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable { onEvent(SwapPlayerUiEvent.GuestClicked) },
                                )
                            }
                        }
                        items(state.candidates, key = { it.id.value }) { candidate ->
                            ListItem(
                                headlineContent = { Text(candidate.name) },
                                leadingContent = { OccupantDot(candidate.color.toColor(), size = 16.dp) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onEvent(SwapPlayerUiEvent.PersonPicked(candidate.id)) },
                            )
                        }
                        item(key = "new") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onEvent(SwapPlayerUiEvent.NewPersonClicked) }
                                    .height(48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width(16.dp))
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(R.string.feature_game_new_person))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                state.showNewPerson -> TextButton(
                    onClick = { onEvent(SwapPlayerUiEvent.NewPersonConfirmed) },
                    enabled = state.newPersonName.isNotBlank(),
                ) { Text(stringResource(R.string.feature_game_ok)) }
                state.showGuestField -> TextButton(onClick = { onEvent(SwapPlayerUiEvent.GuestConfirmed) }) {
                    Text(stringResource(R.string.feature_game_ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(SwapPlayerUiEvent.Dismiss) }) { Text(stringResource(R.string.feature_game_cancel)) }
        },
    )
}
