package ch.tichu.counter.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.model.ThemeMode
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.feature.settings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenGroupPicker: () -> Unit,
    onOpenGroupManagement: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unavailable = stringResource(R.string.feature_settings_unavailable)
    val context = LocalContext.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsUiEffect.OpenGroupPicker -> onOpenGroupPicker()
            SettingsUiEffect.OpenGroupManagement -> onOpenGroupManagement()
            SettingsUiEffect.ShowUnavailable -> android.widget.Toast.makeText(
                context,
                unavailable,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            SettingsUiEffect.OpenBugReport -> openBugReport(context)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_settings_back))
                    }
                },
            )
        },
    ) { padding -> SettingsContent(state, viewModel::onEvent, Modifier.padding(padding)) }
}

@Composable
fun SettingsContent(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        SectionTitle(stringResource(R.string.feature_settings_group))
        ListItem(
            headlineContent = { Text(stringResource(R.string.feature_settings_active_group)) },
            supportingContent = { Text(state.activeGroupName ?: "Quick play") },
            trailingContent = { Text("›", style = MaterialTheme.typography.titleLarge) },
            modifier = Modifier.clickable { onEvent(SettingsUiEvent.ActiveGroupClicked) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.feature_settings_manage_groups)) },
            trailingContent = { Text("›", style = MaterialTheme.typography.titleLarge) },
            modifier = Modifier.clickable { onEvent(SettingsUiEvent.ManageGroupsClicked) },
        )
        HorizontalDivider()
        SectionTitle(stringResource(R.string.feature_settings_game))
        TargetScoreSetting(state, onEvent)
        ToggleItem(stringResource(R.string.feature_settings_finish_on_tie), state.finishOnTie) {
            onEvent(SettingsUiEvent.FinishOnTieChanged(it))
        }
        HorizontalDivider()
        SectionTitle(stringResource(R.string.feature_settings_display))
        ThemeSetting(state, onEvent)
        ToggleItem(stringResource(R.string.feature_settings_keep_screen_on), state.keepScreenOn) {
            onEvent(SettingsUiEvent.KeepScreenOnChanged(it))
        }
        ToggleItem(stringResource(R.string.feature_settings_haptic), state.hapticFeedback) {
            onEvent(SettingsUiEvent.HapticChanged(it))
        }
        HorizontalDivider()
        SectionTitle(stringResource(R.string.feature_settings_data))
        ListItem(
            headlineContent = { Text(stringResource(R.string.feature_settings_export)) },
            trailingContent = { Text("›", style = MaterialTheme.typography.titleLarge) },
            modifier = Modifier.clickable { onEvent(SettingsUiEvent.ExportClicked) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.feature_settings_import)) },
            trailingContent = { Text("›", style = MaterialTheme.typography.titleLarge) },
            modifier = Modifier.clickable { onEvent(SettingsUiEvent.ImportClicked) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.feature_settings_report_bug)) },
            supportingContent = { Text(stringResource(R.string.feature_settings_report_bug_description)) },
            trailingContent = { Text("›", style = MaterialTheme.typography.titleLarge) },
            modifier = Modifier.clickable { onEvent(SettingsUiEvent.ReportBugClicked) },
        )
        HorizontalDivider()
        SectionTitle(stringResource(R.string.feature_settings_about))
        ListItem(headlineContent = { Text(stringResource(R.string.feature_settings_version)) })
    }
}

private fun openBugReport(context: android.content.Context) {
    val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
    val body = buildString {
        appendLine("What happened?")
        appendLine()
        appendLine("Steps to reproduce")
        appendLine("1. ")
        appendLine()
        appendLine("Device")
        appendLine(device)
    }
    val intent = android.content.Intent(
        android.content.Intent.ACTION_SENDTO,
        android.net.Uri.parse("mailto:tichucounter.bugs@gmail.com"),
    )
        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Tichu Counter bug report")
        .putExtra(android.content.Intent.EXTRA_TEXT, body)
    context.startActivity(intent)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ToggleItem(title: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChanged) },
    )
}

@Composable
private fun TargetScoreSetting(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.feature_settings_default_target)) },
        trailingContent = {
            TextButton(onClick = { expanded = true }) {
                Text(state.defaultTargetScore.toString())
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.targetScoreOptions.forEach { score ->
                    DropdownMenuItem(
                        text = { Text(score.toString()) },
                        onClick = {
                            expanded = false
                            onEvent(SettingsUiEvent.TargetScoreChanged(score))
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun ThemeSetting(state: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.feature_settings_theme)) },
        trailingContent = {
            TextButton(onClick = { expanded = true }) {
                Text(
                    stringResource(
                        when (state.themeMode) {
                            ThemeMode.SYSTEM -> R.string.feature_settings_theme_system
                            ThemeMode.LIGHT -> R.string.feature_settings_theme_light
                            ThemeMode.DARK -> R.string.feature_settings_theme_dark
                        },
                    ),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> R.string.feature_settings_theme_system
                                        ThemeMode.LIGHT -> R.string.feature_settings_theme_light
                                        ThemeMode.DARK -> R.string.feature_settings_theme_dark
                                    },
                                ),
                            )
                        },
                        onClick = {
                            expanded = false
                            onEvent(SettingsUiEvent.ThemeChanged(mode))
                        },
                    )
                }
            }
        },
    )
}
