package com.kaeru.app

import android.app.Application
import com.kaeru.app.data.utils.CrashHandler
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        CrashHandler.install(this)
    }
}