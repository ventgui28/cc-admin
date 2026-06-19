package com.ventgui.app.ui.screens.team

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.github.jan.supabase.storage.storage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.utils.UserLogger
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.JoinedRaceResult
import com.ventgui.app.data.model.Race
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import com.ventgui.app.ui.components.*
import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

val AthleteSaver = Saver<Athlete?, String>(
    save = { athlete -> if (athlete != null) Json.encodeToString(athlete) else "" },
    restore = { jsonStr -> if (jsonStr.isNotEmpty()) Json.decodeFromString<Athlete>(jsonStr) else null }
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EquipaScreen(
    innerPadding: PaddingValues, 
    onSelectionModeChange: (Boolean) -> Unit = {},
    initialOpenAddDialog: Boolean = false,
    onDialogOpened: () -> Unit = {},
    onNavigateToAthleteDetails: (Athlete) -> Unit = {},
    onOpenDrawer: () -> Unit,
    viewModel: TeamViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val athletes = uiState.athletes
    val isLoading = uiState.isLoading
    val isRefreshing = uiState.isRefreshing
    
    var athleteToDelete by rememberSaveable(stateSaver = AthleteSaver) { mutableStateOf<Athlete?>(null) }
    var showFormDialog by rememberSaveable { mutableStateOf(false) }
    var editingAthlete by rememberSaveable(stateSaver = AthleteSaver) { mutableStateOf<Athlete?>(null) }
    var expandedAthleteId by rememberSaveable { mutableStateOf<String?>(null) }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    
    val filteredAthletes = athletes.filter { athlete ->
        val matchesSearch = athlete.name.contains(searchQuery, ignoreCase = true)
        val matchesStatus = statusFilter == null || athlete.status == statusFilter
        val matchesCategory = categoryFilter == null || athlete.category.equals(categoryFilter, ignoreCase = true)
        matchesSearch && matchesStatus && matchesCategory
    }

    LaunchedEffect(Unit) {
        onSelectionModeChange(false)
        viewModel.loadAthletes()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PremiumMeshBackground()

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // --- FIXED HEADER ---
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                PremiumTopBar(
                    onLeftClick = onOpenDrawer,
                    onRightClick = { 
                        editingAthlete = null
                        showFormDialog = true 
                    },
                    rightIcon = Icons.Rounded.Add
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Title and Search Row (Refined Layout)
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = isSearchExpanded,
                            transitionSpec = {
                                if (targetState) {
                                    (fadeIn() + expandHorizontally()).togetherWith(fadeOut() + shrinkHorizontally())
                                } else {
                                    (fadeIn() + expandHorizontally()).togetherWith(fadeOut() + shrinkHorizontally())
                                }
                            },
                            label = "HeaderTransition"
                        ) { expanded ->
                            if (expanded) {
                                PremiumTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = null,
                                    placeholder = stringResource(R.string.team_search_placeholder),
                                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
                                    leadingIcon = Icons.Rounded.Search
                                )
                            } else {
                                Column {
                                    Text(stringResource(R.string.nav_team), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                    Text(stringResource(R.string.team_subtitle), color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(
                            onClick = { 
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) searchQuery = ""
                            },
                            modifier = Modifier.size(32.dp).background(if (isSearchExpanded) CyberCyan.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search, null, tint = if (isSearchExpanded) CyberCyan else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        }
                        
                        if (!isSearchExpanded) {
                            // Category Filter
                            var catMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                Surface(
                                    modifier = Modifier.height(36.dp),
                                    color = if (categoryFilter != null) CyberCyan else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    onClick = { catMenuExpanded = true }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                        val filterLabel = categoryFilter ?: stringResource(R.string.team_athlete_category)
                                        Text(filterLabel.uppercase(), color = if (categoryFilter != null) MidnightBlue else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Rounded.Tune, null, tint = if (categoryFilter != null) MidnightBlue else Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                                
                                val availableCategories = remember(athletes) {
                                    (listOf("Escolas", "Cadetes") + athletes.map { it.category }).distinct().filter { it.isNotBlank() }
                                }

                                DropdownMenu(
                                    expanded = catMenuExpanded,
                                    onDismissRequest = { catMenuExpanded = false },
                                    modifier = Modifier.background(MidnightBlue).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.team_all_categories).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { categoryFilter = null; catMenuExpanded = false })
                                    availableCategories.forEach { catName ->
                                        DropdownMenuItem(text = { Text(catName.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { categoryFilter = catName; catMenuExpanded = false })
                                    }
                                }
                            }

                            // Status Filter
                            var statusMenuExpanded by remember { mutableStateOf(false) }
                            Box {
                                Surface(
                                    modifier = Modifier.height(36.dp),
                                    color = if (statusFilter != null) CyberCyan else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                    onClick = { statusMenuExpanded = true }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                        val filterLabel = when (statusFilter) {
                                            "active" -> stringResource(R.string.team_status_active)
                                            "injured" -> stringResource(R.string.team_status_injured)
                                            else -> "ESTADO"
                                        }
                                        Text(filterLabel.uppercase(), color = if (statusFilter != null) MidnightBlue else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Rounded.Tune, null, tint = if (statusFilter != null) MidnightBlue else Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = statusMenuExpanded,
                                    onDismissRequest = { statusMenuExpanded = false },
                                    modifier = Modifier.background(MidnightBlue).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(text = { Text("TODOS OS ESTADOS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { statusFilter = null; statusMenuExpanded = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.team_status_active).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { statusFilter = "active"; statusMenuExpanded = false })
                                    DropdownMenuItem(text = { Text(stringResource(R.string.team_status_injured).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { statusFilter = "injured"; statusMenuExpanded = false })
                                }
                            }
                        }
                    }
                }
            }
            
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.loadAthletes(isRefresh = true)
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                     if (isLoading) {
                         item {
                             Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CyberCyan) }
                         }
                     } else {
                         items(filteredAthletes.reversed()) { athlete ->
                             AthleteCardElite(
                                 athlete = athlete,
                                 onClick = { onNavigateToAthleteDetails(athlete) },
                                 onEditClick = {
                                     editingAthlete = athlete
                                     showFormDialog = true
                                 },
                                 onDeleteClick = { athleteToDelete = athlete }
                             )
                             Spacer(modifier = Modifier.height(16.dp))
                         }
                     }
                 }
            }
        }

        if (showFormDialog) {
            AthleteFormDialog(athlete = editingAthlete, onDismiss = { showFormDialog = false; editingAthlete = null }, onSave = { updatedAthlete ->
                viewModel.saveAthlete(updatedAthlete) { success ->
                    if (success) {
                        showFormDialog = false
                        editingAthlete = null
                    } else {
                        android.widget.Toast.makeText(context, "Erro ao salvar atleta. Verifica os dados.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            })
        }

        // Delete Confirmation Dialog
        if (athleteToDelete != null) {
            AlertDialog(
                onDismissRequest = { athleteToDelete = null },
                containerColor = Color(0xFF001A33),
                title = { Text(stringResource(R.string.team_delete_athlete_title), color = Color.White) },
                text = { Text("Tens a certeza que pretendes eliminar este atleta? Esta ação é irreversível.", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteAthlete(athleteToDelete!!) { success ->
                            if (success) {
                                athleteToDelete = null
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))) { Text("ELIMINAR", fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { athleteToDelete = null }) { Text("CANCELAR", color = Color.White.copy(alpha = 0.6f)) } }
            )
        }
    }
}


