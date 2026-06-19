package com.ventgui.app.ui.screens.reports

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.R
import com.ventgui.app.data.utils.PdfReportGenerator
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.utils.UserLogger
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.ui.components.*
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    innerPadding: PaddingValues,
    onOpenDrawer: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    var startDate by rememberSaveable { mutableStateOf(LocalDate.now().minusMonths(6).toString()) }
    var endDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var locationQuery by rememberSaveable { mutableStateOf("") }
    
    var showPodiums by rememberSaveable { mutableStateOf(true) }
    var showGeneral by rememberSaveable { mutableStateOf(true) }
    var showTeams by rememberSaveable { mutableStateOf(true) }
    var showOnlyTop5 by rememberSaveable { mutableStateOf(false) }

    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }

    var generatedResults by remember { mutableStateOf<List<Pair<RaceResult, Athlete>>>(emptyList()) }
    var matchingRaces by remember { mutableStateOf<List<Race>>(emptyList()) }

    // Stats
    var totalKm by remember { mutableStateOf(0.0) }
    var avgPosition by remember { mutableStateOf(0.0) }
    var totalPodiums by remember { mutableStateOf(0) }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    showStartDatePicker = false
                }) { Text("OK", color = CyberCyan) }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("CANCELAR", color = Color.White.copy(alpha = 0.6f)) } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        endDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    }
                    showEndDatePicker = false
                }) { Text("OK", color = CyberCyan) }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("CANCELAR", color = Color.White.copy(alpha = 0.6f)) } }
        ) { DatePicker(state = datePickerState) }
    }

    fun generateReportData() {
        if (!isRefreshing) isLoading = true
        scope.launch {
            try {
                // Fetch all data in memory for rich stats compilation
                val allRaces = SupabaseClient.client.from("races").select().decodeList<Race>()
                val allResults = SupabaseClient.client.from("race_results").select().decodeList<RaceResult>()
                val allAthletes = SupabaseClient.client.from("athletes").select().decodeList<Athlete>()

                val startLocal = LocalDate.parse(startDate)
                val endLocal = LocalDate.parse(endDate)

                // Filter races
                val filteredRaces = allRaces.filter { race ->
                    val raceDate = try {
                        if (race.date.contains("T")) LocalDate.parse(race.date.substringBefore("T")) else LocalDate.parse(race.date)
                    } catch (e: Exception) {
                        null
                    }
                    val dateMatch = raceDate != null && !raceDate.isBefore(startLocal) && !raceDate.isAfter(endLocal)
                    val locationMatch = locationQuery.isBlank() || (race.location?.contains(locationQuery, ignoreCase = true) == true)
                    dateMatch && locationMatch
                }

                matchingRaces = filteredRaces

                // Filter results
                val raceIds = filteredRaces.mapNotNull { it.id }.toSet()
                val filteredResults = allResults.filter { res ->
                    res.race_id in raceIds
                }.mapNotNull { res ->
                    val athlete = allAthletes.firstOrNull { it.id == res.athlete_id }
                    if (athlete != null) res to athlete else null
                }.filter { (res, _) ->
                    if (showOnlyTop5) {
                        res.position != null && res.position!! in 1..5
                    } else {
                        val isPodium = res.position in 1..3
                        val isGeneral = res.position == null || res.position > 3
                        
                        if (showPodiums && isPodium) return@filter true
                        if (showGeneral && isGeneral) return@filter true
                        
                        false
                    }
                }.sortedWith(compareBy<Pair<RaceResult, Athlete>> { item ->
                    val r = filteredRaces.firstOrNull { it.id == item.first.race_id }
                    r?.date ?: ""
                }.thenBy { it.first.position ?: 999 })

                generatedResults = filteredResults

                // Compile stats
                totalKm = filteredRaces.sumOf { r ->
                    // Extract distance if defined
                    val cleanDesc = r.description ?: ""
                    val kmMatch = Regex("(\\d+(\\.\\d+)?)\\s*km").find(cleanDesc)
                    kmMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                }

                val rankedResults = filteredResults.filter { it.first.position != null && it.first.position!! > 0 }
                avgPosition = if (rankedResults.isNotEmpty()) rankedResults.map { it.first.position!! }.average() else 0.0
                totalPodiums = if (showOnlyTop5) {
                    filteredResults.count { it.first.position in 1..5 }
                } else {
                    filteredResults.count { it.first.position in 1..3 }
                }

                isLoading = false
                isRefreshing = false
                UserLogger.log("Gerou um relatório de conquistas de $startDate a $endDate")
            } catch (e: Exception) {
                isLoading = false
                isRefreshing = false
                Toast.makeText(context, "Erro ao carregar dados: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        PremiumMeshBackground()
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) { Icon(Icons.Rounded.Menu, null, tint = Color.White) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("C", color = MidnightBlue, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CANTANHEDE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("CYCLING HUB", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }

                Box(modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = stringResource(R.string.nav_reports),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Gere estatísticas e compile conquistas oficiais",
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    generateReportData()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                item {
                    HyperGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberCyan
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("CONFIGURAR FILTROS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Dates
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DATA INICIAL", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { showStartDatePicker = true }
                                            .padding(12.dp)
                                    ) {
                                        Text(startDate, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("DATA FINAL", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .clickable { showEndDatePicker = true }
                                            .padding(12.dp)
                                    ) {
                                        Text(endDate, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Location
                            PremiumTextField(
                                value = locationQuery,
                                onValueChange = { locationQuery = it },
                                label = "FILTRAR POR LOCALIZAÇÃO",
                                placeholder = "Ex: Portugal, Cantanhede...",
                                leadingIcon = Icons.Rounded.LocationOn
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Checkboxes
                            Text("CATEGORIAS DE RESULTADO", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = showPodiums,
                                        onCheckedChange = { showPodiums = it },
                                        enabled = !showOnlyTop5,
                                        colors = CheckboxDefaults.colors(checkedColor = CyberCyan, uncheckedColor = Color.White.copy(alpha = 0.4f))
                                    )
                                    Text("Pódios", color = if (showOnlyTop5) Color.White.copy(alpha = 0.3f) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = showGeneral,
                                        onCheckedChange = { showGeneral = it },
                                        enabled = !showOnlyTop5,
                                        colors = CheckboxDefaults.colors(checkedColor = CyberCyan, uncheckedColor = Color.White.copy(alpha = 0.4f))
                                    )
                                    Text("Geral", color = if (showOnlyTop5) Color.White.copy(alpha = 0.3f) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Checkbox(
                                        checked = showTeams,
                                        onCheckedChange = { showTeams = it },
                                        colors = CheckboxDefaults.colors(checkedColor = CyberCyan, uncheckedColor = Color.White.copy(alpha = 0.4f))
                                    )
                                    Text("Equipas", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = showOnlyTop5,
                                    onCheckedChange = { showOnlyTop5 = it },
                                    colors = CheckboxDefaults.colors(checkedColor = CyberCyan, uncheckedColor = Color.White.copy(alpha = 0.4f))
                                )
                                Text("Apenas Top 5 (1º ao 5º lugar)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            PremiumButton(
                                text = "GERAR RELATÓRIO",
                                onClick = { generateReportData() },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Rounded.BarChart
                            )
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CyberCyan)
                        }
                    }
                } else if (generatedResults.isNotEmpty() || matchingRaces.any { it.team_classification != null }) {
                    // Stats Grid
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (showOnlyTop5) "TOP 5" else "PÓDIOS", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$totalPodiums",
                                        color = CyberCyan,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1.3f),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("MÉDIA POSIÇÃO", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (avgPosition > 0) String.format("%.1fº", avgPosition) else "--",
                                        color = CyberCyan,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("KM TOTAL", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format("%.0f", totalKm),
                                        color = CyberCyan,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // Achievements listing
                    itemsIndexed(generatedResults) { index, item ->
                        val res = item.first
                        val athlete = item.second
                        val race = matchingRaces.firstOrNull { it.id == res.race_id }

                        if (race != null) {
                            val rank = res.position ?: 0
                            val isPodium = rank in 1..3
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(36.dp).background(if (isPodium) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
                                        if (isPodium) {
                                            Icon(Icons.Rounded.EmojiEvents, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text(if (rank > 0) "$rank" else "--", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(athlete.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("${race.title}  |  ${race.location ?: "N/A"}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                    }
                                    Text(res.time ?: "--:--", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Team Achievements
                    val teamAchievements = matchingRaces.filter { it.team_classification != null }
                    if (showTeams && teamAchievements.isNotEmpty()) {
                        item {
                            Text("CLASSIFICAÇÃO DE EQUIPA", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(top = 16.dp))
                        }
                        itemsIndexed(teamAchievements) { _, race ->
                            val rank = race.team_classification!!
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(36.dp).background(CyberCyan.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                                        Text("${rank}º", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Cantanhede Cycling Team", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(race.title, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Export actions
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumButton(
                                text = "COPIAR TEXTO",
                                onClick = {
                                    val formattedText = buildString {
                                        appendLine("🏆 RELATÓRIO DE CONQUISTAS - CANTANHEDE CYCLING HUB 🏆")
                                        appendLine("Período: $startDate a $endDate")
                                        if (locationQuery.isNotBlank()) appendLine("Localização: $locationQuery")
                                        appendLine("----------------------------------------")
                                        appendLine("📊 ESTATÍSTICAS ACUMULADAS:")
                                        appendLine("- ${if (showOnlyTop5) "Top 5" else "Pódios"}: $totalPodiums")
                                        appendLine("- Posição Média: ${if (avgPosition > 0) String.format("%.1fº", avgPosition) else "N/A"}")
                                        appendLine("- Distância Total: ${String.format("%.1f km", totalKm)}")
                                        appendLine("----------------------------------------")
                                        appendLine("🏁 CONQUISTAS INDIVIDUAIS:")
                                        generatedResults.forEach { (res, athlete) ->
                                            val race = matchingRaces.firstOrNull { it.id == res.race_id }
                                            if (race != null) {
                                                val pos = if (res.position != null) "${res.position}º" else res.time ?: "N/A"
                                                appendLine("- $pos lugar: ${athlete.name} em '${race.title}'")
                                            }
                                        }
                                        if (teamAchievements.isNotEmpty()) {
                                            appendLine("----------------------------------------")
                                            appendLine("👥 CLASSIFICAÇÕES COLETIVAS (EQUIPA):")
                                            teamAchievements.forEach { race ->
                                                appendLine("- ${race.team_classification}º lugar em '${race.title}'")
                                            }
                                        }
                                    }
                                    clipboardManager.setText(AnnotatedString(formattedText))
                                    Toast.makeText(context, "Texto copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.ContentCopy
                            )

                            PremiumButton(
                                text = "GERAR PDF",
                                onClick = {
                                    PdfReportGenerator.generateAndShareReport(
                                        context = context,
                                        startDate = startDate,
                                        endDate = endDate,
                                        location = locationQuery,
                                        races = matchingRaces,
                                        results = generatedResults,
                                        totalKm = totalKm,
                                        avgPosition = avgPosition,
                                        totalPodiums = totalPodiums,
                                        showOnlyTop5 = showOnlyTop5
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.PictureAsPdf,
                                variant = "solid"
                            )
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Configura os teus filtros e clica em Gerar Relatório.",
                                color = Color.White.copy(alpha = 0.2f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
