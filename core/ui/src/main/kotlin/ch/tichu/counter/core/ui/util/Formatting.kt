package ch.tichu.counter.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.text.format.DateUtils
import kotlinx.datetime.Instant

@Composable
fun Instant.relativeDateText(): String {
    val context = LocalContext.current
    return DateUtils.getRelativeDateTimeString(
        context,
        toEpochMilliseconds(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.WEEK_IN_MILLIS,
        0,
    ).toString()
}

@Composable
fun Instant.shortDateText(): String {
    val context = LocalContext.current
    return DateUtils.formatDateTime(
        context,
        toEpochMilliseconds(),
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_YEAR,
    )
}

fun Int.signed(): String = if (this > 0) "+$this" else toString()
