package ch.tichu.counter.feature.groups.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.feature.groups.R

@Composable
fun GroupPickerScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToGroupEdit: (GroupId?) -> Unit,
    onNavigateToSetup: () -> Unit,
    onDismiss: () -> Unit,
    showBackButton: Boolean = false,
    onBugReport: () -> Unit,
    viewModel: GroupPickerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            GroupPickerUiEffect.NavigateToHome -> onNavigateToHome()
            is GroupPickerUiEffect.NavigateToGroupEdit -> onNavigateToGroupEdit(effect.groupId)
            GroupPickerUiEffect.NavigateToSetup -> onNavigateToSetup()
            GroupPickerUiEffect.Dismiss -> onDismiss()
        }
    }
    Scaffold { padding ->
        GroupPickerContent(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
            showBackButton = showBackButton,
            onBack = onDismiss,
            onBugReport = onBugReport,
        )
    }
}

@Composable
fun GroupPickerContent(
    state: GroupPickerUiState,
    onEvent: (GroupPickerUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    onBugReport: () -> Unit,
) {
    if (state.isLoading) return
    if (state.isFirstStart && state.groups.isEmpty()) {
        FirstStartContent(state, onEvent, modifier, showBackButton, onBack, onBugReport)
    } else {
        PickerContent(state, onEvent, modifier, showBackButton, onBack, onBugReport)
    }
}

@Composable
private fun FirstStartContent(
    state: GroupPickerUiState,
    onEvent: (GroupPickerUiEvent) -> Unit,
    modifier: Modifier,
    showBackButton: Boolean,
    onBack: (() -> Unit)?,
    onBugReport: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (showBackButton && onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
            Text(
                stringResource(R.string.feature_groups_app_name),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            BugReportActionButton(onBugReport)
        }
        Spacer(Modifier.height(48.dp))
        ChoiceCard(
            icon = Icons.Default.PlayArrow,
            title = stringResource(R.string.feature_groups_just_play).uppercase(),
            subtitle = stringResource(R.string.feature_groups_just_play_subtitle),
            primary = true,
            onClick = { onEvent(GroupPickerUiEvent.JustPlayClicked) },
        )
        Spacer(Modifier.height(16.dp))
        if (state.isCreating) {
            CreateGroupInline(state, onEvent)
        } else {
            ChoiceCard(
                icon = Icons.Default.Add,
                title = stringResource(R.string.feature_groups_create_group).uppercase(),
                subtitle = stringResource(R.string.feature_groups_create_group_subtitle),
                primary = false,
                onClick = { onEvent(GroupPickerUiEvent.CreateGroupClicked) },
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val colors = if (primary) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = colors) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PickerContent(
    state: GroupPickerUiState,
    onEvent: (GroupPickerUiEvent) -> Unit,
    modifier: Modifier,
    showBackButton: Boolean,
    onBack: (() -> Unit)?,
    onBugReport: () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBackButton && onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.feature_groups_picker_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.feature_groups_picker_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BugReportActionButton(onBugReport)
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.groups, key = { it.id.value }) { group ->
                GroupCard(group, onEvent)
            }
        }
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.isCreating) {
                CreateGroupInline(state, onEvent)
            } else {
                OutlinedButton(
                    onClick = { onEvent(GroupPickerUiEvent.CreateGroupClicked) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.feature_groups_create_group))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    "  ${stringResource(R.string.feature_groups_or)}  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { onEvent(GroupPickerUiEvent.JustPlayClicked) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feature_groups_just_play) + " (" + stringResource(R.string.feature_groups_just_play_subtitle).lowercase().substringBefore('.') + ")")
            }
        }
    }
}

@Composable
private fun GroupCard(group: GroupUi, onEvent: (GroupPickerUiEvent) -> Unit) {
    val colors = if (group.isActive) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(onClick = { onEvent(GroupPickerUiEvent.GroupSelected(group.id)) }, colors = colors, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.feature_groups_members_games, group.memberCount, group.gameCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onEvent(GroupPickerUiEvent.EditGroup(group.id)) }) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.feature_groups_edit_group))
            }
        }
    }
}

@Composable
private fun CreateGroupInline(state: GroupPickerUiState, onEvent: (GroupPickerUiEvent) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.newGroupName,
            onValueChange = { onEvent(GroupPickerUiEvent.NewGroupNameChanged(it)) },
            label = { Text(stringResource(R.string.feature_groups_group_name)) },
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.feature_groups_name_error)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(GroupPickerUiEvent.ConfirmCreateGroup) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { onEvent(GroupPickerUiEvent.CancelCreateGroup) }) {
                Text(stringResource(R.string.feature_groups_cancel))
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onEvent(GroupPickerUiEvent.ConfirmCreateGroup) },
                enabled = state.newGroupName.isNotBlank(),
            ) { Text(stringResource(R.string.feature_groups_create)) }
        }
    }
}
