package com.ventgui.app

import android.app.Application
import com.ventgui.app.data.utils.UserLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CantanhedeApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        UserLogger.initialize(applicationScope)
    }
}
