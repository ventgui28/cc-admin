package com.ventgui.app.ui.screens.races

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ventgui.app.R
import com.ventgui.app.ui.components.*
import java.time.Instant
import java.time.ZoneId

// Helper to parse date flexibly
fun parseFlexibleDate(dateStr: String): Instant? {
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
        } catch (e2: Exception) { null }
    }
}

@Composable
fun InfoPillCard(icon: ImageVector, value: String, label: String, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier
            .width(130.dp)
            .height(56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = label.uppercase(),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun RaceTabSwitcher(selectedTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf("NOTES", "RESULTS", "STATS")
    Surface(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(if (isSelected) CyberCyan else Color.Transparent).clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    val tabLabel = when (tab) {
                        "NOTES" -> "Notas"
                        "RESULTS" -> stringResource(R.string.races_tab_results)
                        "STATS" -> stringResource(R.string.races_tab_stats)
                        else -> tab
                    }
                    Text(
                        text = tabLabel.uppercase(),
                        color = if (isSelected) MidnightBlue else Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PodiumResultItem(
    rank: Int,
    name: String,
    category: String,
    time: String,
    isSelected: Boolean = false,
    photoUrl: String? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val bgGradient = Brush.horizontalGradient(listOf(if (rank == 1) VividAmber.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f), Color.Transparent))
    HyperGlassCard(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        borderColor = if (isSelected) CyberCyan else null,
        borderAlpha = if (isSelected) 0.8f else 0.15f
    ) {
        val overlayColor = if (isSelected) CyberCyan.copy(alpha = 0.12f) else Color.Transparent
        Box(modifier = Modifier.fillMaxSize().background(bgGradient).background(overlayColor)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val rankColor = when (rank) {
                    1 -> VividAmber
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> CyberCyan
                }
                Box(modifier = Modifier.size(44.dp).background(rankColor.copy(alpha = 0.1f), CircleShape).border(1.dp, rankColor.copy(alpha = 0.3f), CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Avatar de $name",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val isStatus = time in listOf("DNF", "DNS", "DSQ", "OTL")
                        val displayText = if (isStatus) time else if (rank > 0) "$rank" else "—"
                        Text(
                            text = displayText, 
                            color = if (isStatus) Color(0xFFFF5252) else rankColor, 
                            fontSize = if (isStatus) 12.sp else 20.sp, 
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text("🇵🇹 $category", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val isStatus = time in listOf("DNF", "DNS", "DSQ", "OTL")
                    Text(
                        text = if (isStatus) time else "${rank}º LUGAR", 
                        color = if (isStatus) Color(0xFFFF5252) else rankColor, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    if (!isStatus && time.isNotBlank()) {
                        Text(time, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AthleteStartListItem(
    name: String,
    category: String,
    isSelected: Boolean = false,
    photoUrl: String? = null,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    HyperGlassCard(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        borderColor = if (isSelected) CyberCyan else null,
        borderAlpha = if (isSelected) 0.8f else 0.15f
    ) {
        val overlayColor = if (isSelected) CyberCyan.copy(alpha = 0.12f) else Color.Transparent
        Box(modifier = Modifier.fillMaxSize().background(overlayColor)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).background(if (isSelected) CyberCyan else Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.1f), CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) { 
                    if (isSelected) {
                        Icon(Icons.Rounded.Check, null, tint = MidnightBlue, modifier = Modifier.size(24.dp))
                    } else if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Avatar de $name",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text("🇵🇹 $category", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
