package com.ventgui.app.ui.screens.races

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R
import androidx.compose.ui.text.input.KeyboardType
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.utils.UserLogger
import com.ventgui.app.data.model.*
import com.ventgui.app.ui.screens.dashboard.StatCard
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ventgui.app.ui.components.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import com.ventgui.app.ui.components.*
import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

val RaceSaver = Saver<Race?, String>(
    save = { race -> if (race != null) Json.encodeToString(race) else "" },
    restore = { jsonStr -> if (jsonStr.isNotEmpty()) Json.decodeFromString<Race>(jsonStr) else null }
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProvasScreen(
    innerPadding: PaddingValues, 
    onSelectionModeChange: (Boolean) -> Unit = {},
    initialOpenAddDialog: Boolean = false,
    onDialogOpened: () -> Unit = {},
    onOpenDrawer: () -> Unit,
    viewModel: RacesViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val races = uiState.races
    val isLoading = uiState.isLoading
    val isRefreshing = uiState.isRefreshing
    val allAthletes = uiState.allAthletes
    val selectedRaceResults = uiState.selectedRaceResults
    val raceWeatherTemp = uiState.raceWeatherTemp
    val raceWeatherDesc = uiState.raceWeatherDesc
    val isFetchingDetails = uiState.isFetchingDetails

    var selectedTab by rememberSaveable { mutableStateOf("RESULTS") }
    var selectedRace by rememberSaveable(stateSaver = RaceSaver) { mutableStateOf<Race?>(null) }
    var selectedRaces by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showFormDialog by rememberSaveable { mutableStateOf(false) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var editingRace by rememberSaveable(stateSaver = RaceSaver) { mutableStateOf<Race?>(null) }
    var raceToDelete by rememberSaveable(stateSaver = RaceSaver) { mutableStateOf<Race?>(null) }
    var initialFormStep by rememberSaveable { mutableStateOf(1) }
    
    var initialSelectedAthleteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingSelectedAthletes by remember { mutableStateOf(false) }
    
    var selectedAthleteIdsForBulkAction by remember { mutableStateOf(setOf<String>()) }
    var showPodiumsDialog by remember { mutableStateOf(false) }
    var showAthletesDialog by remember { mutableStateOf(false) }
    
    val isInSelectionMode = selectedRaces.isNotEmpty()

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

    LaunchedEffect(editingRace) {
        if (editingRace != null) {
            isLoadingSelectedAthletes = true
            initialSelectedAthleteIds = viewModel.getSelectedAthleteIdsForRace(editingRace!!.id!!)
            isLoadingSelectedAthletes = false
        } else {
            initialSelectedAthleteIds = emptySet()
        }
    }

    LaunchedEffect(Unit) {
        onSelectionModeChange(false)
        viewModel.loadRacesAndAthletes()
    }

    LaunchedEffect(selectedRace) {
        selectedAthleteIdsForBulkAction = emptySet()
        if (selectedRace != null) {
            viewModel.fetchDetailsForRace(selectedRace!!)
        }
    }

    LaunchedEffect(isInSelectionMode) {
        onSelectionModeChange(isInSelectionMode)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PremiumMeshBackground()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.loadRacesAndAthletes(isRefresh = true)
                if (selectedRace != null) {
                    viewModel.fetchDetailsForRace(selectedRace!!)
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
            item {
                Column(modifier = Modifier.padding(top = innerPadding.calculateTopPadding()).padding(horizontal = 24.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- TOP HEADER ---
                    PremiumTopBar(
                        onLeftClick = { 
                            if (selectedRace != null) {
                                selectedRace = null 
                            } else {
                                onOpenDrawer()
                            }
                        },
                        showBackArrow = selectedRace != null,
                        onRightClick = if (selectedRace == null) {
                            {
                                initialFormStep = 1
                                showFormDialog = true 
                            }
                        } else null,
                        rightIcon = if (selectedRace == null) Icons.Rounded.Add else null
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (selectedRace != null) {
                        RaceDetailContent(
                            race = selectedRace!!,
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            raceWeatherTemp = raceWeatherTemp,
                            raceWeatherDesc = raceWeatherDesc,
                            onFinishClick = { showFinishDialog = true },
                            onReactivateClick = {
                                viewModel.reactivateRace(selectedRace!!, selectedRaceResults) { success ->
                                    if (success) {
                                        selectedRace = selectedRace!!.copy(status = "Agendada")
                                    }
                                }
                            },
                            parseFlexibleDate = ::parseFlexibleDate
                        )
                    } else {
                        // --- GLOBAL VIEW HEADER ---
                        Column {
                            Text("Calendário", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            Text("Acompanha o desempenho da equipa em todas as provas.", color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (selectedRace == null) {
                // --- GLOBAL VIEW CONTENT ---
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 24.dp)) {
                        CalendarWidget(races = races)
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(stringResource(R.string.races_upcoming_races), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        races.forEach { race ->
                            RaceListCard(
                                race = race,
                                onClick = { selectedRace = race },
                                onEditClick = {
                                    editingRace = race
                                    initialFormStep = 1
                                    showFormDialog = true
                                },
                                onDeleteClick = {
                                    raceToDelete = race
                                },
                                onAssociateAthleteClick = {
                                    editingRace = race
                                    initialFormStep = 3
                                    showFormDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else {
                // --- SELECTED RACE CONTENT ---
                if (selectedTab == "NOTES") {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp)) {
                            if (!selectedRace!!.description.isNullOrBlank()) {
                                HyperGlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "NOTAS DA PROVA",
                                            color = CyberCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = selectedRace!!.description!!,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                HyperGlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                                    Column(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "NOTAS DA PROVA",
                                            color = CyberCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.align(Alignment.Start)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Icon(
                                            Icons.Rounded.Description,
                                            null,
                                            tint = Color.White.copy(alpha = 0.1f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Nenhuma nota registada para esta prova.",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                if (selectedTab == "RESULTS") {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp)) {
                            val isConcluded = selectedRace!!.status == "Concluída"
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isConcluded) "CLASSIFICAÇÃO FINAL" else "ATLETAS INSCRITOS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(if (selectedRaceResults.isNotEmpty()) NeonEmerald else Color.Gray, CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isConcluded) "RESULTADOS OFICIAIS" else "LISTA DE PARTIDA", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (isFetchingDetails) {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CyberCyan)
                                }
                            } else if (selectedRaceResults.isEmpty()) {
                                HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                        Text(if (isConcluded) "Nenhum resultado registado para esta prova." else "Nenhum atleta inscrito nesta prova.", color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                selectedRaceResults.forEach { (res, athlete) ->
                                    val isSelected = selectedAthleteIdsForBulkAction.contains(athlete.id)
                                    if (isConcluded) {
                                        PodiumResultItem(
                                            rank = res.position ?: 0,
                                            name = athlete.name,
                                            category = athlete.category,
                                            time = res.time ?: "--:--",
                                            isSelected = isSelected,
                                            photoUrl = athlete.photo_url,
                                            onClick = {
                                                selectedAthleteIdsForBulkAction = if (isSelected) {
                                                    selectedAthleteIdsForBulkAction - athlete.id!!
                                                } else {
                                                    selectedAthleteIdsForBulkAction + athlete.id!!
                                                }
                                            },
                                            onLongClick = {
                                                selectedAthleteIdsForBulkAction = if (isSelected) {
                                                    selectedAthleteIdsForBulkAction - athlete.id!!
                                                } else {
                                                    selectedAthleteIdsForBulkAction + athlete.id!!
                                                }
                                            }
                                        )
                                    } else {
                                        AthleteStartListItem(
                                            name = athlete.name,
                                            category = athlete.category,
                                            isSelected = isSelected,
                                            photoUrl = athlete.photo_url,
                                            onClick = {
                                                selectedAthleteIdsForBulkAction = if (isSelected) {
                                                    selectedAthleteIdsForBulkAction - athlete.id!!
                                                } else {
                                                    selectedAthleteIdsForBulkAction + athlete.id!!
                                                }
                                            },
                                            onLongClick = {
                                                selectedAthleteIdsForBulkAction = if (isSelected) {
                                                    selectedAthleteIdsForBulkAction - athlete.id!!
                                                } else {
                                                    selectedAthleteIdsForBulkAction + athlete.id!!
                                                }
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }

                if (selectedTab == "STATS") {
                    item {
                        RaceStatsContent(
                            selectedRaceResults = selectedRaceResults,
                            onShowPodiumsDialog = { showPodiumsDialog = true },
                            onShowAthletesDialog = { showAthletesDialog = true }
                        )
                    }
                }
            }
        }


        // Athlete Selection Action Bar
        AnimatedVisibility(
            visible = selectedRace != null && selectedAthleteIdsForBulkAction.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(64.dp).shadow(30.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp)).background(Color(0xFF002B5B)).border(1.dp, Color(0xFF80D8FF).copy(alpha = 0.3f), RoundedCornerShape(32.dp))) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedAthleteIdsForBulkAction = emptySet() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${selectedAthleteIdsForBulkAction.size}",
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = {
                                    viewModel.disassociateAthletes(
                                        raceId = selectedRace!!.id!!,
                                        athleteIds = selectedAthleteIdsForBulkAction.toList()
                                    ) { success ->
                                        if (success) {
                                            selectedAthleteIdsForBulkAction = emptySet()
                                        }
                                    }
                                },
                                color = Color(0xFFFF5252),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Delete, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Desassociar", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Surface(
                                onClick = {
                                    viewModel.keepAthletes(
                                        raceId = selectedRace!!.id!!,
                                        athleteIdsToKeep = selectedAthleteIdsForBulkAction.toList()
                                    ) { success ->
                                        if (success) {
                                            selectedAthleteIdsForBulkAction = emptySet()
                                        }
                                    }
                                },
                                color = CyberCyan,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Check, null, tint = MidnightBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Manter", color = MidnightBlue, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (raceToDelete != null) {
            AlertDialog(
                onDismissRequest = { raceToDelete = null; selectedRaces = emptySet() },
                containerColor = Color(0xFF001A33),
                title = { Text("Eliminar Prova?", color = Color.White) },
                text = { Text("Tens a certeza que pretendes eliminar esta prova? Esta ação é irreversível.", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteRace(raceToDelete!!) { success ->
                            if (success) {
                                selectedRaces = emptySet()
                                raceToDelete = null
                                selectedRace = null
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))) { Text("ELIMINAR", fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { raceToDelete = null; selectedRaces = emptySet() }) { Text("CANCELAR", color = Color.White.copy(alpha = 0.6f)) } }
            )
        }

        if (showFormDialog) {
            RaceFormDialog(
                race = editingRace,
                allAthletes = allAthletes,
                initialSelectedAthleteIds = initialSelectedAthleteIds,
                initialStep = initialFormStep,
                onDismiss = { showFormDialog = false; editingRace = null },
                onSave = { title, date, cat, status, gender, subCats, loc, desc, selectedAthleteIds, startTime, link ->
                    val r = Race(
                        id = editingRace?.id,
                        title = title,
                        date = date,
                        category = cat,
                        status = status,
                        gender = gender,
                        sub_categories = subCats,
                        location = if (loc.isBlank()) null else loc,
                        description = if (desc.isBlank()) null else desc,
                        team_classification = editingRace?.team_classification,
                        start_time = if (startTime.isBlank()) null else startTime,
                        link = if (link.isBlank()) null else link
                    )
                    val success = viewModel.saveRace(r, selectedAthleteIds)
                    if (success) {
                        showFormDialog = false
                        editingRace = null
                    }
                    success
                }
            )
        }

        if (showFinishDialog && selectedRace != null) {
            RaceFinishDialog(
                race = selectedRace!!,
                results = selectedRaceResults,
                onDismiss = { showFinishDialog = false },
                onSave = { updatedResults ->
                    viewModel.finishRace(selectedRace!!, updatedResults) { success ->
                        if (success) {
                            selectedRace = selectedRace!!.copy(status = "Concluída")
                            showFinishDialog = false
                        }
                    }
                }
            )
        }

        if (showPodiumsDialog) {
            Dialog(onDismissRequest = { showPodiumsDialog = false }) {
                HyperGlassCard(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
                    color = VividAmber,
                    variant = "solid"
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(VividAmber.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.EmojiEvents, null, tint = VividAmber, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Pódios da Equipa", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            IconButton(onClick = { showPodiumsDialog = false }) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val podiumResults = selectedRaceResults.filter { val pos = it.first.position ?: 99; pos in 1..3 }.sortedWith(compareBy { it.first.position ?: 99 })
                        
                        if (podiumResults.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Nenhum atleta no pódio nesta prova.", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(podiumResults) { (res, athlete) ->
                                    val rank = res.position ?: 0
                                    val rankColor = when (rank) {
                                        1 -> VividAmber
                                        2 -> Color(0xFFC0C0C0) // Silver
                                        3 -> Color(0xFFCD7F32) // Bronze
                                        else -> CyberCyan
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(rankColor.copy(alpha = 0.2f), CircleShape)
                                                .border(1.dp, rankColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "${rank}º", color = rankColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(athlete.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(athlete.category, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                        }
                                        if (!res.time.isNullOrBlank()) {
                                            Text(res.time, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAthletesDialog) {
            Dialog(onDismissRequest = { showAthletesDialog = false }) {
                HyperGlassCard(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f),
                    color = CyberCyan,
                    variant = "solid"
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(CyberCyan.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Groups, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Atletas Inscritos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            IconButton(onClick = { showAthletesDialog = false }) {
                                Icon(Icons.Rounded.Close, null, tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (selectedRaceResults.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Nenhum atleta inscrito nesta prova.", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(selectedRaceResults) { (res, athlete) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                                                .border(1.dp, CyberCyan.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = athlete.name.take(2).uppercase(),
                                                color = CyberCyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(athlete.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(athlete.category, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                        }
                                        val displayPos = when (res.time) {
                                            "DNF", "DNS", "DSQ", "OTL" -> res.time
                                            else -> if (res.position != null) "${res.position}º" else "--"
                                        }
                                        Text(text = displayPos, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

