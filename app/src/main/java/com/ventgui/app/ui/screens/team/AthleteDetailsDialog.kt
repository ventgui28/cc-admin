package com.ventgui.app.ui.screens.team

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ventgui.app.data.network.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.JoinedRaceResult
import com.ventgui.app.data.model.Race
import com.ventgui.app.ui.components.*

@Composable
fun StatMetricItem(label: String, value: String, sub: String, color: Color) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(sub, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PowerPoint(label: String, watts: String, wkg: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(watts, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(wkg, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DayBar(day: String, level: Float, color: Color = CyberCyan) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.width(10.dp).height(40.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(2.dp)), contentAlignment = Alignment.BottomCenter) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(level).background(color, RoundedCornerShape(2.dp)))
        }
        Text(day, color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun AthleteDetailsDialog(athlete: Athlete, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<Pair<JoinedRaceResult, Race>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(athlete.id) {
        scope.launch {
            try {
                isLoading = true
                val response = SupabaseClient.client.postgrest.from("race_results").select(Columns.raw("*, races(*)")) { filter { eq("athlete_id", athlete.id!!) } }
                val list = response.decodeList<JoinedRaceResult>()
                results = list.map { res -> 
                    res to (res.races ?: Race(title = "Desconhecida", date = "-", category = "-")) 
                }.sortedByDescending { pair -> pair.second.date }
            } catch (e: Exception) {} finally { isLoading = false }
        }
    }

    var showEnlargedPhoto by remember { mutableStateOf(false) }

    if (showEnlargedPhoto && !athlete.photo_url.isNullOrBlank()) {
        Dialog(onDismissRequest = { showEnlargedPhoto = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.95f))
                    .border(2.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = athlete.photo_url,
                    contentDescription = "Foto ampliada de ${athlete.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showEnlargedPhoto = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Fechar",
                        tint = Color.White
                    )
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            color = CyberCyan,
            variant = "solid"
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, CyberCyan.copy(alpha = 0.3f), CircleShape)
                            .then(
                                if (!athlete.photo_url.isNullOrBlank()) {
                                    Modifier.clickable { showEnlargedPhoto = true }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!athlete.photo_url.isNullOrBlank()) {
                            AsyncImage(
                                model = athlete.photo_url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Rounded.AccountCircle, null, tint = CyberCyan, modifier = Modifier.size(44.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(athlete.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(athlete.category.uppercase(), color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Stats Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AthleteStatCard(Modifier.weight(1f), "PROVAS", if (isLoading) "..." else results.size.toString(), Icons.Rounded.DirectionsBike)
                    AthleteStatCard(Modifier.weight(1f), "PÓDIOS", if (isLoading) "..." else results.count { (it.first.position ?: 99) <= 3 }.toString(), Icons.Rounded.EmojiEvents, Color(0xFFFFD600))
                    AthleteStatCard(Modifier.weight(1f), "VITÓRIAS", if (isLoading) "..." else results.count { it.first.position == 1 }.toString(), Icons.Rounded.WorkspacePremium, Color(0xFF00E676))
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("HISTÓRICO RECENTE", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = CyberCyan)
                    } else if (results.isEmpty()) {
                        Text("Sem registos competitivos", color = Color.White.copy(alpha = 0.2f), modifier = Modifier.align(Alignment.Center), fontSize = 14.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(results) { RaceHistoryItem(it.first, it.second) }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                PremiumButton(
                    text = "FECHAR DETALHES",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AthleteStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color = CyberCyan) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun RaceHistoryItem(result: JoinedRaceResult, race: Race) {
    val podiumColor = when(result.position) { 1 -> Color(0xFFFFD600); 2 -> Color(0xFFE0E0E0); 3 -> Color(0xFFCD7F32); else -> Color(0xFF80D8FF).copy(alpha = 0.4f) }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.03f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).background(podiumColor.copy(alpha = 0.15f), CircleShape).border(1.dp, podiumColor.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) { Text(result.position?.toString() ?: "-", color = podiumColor, fontWeight = FontWeight.Black, fontSize = 12.sp) }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(race.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Text("${race.date} • ${race.category}", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
        }
    }
}

