package ch.tichu.counter.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultTargetScore: Int = RuleSet.DEFAULT.targetScore,
    val finishOnTie: Boolean = RuleSet.DEFAULT.finishOnTie,
    val keepScreenOn: Boolean = true,
    val hapticFeedback: Boolean = true,
    val onboardingDone: Boolean = false,
    val activeGroupId: GroupId? = null,
) {
    val isQuickPlay: Boolean get() = onboardingDone && activeGroupId == null

    fun defaultRuleSet(): RuleSet = RuleSet.DEFAULT.copy(
        targetScore = defaultTargetScore,
        finishOnTie = finishOnTie,
    )
}
