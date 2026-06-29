package com.ventgui.app

import android.app.Application
import com.ventgui.app.data.utils.UserLogger
import com.ventgui.app.data.utils.NotificationHelper
import com.ventgui.app.data.utils.BirthdayWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CantanhedeApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        UserLogger.initialize(applicationScope)
        NotificationHelper.createBirthdayChannel(this)
        BirthdayWorkScheduler.scheduleIfNeeded(this)
    }
}
