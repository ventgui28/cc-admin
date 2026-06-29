package com.ventgui.app.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ventgui.app.data.repository.BirthdayRepository
import com.ventgui.app.data.repository.TeamRepository
import com.ventgui.app.data.utils.NotificationHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BirthdayCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val teamRepository = TeamRepository()
    private val birthdayRepository = BirthdayRepository()

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            val athletes = teamRepository.getAthletes()

            for (athlete in athletes) {
                val birthDate = parseBirthDate(athlete.birth_date) ?: continue
                val thisYearBirthday = birthDate.withYear(today.year)

                val daysUntil = ChronoUnit.DAYS.between(today, thisYearBirthday).toInt()

                val adjustedDaysUntil = when {
                    daysUntil < 0 -> {
                        val nextYearBirthday = birthDate.withYear(today.year + 1)
                        ChronoUnit.DAYS.between(today, nextYearBirthday).toInt()
                    }
                    else -> daysUntil
                }

                if (adjustedDaysUntil == 2 || adjustedDaysUntil == 1) {
                    val year = if (daysUntil < 0) today.year + 1 else today.year
                    
                    val alreadySent = birthdayRepository.wasAlreadySent(athlete.id!!, year, adjustedDaysUntil)
                    if (!alreadySent) {
                        NotificationHelper.showBirthdayNotification(
                            applicationContext, athlete.id, athlete.name, adjustedDaysUntil
                        )
                        birthdayRepository.markAsSent(athlete.id, year, adjustedDaysUntil)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun parseBirthDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) { null }
    }
}
