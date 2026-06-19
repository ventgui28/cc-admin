package com.ventgui.app.ui.screens.team

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R
import coil.compose.AsyncImage
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.ui.components.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AthleteCardElite(
    athlete: Athlete, 
    onClick: () -> Unit, 
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        HyperGlassCard(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
            borderColor = null,
            borderAlpha = 0.15f
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
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
                            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(athlete.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val ageStr = try {
                                if (!athlete.birth_date.isNullOrBlank()) {
                                    val cleanDate = athlete.birth_date.substringBefore("T").trim()
                                    val birth = java.time.LocalDate.parse(cleanDate)
                                    "${java.time.Period.between(birth, java.time.LocalDate.now()).years} anos"
                                } else {
                                    "-- anos"
                                }
                            } catch(e: Exception) {
                                "-- anos"
                            }
                            Text("🇵🇹 POR", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(" • $ageStr", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val statusColor = when(athlete.status) {
                            "active" -> NeonEmerald
                            "developing" -> CyberCyan
                            "injured" -> Color(0xFFFF5252)
                            else -> Color.White.copy(alpha = 0.4f)
                        }
                        Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        val statusLabel = when (athlete.status) {
                            "active" -> stringResource(R.string.team_status_active)
                            "developing" -> stringResource(R.string.team_status_developing)
                            "injured" -> stringResource(R.string.team_status_injured)
                            else -> athlete.status
                        }
                        Text(statusLabel.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.4f))
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MidnightBlue).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
        ) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AthleteCardPremium(athlete: Athlete, isSelected: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    HyperGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = if (isSelected) CyberCyan else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) CyberCyan else CyberCyan.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.1f), CircleShape), 
                contentAlignment = Alignment.Center
            ) { 
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check, 
                        null, 
                        tint = MidnightBlue, 
                        modifier = Modifier.size(28.dp)
                    )
                } else if (!athlete.photo_url.isNullOrBlank()) {
                    AsyncImage(
                        model = athlete.photo_url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.Person, 
                        null, 
                        tint = CyberCyan, 
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = athlete.name, 
                    color = Color.White, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = athlete.category.uppercase(), 
                    color = CyberCyan, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.2f))
        }
    }
}

