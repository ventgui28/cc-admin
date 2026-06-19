package com.ventgui.app.ui.screens.races

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.data.model.Race
import com.ventgui.app.ui.components.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RaceListCard(
    race: Race, 
    onClick: () -> Unit, 
    onEditClick: () -> Unit, 
    onDeleteClick: () -> Unit,
    onAssociateAthleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        HyperGlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            onLongClick = { showMenu = true },
            borderColor = null,
            borderAlpha = 0.15f
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DirectionsBike, null, tint = if (race.status == "Concluída") NeonEmerald else CyberCyan, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(race.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(race.category, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier.size(3.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)); Spacer(modifier = Modifier.width(8.dp))
                        Text(race.status.uppercase(), color = if (race.status == "Concluída") NeonEmerald else CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    val dateStr = try { 
                        val instant = parseFlexibleDate(race.date)
                        if (instant != null) {
                            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                            "${localDate.dayOfMonth} ${localDate.month.getDisplayName(TextStyle.SHORT, Locale("pt", "PT")).uppercase()}"
                        } else race.date
                    } catch(e: Exception) { race.date }
                    Text(dateStr, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (!race.start_time.isNullOrBlank()) {
                        Text(race.start_time, color = CyberCyan.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MidnightBlue).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        ) {
            DropdownMenuItem(
                text = { Text("Associar atleta", color = Color.White) },
                onClick = {
                    showMenu = false
                    onAssociateAthleteClick()
                },
                leadingIcon = { Icon(Icons.Rounded.PersonAdd, null, tint = CyberCyan) }
            )
            DropdownMenuItem(
                text = { Text("Editar", color = Color.White) },
                onClick = {
                    showMenu = false
                    onEditClick()
                },
                leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = CyberCyan) }
            )
            DropdownMenuItem(
                text = { Text("Eliminar", color = Color(0xFFFF5252)) },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF5252)) }
            )
        }
    }
}
