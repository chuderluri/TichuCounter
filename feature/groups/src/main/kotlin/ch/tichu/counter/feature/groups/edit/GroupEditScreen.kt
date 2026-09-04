package ch.tichu.counter.feature.groups.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.ui.component.PersonAvatar
import ch.tichu.counter.core.ui.theme.toColor
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.feature.groups.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayerEdit: (PersonId) -> Unit,
    viewModel: GroupEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val inGameMessage = stringResource(R.string.feature_groups_person_in_game_error)
    val savedMessage = stringResource(R.string.feature_groups_saved)
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is GroupEditUiEffect.NavigateToPlayerEdit -> onNavigateToPlayerEdit(effect.personId)
            is GroupEditUiEffect.ShowMessage -> snackbar.showSnackbar(
                when (effect.message) {
                    GroupEditUiEffect.Message.PERSON_IN_GAME -> inGameMessage
                    GroupEditUiEffect.Message.SAVED -> savedMessage
                },
            )
            GroupEditUiEffect.NavigateBack -> onNavigateBack()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_groups_edit_group)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(GroupEditUiEvent.Save) }, enabled = state.canSave) {
                        Text(stringResource(R.string.feature_groups_save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        GroupEditContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@Composable
fun GroupEditContent(
    state: GroupEditUiState,
    onEvent: (GroupEditUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) return
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(GroupEditUiEvent.NameChanged(it)) },
            label = { Text(stringResource(R.string.feature_groups_group_name)) },
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.feature_groups_name_error)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
        Text(
            stringResource(R.string.feature_groups_members, state.members.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            items(state.members, key = { it.id.value }) { member ->
                ListItem(
                    leadingContent = { PersonAvatar(member.name, member.color.toColor(), size = 36.dp) },
                    headlineContent = { Text(member.name) },
                    supportingContent = if (member.isInCurrentGame) {
                        { Text(stringResource(R.string.feature_groups_in_current_game)) }
                    } else {
                        null
                    },
                    trailingContent = {
                        Row {
                            IconButton(
                                onClick = { onEvent(GroupEditUiEvent.EditMember(member.id)) },
                                enabled = !member.isInCurrentGame,
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.feature_groups_edit_person))
                            }
                            IconButton(
                                onClick = { onEvent(GroupEditUiEvent.RemoveMember(member.id)) },
                                enabled = !member.isInCurrentGame,
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.feature_groups_remove_member))
                            }
                        }
                    },
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { onEvent(GroupEditUiEvent.AddExistingClicked) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_groups_add_existing))
            }
            OutlinedButton(onClick = { onEvent(GroupEditUiEvent.AddNewClicked) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_groups_add_new))
            }
        }
        HorizontalDivider()
        TextButton(
            onClick = { onEvent(GroupEditUiEvent.ToggleArchive) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Text(
                stringResource(
                    if (state.isArchived) R.string.feature_groups_unarchive_group else R.string.feature_groups_archive_group,
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (state.showAddExisting) {
        AddExistingDialog(state, onEvent)
    }
    if (state.showAddNew) {
        AddNewPersonDialog(state, onEvent)
    }
}

@Composable
private fun AddExistingDialog(state: GroupEditUiState, onEvent: (GroupEditUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(GroupEditUiEvent.DismissDialogs) },
        title = { Text(stringResource(R.string.feature_groups_add_existing_title)) },
        text = {
            if (state.candidates.isEmpty()) {
                Text(stringResource(R.string.feature_groups_no_other_persons))
            } else {
                LazyColumn {
                    items(state.candidates, key = { it.id.value }) { candidate ->
                        ListItem(
                            leadingContent = { PersonAvatar(candidate.name, candidate.color.toColor(), size = 32.dp) },
                            headlineContent = { Text(candidate.name) },
                            modifier = Modifier.padding(0.dp),
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            trailingContent = {
                                TextButton(onClick = { onEvent(GroupEditUiEvent.AddExisting(candidate.id)) }) {
                                    Text(stringResource(R.string.feature_groups_add))
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(GroupEditUiEvent.DismissDialogs) }) {
                Text(stringResource(R.string.feature_groups_cancel))
            }
        },
    )
}

@Composable
private fun AddNewPersonDialog(state: GroupEditUiState, onEvent: (GroupEditUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(GroupEditUiEvent.DismissDialogs) },
        title = { Text(stringResource(R.string.feature_groups_add_new)) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.newPersonName,
                    onValueChange = { onEvent(GroupEditUiEvent.NewPersonNameChanged(it)) },
                    label = { Text(stringResource(R.string.feature_groups_new_person_name)) },
                    isError = state.newPersonError,
                    singleLine = true,
                )
                if (state.newPersonError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.feature_groups_name_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(GroupEditUiEvent.ConfirmNewPerson) },
                enabled = state.newPersonName.isNotBlank(),
            ) { Text(stringResource(R.string.feature_groups_add)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(GroupEditUiEvent.DismissDialogs) }) {
                Text(stringResource(R.string.feature_groups_cancel))
            }
        },
    )
}
