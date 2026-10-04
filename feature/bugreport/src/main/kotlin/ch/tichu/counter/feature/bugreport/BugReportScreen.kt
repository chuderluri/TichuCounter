package ch.tichu.counter.feature.bugreport

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.tichu.counter.core.ui.util.CollectEffects
import ch.tichu.counter.core.ui.util.ScreenshotHolder
import ch.tichu.counter.core.ui.util.captureCurrentView
import java.io.File
import java.io.FileOutputStream

private const val TAG = "BugReportScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: BugReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is BugReportUiEffect.PrepareEmail -> {
                prepareEmail(context, effect)
                ScreenshotHolder.clear()
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_bugreport_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_bugreport_back))
                    }
                },
            )
        },
    ) { padding ->
        BugReportContent(state, viewModel::onEvent, Modifier.padding(padding))
    }
}

@Composable
private fun BugReportContent(state: BugReportUiState, onEvent: (BugReportUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = state.description,
            onValueChange = { onEvent(BugReportUiEvent.DescriptionChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feature_bugreport_description)) },
            placeholder = { Text(stringResource(R.string.feature_bugreport_description_hint)) },
            minLines = 4,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.attachScreenshot,
                onCheckedChange = { onEvent(BugReportUiEvent.ScreenshotChanged(it)) },
            )
            Text(stringResource(R.string.feature_bugreport_add_screenshot))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                enabled = state.hasCrashLog,
                checked = state.attachCrashLog,
                onCheckedChange = { onEvent(BugReportUiEvent.CrashLogChanged(it)) },
            )
            Text(
                stringResource(
                    if (state.hasCrashLog) {
                        R.string.feature_bugreport_add_crash_log
                    } else {
                        R.string.feature_bugreport_no_crash_log
                    },
                ),
            )
        }
        Button(
            onClick = { onEvent(BugReportUiEvent.PrepareEmail) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.feature_bugreport_prepare_email))
        }
    }
}

private fun prepareEmail(
    context: android.content.Context,
    effect: BugReportUiEffect.PrepareEmail,
) {
    val cacheDir = File(context.cacheDir, "bugreport").apply { mkdirs() }
    val attachmentUris = mutableListOf<Uri>()

    val screenshot = effect.screenshot ?: captureCurrentView(context)
    if (screenshot != null) {
        val file = File(cacheDir, "screenshot.png")
        FileOutputStream(file).use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        attachmentUris += uriFor(context, file)
    }
    if (!effect.crashLog.isNullOrBlank()) {
        val file = File(cacheDir, "crash_log.txt")
        file.writeText(effect.crashLog)
        attachmentUris += uriFor(context, file)
    }

    val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
    val body = buildString {
        appendLine("What happened?")
        appendLine(effect.description.ifBlank { "-" })
        appendLine()
        appendLine("Steps to reproduce")
        appendLine("1. ")
        appendLine()
        appendLine("Device")
        appendLine(device)
        appendLine("App version")
        appendLine(appVersion(context))
    }

    val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
        type = "message/rfc822"
        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("tichucounter.bugs@gmail.com"))
        putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.feature_bugreport_email_subject))
        putExtra(android.content.Intent.EXTRA_TEXT, body)
        if (attachmentUris.isNotEmpty()) {
            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, ArrayList(attachmentUris))
        }
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        android.content.Intent.createChooser(
            intent,
            context.getString(R.string.feature_bugreport_email_chooser),
        ),
    )
}

private fun uriFor(context: android.content.Context, file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
private fun appVersion(context: android.content.Context): String = try {
    val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
    "${pkg.versionName} (${pkg.versionCode})"
} catch (e: android.content.pm.PackageManager.NameNotFoundException) {
    android.util.Log.w(TAG, "App version unavailable", e)
    "unknown"
}
