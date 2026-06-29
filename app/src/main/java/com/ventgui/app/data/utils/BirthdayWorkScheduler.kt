package com.ventgui.app.data.utils

import android.content.Context
import androidx.work.*
import com.ventgui.app.data.workers.BirthdayCheckWorker
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object BirthdayWorkScheduler {

    private const val WORK_NAME = "birthday_check_daily"
    private const val PREFS_NAME = "birthday_scheduler_prefs"
    private const val KEY_HOUR = "notif_hour"
    private const val KEY_MINUTE = "notif_minute"

    fun getScheduledTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt(KEY_HOUR, 9) // Default: 09:00
        val minute = prefs.getInt(KEY_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun saveAndReschedule(context: Context, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
        
        schedule(context, hour, minute)
    }

    fun scheduleIfNeeded(context: Context) {
        val (hour, minute) = getScheduledTime(context)
        schedule(context, hour, minute)
    }

    private fun schedule(context: Context, hour: Int, minute: Int) {
        val initialDelay = calculateInitialDelay(hour, minute)

        val request = PeriodicWorkRequestBuilder<BirthdayCheckWorker>(
            24, TimeUnit.HOURS
        )
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Atualiza se já existir para aplicar a nova hora/atraso
            request
        )
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = LocalTime.now()
        val target = LocalTime.of(hour, minute)
        
        var delay = Duration.between(now, target).toMillis()
        if (delay < 0) {
            // Se a hora já passou hoje, agenda para a mesma hora de amanhã
            delay += TimeUnit.DAYS.toMillis(1)
        }
        return delay
    }
}
