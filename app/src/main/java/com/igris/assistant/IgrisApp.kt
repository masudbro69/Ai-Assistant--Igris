package com.igris.assistant

import android.app.Application

class IgrisApp : Application() {
    lateinit var locator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        locator = ServiceLocator(applicationContext)
    }
}
