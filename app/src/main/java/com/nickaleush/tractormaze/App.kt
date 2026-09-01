package com.nickaleush.tractormaze

import android.app.Application
import com.nickaleush.tractormaze.core.AppServiceLocator

class App : Application() {

    lateinit var serviceLocator: AppServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = AppServiceLocator(this)
    }

    override fun onTerminate() {
        serviceLocator.soundManager.release()
        super.onTerminate()
    }
}
