package ch.tichu.counter.feature.groups.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ch.tichu.counter.core.model.GroupId
import ch.tichu.counter.core.model.PersonId
import ch.tichu.counter.core.ui.navigation.GroupEditRoute
import ch.tichu.counter.core.ui.navigation.GroupPickerRoute
import ch.tichu.counter.feature.groups.edit.GroupEditScreen
import ch.tichu.counter.feature.groups.picker.GroupPickerScreen

fun NavGraphBuilder.groupsGraph(
    onNavigateToHome: () -> Unit,
    onNavigateToGroupEdit: (GroupId?) -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToPlayerEdit: (PersonId) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<GroupPickerRoute> {
        GroupPickerScreen(
            onNavigateToHome = onNavigateToHome,
            onNavigateToGroupEdit = onNavigateToGroupEdit,
            onNavigateToSetup = onNavigateToSetup,
            onDismiss = onNavigateBack,
            showBackButton = true,
        )
    }
    composable<GroupEditRoute> {
        GroupEditScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToPlayerEdit = onNavigateToPlayerEdit,
        )
    }
}
