package com.ventgui.app.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
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
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatsWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val wins = prefs[intPreferencesKey("wins")] ?: 0
            val podiums = prefs[intPreferencesKey("podiums")] ?: 0
            val athletes = prefs[intPreferencesKey("athletes")] ?: 0

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("cantanhedehub://dashboard")).apply {
                setClass(context, MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetTheme.MidnightBlue)
                    .padding(8.dp)
                    .clickable(actionStartActivity(intent)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTATÍSTICAS DA ÉPOCA",
                        style = TextStyle(
                            color = WidgetTheme.CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Vitórias
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏆", style = TextStyle(fontSize = 16.sp))
                            Text(
                                text = wins.toString(),
                                style = TextStyle(
                                    color = WidgetTheme.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Vitórias",
                                style = TextStyle(color = WidgetTheme.WhiteSemi, fontSize = 9.sp)
                            )
                        }

                        // Pódios
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🥈", style = TextStyle(fontSize = 16.sp))
                            Text(
                                text = podiums.toString(),
                                style = TextStyle(
                                    color = WidgetTheme.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Pódios",
                                style = TextStyle(color = WidgetTheme.WhiteSemi, fontSize = 9.sp)
                            )
                        }

                        // Atletas Activos
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🚴", style = TextStyle(fontSize = 16.sp))
                            Text(
                                text = athletes.toString(),
                                style = TextStyle(
                                    color = WidgetTheme.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Atletas",
                                style = TextStyle(color = WidgetTheme.WhiteSemi, fontSize = 9.sp)
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Button(
                        text = "Atualizar",
                        onClick = actionRunCallback<UpdateStatsCallback>(),
                        modifier = GlanceModifier.fillMaxWidth().height(32.dp)
                    )
                }
            }
        }
    }
}

class UpdateStatsCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        withContext(Dispatchers.IO) {
            try {
                SupabaseClient.initialize(context)
                val allAthletes = SupabaseClient.client.postgrest.from("athletes").select().decodeList<Athlete>()
                val allRaces = SupabaseClient.client.postgrest.from("races").select().decodeList<Race>()
                val allResults = SupabaseClient.client.postgrest.from("race_results").select().decodeList<RaceResult>()

                val activeAthletesCount = allAthletes.filter { it.status.equals("active", ignoreCase = true) }.size
                val victoriesCount = allResults.filter { res ->
                    res.position == 1 && allAthletes.any { it.id == res.athlete_id } && allRaces.any { it.id == res.race_id }
                }.size
                val podiumsCount = allResults.filter { res ->
                    res.position in 1..3 && allAthletes.any { it.id == res.athlete_id } && allRaces.any { it.id == res.race_id }
                }.size

                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[intPreferencesKey("wins")] = victoriesCount
                        this[intPreferencesKey("podiums")] = podiumsCount
                        this[intPreferencesKey("athletes")] = activeAthletesCount
                    }
                }
                StatsWidget().update(context, glanceId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
