package com.ventgui.app.ui.screens.races

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.ui.components.*
import com.ventgui.app.ui.screens.dashboard.StatCard

@Composable
fun RaceStatsContent(
    selectedRaceResults: List<Pair<RaceResult, Athlete>>,
    onShowPodiumsDialog: () -> Unit,
    onShowAthletesDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 24.dp).padding(top = 16.dp)) {
        Text("ANÁLISE RÁPIDA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val top3 = selectedRaceResults.count { val pos = it.first.position ?: 99; pos in 1..3 }
            StatCard("Pódios", top3.toString(), Icons.Rounded.EmojiEvents, VividAmber, Modifier.weight(1f)) {
                onShowPodiumsDialog()
            }
            StatCard("Atletas", selectedRaceResults.size.toString(), Icons.Rounded.Groups, CyberCyan, Modifier.weight(1f)) {
                onShowAthletesDialog()
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val normalPositions = selectedRaceResults.mapNotNull { it.first.position }.filter { it > 0 }
            val avgPos = if (normalPositions.isNotEmpty()) normalPositions.average().toInt() else 0
            StatCard("Pos. Média", "#$avgPos", Icons.Rounded.Leaderboard, NeonEmerald, Modifier.weight(1f)) {}
            StatCard("Eficiência", if (selectedRaceResults.isNotEmpty()) "84%" else "0%", Icons.Rounded.Timeline, ElectricBlue, Modifier.weight(1f)) {}
        }
    }
}
