package com.ventgui.app.ui.screens.races

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.R
import com.ventgui.app.data.model.Race
import com.ventgui.app.ui.components.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RaceDetailContent(
    race: Race,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    raceWeatherTemp: String,
    raceWeatherDesc: String,
    onFinishClick: () -> Unit,
    onReactivateClick: () -> Unit,
    parseFlexibleDate: (String) -> Instant?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.races_details), color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(race.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    val formattedCategory = race.category.split(", ").joinToString(" & ")
                    Text("🇵🇹 $formattedCategory", color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                
                // Render active subcategories (escalões)
                if (race.sub_categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        race.sub_categories.forEach { subCat ->
                            val categoryPrefix = if (subCat.startsWith("BTT ")) "BTT: " else if (subCat.startsWith("Estrada ")) "Estrada: " else if (subCat.startsWith("Pista ")) "Pista: " else ""
                            val displayName = subCat.substringAfter("BTT ").substringAfter("Estrada ").substringAfter("Pista ")
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = "$categoryPrefix$displayName",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (race.gender?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (race.gender.contains(":")) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            race.gender.split(", ").forEach { part ->
                                val splitPart = part.split(": ")
                                if (splitPart.size == 2) {
                                    val cat = splitPart[0]
                                    val gen = splitPart[1]
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(CyberCyan, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$cat: $gen",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(CyberCyan, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "GÉNERO: ${race.gender}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarToday, null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val formattedDate = try {
                            val instant = parseFlexibleDate(race.date)
                            if (instant != null) {
                                val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                                "${localDate.dayOfMonth} ${localDate.month.getDisplayName(TextStyle.SHORT, Locale("pt", "PT")).uppercase()} ${localDate.year}"
                            } else race.date
                        } catch(e: Exception) { race.date }
                        val timeSuffix = if (!race.start_time.isNullOrBlank()) " às ${race.start_time}" else ""
                        Text("$formattedDate$timeSuffix", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!race.location.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(race.location, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (!race.link.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val uriHandler = LocalUriHandler.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCyan.copy(alpha = 0.1f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val formattedUrl = if (!race.link.startsWith("http://") && !race.link.startsWith("https://")) {
                                        "https://${race.link}"
                                    } else {
                                        race.link
                                    }
                                    uriHandler.openUri(formattedUrl)
                                } catch (e: Exception) {}
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Link, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VER REGULAMENTO / LINK", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                InfoPillCard(Icons.Rounded.EmojiEvents, race.status.uppercase(), "ESTADO")
                if (race.status == "Concluída") {
                    InfoPillCard(
                        icon = Icons.Rounded.Group, 
                        value = if (race.team_classification != null) "${race.team_classification}º" else "--", 
                        label = "EQUIPA"
                    )
                } else {
                    val isRainy = raceWeatherDesc.contains("Chuvoso") || raceWeatherDesc.contains("Rain")
                    InfoPillCard(if (isRainy) Icons.Rounded.CloudQueue else Icons.Rounded.Cloud, raceWeatherTemp, raceWeatherDesc)
                }
            }
        }

        if (race.status != "Concluída") {
            Spacer(modifier = Modifier.height(16.dp))
            PremiumButton(
                text = stringResource(R.string.races_finish_race),
                onClick = onFinishClick,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Rounded.CheckCircle
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumButton(
                    text = "Editar Resultados",
                    onClick = onFinishClick,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Edit
                )
                PremiumButton(
                    text = "Reativar Prova",
                    onClick = onReactivateClick,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Undo,
                    variant = "outline"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        RaceTabSwitcher(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }
}
