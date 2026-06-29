package com.ventgui.app.ui.screens.races

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.ventgui.app.R
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.ui.components.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UpdateRaceForm(
    race: Race,
    allAthletes: List<Athlete>,
    initialSelectedAthleteIds: Set<String>,
    initialStep: Int = 1,
    onDismiss: () -> Unit,
    onSave: suspend (Race, Set<String>) -> Boolean
) {
    var title by rememberSaveable { mutableStateOf(race.title) }
    var date by rememberSaveable { mutableStateOf(race.date) }
    var category by rememberSaveable { mutableStateOf(race.category) }
    var status by rememberSaveable { mutableStateOf(race.status) }
    var location by rememberSaveable { mutableStateOf(race.location ?: "") }
    var description by rememberSaveable { mutableStateOf(race.description ?: "") }
    var link by rememberSaveable { mutableStateOf(race.link ?: "") }
    var selectedSubCats by rememberSaveable { mutableStateOf(race.sub_categories) }
    var selectedAthleteIds by remember { mutableStateOf(initialSelectedAthleteIds) }
    var startTime by rememberSaveable { mutableStateOf(race.start_time ?: "") }
    
    var raceFormat by rememberSaveable { mutableStateOf(race.race_format ?: "Estrada") }
    var distanceKm by rememberSaveable { mutableStateOf(race.distance_km?.toString() ?: "") }
    var durationMinutes by rememberSaveable { mutableStateOf(race.duration_minutes?.toString() ?: "") }
    var subStagesList by remember { mutableStateOf(race.sub_stages ?: emptyList()) }
    
    var showAddSubStageDialog by remember { mutableStateOf(false) }
    var subStageName by remember { mutableStateOf("") }
    var subStageType by remember { mutableStateOf("gincana") }
    var subStageDistance by remember { mutableStateOf("") }
    var subStageDuration by remember { mutableStateOf("") }
    
    LaunchedEffect(initialSelectedAthleteIds) {
        selectedAthleteIds = initialSelectedAthleteIds
    }
    
    var bttGender by rememberSaveable {
        mutableStateOf(
            if (race.category.split(", ").contains("BTT")) {
                val g = race.gender ?: "Misto"
                if (g.contains("BTT: ")) {
                    g.substringAfter("BTT: ").substringBefore(",")
                } else g
            } else "Misto"
        )
    }
    var estradaGender by rememberSaveable {
        mutableStateOf(
            if (race.category.split(", ").contains("Estrada")) {
                val g = race.gender ?: "Misto"
                if (g.contains("Estrada: ")) {
                    g.substringAfter("Estrada: ").substringBefore(",")
                } else g
            } else "Misto"
        )
    }
    var pistaGender by rememberSaveable {
        mutableStateOf(
            if (race.category.split(", ").contains("Pista")) {
                val g = race.gender ?: "Misto"
                if (g.contains("Pista: ")) {
                    g.substringAfter("Pista: ").substringBefore(",")
                } else g
            } else "Misto"
        )
    }
    var bttAgeGroupTypes by remember {
        mutableStateOf(
            buildSet {
                if (race.sub_categories.any { it.startsWith("BTT Sub-7") || it.startsWith("BTT Sub-9") || it.startsWith("BTT Sub-11") || it.startsWith("BTT Sub-13") || it.startsWith("BTT Sub-15") }) {
                    add("Escolas")
                }
                if (race.sub_categories.any { it.startsWith("BTT Sub-17") }) {
                    add("Cadetes")
                }
            }
        )
    }
    var estradaAgeGroupTypes by remember {
        mutableStateOf(
            buildSet {
                if (race.sub_categories.any { it.startsWith("Estrada Sub-7") || it.startsWith("Estrada Sub-9") || it.startsWith("Estrada Sub-11") || it.startsWith("Estrada Sub-13") || it.startsWith("Estrada Sub-15") }) {
                    add("Escolas")
                }
                if (race.sub_categories.any { it.startsWith("Estrada Sub-17") }) {
                    add("Cadetes")
                }
            }
        )
    }
    var pistaAgeGroupTypes by remember {
        mutableStateOf(
            buildSet {
                if (race.sub_categories.any { it.startsWith("Pista Sub-7") || it.startsWith("Pista Sub-9") || it.startsWith("Pista Sub-11") || it.startsWith("Pista Sub-13") || it.startsWith("Pista Sub-15") }) {
                    add("Escolas")
                }
                if (race.sub_categories.any { it.startsWith("Pista Sub-17") }) {
                    add("Cadetes")
                }
            }
        )
    }
    
    var activeAccordionCategory by rememberSaveable {
        mutableStateOf(
            if (category.split(", ").contains("BTT")) "BTT"
            else if (category.split(", ").contains("Estrada")) "Estrada"
            else "Pista"
        )
    }
    
    var currentStep by rememberSaveable { mutableStateOf(initialStep) }
    
    val categories = listOf("BTT", "Estrada", "Pista")
    val statuses = listOf("Agendada", "A decorrer", "Concluída", "Cancelada")
    val genders = listOf("Masculino", "Feminino", "Misto")
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var statusExpanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

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
            } catch (e2: Exception) {
                null
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = java.time.Instant.ofEpochMilli(millis).toString()
                    }
                    showDatePicker = false
                }) { Text("OK", color = CyberCyan) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel).uppercase(), color = Color.White.copy(alpha = 0.6f)) } }
        ) { DatePicker(state = datePickerState) }
    }

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f), color = CyberCyan, variant = "solid") {
            Column(modifier = Modifier.padding(24.dp).fillMaxHeight()) {
                // Header with Progress Indicator
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(CyberCyan.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.races_edit_race),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Step Indicator Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (step in 1..4) {
                            val isCompleted = step < currentStep
                            val isActive = step == currentStep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when {
                                            isCompleted -> CyberCyan
                                            isActive -> CyberCyan.copy(alpha = 0.6f)
                                            else -> Color.White.copy(alpha = 0.1f)
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (currentStep) {
                            1 -> "Passo 1: Informações Básicas"
                            2 -> "Passo 2: Género e Escalões"
                            3 -> "Passo 3: Atletas Participantes"
                            else -> "Passo 4: Revisão e Confirmação"
                        },
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scrollable Form content for current step
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        1 -> {
                            PremiumTextField(value = title, onValueChange = { title = it }, label = stringResource(R.string.races_race_title).uppercase(), placeholder = stringResource(R.string.races_race_title_placeholder), leadingIcon = Icons.Rounded.DriveFileRenameOutline)
                            Spacer(modifier = Modifier.height(20.dp))
                            Column {
                                Text(stringResource(R.string.races_race_date).uppercase(), color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)).clickable { showDatePicker = true }.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) { 
                                        Icon(Icons.Rounded.Event, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        val defaultSelectDate = stringResource(R.string.races_select_date)
                                        val displayDate = try {
                                            val instant = parseFlexibleDate(date)
                                            if (instant != null) {
                                                val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                                                "${localDate.dayOfMonth} ${localDate.month.getDisplayName(TextStyle.SHORT, Locale("pt", "PT")).uppercase()} ${localDate.year}"
                                            } else if (date.isBlank()) defaultSelectDate else date
                                        } catch(e: Exception) { date }
                                        Text(text = displayDate, color = if (date.isBlank()) Color.White.copy(alpha = 0.2f) else Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) 
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Column {
                                Text("HORA DE INÍCIO", color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                                var hourText by remember(startTime) { mutableStateOf(if (startTime.contains(":")) startTime.substringBefore(":") else "") }
                                var minuteText by remember(startTime) { mutableStateOf(if (startTime.contains(":")) startTime.substringAfter(":") else "") }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = hourText,
                                        onValueChange = { 
                                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                                hourText = it
                                                startTime = if (it.isNotEmpty() || minuteText.isNotEmpty()) "$it:${minuteText.padStart(2, '0')}" else ""
                                            }
                                        },
                                        placeholder = { Text("HH", color = Color.White.copy(alpha = 0.2f)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    Text(":", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = minuteText,
                                        onValueChange = { 
                                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                                minuteText = it
                                                startTime = if (hourText.isNotEmpty() || it.isNotEmpty()) "${hourText.padStart(2, '0')}:$it" else ""
                                            }
                                        },
                                        placeholder = { Text("MM", color = Color.White.copy(alpha = 0.2f)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            PremiumTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = "LOCAL",
                                placeholder = "Ex: Cantanhede, Portugal",
                                leadingIcon = Icons.Rounded.LocationOn
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PremiumTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = "DESCRIÇÃO",
                                placeholder = "Ex: Prova de BTT a contar para...",
                                leadingIcon = Icons.Rounded.Description
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            PremiumTextField(
                                value = link,
                                onValueChange = { link = it },
                                label = "LINK (REGULAMENTO, ETC.)",
                                placeholder = "Ex: https://...",
                                leadingIcon = Icons.Rounded.Link
                            )
                            val categoriesList = listOf("BTT", "Estrada", "Pista")
                            Spacer(modifier = Modifier.height(20.dp))
                            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = if (category.isBlank()) "Nenhuma selecionada" else category,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.races_base_category), color = Color.White.copy(alpha = 0.4f)) },
                                    leadingIcon = { Icon(Icons.Rounded.Category, null, tint = CyberCyan.copy(alpha = 0.5f)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, modifier = Modifier.background(MidnightBlue)) {
                                    categoriesList.forEach { item ->
                                        val isSelected = category.split(", ").contains(item)
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = null,
                                                        colors = CheckboxDefaults.colors(checkedColor = CyberCyan, uncheckedColor = Color.White.copy(alpha = 0.4f))
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(item, color = Color.White)
                                                }
                                            },
                                            onClick = {
                                                val currentList = if (category.isBlank()) emptyList() else category.split(", ")
                                                val newList = if (isSelected) {
                                                    currentList - item
                                                } else {
                                                    currentList + item
                                                }
                                                category = newList.sorted().joinToString(", ")
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(value = status, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.races_race_status), color = Color.White.copy(alpha = 0.4f)) }, leadingIcon = { Icon(Icons.Rounded.Info, null, tint = CyberCyan.copy(alpha = 0.5f)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp))
                                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }, modifier = Modifier.background(MidnightBlue)) { statuses.forEach { DropdownMenuItem(text = { Text(it, color = Color.White) }, onClick = { status = it; statusExpanded = false }) } }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Formato da Prova (Dropdown)
                            var formatExpanded by remember { mutableStateOf(false) }
                            val formatsList = listOf("Estrada", "Pista", "BTT", "Encontro de Escolas")
                            Text("FORMATO DA PROVA", color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                            ExposedDropdownMenuBox(expanded = formatExpanded, onExpandedChange = { formatExpanded = !formatExpanded }, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = raceFormat,
                                    onValueChange = {},
                                    readOnly = true,
                                    leadingIcon = { Icon(Icons.Rounded.AltRoute, null, tint = CyberCyan.copy(alpha = 0.5f)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                ExposedDropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }, modifier = Modifier.background(MidnightBlue)) {
                                    formatsList.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f, color = Color.White) },
                                            onClick = {
                                                raceFormat = f
                                                formatExpanded = false
                                                if (f == "Encontro de Escolas") {
                                                    category = "Estrada, BTT" // Encontros de escolas geralmente misturam
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (raceFormat != "Encontro de Escolas") {
                                // Distância e Duração gerais
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PremiumTextField(
                                            value = distanceKm,
                                            onValueChange = { distanceKm = it },
                                            label = "DISTÂNCIA (KM)",
                                            placeholder = "Ex: 45.0",
                                            leadingIcon = Icons.Rounded.Navigation
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        PremiumTextField(
                                            value = durationMinutes,
                                            onValueChange = { durationMinutes = it },
                                            label = "DURAÇÃO (MINUTOS)",
                                            placeholder = "Ex: 90",
                                            leadingIcon = Icons.Rounded.Timer
                                        )
                                    }
                                }
                            } else {
                                // Interface de Escolas: Adicionar sub-provas/exercícios
                                Text("EXERCÍCIOS / SUB-PROVAS", color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    if (subStagesList.isEmpty()) {
                                        Text(
                                            text = "Nenhum exercício adicionado. (Opcional - Podes deixar em branco e adicionar mais tarde ao editar a prova).",
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                        )
                                    } else {
                                        subStagesList.forEachIndexed { index, stage ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = when (stage.stage_type) {
                                                            "gincana" -> Icons.Rounded.SportsScore
                                                            "linha" -> Icons.Rounded.Flag
                                                            else -> Icons.Rounded.DirectionsBike
                                                        },
                                                        contentDescription = null,
                                                        tint = CyberCyan,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(stage.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text(
                                                            text = "Tipo: ${stage.stage_type.uppercase()}" + 
                                                                    (if (stage.distance_km != null) " | ${stage.distance_km}km" else "") + 
                                                                    (if (stage.duration_minutes != null) " | ${stage.duration_minutes}min" else ""),
                                                            color = Color.White.copy(alpha = 0.5f),
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        subStagesList = subStagesList.filterIndexed { i, _ -> i != index }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    PremiumButton(
                                        text = "ADICIONAR EXERCÍCIO",
                                        onClick = { showAddSubStageDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        variant = "outline"
                                    )
                                }
                            }
                        }
                        2 -> {
                            val activeCategories = if (category.isBlank()) listOf("BTT") else category.split(", ")
                            activeCategories.forEach { catName ->
                                val isExpanded = activeAccordionCategory == catName
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            activeAccordionCategory = catName
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) CyberCyan.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f)
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isExpanded) CyberCyan else Color.White.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (catName == "BTT") Icons.Rounded.DirectionsBike else if (catName == "Estrada") Icons.Rounded.DirectionsRun else Icons.Rounded.Loop,
                                                    contentDescription = null,
                                                    tint = if (isExpanded) CyberCyan else Color.White.copy(alpha = 0.6f)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "CONFIGURAÇÕES: $catName",
                                                    color = if (isExpanded) CyberCyan else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.4f)
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            val currentGender = when (catName) {
                                                "BTT" -> bttGender
                                                "Estrada" -> estradaGender
                                                else -> pistaGender
                                            }
                                            val currentAgeGroupTypes = when (catName) {
                                                "BTT" -> bttAgeGroupTypes
                                                "Estrada" -> estradaAgeGroupTypes
                                                else -> pistaAgeGroupTypes
                                            }

                                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                                Text(
                                                    text = "GÉNERO",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 1.sp,
                                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                                )
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    genders.forEach { g ->
                                                        val isSelected = currentGender == g
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(if (isSelected) CyberCyan else Color.White.copy(alpha = 0.03f))
                                                                .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    when (catName) {
                                                                        "BTT" -> bttGender = g
                                                                        "Estrada" -> estradaGender = g
                                                                        "Pista" -> pistaGender = g
                                                                    }
                                                                    selectedSubCats = selectedSubCats.filterNot { it.startsWith(catName) }
                                                                }
                                                                .padding(vertical = 10.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(text = g, color = if (isSelected) MidnightBlue else Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(20.dp))
                                                Text(
                                                    text = "TIPO DE ESCALÃO",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 1.sp,
                                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                                                )
                                                val ageGroupTypesList = listOf("Escolas", "Cadetes")
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    val isNenhumSelected = currentAgeGroupTypes.isEmpty()
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (isNenhumSelected) CyberCyan else Color.White.copy(alpha = 0.03f))
                                                            .border(1.dp, if (isNenhumSelected) CyberCyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                when (catName) {
                                                                    "BTT" -> bttAgeGroupTypes = emptySet()
                                                                    "Estrada" -> estradaAgeGroupTypes = emptySet()
                                                                    "Pista" -> pistaAgeGroupTypes = emptySet()
                                                                }
                                                                selectedSubCats = selectedSubCats.filterNot { it.startsWith(catName) }
                                                            }
                                                            .padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(text = "Nenhum", color = if (isNenhumSelected) MidnightBlue else Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    
                                                    ageGroupTypesList.forEach { type ->
                                                        val isSelected = type in currentAgeGroupTypes
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(if (isSelected) CyberCyan else Color.White.copy(alpha = 0.03f))
                                                                .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                                                .clickable {
                                                                    val newTypes = if (isSelected) {
                                                                        currentAgeGroupTypes - type
                                                                    } else {
                                                                        currentAgeGroupTypes + type
                                                                    }
                                                                    when (catName) {
                                                                        "BTT" -> bttAgeGroupTypes = newTypes
                                                                        "Estrada" -> estradaAgeGroupTypes = newTypes
                                                                        "Pista" -> pistaAgeGroupTypes = newTypes
                                                                    }
                                                                    selectedSubCats = selectedSubCats.filterNot { subCat ->
                                                                        subCat.startsWith(catName) && when (type) {
                                                                            "Escolas" -> subCat.contains("Sub-7") || subCat.contains("Sub-9") || subCat.contains("Sub-11") || subCat.contains("Sub-13") || subCat.contains("Sub-15")
                                                                            "Cadetes" -> subCat.contains("Sub-17")
                                                                            else -> false
                                                                        }
                                                                    }
                                                                }
                                                                .padding(vertical = 10.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(text = type, color = if (isSelected) MidnightBlue else Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }

                                                currentAgeGroupTypes.forEach { ageGroup ->
                                                    Spacer(modifier = Modifier.height(24.dp))
                                                    Text(
                                                        text = "ESCALÕES PARTICIPANTES - $ageGroup".uppercase(),
                                                        color = CyberCyan.copy(alpha = 0.6f),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 1.sp,
                                                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                                                    )

                                                    val filteredSubCats = when (ageGroup) {
                                                        "Escolas" -> {
                                                            when (currentGender) {
                                                                "Masculino" -> listOf("Sub-7 Masculino", "Sub-9 Masculino", "Sub-11 Masculino", "Sub-13 Masculino", "Sub-15 Masculino").map { "$catName $it" }
                                                                "Feminino" -> listOf("Sub-7 Feminino", "Sub-9 Feminino", "Sub-11 Feminino", "Sub-13 Feminino", "Sub-15 Feminino").map { "$catName $it" }
                                                                "Misto" -> null
                                                                else -> emptyList()
                                                            }
                                                        }
                                                        "Cadetes" -> {
                                                            when (currentGender) {
                                                                "Masculino" -> listOf("Sub-17 Masculino").map { "$catName $it" }
                                                                "Feminino" -> listOf("Sub-17 Feminino").map { "$catName $it" }
                                                                "Misto" -> listOf("Sub-17 Masculino", "Sub-17 Feminino").map { "$catName $it" }
                                                                else -> emptyList()
                                                            }
                                                        }
                                                        else -> emptyList()
                                                    }

                                                    if (filteredSubCats != null) {
                                                        FlowRow(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            filteredSubCats.forEach { cat ->
                                                                val isSelected = cat in selectedSubCats
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                                                                        .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                                        .clickable {
                                                                            selectedSubCats = if (isSelected) selectedSubCats - cat else selectedSubCats + cat
                                                                        }
                                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = cat.substringAfter("$catName "),
                                                                        color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.5f),
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                            Text(
                                                                text = "Escalões Masculinos",
                                                                color = Color.White.copy(alpha = 0.6f),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            val mascCats = listOf("Sub-7 Masculino", "Sub-9 Masculino", "Sub-11 Masculino", "Sub-13 Masculino", "Sub-15 Masculino").map { "$catName $it" }
                                                            FlowRow(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                mascCats.forEach { cat ->
                                                                    val isSelected = cat in selectedSubCats
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                                                                            .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                                            .clickable {
                                                                                selectedSubCats = if (isSelected) selectedSubCats - cat else selectedSubCats + cat
                                                                            }
                                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = cat.substringAfter("$catName "),
                                                                            color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.5f),
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Text(
                                                                text = "Escalões Femininos",
                                                                color = Color.White.copy(alpha = 0.6f),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            val femCats = listOf("Sub-7 Feminino", "Sub-9 Feminino", "Sub-11 Feminino", "Sub-13 Feminino", "Sub-15 Feminino").map { "$catName $it" }
                                                            FlowRow(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                femCats.forEach { cat ->
                                                                    val isSelected = cat in selectedSubCats
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                                                                            .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                                            .clickable {
                                                                                selectedSubCats = if (isSelected) selectedSubCats - cat else selectedSubCats + cat
                                                                            }
                                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = cat.substringAfter("$catName "),
                                                                            color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.5f),
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.Bold
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
                        3 -> {
                            Text("ATLETAS PARTICIPANTES", color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
                            if (allAthletes.isEmpty()) {
                                Text("Nenhum atleta disponível", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
                            } else {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    allAthletes.forEach { athlete ->
                                        val isSelected = athlete.id in selectedAthleteIds
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                                .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    selectedAthleteIds = if (isSelected) {
                                                        selectedAthleteIds - athlete.id!!
                                                    } else {
                                                        selectedAthleteIds + athlete.id!!
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(if (isSelected) CyberCyan else Color.White.copy(alpha = 0.3f), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = athlete.name, color = if (isSelected) CyberCyan else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
                            Text("REVISÃO DA PROVA", color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val reviewItems = listOf(
                                    "Título" to title.ifBlank { "(Não definido)" },
                                    "Data" to try {
                                        val instant = parseFlexibleDate(date)
                                        if (instant != null) {
                                            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                                            "${localDate.dayOfMonth}/${localDate.monthValue}/${localDate.year}"
                                        } else if (date.isBlank()) "(Não definida)" else date
                                    } catch(e: Exception) { date },
                                    "Hora de Início" to startTime.ifBlank { "(Não definida)" },
                                    "Local" to location.ifBlank { "(Não definido)" },
                                    "Descrição" to description.ifBlank { "(Não definida)" },
                                    "Modalidade" to category,
                                    "Estado" to status,
                                    "Configuração BTT" to if (category.split(", ").contains("BTT")) "Género: $bttGender | Escalão: ${if (bttAgeGroupTypes.isEmpty()) "Nenhum" else bttAgeGroupTypes.joinToString(", ")}" else "N/A",
                                    "Configuração Estrada" to if (category.split(", ").contains("Estrada")) "Género: $estradaGender | Escalão: ${if (estradaAgeGroupTypes.isEmpty()) "Nenhum" else estradaAgeGroupTypes.joinToString(", ")}" else "N/A",
                                    "Configuração Pista" to if (category.split(", ").contains("Pista")) "Género: $pistaGender | Escalão: ${if (pistaAgeGroupTypes.isEmpty()) "Nenhum" else pistaAgeGroupTypes.joinToString(", ")}" else "N/A",
                                    "Escalões Selecionados" to if (selectedSubCats.isEmpty()) "Nenhum selecionado" else selectedSubCats.joinToString(", "),
                                    "Atletas inscritos" to if (selectedAthleteIds.isEmpty()) "Nenhum selecionado" else {
                                        selectedAthleteIds.mapNotNull { id -> allAthletes.find { it.id == id }?.name }.joinToString(", ")
                                    }
                                )
                                
                                reviewItems.forEach { (label, valStr) ->
                                    Column {
                                        Text(text = label.uppercase(), color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = valStr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        PremiumButton(
                            text = "VOLTAR",
                            onClick = { currentStep-- },
                            variant = "secondary"
                        )
                    } else {
                        PremiumButton(
                            text = stringResource(R.string.common_cancel),
                            onClick = onDismiss,
                            variant = "outline"
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    if (currentStep < 4) {
                        PremiumButton(
                            text = "SEGUINTE",
                            onClick = { currentStep++ },
                            modifier = Modifier.weight(1f),
                            enabled = when (currentStep) {
                                1 -> title.isNotBlank() && date.isNotBlank()
                                else -> true
                            }
                        )
                    } else {
                        PremiumButton(
                            text = stringResource(R.string.races_save_race),
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    
                                    // Validar regras oficiais de distância e duração da UVP-FPC
                                    val validationResult = validateRaceRules(
                                        format = raceFormat,
                                        categories = category,
                                        subCats = selectedSubCats,
                                        distStr = distanceKm,
                                        durStr = durationMinutes
                                    )
                                    if (!validationResult.first) {
                                        android.widget.Toast.makeText(context, validationResult.second, android.widget.Toast.LENGTH_LONG).show()
                                        isSaving = false
                                        return@launch
                                    }
                                    
                                    val activeCats = category.split(", ").filter { it.isNotBlank() }
                                    val finalGender = if (activeCats.size > 1) {
                                        activeCats.joinToString(", ") { cat ->
                                            val g = when (cat) {
                                                "BTT" -> bttGender
                                                "Estrada" -> estradaGender
                                                else -> pistaGender
                                            }
                                            "$cat: $g"
                                        }
                                    } else {
                                        when (activeCats.firstOrNull()) {
                                            "BTT" -> bttGender
                                            "Estrada" -> estradaGender
                                            else -> pistaGender
                                        }
                                    }
                                    val updatedRace = race.copy(
                                        title = title,
                                        date = date,
                                        category = category,
                                        status = status,
                                        location = location.ifBlank { null },
                                        description = description.ifBlank { null },
                                        gender = finalGender,
                                        sub_categories = selectedSubCats,
                                        start_time = startTime.ifBlank { null },
                                        link = link.ifBlank { null },
                                        race_format = raceFormat,
                                        distance_km = distanceKm.toDoubleOrNull(),
                                        duration_minutes = durationMinutes.toIntOrNull(),
                                        sub_stages = subStagesList
                                    )
                                    val success = onSave(updatedRace, selectedAthleteIds)
                                    if (!success) isSaving = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            isLoading = isSaving
                        )
                    }
                }
            }
        }

        if (showAddSubStageDialog) {
            Dialog(onDismissRequest = { showAddSubStageDialog = false }) {
                HyperGlassCard(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = CyberCyan,
                    variant = "solid"
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ADICIONAR EXERCÍCIO / SUB-PROVA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        PremiumTextField(
                            value = subStageName,
                            onValueChange = { subStageName = it },
                            label = "NOME DO EXERCÍCIO",
                            placeholder = "Ex: Gincana de Destreza",
                            leadingIcon = Icons.Rounded.SportsScore
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Tipo de exercício
                        Text("TIPO", color = CyberCyan.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        var typeExpanded by remember { mutableStateOf(false) }
                        val types = listOf("gincana", "linha", "xcc", "cronometro")
                        ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                            OutlinedTextField(
                                value = subStageType.uppercase(),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = CyberCyan, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }, modifier = Modifier.background(MidnightBlue)) {
                                types.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.uppercase(), color = Color.White) },
                                        onClick = {
                                            subStageType = t
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(value = subStageDistance, onValueChange = { subStageDistance = it }, label = "DISTÂNCIA (KM)", placeholder = "Ex: 2.5", leadingIcon = Icons.Rounded.Navigation)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PremiumTextField(value = subStageDuration, onValueChange = { subStageDuration = it }, label = "DURAÇÃO (MIN)", placeholder = "Ex: 15", leadingIcon = Icons.Rounded.Timer)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PremiumButton(
                                text = "CANCELAR",
                                onClick = {
                                    showAddSubStageDialog = false
                                    subStageName = ""
                                    subStageDistance = ""
                                    subStageDuration = ""
                                },
                                modifier = Modifier.weight(1f),
                                variant = "outline"
                            )
                            PremiumButton(
                                text = "ADICIONAR",
                                onClick = {
                                    if (subStageName.isBlank()) {
                                        android.widget.Toast.makeText(context, "Insira o nome do exercício.", android.widget.Toast.LENGTH_SHORT).show()
                                        return@PremiumButton
                                    }
                                    val newStage = com.ventgui.app.data.model.RaceSubStage(
                                        id = null,
                                        race_id = "",
                                        name = subStageName,
                                        stage_type = subStageType,
                                        distance_km = subStageDistance.toDoubleOrNull(),
                                        duration_minutes = subStageDuration.toIntOrNull()
                                    )
                                    subStagesList = subStagesList + newStage
                                    showAddSubStageDialog = false
                                    
                                    // Limpar campos
                                    subStageName = ""
                                    subStageDistance = ""
                                    subStageDuration = ""
                                },
                                enabled = subStageName.isNotBlank(),
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun validateRaceRules(
    format: String,
    categories: String,
    subCats: List<String>,
    distStr: String,
    durStr: String
): Pair<Boolean, String> {
    val dist = distStr.toDoubleOrNull()
    val dur = durStr.toIntOrNull()
    
    val disciplines = categories.split(", ").map { it.uppercase() }
    val hasEstrada = "ESTRADA" in disciplines
    val hasBtt = "BTT" in disciplines
    
    val hasSub15 = subCats.any { it.contains("Sub-15", ignoreCase = true) }
    val hasSub17 = subCats.any { it.contains("Sub-17", ignoreCase = true) || it.contains("Cadetes", ignoreCase = true) }
    val hasSub17Masc = subCats.any { (it.contains("Sub-17", ignoreCase = true) || it.contains("Cadetes", ignoreCase = true)) && it.contains("Masculino", ignoreCase = true) }
    val hasSub17Fem = subCats.any { (it.contains("Sub-17", ignoreCase = true) || it.contains("Cadetes", ignoreCase = true)) && it.contains("Feminino", ignoreCase = true) }

    if (format == "Encontro de Escolas") {
        return Pair(true, "")
    }

    // Estrada Sub-15
    if (hasEstrada && hasSub15) {
        if (dist != null && dist > 30.0) {
            return Pair(false, "A distância máxima regulamentar da UVP-FPC para Sub-15 Estrada é de 30 km. (Distância inserida: ${dist}km)")
        }
    }

    // Estrada Sub-17
    if (hasEstrada && hasSub17) {
        if (hasSub17Masc && dist != null && dist > 80.0) {
            return Pair(false, "A distância máxima regulamentar da UVP-FPC para Sub-17 Masculino Estrada é de 80 km. (Distância inserida: ${dist}km)")
        }
        if (hasSub17Fem && dist != null && dist > 60.0) {
            return Pair(false, "A distância máxima regulamentar da UVP-FPC para Sub-17 Feminino Estrada é de 60 km. (Distância inserida: ${dist}km)")
        }
    }

    // BTT XCC Sub-15
    val isXcc = format.uppercase().contains("XCC") || format.uppercase().contains("XCO")
    if (hasBtt && hasSub15 && isXcc) {
        if (dur != null && (dur < 20 || dur > 30)) {
            return Pair(false, "A duração regulamentar para Sub-15 BTT XCC deve situar-se entre 20 e 30 minutos (corrida com 8 a 12 km). (Duração inserida: ${dur}min)")
        }
    }

    return Pair(true, "")
}
