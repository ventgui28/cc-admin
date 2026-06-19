package com.ventgui.app.ui.screens.updates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.R
import com.ventgui.app.data.utils.AppUpdateInfo
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.ui.components.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    innerPadding: PaddingValues,
    onOpenDrawer: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var updatesList by remember { mutableStateOf<List<AppUpdateInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val fetchUpdates = {
        scope.launch {
            try {
                if (!isRefreshing) isLoading = true
                val result = SupabaseClient.client.postgrest.from("app_updates")
                    .select()
                    .decodeList<AppUpdateInfo>()
                // Sort by version_code descending (latest version first)
                updatesList = result.sortedByDescending { it.version_code }
            } catch (e: Exception) {
                // Fail silently
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchUpdates()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PremiumMeshBackground()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                fetchUpdates()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
            item {
                Column(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding())
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- TOP HEADER ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Menu, null, tint = Color.White)
                        }

                        // BRAND LOGO
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("C", color = MidnightBlue, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CANTANHEDE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text("CYCLING HUB", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }

                        // Placeholder to balance header
                        Box(modifier = Modifier.size(44.dp))
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Title
                    Column {
                        Text(
                            text = "Histórico",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Atualizações",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Registo completo de melhorias e novas versões.",
                            color = CyberCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyberCyan)
                    }
                }
            } else if (updatesList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma atualização registada na base de dados.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                items(updatesList) { update ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        UpdateCard(update = update)
                    }
                }
            }
        }
    }
    }
}

@Composable
fun UpdateCard(update: AppUpdateInfo) {
    HyperGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, CyberCyan.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Versão ${update.version_name}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Surface(
                    color = if (update.is_mandatory) Color(0xFFFF5252).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (update.is_mandatory) Color(0xFFFF5252).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "CODE ${update.version_code}",
                        color = if (update.is_mandatory) Color(0xFFFF5252) else CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Release Notes Text
            val notes = update.release_notes ?: "Sem notas de lançamento disponíveis."
            
            // Format notes: if they contain pipe character |, show as bullet points
            if (notes.contains("|")) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    notes.split("|").map { it.trim() }.filter { it.isNotEmpty() }.forEach { bullet ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", color = CyberCyan, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = bullet.removePrefix("-").trim(),
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = notes,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // Date formatting if present
            update.created_at?.let { dateStr ->
                Spacer(modifier = Modifier.height(16.dp))
                val displayDate = try {
                    val instant = Instant.parse(dateStr)
                    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                    val month = localDate.month.getDisplayName(TextStyle.SHORT, Locale("pt", "PT")).uppercase()
                    "${localDate.dayOfMonth} $month ${localDate.year}"
                } catch (e: Exception) {
                    ""
                }
                if (displayDate.isNotEmpty()) {
                    Text(
                        text = displayDate,
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
