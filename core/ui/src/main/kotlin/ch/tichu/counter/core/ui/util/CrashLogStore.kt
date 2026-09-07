package ch.tichu.counter.core.ui.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject

class CrashLogStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val file: File
        get() = File(context.filesDir, "last_crash.txt")

    fun write(throwable: Throwable) {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        file.writeText(writer.toString())
    }

    fun read(): String? = file.takeIf { it.exists() }?.readText()
}
