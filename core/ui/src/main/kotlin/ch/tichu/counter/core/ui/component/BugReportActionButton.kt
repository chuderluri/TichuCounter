package ch.tichu.counter.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.tichu.counter.core.ui.R

@Composable
fun BugReportActionButton(
    onBugReport: () -> Unit,
) {
    IconButton(onClick = onBugReport) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = stringResource(R.string.core_ui_report_bug),
        )
    }
}
