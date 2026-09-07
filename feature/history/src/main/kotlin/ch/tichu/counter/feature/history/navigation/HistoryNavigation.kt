package ch.tichu.counter.feature.history.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.ui.navigation.GameDetailRoute
import ch.tichu.counter.core.ui.navigation.GameListRoute
import ch.tichu.counter.core.ui.navigation.HistoryGraphRoute
import ch.tichu.counter.feature.history.GameDetailScreen
import ch.tichu.counter.feature.history.GameListScreen

fun NavGraphBuilder.historyGraph(
    onNavigateToDetail: (GameId) -> Unit,
    onNavigateToScoring: (GameId) -> Unit,
    onOpenGroupPicker: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    navigation<HistoryGraphRoute>(startDestination = GameListRoute) {
        composable<GameListRoute> {
            GameListScreen(
                onNavigateToDetail = onNavigateToDetail,
                onOpenGroupPicker = onOpenGroupPicker,
            )
        }
        composable<GameDetailRoute> {
            GameDetailScreen(onNavigateBack = onNavigateBack, onNavigateToScoring = onNavigateToScoring)
        }
    }
}
