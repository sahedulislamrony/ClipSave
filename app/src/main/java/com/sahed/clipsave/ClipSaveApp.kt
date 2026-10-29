package com.sahed.clipsave

import android.app.Application
import com.sahed.clipsave.download.Notifications

class ClipSaveApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        container = AppContainer(this)
    }
}
