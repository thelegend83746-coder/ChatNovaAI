package com.chatnova.ai

import android.app.Application
import com.chatnova.ai.di.AppContainer

class ChatNovaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
