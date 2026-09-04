package ch.tichu.counter.feature.game.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import ch.tichu.counter.core.model.GameId
import ch.tichu.counter.core.ui.navigation.GameSetupRoute
import ch.tichu.counter.core.ui.navigation.HomeRoute
import ch.tichu.counter.core.ui.navigation.SwapPlayerRoute
import ch.tichu.counter.feature.game.home.HomeScreen
import ch.tichu.counter.feature.game.setup.GameSetupScreen
import ch.tichu.counter.feature.game.swap.SwapPlayerDialog

fun NavGraphBuilder.gameGraph(
    onNavigateToSetup: (abandonCurrent: Boolean) -> Unit,
    onNavigateToScoring: (GameId) -> Unit,
    onOpenGroupPicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(
            onNavigateToSetup = onNavigateToSetup,
            onNavigateToScoring = onNavigateToScoring,
            onOpenGroupPicker = onOpenGroupPicker,
            onOpenSettings = onOpenSettings,
        )
    }
    composable<GameSetupRoute> {
        GameSetupScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToScoring = onNavigateToScoring,
            onOpenGroupPicker = onOpenGroupPicker,
        )
    }
    dialog<SwapPlayerRoute> {
        SwapPlayerDialog(onDismiss = onNavigateBack, onError = onShowMessage)
    }
}
