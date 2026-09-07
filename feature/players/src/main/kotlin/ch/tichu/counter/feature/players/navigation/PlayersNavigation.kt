package ch.tichu.counter.feature.players.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.ui.navigation.PlayerEditRoute
import ch.tichu.counter.core.ui.navigation.PlayerListRoute
import ch.tichu.counter.core.ui.navigation.PlayersGraphRoute
import ch.tichu.counter.feature.players.edit.PlayerEditScreen
import ch.tichu.counter.feature.players.list.PlayerListScreen

fun NavGraphBuilder.playersGraph(
    onNavigateToEdit: (PersonId?) -> Unit,
    onOpenGroupPicker: () -> Unit,
    onNavigateBack: () -> Unit,
    onBugReport: () -> Unit,
) {
    navigation<PlayersGraphRoute>(startDestination = PlayerListRoute) {
        composable<PlayerListRoute> {
            PlayerListScreen(
                onNavigateToEdit = onNavigateToEdit,
                onOpenGroupPicker = onOpenGroupPicker,
                onBugReport = onBugReport,
            )
        }
        composable<PlayerEditRoute> {
            PlayerEditScreen(onNavigateBack = onNavigateBack, onBugReport = onBugReport)
        }
    }
}
