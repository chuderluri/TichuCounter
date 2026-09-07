package ch.tichu.counter.feature.bugreport

import androidx.lifecycle.ViewModel
import ch.tichu.counter.core.ui.util.CrashLogStore
import ch.tichu.counter.core.ui.util.ScreenshotHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BugReportViewModel @Inject constructor(
    private val crashLogStore: CrashLogStore,
) : ViewModel() {

    private val capturedScreenshot = ScreenshotHolder.bitmap

    private val _state = MutableStateFlow(
        BugReportUiState(
            hasCrashLog = crashLogStore.read()?.isNotBlank() == true,
            attachScreenshot = capturedScreenshot != null,
        ),
    )
    val state: StateFlow<BugReportUiState> = _state.asStateFlow()

    private val _effects = Channel<BugReportUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: BugReportUiEvent) {
        when (event) {
            is BugReportUiEvent.DescriptionChanged -> _state.update { it.copy(description = event.description) }
            is BugReportUiEvent.ScreenshotChanged -> _state.update { it.copy(attachScreenshot = event.enabled) }
            is BugReportUiEvent.CrashLogChanged -> _state.update { it.copy(attachCrashLog = event.enabled) }
            BugReportUiEvent.PrepareEmail -> {
                val current = state.value
                _effects.trySend(
                    BugReportUiEffect.PrepareEmail(
                        description = current.description,
                        screenshot = if (current.attachScreenshot) capturedScreenshot else null,
                        crashLog = if (current.attachCrashLog && current.hasCrashLog) {
                            crashLogStore.read().orEmpty()
                        } else {
                            null
                        },
                    ),
                )
            }
        }
    }
}
