package com.ventgui.app.ui.screens.sponsors

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.utils.UserLogger
import com.ventgui.app.data.model.Sponsor
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.ventgui.app.ui.components.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsScreen(
    innerPadding: PaddingValues, 
    onSelectionModeChange: (Boolean) -> Unit = {},
    onOpenDrawer: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sponsors by remember { mutableStateOf<List<Sponsor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var selectedSponsorIds by remember { mutableStateOf(setOf<String>()) }
    val isInSelectionMode = selectedSponsorIds.isNotEmpty()

    LaunchedEffect(isInSelectionMode) {
        onSelectionModeChange(isInSelectionMode)
    }

    val fetchSponsors = {
        scope.launch {
            try {
                if (!isRefreshing) isLoading = true
                sponsors = SupabaseClient.client.postgrest.from("sponsors")
                    .select()
                    .decodeList<Sponsor>()
            } catch (e: Exception) {
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchSponsors() }

    Box(modifier = Modifier.fillMaxSize()) {
        PremiumMeshBackground()
        
        Column(
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- HEADER ---
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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

                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(
                            onClick = { },
                            modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) { Icon(Icons.Rounded.NotificationsNone, null, tint = Color.White) }
                        Box(modifier = Modifier.padding(4.dp).size(10.dp).background(CyberCyan, CircleShape).border(2.dp, MidnightBlue, CircleShape))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (isInSelectionMode) stringResource(R.string.sponsors_selected_count, selectedSponsorIds.size) else stringResource(R.string.sponsors_title),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (isInSelectionMode) stringResource(R.string.sponsors_select_partners) else stringResource(R.string.sponsors_subtitle),
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && sponsors.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyberCyan)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        fetchSponsors()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.heightIn(max = 2000.dp), // Adjust or use nested scrolling solution
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            userScrollEnabled = false
                        ) {
                            itemsIndexed(sponsors) { index, sponsor ->
                                val isSelected = sponsor.id?.let { selectedSponsorIds.contains(it) } ?: false
                                SponsorGridItem(
                                    sponsor = sponsor,
                                    isSelected = isSelected,
                                    onClick = { id ->
                                        if (isInSelectionMode) {
                                            selectedSponsorIds = if (selectedSponsorIds.contains(id)) selectedSponsorIds - id else selectedSponsorIds + id
                                        }
                                    },
                                    onLongClick = { id -> if (!isInSelectionMode) selectedSponsorIds = setOf(id) }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        BecomePartnerCard()
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }
        }

        // Selection Action Bar
        AnimatedVisibility(
            visible = isInSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        ) {
            HyperGlassCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(80.dp),
                color = Color(0xFFFF5252)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.sponsors_selected_bar_title, selectedSponsorIds.size), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text(stringResource(R.string.common_irreversible_action), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selectedSponsorIds = emptySet() }) {
                            Text(stringResource(R.string.common_cancel), color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { 
                                scope.launch {
                                    try {
                                        selectedSponsorIds.forEach { id ->
                                            val deletedSponsor = sponsors.find { it.id == id }
                                            val name = deletedSponsor?.name ?: "Parceiro"
                                            SupabaseClient.client.postgrest.from("sponsors").delete { filter { eq("id", id) } }
                                            UserLogger.log("Eliminou o parceiro $name")
                                        }
                                        selectedSponsorIds = emptySet()
                                        fetchSponsors()
                                    } catch (e: Exception) {}
                                }
                            },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) { Icon(Icons.Rounded.Delete, null, tint = Color.White) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SponsorGridItem(sponsor: Sponsor, isSelected: Boolean, onClick: (String) -> Unit, onLongClick: (String) -> Unit) {
    HyperGlassCard(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { onClick(sponsor.id!!) },
                onLongClick = { onLongClick(sponsor.id!!) }
            ),
        borderAlpha = if (isSelected) 0.8f else 0.1f,
        borderColor = if (isSelected) CyberCyan else Color.White
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = sponsor.logo_url ?: "https://ui-avatars.com/api/?name=${sponsor.name}&background=random",
                contentDescription = sponsor.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            if (isSelected) {
                Box(modifier = Modifier.fillMaxSize().background(CyberCyan.copy(alpha = 0.2f)))
                Icon(Icons.Rounded.CheckCircle, null, tint = CyberCyan, modifier = Modifier.align(Alignment.TopEnd).size(16.dp))
            }
        }
    }
}

@Composable
fun BecomePartnerCard() {
    HyperGlassCard(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable { },
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(CyberCyan.copy(alpha = 0.1f), CircleShape).border(1.dp, CyberCyan.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Handshake, null, tint = CyberCyan, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.sponsors_become_partner), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.sponsors_become_partner_desc), color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = CyberCyan)
        }
    }
}
