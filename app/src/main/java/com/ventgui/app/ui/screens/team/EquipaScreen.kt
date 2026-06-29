package com.ventgui.app.ui.screens.team

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var editingAthlete by rememberSaveable(stateSaver = AthleteSaver) { mutableStateOf<Athlete?>(null) }
    var expandedAthleteId by rememberSaveable { mutableStateOf<String?>(null) }
    
    var activeTab by rememberSaveable { mutableStateOf("Atletas") }
    var staffList by remember { mutableStateOf(emptyList<com.ventgui.app.data.model.TeamStaff>()) }
    var isLoadingStaff by remember { mutableStateOf(false) }
    var showCreateStaffDialog by rememberSaveable { mutableStateOf(false) }
    var showEditStaffDialog by rememberSaveable { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<com.ventgui.app.data.model.TeamStaff?>(null) }
    var staffToDelete by remember { mutableStateOf<com.ventgui.app.data.model.TeamStaff?>(null) }
    
    fun loadStaff() {
        scope.launch {
            try {
                isLoadingStaff = true
                val response = SupabaseClient.client.postgrest.from("team_staff").select()
                staffList = response.decodeList<com.ventgui.app.data.model.TeamStaff>().sortedBy { it.name }
            } catch (e: Exception) {
            } finally {
                isLoadingStaff = false
            }
        }
    }

    LaunchedEffect(initialOpenAddDialog) {
        if (initialOpenAddDialog) {
            showCreateDialog = true
            onDialogOpened()
        }
    }
    
    LaunchedEffect(activeTab) {
        if (activeTab == "Staff") {
            loadStaff()
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var genderFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var escalaoFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    
    val filteredAthletes = athletes.filter { athlete ->
        val matchesSearch = athlete.name.contains(searchQuery, ignoreCase = true)
        
        val matchesStatus = when (statusFilter) {
            null -> true
            else -> athlete.status == statusFilter
        }
        
        val matchesGender = when (genderFilter) {
            null -> true
            "Feminino" -> athlete.category.contains("Feminino", ignoreCase = true)
            "Masculino" -> !athlete.category.contains("Feminino", ignoreCase = true)
            else -> true
        }
        
        val matchesEscalao = when (escalaoFilter) {
            null -> true
            "Cadetes" -> athlete.category.contains("Sub-17", ignoreCase = true)
            "Escolas" -> !athlete.category.contains("Sub-17", ignoreCase = true)
            else -> true
        }
        
        val catFilter = categoryFilter
        val matchesCategory = when (catFilter) {
            null -> true
            else -> athlete.category.startsWith(catFilter, ignoreCase = true)
        }
        
        matchesSearch && matchesStatus && matchesGender && matchesEscalao && matchesCategory
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
                        if (activeTab == "Atletas") {
                            editingAthlete = null
                            showCreateDialog = true 
                        } else {
                            editingStaff = null
                            showCreateStaffDialog = true
                        }
                    },
                    rightIcon = Icons.Rounded.Add
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Tabs de Seleção de Segmento da Equipa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Atletas", "Staff").forEach { tab ->
                        val isSelected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { activeTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab == "Atletas") "ATLETAS" else "STAFF TÉCNICO",
                                color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == "Atletas") {
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
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Estado Filter
                                var statusMenuExpanded by remember { mutableStateOf(false) }
                                val statusInteractionSource = remember { MutableInteractionSource() }
                                val isStatusPressed by statusInteractionSource.collectIsPressedAsState()
                                val statusScale by animateFloatAsState(if (isStatusPressed) 0.92f else 1f, label = "statusScale")

                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .graphicsLayer {
                                                scaleX = statusScale
                                                scaleY = statusScale
                                            },
                                        color = if (statusFilter != null) CyberCyan else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        interactionSource = statusInteractionSource,
                                        onClick = { statusMenuExpanded = true }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                            val filterLabel = when (statusFilter) {
                                                "active" -> stringResource(R.string.team_status_active)
                                                "inactive" -> stringResource(R.string.team_status_inactive)
                                                "injured" -> stringResource(R.string.team_status_injured)
                                                "developing" -> stringResource(R.string.team_status_developing)
                                                null -> "ESTADO"
                                                else -> statusFilter!!.uppercase()
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
                                        listOf("active", "injured", "inactive", "developing").forEach { statusVal ->
                                            val statusLabel = when (statusVal) {
                                                "active" -> stringResource(R.string.team_status_active)
                                                "inactive" -> stringResource(R.string.team_status_inactive)
                                                "injured" -> stringResource(R.string.team_status_injured)
                                                "developing" -> stringResource(R.string.team_status_developing)
                                                else -> statusVal.uppercase()
                                            }
                                            DropdownMenuItem(text = { Text(statusLabel.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { statusFilter = statusVal; statusMenuExpanded = false })
                                        }
                                    }
                                }

                                // 2. Escalão Filter
                                var escalaoMenuExpanded by remember { mutableStateOf(false) }
                                val escalaoInteractionSource = remember { MutableInteractionSource() }
                                val isEscalaoPressed by escalaoInteractionSource.collectIsPressedAsState()
                                val escalaoScale by animateFloatAsState(if (isEscalaoPressed) 0.92f else 1f, label = "escalaoScale")

                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .graphicsLayer {
                                                scaleX = escalaoScale
                                                scaleY = escalaoScale
                                            },
                                        color = if (escalaoFilter != null) CyberCyan else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        interactionSource = escalaoInteractionSource,
                                        onClick = { escalaoMenuExpanded = true }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                            val filterLabel = escalaoFilter ?: "ESCALÃO"
                                            Text(filterLabel.uppercase(), color = if (escalaoFilter != null) MidnightBlue else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Rounded.Tune, null, tint = if (escalaoFilter != null) MidnightBlue else Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = escalaoMenuExpanded,
                                        onDismissRequest = { escalaoMenuExpanded = false },
                                        modifier = Modifier.background(MidnightBlue).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    ) {
                                        DropdownMenuItem(text = { Text("TODOS OS ESCALÕES", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { escalaoFilter = null; escalaoMenuExpanded = false })
                                        listOf("Escolas", "Cadetes").forEach { esc ->
                                            DropdownMenuItem(text = { Text(esc.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { escalaoFilter = esc; escalaoMenuExpanded = false })
                                        }
                                    }
                                }

                                // 3. Sexo Filter
                                var genderMenuExpanded by remember { mutableStateOf(false) }
                                val genderInteractionSource = remember { MutableInteractionSource() }
                                val isGenderPressed by genderInteractionSource.collectIsPressedAsState()
                                val genderScale by animateFloatAsState(if (isGenderPressed) 0.92f else 1f, label = "genderScale")

                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .graphicsLayer {
                                                scaleX = genderScale
                                                scaleY = genderScale
                                            },
                                        color = if (genderFilter != null) CyberCyan else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        interactionSource = genderInteractionSource,
                                        onClick = { genderMenuExpanded = true }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp)) {
                                            val filterLabel = genderFilter ?: "SEXO"
                                            Text(filterLabel.uppercase(), color = if (genderFilter != null) MidnightBlue else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Rounded.Tune, null, tint = if (genderFilter != null) MidnightBlue else Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = genderMenuExpanded,
                                        onDismissRequest = { genderMenuExpanded = false },
                                        modifier = Modifier.background(MidnightBlue).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    ) {
                                        DropdownMenuItem(text = { Text("TODOS OS GÉNEROS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { genderFilter = null; genderMenuExpanded = false })
                                        listOf("Masculino", "Feminino").forEach { genderOption ->
                                            DropdownMenuItem(text = { Text(genderOption.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { genderFilter = genderOption; genderMenuExpanded = false })
                                        }
                                    }
                                }

                                // 4. Categoria Filter
                                var catMenuExpanded by remember { mutableStateOf(false) }
                                val catInteractionSource = remember { MutableInteractionSource() }
                                val isCatPressed by catInteractionSource.collectIsPressedAsState()
                                val catScale by animateFloatAsState(if (isCatPressed) 0.92f else 1f, label = "catScale")

                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .graphicsLayer {
                                                scaleX = catScale
                                                scaleY = catScale
                                            },
                                        color = if (categoryFilter != null) CyberCyan else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(18.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        interactionSource = catInteractionSource,
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
                                        athletes.map { athlete ->
                                            athlete.category.split(" ").firstOrNull() ?: athlete.category
                                        }.distinct().filter { it.isNotBlank() }.sorted()
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
                            }
                        }
                    }
                }
                
                if (activeTab == "Staff") {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("STAFF TÉCNICO", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("Treinadores e Diretores Desportivos Registados", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            if (activeTab == "Atletas") {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            viewModel.loadAthletes(isRefresh = true)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
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
                                             showEditDialog = true
                                         },
                                         onDeleteClick = { athleteToDelete = athlete }
                                     )
                                     Spacer(modifier = Modifier.height(16.dp))
                                 }
                             }
                         }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    PullToRefreshBox(
                        isRefreshing = isLoadingStaff,
                        onRefresh = {
                            loadStaff()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)
                    ) {
                        if (isLoadingStaff && staffList.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = CyberCyan)
                                }
                            }
                        } else if (staffList.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("Sem membros do staff técnico registados.", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(staffList) { staff ->
                                // Cartão do Staff em Hyper-Glass
                                HyperGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    color = if (staff.is_federated) CyberCyan else Color.White.copy(alpha = 0.2f)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = staff.name,
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    // Badge de cargo/função
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(CyberCyan.copy(alpha = 0.1f))
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = staff.role.uppercase(),
                                                            color = CyberCyan,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                    
                                                    // Badge de Licença
                                                    if (!staff.license_number.isNullOrBlank()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color.White.copy(alpha = 0.05f))
                                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "L.: ${staff.license_number}",
                                                                color = Color.White.copy(alpha = 0.6f),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            // Status de Ativo / Inativo
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (staff.is_federated) Color(0x224CAF50) else Color(0x22F44336))
                                                    .border(1.dp, if (staff.is_federated) Color(0xFF4CAF50) else Color(0xFFF44336), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (staff.is_federated) "ATIVO" else "INATIVO",
                                                    color = if (staff.is_federated) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Regra do Secretariado: Autorizado a levantar dorsais?
                                        val isTreinadorOrDD = staff.role.uppercase().contains("TREINADOR") || staff.role.uppercase().contains("DIRETOR")
                                        val isAuthorized = isTreinadorOrDD && staff.is_federated
                                        
                                        if (isAuthorized) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x114CAF50))
                                                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.VerifiedUser,
                                                        contentDescription = null,
                                                        tint = Color(0xFF4CAF50),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "AUTORIZADO A EFETUAR LEVANTAMENTO DE DORSAIS",
                                                        color = Color(0xFF4CAF50),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                        
                                        // Detalhes de Contacto
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Phone, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(staff.phone ?: "Sem Telefone", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                            }
                                            
                                            if (!staff.email.isNullOrBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Mail, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(staff.email, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Botões de Ação
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                // Botão de ligar
                                                IconButton(
                                                    onClick = {
                                                        try {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${staff.phone}"))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {}
                                                    },
                                                    modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                                                ) {
                                                    Icon(Icons.Rounded.Call, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                                }
                                                
                                                // Botão de email
                                                if (!staff.email.isNullOrBlank()) {
                                                    IconButton(
                                                        onClick = {
                                                            try {
                                                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:${staff.email}"))
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {}
                                                        },
                                                        modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                                                    ) {
                                                        Icon(Icons.Rounded.Email, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        editingStaff = staff
                                                        showEditStaffDialog = true
                                                    },
                                                    modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                                                ) {
                                                    Icon(Icons.Rounded.Edit, null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        staffToDelete = staff
                                                    },
                                                    modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                                                ) {
                                                    Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
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

        if (showCreateDialog) {
            CreateAthleteForm(
                onDismiss = { showCreateDialog = false },
                onSave = { newAthlete ->
                    viewModel.handleCreateAthlete(newAthlete) { success ->
                        if (success) {
                            showCreateDialog = false
                        } else {
                            android.widget.Toast.makeText(context, "Erro ao salvar atleta. Verifica os dados.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        if (showEditDialog && editingAthlete != null) {
            UpdateAthleteForm(
                athlete = editingAthlete!!,
                onDismiss = { showEditDialog = false; editingAthlete = null },
                onSave = { updatedAthlete ->
                    viewModel.handleUpdateAthlete(updatedAthlete) { success ->
                        if (success) {
                            showEditDialog = false
                            editingAthlete = null
                        } else {
                            android.widget.Toast.makeText(context, "Erro ao salvar atleta. Verifica os dados.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        if (athleteToDelete != null) {
            AlertDialog(
                onDismissRequest = { athleteToDelete = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = MidnightBlue,
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                title = { Text(stringResource(R.string.team_delete_athlete_title), color = Color.White, fontWeight = FontWeight.Black) },
                text = { Text("Tens a certeza que pretendes eliminar este atleta? Esta ação é irreversível.", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    PremiumButton(
                        text = "ELIMINAR",
                        onClick = {
                            viewModel.deleteAthlete(athleteToDelete!!) { success ->
                                if (success) {
                                    athleteToDelete = null
                                }
                            }
                        },
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    )
                },
                dismissButton = {
                    PremiumButton(
                        text = "CANCELAR",
                        onClick = { athleteToDelete = null },
                        variant = "outline"
                    )
                }
            )
        }

        // Diálogos de Staff Técnico
        if (showCreateStaffDialog || showEditStaffDialog) {
            val isEdit = showEditStaffDialog
            val staff = editingStaff
            
            var name by remember { mutableStateOf(staff?.name ?: "") }
            var role by remember { mutableStateOf(staff?.role ?: "Treinador Grau I") }
            var license by remember { mutableStateOf(staff?.license_number ?: "") }
            var phone by remember { mutableStateOf(staff?.phone ?: "") }
            var email by remember { mutableStateOf(staff?.email ?: "") }
            var isFederated by remember { mutableStateOf(staff?.is_federated ?: true) }
            
            var isSavingStaff by remember { mutableStateOf(false) }
            
            val roles = listOf("Treinador Grau I", "Treinador Grau II", "Treinador Grau III", "Diretor Desportivo Grau I", "Diretor Desportivo Grau II", "Mecânico", "Presidente", "Outro")
            var roleExpanded by remember { mutableStateOf(false) }
            
            Dialog(onDismissRequest = {
                showCreateStaffDialog = false
                showEditStaffDialog = false
            }) {
                HyperGlassCard(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
                    color = CyberCyan,
                    variant = "solid"
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (isEdit) "EDITAR MEMBRO DO STAFF" else "ADICIONAR STAFF TÉCNICO",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        PremiumTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "NOME COMPLETO",
                            placeholder = "Ex: Manuel Silva",
                            leadingIcon = Icons.Rounded.Person
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Cargo Dropdown
                        Text("FUNÇÃO / CARGO", color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                        ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = !roleExpanded }, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = role,
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = { Icon(Icons.Rounded.Work, null, tint = CyberCyan.copy(alpha = 0.5f)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }, modifier = Modifier.background(MidnightBlue)) {
                                roles.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r, color = Color.White) },
                                        onClick = {
                                            role = r
                                            roleExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        PremiumTextField(
                            value = license,
                            onValueChange = { license = it },
                            label = "LICENÇA FPC (OPCIONAL)",
                            placeholder = "Ex: FPC-54912",
                            leadingIcon = Icons.Rounded.CardMembership
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Telefone com OutlinedTextField customizado (para suportar keyboardOptions)
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telefone de Contacto", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                            placeholder = { Text("Ex: 912345678", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = CyberCyan, modifier = Modifier.size(20.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Email com OutlinedTextField customizado (para suportar keyboardOptions)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email (Opcional)", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                            placeholder = { Text("Ex: staff@cantanhede.pt", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Email, null, tint = CyberCyan, modifier = Modifier.size(20.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isFederated = !isFederated }
                                .padding(vertical = 12.dp)
                        ) {
                            Checkbox(
                                checked = isFederated,
                                onCheckedChange = { isFederated = it },
                                colors = CheckboxDefaults.colors(
                                    checkmarkColor = MidnightBlue,
                                    checkedColor = CyberCyan,
                                    uncheckedColor = Color.White.copy(alpha = 0.4f)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Membro do Staff Ativo e Federado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(28.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumButton(
                                text = "CANCELAR",
                                onClick = {
                                    showCreateStaffDialog = false
                                    showEditStaffDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                variant = "outline"
                            )
                            PremiumButton(
                                text = if (isSavingStaff) "A GUARDAR..." else "GRAVAR",
                                onClick = {
                                    if (name.isBlank()) {
                                        android.widget.Toast.makeText(context, "O nome do membro do staff é obrigatório.", android.widget.Toast.LENGTH_SHORT).show()
                                        return@PremiumButton
                                    }
                                    scope.launch {
                                        try {
                                            isSavingStaff = true
                                            val payload = com.ventgui.app.data.model.TeamStaff(
                                                id = staff?.id,
                                                name = name,
                                                role = role,
                                                coach_level = staff?.coach_level,
                                                license_number = license.ifBlank { null },
                                                phone = phone.ifBlank { null },
                                                email = email.ifBlank { null },
                                                is_federated = isFederated
                                            )
                                            if (isEdit) {
                                                SupabaseClient.client.postgrest.from("team_staff").update(payload) {
                                                    filter { eq("id", staff!!.id!!) }
                                                }
                                                android.widget.Toast.makeText(context, "Staff atualizado com sucesso! 🟢", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                SupabaseClient.client.postgrest.from("team_staff").insert(payload)
                                                android.widget.Toast.makeText(context, "Staff adicionado com sucesso! 🟢", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                            showCreateStaffDialog = false
                                            showEditStaffDialog = false
                                            loadStaff()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Erro ao gravar staff: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                        } finally {
                                            isSavingStaff = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                enabled = name.isNotBlank() && !isSavingStaff
                            )
                        }
                    }
                }
            }
        }
        
        if (staffToDelete != null) {
            AlertDialog(
                onDismissRequest = { staffToDelete = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = MidnightBlue,
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                title = { Text("Eliminar Membro do Staff", color = Color.White, fontWeight = FontWeight.Black) },
                text = { Text("Tem a certeza que pretende eliminar ${staffToDelete!!.name}? Esta ação é irreversível.", color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    PremiumButton(
                        text = "ELIMINAR",
                        onClick = {
                            scope.launch {
                                try {
                                    SupabaseClient.client.postgrest.from("team_staff").delete {
                                        filter { eq("id", staffToDelete!!.id!!) }
                                    }
                                    android.widget.Toast.makeText(context, "Membro do staff eliminado.", android.widget.Toast.LENGTH_SHORT).show()
                                    staffToDelete = null
                                    loadStaff()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Erro ao eliminar: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    )
                },
                dismissButton = {
                    PremiumButton(
                        text = "CANCELAR",
                        onClick = { staffToDelete = null },
                        variant = "outline"
                    )
                }
            )
        }
    }
}
}


