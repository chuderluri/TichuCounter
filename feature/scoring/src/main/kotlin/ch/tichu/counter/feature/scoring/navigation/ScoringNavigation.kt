package ch.tichu.counter.feature.scoring.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.model.Seat
import ch.tichu.counter.core.ui.navigation.ScoringRoute
import ch.tichu.counter.feature.scoring.ScoringScreen

fun NavGraphBuilder.scoringGraph(
    onNavigateBack: () -> Unit,
    onNavigateToSwap: (GameId, Seat) -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToScoring: (GameId) -> Unit,
    onBugReport: () -> Unit,
) {
    composable<ScoringRoute> {
        ScoringScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSwap = onNavigateToSwap,
            onNavigateToSetup = onNavigateToSetup,
            onNavigateHome = onNavigateHome,
            onNavigateToScoring = onNavigateToScoring,
            onBugReport = onBugReport,
        )
    }
}
