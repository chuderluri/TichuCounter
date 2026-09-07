package ch.tichu.counter.feature.bugreport.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ch.tichu.counter.core.ui.navigation.BugReportRoute
import ch.tichu.counter.feature.bugreport.BugReportScreen

fun NavGraphBuilder.bugreportGraph(
    onNavigateBack: () -> Unit,
) {
    composable<BugReportRoute> {
        BugReportScreen(onNavigateBack = onNavigateBack)
    }
}
