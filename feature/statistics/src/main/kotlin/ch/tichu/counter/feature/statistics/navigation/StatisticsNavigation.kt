package ch.tichu.counter.feature.statistics.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import ch.tichu.counter.core.ui.navigation.LeaderboardRoute
import ch.tichu.counter.core.ui.navigation.StatisticsGraphRoute
import ch.tichu.counter.feature.statistics.StatisticsScreen

fun NavGraphBuilder.statisticsGraph(
    onOpenGroupPicker: () -> Unit,
    onBugReport: () -> Unit,
) {
    navigation<StatisticsGraphRoute>(startDestination = LeaderboardRoute) {
        composable<LeaderboardRoute> {
            StatisticsScreen(onOpenGroupPicker = onOpenGroupPicker, onBugReport = onBugReport)
        }
    }
}
