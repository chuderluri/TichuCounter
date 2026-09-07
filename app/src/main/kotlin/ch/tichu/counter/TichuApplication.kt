package ch.tichu.counter

import android.app.Application
import ch.tichu.counter.core.ui.util.CrashLogStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TichuApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashLogStore = CrashLogStore(this)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            crashLogStore.write(throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
