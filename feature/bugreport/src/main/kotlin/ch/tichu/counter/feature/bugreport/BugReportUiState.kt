package ch.tichu.counter.feature.bugreport

import androidx.compose.runtime.Immutable

@Immutable
data class BugReportUiState(
    val description: String = "",
    val attachScreenshot: Boolean = false,
    val attachCrashLog: Boolean = false,
    val hasCrashLog: Boolean = false,
)

sealed interface BugReportUiEvent {
    data class DescriptionChanged(val description: String) : BugReportUiEvent

    data class ScreenshotChanged(val enabled: Boolean) : BugReportUiEvent

    data class CrashLogChanged(val enabled: Boolean) : BugReportUiEvent

    data object PrepareEmail : BugReportUiEvent
}

sealed interface BugReportUiEffect {
    data class PrepareEmail(
        val description: String,
        val screenshot: android.graphics.Bitmap?,
        val crashLog: String?,
    ) : BugReportUiEffect
}
