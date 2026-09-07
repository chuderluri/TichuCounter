package ch.tichu.counter.feature.statistics

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import ch.tichu.counter.core.domain.usecase.group.ObserveActiveGroupUseCase
import ch.tichu.counter.core.ui.component.BugReportActionButton
import ch.tichu.counter.core.ui.component.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(observeActiveGroup: ObserveActiveGroupUseCase) : ViewModel() {
    val isQuickPlay: StateFlow<Boolean> = observeActiveGroup().map { it.isQuickPlay }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onOpenGroupPicker: () -> Unit,
    onBugReport: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val isQuickPlay by viewModel.isQuickPlay.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_statistics_title)) },
                actions = {
                    BugReportActionButton(onBugReport)
                },
            )
        },
    ) { padding ->
        if (isQuickPlay) {
            EmptyState(
                icon = Icons.Default.Groups,
                title = stringResource(R.string.feature_statistics_unavailable_title),
                description = stringResource(R.string.feature_statistics_unavailable_description),
                modifier = Modifier.fillMaxSize().then(Modifier.padding(padding)),
                action = {
                    TextButton(onClick = onOpenGroupPicker) {
                        Text(stringResource(R.string.feature_statistics_create_group))
                    }
                },
            )
        } else {
            EmptyState(
                icon = Icons.Default.BarChart,
                title = stringResource(R.string.feature_statistics_title),
                description = stringResource(R.string.feature_statistics_coming_soon),
                modifier = Modifier.fillMaxSize().then(Modifier.padding(padding)),
            )
        }
    }
}
