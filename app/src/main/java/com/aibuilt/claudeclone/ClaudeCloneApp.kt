package com.aibuilt.claudeclone

import android.app.Application

class ClaudeCloneApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
