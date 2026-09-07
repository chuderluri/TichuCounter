package ch.tichu.counter.feature.players.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.feature.players.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerEditScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlayerEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val messages = mapOf(
        PlayerEditUiEffect.Message.SAVED to stringResource(R.string.feature_players_saved),
        PlayerEditUiEffect.Message.LOCKED to stringResource(R.string.feature_players_locked_banner),
        PlayerEditUiEffect.Message.LAST_GROUP to stringResource(R.string.feature_players_last_group_error),
        PlayerEditUiEffect.Message.NAME_ERROR to stringResource(R.string.feature_players_name_error),
    )
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PlayerEditUiEffect.NavigateBack -> onNavigateBack()
            is PlayerEditUiEffect.ShowMessage -> messages[effect.message]?.let { snackbar.showSnackbar(it) }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.isNew) R.string.feature_players_new_title else R.string.feature_players_edit_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_players_back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(PlayerEditUiEvent.Save) }, enabled = state.canSave) {
                        Text(stringResource(R.string.feature_players_save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PlayerEditContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerEditContent(
    state: PlayerEditUiState,
    onEvent: (PlayerEditUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) return
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.isLocked) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.feature_players_locked_banner), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(PlayerEditUiEvent.NameChanged(it)) },
            label = { Text(stringResource(R.string.feature_players_name)) },
            isError = state.nameError,
            enabled = !state.isLocked,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.feature_players_name_error)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!state.isNew && state.groups.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.feature_players_groups),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                state.groups.forEach { group ->
                    FilterChip(
                        selected = group.isMember,
                        onClick = { onEvent(PlayerEditUiEvent.GroupToggled(group.groupId)) },
                        label = { Text(group.name) },
                        enabled = !state.isLocked,
                    )
                }
            }
        }
        if (!state.isNew) {
            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            TextButton(
                onClick = { onEvent(PlayerEditUiEvent.ToggleArchive) },
                enabled = !state.isLocked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (state.isArchived) R.string.feature_players_unarchive else R.string.feature_players_archive))
            }
            TextButton(
                onClick = { onEvent(PlayerEditUiEvent.Delete) },
                enabled = state.canDelete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.feature_players_delete), color = MaterialTheme.colorScheme.error)
            }
            if (!state.canDelete) {
                Text(
                    stringResource(R.string.feature_players_delete_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
