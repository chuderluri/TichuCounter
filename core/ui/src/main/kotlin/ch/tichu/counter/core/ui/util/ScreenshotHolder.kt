package ch.tichu.counter.core.ui.util

import android.graphics.Bitmap

object ScreenshotHolder {
    @Volatile
    var bitmap: Bitmap? = null

    fun clear() {
        bitmap = null
    }
}
