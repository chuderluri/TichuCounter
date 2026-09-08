package ch.tichu.counter.feature.groups.create

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.feature.groups.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCreateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onBugReport: () -> Unit,
    viewModel: GroupCreateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.feature_groups_create_error)
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            GroupCreateUiEffect.NavigateToHome -> onNavigateToHome()
            GroupCreateUiEffect.ShowError -> snackbar.showSnackbar(errorMessage)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_groups_create_group)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    BugReportActionButton(onBugReport)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        GroupCreateContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@Composable
fun GroupCreateContent(
    state: GroupCreateUiState,
    onEvent: (GroupCreateUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        when (state.phase) {
            GroupCreateUiState.Phase.NAME -> NameStep(state, onEvent)
            GroupCreateUiState.Phase.MEMBERS -> MembersStep(state, onEvent)
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
private fun NameStep(state: GroupCreateUiState, onEvent: (GroupCreateUiEvent) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.feature_groups_create_name_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(GroupCreateUiEvent.NameChanged(it)) },
            label = { Text(stringResource(R.string.feature_groups_group_name)) },
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.feature_groups_name_error)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onEvent(GroupCreateUiEvent.CreateClicked) },
            enabled = state.canCreate,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(stringResource(R.string.feature_groups_create_and_continue))
        }
    }
}

@Composable
private fun MembersStep(state: GroupCreateUiState, onEvent: (GroupCreateUiEvent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.feature_groups_members, state.members.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            items(state.members, key = { it.id.value }) { member ->
                ListItem(
                    headlineContent = { Text(member.name) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { onEvent(GroupCreateUiEvent.AddExistingClicked) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_groups_add_existing))
            }
            OutlinedButton(onClick = { onEvent(GroupCreateUiEvent.AddNewClicked) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_groups_add_new))
            }
        }
        HorizontalDivider()
        Button(
            onClick = { onEvent(GroupCreateUiEvent.DoneClicked) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) {
            Text(stringResource(R.string.feature_groups_done))
        }
    }
}

@Composable
private fun AddExistingDialog(state: GroupCreateUiState, onEvent: (GroupCreateUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(GroupCreateUiEvent.DismissDialogs) },
        title = { Text(stringResource(R.string.feature_groups_add_existing_title)) },
        text = {
            if (state.candidates.isEmpty()) {
                Text(stringResource(R.string.feature_groups_no_other_persons))
            } else {
                LazyColumn {
                    items(state.candidates, key = { it.id.value }) { candidate ->
                        ListItem(
                            headlineContent = { Text(candidate.name) },
                            modifier = Modifier.padding(0.dp),
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            trailingContent = {
                                TextButton(onClick = { onEvent(GroupCreateUiEvent.AddExisting(candidate.id)) }) {
                                    Text(stringResource(R.string.feature_groups_add))
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(GroupCreateUiEvent.DismissDialogs) }) {
                Text(stringResource(R.string.feature_groups_cancel))
            }
        },
    )
}

@Composable
private fun AddNewPersonDialog(state: GroupCreateUiState, onEvent: (GroupCreateUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(GroupCreateUiEvent.DismissDialogs) },
        title = { Text(stringResource(R.string.feature_groups_add_new)) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.newPersonName,
                    onValueChange = { onEvent(GroupCreateUiEvent.NewPersonNameChanged(it)) },
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
                onClick = { onEvent(GroupCreateUiEvent.ConfirmNewPerson) },
                enabled = state.newPersonName.isNotBlank(),
            ) { Text(stringResource(R.string.feature_groups_add)) }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(GroupCreateUiEvent.DismissDialogs) }) {
                Text(stringResource(R.string.feature_groups_cancel))
            }
        },
    )
}
