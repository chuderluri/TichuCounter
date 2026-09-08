package ch.tichu.counter.core.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object GroupPickerRoute

@Serializable
data class GroupEditRoute(val groupId: String? = null)

@Serializable
data object GroupCreateRoute

@Serializable
data object HomeRoute

@Serializable
data object PlayersGraphRoute

@Serializable
data object PlayerListRoute

@Serializable
data class PlayerEditRoute(val personId: String? = null)

@Serializable
data class GameSetupRoute(val abandonCurrent: Boolean = false)

@Serializable
data class ScoringRoute(val gameId: String)

@Serializable
data class SwapPlayerRoute(val gameId: String, val seat: String)

@Serializable
data object HistoryGraphRoute

@Serializable
data object GameListRoute

@Serializable
data class GameDetailRoute(val gameId: String)

@Serializable
data object StatisticsGraphRoute

@Serializable
data object LeaderboardRoute

@Serializable
data class PersonStatisticsRoute(val personId: String)

@Serializable
data object SettingsRoute

@Serializable
data object BugReportRoute
