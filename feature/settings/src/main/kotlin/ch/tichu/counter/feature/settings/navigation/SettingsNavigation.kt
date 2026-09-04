package ch.tichu.counter.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ch.tichu.counter.core.ui.navigation.SettingsRoute
import ch.tichu.counter.feature.settings.SettingsScreen

fun NavGraphBuilder.settingsGraph(
    onNavigateBack: () -> Unit,
    onOpenGroupPicker: () -> Unit,
    onOpenGroupManagement: () -> Unit,
) {
    composable<SettingsRoute> {
        SettingsScreen(
            onNavigateBack = onNavigateBack,
            onOpenGroupPicker = onOpenGroupPicker,
            onOpenGroupManagement = onOpenGroupManagement,
        )
    }
}
