package com.ventgui.app.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.data.model.DetailedResult
import com.ventgui.app.ui.components.*

@Composable
fun DetailedResultsPopup(
    title: String,
    results: List<DetailedResult>,
    onDismiss: () -> Unit,
    accentColor: Color = CyberCyan
) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            color = accentColor,
            variant = "solid"
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.4f))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                if (results.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Nenhum resultado registado.", color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(results) { index, item ->
                            val isExpanded = expandedIndex == index
                            val rankColor = when (item.position) {
                                1 -> VividAmber
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> CyberCyan
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedIndex = if (isExpanded) null else index },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.03f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isExpanded) rankColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(rankColor.copy(alpha = 0.1f), CircleShape)
                                                .border(1.dp, rankColor.copy(alpha = 0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${item.position}º",
                                                color = rankColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.athleteName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = item.raceTitle,
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(top = 12.dp, start = 44.dp)
                                                .fillMaxWidth()
                                        ) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(bottom = 8.dp),
                                                color = Color.White.copy(alpha = 0.05f)
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.05f))
                                                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!item.athletePhotoUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = item.athletePhotoUrl,
                                                            contentDescription = "Avatar de ${item.athleteName}",
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(Icons.Rounded.Person, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Categoria: ${item.raceCategory}",
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 12.sp
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Data: ${item.raceDate}",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                            if (!item.raceLocation.isNullOrBlank()) {
                                                Text(
                                                    text = "Local: ${item.raceLocation}",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (!item.time.isNullOrBlank()) {
                                                Text(
                                                    text = "Tempo: ${item.time}",
                                                    color = CyberCyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
