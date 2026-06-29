package com.ventgui.app.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ventgui.app.MainActivity

class QuickActionsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val addRaceIntent = Intent(Intent.ACTION_VIEW, Uri.parse("cantanhedehub://races?action=add")).apply {
                setClass(context, MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val registerResultIntent = Intent(Intent.ACTION_VIEW, Uri.parse("cantanhedehub://races")).apply {
                setClass(context, MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val contentFactoryIntent = Intent(Intent.ACTION_VIEW, Uri.parse("cantanhedehub://content")).apply {
                setClass(context, MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetTheme.MidnightBlue)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ATALHOS RÁPIDOS",
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
                        // Nova Prova
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(WidgetTheme.BorderGlass)
                                .padding(8.dp)
                                .clickable(actionStartActivity(addRaceIntent)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("➕", style = TextStyle(fontSize = 14.sp))
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = "Nova Prova",
                                    style = TextStyle(
                                        color = WidgetTheme.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Registar Resultado
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(WidgetTheme.BorderGlass)
                                .padding(8.dp)
                                .clickable(actionStartActivity(registerResultIntent)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏁", style = TextStyle(fontSize = 14.sp))
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = "Resultados",
                                    style = TextStyle(
                                        color = WidgetTheme.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(4.dp))

                        // Estúdio
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(WidgetTheme.BorderGlass)
                                .padding(8.dp)
                                .clickable(actionStartActivity(contentFactoryIntent)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📷", style = TextStyle(fontSize = 14.sp))
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = "Estúdio",
                                    style = TextStyle(
                                        color = WidgetTheme.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
