package com.ventgui.app.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.ui.components.*
import java.time.Instant
import java.time.ZoneId

@Composable
fun CountdownUnit(value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(unit, color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LiveStatRow(icon: ImageVector, label: String, value: String, unit: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(" $unit", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun QuickActionItem(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    HyperGlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = color
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.1f), CircleShape).border(1.dp, color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    HyperGlassCard(modifier = modifier.height(160.dp).clickable(onClick = onClick), color = color) {
        // Subtle Radial Glow
        Box(modifier = Modifier.size(120.dp).align(Alignment.Center).background(Brush.radialGradient(colors = listOf(color.copy(alpha = 0.15f), Color.Transparent))))

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.1f), CircleShape).border(1.dp, color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Text(text = label.uppercase(), color = color.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
fun ActivityItem(title: String, date: String, category: String, status: String, onClick: () -> Unit) {
    val statusColor = when (status) {
        "A decorrer" -> CyberCyan
        "Concluída" -> NeonEmerald
        "Agendada" -> VividAmber
        "Adiada" -> Color(0xFFFF5252)
        else -> Color.White
    }

    HyperGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        color = statusColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { 
                    Text(
                        text = date.split(" ")[0], 
                        color = statusColor, 
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    ) 
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title, 
                        color = Color.White, 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Black, 
                        maxLines = 1, 
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(text = category.uppercase(), color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            PremiumBadge(
                text = status,
                color = statusColor
            )
        }
    }
}
