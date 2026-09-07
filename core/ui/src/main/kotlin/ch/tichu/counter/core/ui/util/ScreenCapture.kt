package ch.tichu.counter.core.ui.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas

fun captureCurrentView(context: Context): Bitmap? {
    val activity = context as? Activity ?: return null
    val view = activity.window.decorView.rootView
    if (view.width <= 0 || view.height <= 0) return null
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return bitmap
}
