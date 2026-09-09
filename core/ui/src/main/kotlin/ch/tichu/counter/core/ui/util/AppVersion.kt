package ch.tichu.counter.core.ui.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

private const val TAG = "AppVersion"

fun appVersionName(context: Context): String? = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
} catch (e: PackageManager.NameNotFoundException) {
    Log.w(TAG, "App version unavailable", e)
    null
}
