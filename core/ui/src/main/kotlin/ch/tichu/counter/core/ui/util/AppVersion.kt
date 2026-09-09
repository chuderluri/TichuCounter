package ch.tichu.counter.core.ui.util

import android.content.Context
import android.content.pm.PackageManager

fun appVersionName(context: Context): String? = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
} catch (e: PackageManager.NameNotFoundException) {
    null
}
