package com.ventgui.app.ui.screens.races

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.data.model.Race
import com.ventgui.app.ui.components.CyberCyan
import com.ventgui.app.ui.components.HyperGlassCard
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarWidget(races: List<Race>) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstOfMonth = currentMonth.atDay(1)
    val offset = firstOfMonth.dayOfWeek.value - 1
    val previousMonth = currentMonth.minusMonths(1)
    val daysInPrevMonth = previousMonth.lengthOfMonth()
    val days = mutableListOf<LocalDate>()
    for (i in offset - 1 downTo 0) { days.add(previousMonth.atDay(daysInPrevMonth - i)) }
    for (i in 1..daysInMonth) { days.add(currentMonth.atDay(i)) }
    val nextMonth = currentMonth.plusMonths(1)
    var nextDay = 1
    while (days.size < 42) { days.add(nextMonth.atDay(nextDay++)) }

    HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("CALENDÁRIO DE PROVAS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text(currentMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "PT")).uppercase() + " " + currentMonth.year, color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White.copy(alpha = 0.6f)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.6f)) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM").forEach { day -> Text(day, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    week.forEach { date ->
                        val isCurrentMonth = date.month == currentMonth.month && date.year == currentMonth.year
                        val isToday = date == LocalDate.now()
                        val hasEvent = races.any { try { Instant.parse(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == date } catch(e: Exception) { false } }
                        HyperGlassCard(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                            onClick = { },
                            variant = "glass",
                            cornerRadius = 8,
                            borderColor = if (isToday) CyberCyan else if (hasEvent) CyberCyan.copy(alpha = 0.4f) else Color.White,
                            borderAlpha = if (isToday) 0.8f else if (hasEvent) 0.3f else 0.05f
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(if (isToday) CyberCyan.copy(alpha = 0.15f) else if (hasEvent) Color.White.copy(alpha = 0.08f) else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(date.dayOfMonth.toString(), color = if (isToday) CyberCyan else if (hasEvent) Color.White else if (!isCurrentMonth) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = if (isToday || hasEvent) FontWeight.Black else FontWeight.Bold)
                                    if (hasEvent) { Spacer(modifier = Modifier.height(2.dp)); Box(modifier = Modifier.size(4.dp).background(CyberCyan, CircleShape)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
