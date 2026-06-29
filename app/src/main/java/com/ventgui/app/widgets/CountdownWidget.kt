package com.ventgui.app.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ventgui.app.MainActivity
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class CountdownWidget : GlanceAppWidget() {
    
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val title = prefs[stringPreferencesKey("title")] ?: "Sem próximas provas"
            val dateStr = prefs[stringPreferencesKey("date")] ?: ""
            val location = prefs[stringPreferencesKey("location")] ?: ""
            
            val countdownText = if (dateStr.isNotEmpty()) {
                calculateCountdown(dateStr)
            } else {
                ""
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("cantanhedehub://races")).apply {
                setClass(context, MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetTheme.MidnightBlue)
                    .padding(12.dp)
                    .clickable(actionStartActivity(intent)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRÓXIMA PROVA",
                        style = TextStyle(
                            color = WidgetTheme.CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = title,
                        maxLines = 1,
                        style = TextStyle(
                            color = WidgetTheme.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (countdownText.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = countdownText,
                            style = TextStyle(
                                color = WidgetTheme.CyberCyan,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    if (location.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = location,
                            maxLines = 1,
                            style = TextStyle(
                                color = WidgetTheme.WhiteSemi,
                                fontSize = 11.sp
                            )
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    Button(
                        text = "Atualizar",
                        onClick = actionRunCallback<UpdateCountdownCallback>(),
                        modifier = GlanceModifier.fillMaxWidth().height(32.dp)
                    )
                }
            }
        }
    }

    private fun calculateCountdown(dateStr: String): String {
        return try {
            val instant = try {
                Instant.parse(dateStr)
            } catch (e: Exception) {
                val parts = dateStr.split(" ")
                if (parts.size >= 3) {
                    val day = parts[0].toInt()
                    val monthName = parts[1].lowercase().replaceFirstChar { it.uppercase() }
                    val year = parts[2].toInt()
                    val month = java.time.Month.valueOf(monthName.uppercase())
                    java.time.LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else null
            }
            if (instant != null) {
                val now = Instant.now()
                val duration = Duration.between(now, instant)
                if (duration.isNegative || duration.isZero) {
                    "Hoje!"
                } else {
                    val days = duration.toDays()
                    val hours = duration.toHours() % 24
                    val minutes = duration.toMinutes() % 60
                    if (days > 0) {
                        "${days}d ${hours}h"
                    } else if (hours > 0) {
                        "${hours}h ${minutes}m"
                    } else {
                        "${minutes}m"
                    }
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}

class UpdateCountdownCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.initialize(context)
                val allRaces = SupabaseClient.client.postgrest.from("races").select().decodeList<Race>()
                
                val today = java.time.LocalDate.now(ZoneId.systemDefault())
                val nextRace = allRaces
                    .filter { race ->
                        val instant = parseFlexibleDate(race.date)
                        if (instant != null) {
                            val raceDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                            raceDate.isAfter(today) || raceDate.isEqual(today)
                        } else false
                    }
                    .minByOrNull { parseFlexibleDate(it.date) ?: Instant.MAX }
                
                nextRace?.let { race ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[stringPreferencesKey("title")] = race.title
                            this[stringPreferencesKey("date")] = race.date
                            this[stringPreferencesKey("location")] = race.location ?: ""
                        }
                    }
                    CountdownWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseFlexibleDate(dateStr: String): Instant? {
        if (dateStr.isBlank()) return null
        return try {
            Instant.parse(dateStr)
        } catch (e: Exception) {
            try {
                val parts = dateStr.split(" ")
                if (parts.size >= 3) {
                    val day = parts[0].toInt()
                    val monthName = parts[1].lowercase().replaceFirstChar { it.uppercase() }
                    val year = parts[2].toInt()
                    val month = java.time.Month.valueOf(monthName.uppercase())
                    java.time.LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant()
                } else null
            } catch (e2: Exception) {
                null
            }
        }
    }
}
