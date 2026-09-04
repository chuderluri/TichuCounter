package ch.tichu.counter

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.tichu.counter.core.ui.navigation.GameDetailRoute
import ch.tichu.counter.core.ui.navigation.GameListRoute
import ch.tichu.counter.core.ui.navigation.GameSetupRoute
import ch.tichu.counter.core.ui.navigation.GroupEditRoute
import ch.tichu.counter.core.ui.navigation.GroupPickerRoute
import ch.tichu.counter.core.ui.navigation.HomeRoute
import ch.tichu.counter.core.ui.navigation.LeaderboardRoute
import ch.tichu.counter.core.ui.navigation.PlayerEditRoute
import ch.tichu.counter.core.ui.navigation.PlayerListRoute
import ch.tichu.counter.core.ui.navigation.ScoringRoute
import ch.tichu.counter.core.ui.navigation.SettingsRoute
import ch.tichu.counter.core.ui.navigation.SwapPlayerRoute
import ch.tichu.counter.core.domain.usecase.preferences.ObservePreferencesUseCase
import ch.tichu.counter.feature.game.navigation.gameGraph
import ch.tichu.counter.feature.groups.navigation.groupsGraph
import ch.tichu.counter.feature.history.navigation.historyGraph
import ch.tichu.counter.feature.players.navigation.playersGraph
import ch.tichu.counter.feature.scoring.navigation.scoringGraph
import ch.tichu.counter.feature.settings.navigation.settingsGraph
import ch.tichu.counter.feature.statistics.navigation.statisticsGraph
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private data class BottomDestination<T : Any>(
    val route: T,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun TichuApp(
    navController: NavHostController = rememberNavController(),
    viewModel: AppViewModel = hiltViewModel(),
) {
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()
    if (onboardingDone == null) return
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination?.isBottomDestination() == true
    val bottomDestinations = remember {
        listOf(
            BottomDestination(HomeRoute, "Play", Icons.Default.PlayArrow),
            BottomDestination(PlayerListRoute, "Players", Icons.Default.Group),
            BottomDestination(GameListRoute, "History", Icons.Default.History),
            BottomDestination(LeaderboardRoute, "Stats", Icons.Default.BarChart),
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { item ->
                        NavigationBarItem(
                            selected = destination.hasRoute(item.route::class),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(HomeRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingDone == true) HomeRoute else GroupPickerRoute,
            modifier = Modifier.padding(padding),
        ) {
            groupsGraph(
                onNavigateToHome = { navController.navigate(HomeRoute) { popUpTo(GroupPickerRoute) { inclusive = true } } },
                onNavigateToGroupEdit = { groupId -> navController.navigate(GroupEditRoute(groupId?.value)) },
                onNavigateToSetup = { navController.navigate(GameSetupRoute()) },
                onNavigateToPlayerEdit = { personId -> navController.navigate(PlayerEditRoute(personId.value)) },
                onNavigateBack = { navController.popBackStack() },
            )
            gameGraph(
                onNavigateToSetup = { abandon -> navController.navigate(GameSetupRoute(abandon)) },
                onNavigateToScoring = { gameId -> navController.navigate(ScoringRoute(gameId.value)) },
                onOpenGroupPicker = { navController.navigate(GroupPickerRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onNavigateBack = { navController.popBackStack() },
                onShowMessage = {},
            )
            playersGraph(
                onNavigateToEdit = { personId -> navController.navigate(PlayerEditRoute(personId?.value)) },
                onOpenGroupPicker = { navController.navigate(GroupPickerRoute) },
                onNavigateBack = { navController.popBackStack() },
            )
            scoringGraph(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSwap = { gameId, seat -> navController.navigate(SwapPlayerRoute(gameId.value, seat.name)) },
                onNavigateToSetup = { navController.navigate(GameSetupRoute()) },
                onNavigateHome = { navController.navigate(HomeRoute) { popUpTo(HomeRoute) { inclusive = false } } },
            )
            historyGraph(
                onNavigateToDetail = { gameId -> navController.navigate(GameDetailRoute(gameId.value)) },
                onNavigateToScoring = { gameId -> navController.navigate(ScoringRoute(gameId.value)) },
                onOpenGroupPicker = { navController.navigate(GroupPickerRoute) },
                onNavigateBack = { navController.popBackStack() },
            )
            statisticsGraph(onOpenGroupPicker = { navController.navigate(GroupPickerRoute) })
            settingsGraph(
                onNavigateBack = { navController.popBackStack() },
                onOpenGroupPicker = { navController.navigate(GroupPickerRoute) },
                onOpenGroupManagement = { navController.navigate(GroupPickerRoute) },
            )
        }
    }
}

@HiltViewModel
class AppViewModel @Inject constructor(observePreferences: ObservePreferencesUseCase) : ViewModel() {
    val onboardingDone: StateFlow<Boolean?> = observePreferences()
        .map { it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

private fun NavDestination.isBottomDestination(): Boolean = hasRoute<HomeRoute>() || hasRoute<PlayerListRoute>() || hasRoute<GameListRoute>() || hasRoute<LeaderboardRoute>()
