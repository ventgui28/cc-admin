package com.ventgui.app.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.utils.UserLog
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R

import com.ventgui.app.AppDestinations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import com.ventgui.app.ui.components.*
import com.ventgui.app.data.model.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow

import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    innerPadding: PaddingValues, 
    onLogout: () -> Unit, 
    onNavigateToProfile: () -> Unit,
    onNavigateToSection: (AppDestinations, String?) -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val infiniteTransition = rememberInfiniteTransition(label = "weatherPulse")
    val weatherScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weatherScale"
    )
    val currentUser = SupabaseClient.client.auth.currentUserOrNull()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val profile = uiState.profile
    val nextRace = uiState.nextRace
    val socialPosts = uiState.socialPosts
    val isLoading = uiState.isLoading
    val totalAthletes = uiState.totalAthletes
    val totalRaces = uiState.totalRaces
    val totalPodiums = uiState.totalPodiums
    val totalVictories = uiState.totalVictories
    val podiumsList = uiState.podiumsList
    val victoriesList = uiState.victoriesList
    val temp = uiState.temp
    val weatherDescResId = uiState.weatherDescResId
    val isRefreshing = uiState.isRefreshing

    var showPodiumsPopup by remember { mutableStateOf(false) }
    var showVictoriesPopup by remember { mutableStateOf(false) }
    
    // UI States
    var showNotifications by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf<Triple<Long, Long, Long>?>(null) }

    // Helper to parse date flexibly
    fun parseFlexibleDate(dateStr: String): Instant? {
        if (dateStr.isBlank()) return null
        return try {
            // Try ISO format
            Instant.parse(dateStr)
        } catch (e: Exception) {
            try {
                // Try format "16 May 2026" or similar
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

    LaunchedEffect(currentUser) {
        viewModel.loadDashboard(currentUser?.id)
    }

    LaunchedEffect(nextRace) {
        if (nextRace != null) {
            val raceInstant = parseFlexibleDate(nextRace.date)
            if (raceInstant != null) {
                while (true) {
                    val diff = java.time.Duration.between(Instant.now(), raceInstant)
                    if (diff.isNegative) {
                        timeRemaining = Triple(0L, 0L, 0L)
                        break
                    }
                    timeRemaining = Triple(diff.toDays(), diff.toHours() % 24, diff.toMinutes() % 60)
                    kotlinx.coroutines.delay(60000)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PremiumMeshBackground()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.loadDashboard(currentUser?.id, isRefresh = true)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

            // --- TOP HEADER ---
            PremiumTopBar(
                onLeftClick = onOpenDrawer,
                onRightClick = { showNotifications = true },
                rightIcon = Icons.Rounded.Notifications,
                hasRightBadge = socialPosts.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- WELCOME & WEATHER ---
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_welcome), color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                    val displayName = profile?.full_name?.split(" ")?.firstOrNull() ?: stringResource(R.string.dashboard_rider)
                    Text(displayName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.dashboard_motto), color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                HyperGlassCard(modifier = Modifier.size(width = 130.dp, height = 90.dp)) {
                    Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val isRainy = weatherDescResId == R.string.dashboard_weather_rainy || 
                                      weatherDescResId == R.string.dashboard_weather_drizzle || 
                                      weatherDescResId == R.string.dashboard_weather_stormy
                        Icon(
                            imageVector = if (isRainy) Icons.Rounded.CloudQueue else Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = weatherScale
                                    scaleY = weatherScale
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(temp, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(stringResource(R.string.dashboard_weather_cantanhede), color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(weatherDescResId), color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- STATS OVERVIEW CARD ---
            StaggeredFadeInItem(index = 1) {
                HyperGlassCard(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val teamInteractionSource = remember { MutableInteractionSource() }
                        val isTeamPressed by teamInteractionSource.collectIsPressedAsState()
                        val teamScale by animateFloatAsState(if (isTeamPressed) 0.92f else 1f, label = "teamScale")

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = teamScale
                                    scaleY = teamScale
                                }
                                .clickable(
                                    interactionSource = teamInteractionSource,
                                    indication = null,
                                    onClick = { onNavigateToSection(AppDestinations.TEAM, null) }
                                )
                        ) {
                            Icon(Icons.Rounded.Groups, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(totalAthletes.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("ATLETAS", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.08f)))

                        val racesInteractionSource = remember { MutableInteractionSource() }
                        val isRacesPressed by racesInteractionSource.collectIsPressedAsState()
                        val racesScale by animateFloatAsState(if (isRacesPressed) 0.92f else 1f, label = "racesScale")

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = racesScale
                                    scaleY = racesScale
                                }
                                .clickable(
                                    interactionSource = racesInteractionSource,
                                    indication = null,
                                    onClick = { onNavigateToSection(AppDestinations.RACES, null) }
                                )
                        ) {
                            Icon(Icons.Rounded.DirectionsBike, null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(totalRaces.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("PROVAS", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.08f)))

                        val podiumsInteractionSource = remember { MutableInteractionSource() }
                        val isPodiumsPressed by podiumsInteractionSource.collectIsPressedAsState()
                        val podiumsScale by animateFloatAsState(if (isPodiumsPressed) 0.92f else 1f, label = "podiumsScale")

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = podiumsScale
                                    scaleY = podiumsScale
                                }
                                .clickable(
                                    interactionSource = podiumsInteractionSource,
                                    indication = null,
                                    onClick = { showPodiumsPopup = true }
                                )
                        ) {
                            Icon(Icons.Rounded.EmojiEvents, null, tint = VividAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(totalPodiums.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("PÓDIOS", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.08f)))

                        val victoriesInteractionSource = remember { MutableInteractionSource() }
                        val isVictoriesPressed by victoriesInteractionSource.collectIsPressedAsState()
                        val victoriesScale by animateFloatAsState(if (isVictoriesPressed) 0.92f else 1f, label = "victoriesScale")

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = victoriesScale
                                    scaleY = victoriesScale
                                }
                                .clickable(
                                    interactionSource = victoriesInteractionSource,
                                    indication = null,
                                    onClick = { showVictoriesPopup = true }
                                )
                        ) {
                            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(totalVictories.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("VITÓRIAS", color = Color.White.copy(alpha = 0.4f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- NEXT RACE CARD ---
            StaggeredFadeInItem(index = 2) {
                val currentRace = nextRace ?: com.ventgui.app.data.model.Race(title = stringResource(R.string.dashboard_no_upcoming_races), date = "", category = stringResource(R.string.dashboard_relax_train))
                HyperGlassCard(modifier = Modifier.fillMaxWidth().wrapContentHeight(), color = if (nextRace != null) ElectricBlue else Color.Gray.copy(alpha = 0.2f)) {
                    Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Flag, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.dashboard_next_race), color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(currentRace.category.uppercase(), color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(currentRace.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 32.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        if (currentRace.date.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CalendarToday, null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                val formattedDate = try {
                                    val instant = parseFlexibleDate(currentRace.date)
                                    if (instant != null) {
                                        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                                        "${localDate.dayOfMonth} ${localDate.month.getDisplayName(TextStyle.SHORT, Locale("pt", "PT")).uppercase()} ${localDate.year}"
                                    } else currentRace.date
                                } catch(e: Exception) { currentRace.date }
                                Text(formattedDate, color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        if (timeRemaining != null && nextRace != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                                CountdownUnit(timeRemaining!!.first.toString().padStart(2, '0'), stringResource(R.string.dashboard_days))
                                CountdownUnit(timeRemaining!!.second.toString().padStart(2, '0'), stringResource(R.string.dashboard_hrs))
                                CountdownUnit(timeRemaining!!.third.toString().padStart(2, '0'), stringResource(R.string.dashboard_min))
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        Button(onClick = { onNavigateToSection(AppDestinations.RACES, nextRace?.id) }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.dashboard_view_details), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                Icon(Icons.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }

        // --- NOTIFICATIONS DIALOG ---
        if (showNotifications) {
            Dialog(onDismissRequest = { showNotifications = false }) {
                HyperGlassCard(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.dashboard_notifications), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            IconButton(onClick = { showNotifications = false }) { Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.4f)) }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        if (socialPosts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.dashboard_no_new_alerts), color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(socialPosts) { post ->
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).background(CyberCyan.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Newspaper, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(post.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("${post.content.take(60)}...", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, maxLines = 2)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { showNotifications = false }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = MidnightBlue), shape = RoundedCornerShape(12.dp)) {
                            Text(stringResource(R.string.dashboard_dismiss_all), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        if (showPodiumsPopup) {
            DetailedResultsPopup(
                title = "Pódios da Equipa",
                results = podiumsList,
                onDismiss = { showPodiumsPopup = false },
                accentColor = VividAmber
            )
        }

        if (showVictoriesPopup) {
            DetailedResultsPopup(
                title = "Vitórias da Equipa",
                results = victoriesList,
                onDismiss = { showVictoriesPopup = false },
                accentColor = Color(0xFF00E676)
            )
        }
    }
}
