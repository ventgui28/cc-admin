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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RaceFormDialog(
    race: Race?,
    allAthletes: List<Athlete>,
    initialSelectedAthleteIds: Set<String>,
    initialStep: Int = 1,
    onDismiss: () -> Unit,
    onSave: suspend (String, String, String, String, String, List<String>, String, String, Set<String>, String, String) -> Boolean
) {
    var title by rememberSaveable { mutableStateOf(race?.title ?: "") }
    var date by rememberSaveable { mutableStateOf(race?.date ?: "") }
    var category by rememberSaveable { mutableStateOf(race?.category ?: "BTT") }
    var status by rememberSaveable { mutableStateOf(race?.status ?: "Agendada") }
    var gender by rememberSaveable { mutableStateOf(race?.gender ?: "Misto") }
    var location by rememberSaveable { mutableStateOf(race?.location ?: "") }
    var description by rememberSaveable { mutableStateOf(race?.description ?: "") }
    var link by rememberSaveable { mutableStateOf(race?.link ?: "") }
    var selectedSubCats by rememberSaveable { mutableStateOf(race?.sub_categories ?: emptyList<String>()) }
    var selectedAthleteIds by remember { mutableStateOf(initialSelectedAthleteIds) }
    var startTime by rememberSaveable { mutableStateOf(race?.start_time ?: "") }
    
    LaunchedEffect(initialSelectedAthleteIds) {
        selectedAthleteIds = initialSelectedAthleteIds
    }
    
    // Independent configuration states for BTT and Estrada
    var bttGender by rememberSaveable {
        mutableStateOf(
            if (race?.category?.split(", ")?.contains("BTT") == true) {
                val g = race.gender ?: "Misto"
                if (g.contains("BTT: ")) {
                    g.substringAfter("BTT: ").substringBefore(",")
                } else g
            } else "Misto"
        )
    }
    var estradaGender by rememberSaveable {
        mutableStateOf(
            if (race?.category?.split(", ")?.contains("Estrada") == true) {
                val g = race.gender ?: "Misto"
                if (g.contains("Estrada: ")) {
                    g.substringAfter("Estrada: ").substringBefore(",")
                } else g
            } else "Misto"
        )
    }
    var bttAgeGroupTypes by remember {
        mutableStateOf(
            buildSet {
                if (race?.sub_categories?.any { it.startsWith("BTT Sub-7") || it.startsWith("BTT Sub-9") || it.startsWith("BTT Sub-11") || it.startsWith("BTT Sub-13") || it.startsWith("BTT Sub-15") } == true) {
                    add("Escolas")
                }
                if (race?.sub_categories?.any { it.startsWith("BTT Sub-17") } == true) {
                    add("Cadetes")
                }
            }
        )
    }
    var estradaAgeGroupTypes by remember {
        mutableStateOf(
            buildSet {
                if (race?.sub_categories?.any { it.startsWith("Estrada Sub-7") || it.startsWith("Estrada Sub-9") || it.startsWith("Estrada Sub-11") || it.startsWith("Estrada Sub-13") || it.startsWith("Estrada Sub-15") } == true) {
                    add("Escolas")
                }
                if (race?.sub_categories?.any { it.startsWith("Estrada Sub-17") } == true) {
                    add("Cadetes")
                }
            }
        )
    }
    
    var activeAccordionCategory by rememberSaveable {
        mutableStateOf(
            if (category.split(", ").contains("BTT")) "BTT" else "Estrada"
        )
    }
    
    var currentStep by rememberSaveable { mutableStateOf(initialStep) }
    
    val categories = listOf("BTT", "Estrada")
    val subCategories = listOf(
        "Sub-7 Masculino",
        "Sub-7 Feminino",
        "Sub-9 Masculino",
        "Sub-9 Feminino",
        "Sub-11 Masculino",
        "Sub-11 Feminino",
        "Sub-13 Masculino",
        "Sub-13 Feminino",
        "Sub-15 Masculino",
        "Sub-15 Feminino",
        "Sub-17 Masculino",
        "Sub-17 Feminino"
    )
    val statuses = listOf("Agendada", "A decorrer", "Concluída", "Cancelada")
    val genders = listOf("Masculino", "Feminino", "Misto")
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var statusExpanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Save as ISO string for database consistency
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
                                imageVector = if (race == null) Icons.Rounded.AddCircleOutline else Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (race == null) stringResource(R.string.races_add_race) else stringResource(R.string.races_edit_race),
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
                            val categoriesList = listOf("BTT", "Estrada")
                            Spacer(modifier = Modifier.height(20.dp))
                            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = if (category.isBlank()) "Nenhuma selecionada" else category.split(", ").joinToString(", ") { if (it == "BTT") "BTT" else "Estrada" },
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
                            if (race != null) {
                                // Removed from here as per user request to edit on details page
                            }
                        }
                        2 -> {
                            val activeCategories = if (category.isBlank()) listOf("BTT") else category.split(", ")
                            activeCategories.forEach { catName ->
                                val isExpanded = activeAccordionCategory == catName
                                
                                // Accordion Header Card
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
                                                    imageVector = if (catName == "BTT") Icons.Rounded.DirectionsBike else Icons.Rounded.DirectionsRun,
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
                                            val currentGender = if (catName == "BTT") bttGender else estradaGender
                                            val currentAgeGroupTypes = if (catName == "BTT") bttAgeGroupTypes else estradaAgeGroupTypes

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
                                                                    if (catName == "BTT") {
                                                                        bttGender = g
                                                                    } else {
                                                                        estradaGender = g
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
                                                    // "Nenhum" option
                                                    val isNenhumSelected = currentAgeGroupTypes.isEmpty()
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (isNenhumSelected) CyberCyan else Color.White.copy(alpha = 0.03f))
                                                            .border(1.dp, if (isNenhumSelected) CyberCyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                if (catName == "BTT") {
                                                                    bttAgeGroupTypes = emptySet()
                                                                } else {
                                                                    estradaAgeGroupTypes = emptySet()
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
                                                                    if (catName == "BTT") {
                                                                        bttAgeGroupTypes = newTypes
                                                                    } else {
                                                                        estradaAgeGroupTypes = newTypes
                                                                    }
                                                                    // Filter out subcategories matching the toggled type
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
                                                        // Escolas + Misto: Show separate selection for Masculinos and Femininos
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
                                    "Estado Inicial" to status,
                                    "Configuração BTT" to if (category.split(", ").contains("BTT")) "Género: $bttGender | Escalão: ${if (bttAgeGroupTypes.isEmpty()) "Nenhum" else bttAgeGroupTypes.joinToString(", ")}" else "N/A",
                                    "Configuração Estrada" to if (category.split(", ").contains("Estrada")) "Género: $estradaGender | Escalão: ${if (estradaAgeGroupTypes.isEmpty()) "Nenhum" else estradaAgeGroupTypes.joinToString(", ")}" else "N/A",
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
                        Button(
                            onClick = { currentStep-- },
                            modifier = Modifier.height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("VOLTAR", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(50.dp),
                            enabled = !isSaving
                        ) {
                            Text(
                                text = stringResource(R.string.common_cancel).uppercase(),
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    if (currentStep < 4) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = MidnightBlue
                            ),
                            shape = RoundedCornerShape(14.dp),
                            enabled = when (currentStep) {
                                1 -> title.isNotBlank() && date.isNotBlank()
                                else -> true
                            }
                        ) {
                            Text("SEGUINTE", fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    val finalGender = when {
                                        category.split(", ").contains("BTT") && category.split(", ").contains("Estrada") -> {
                                            "BTT: $bttGender, Estrada: $estradaGender"
                                        }
                                        category.split(", ").contains("BTT") -> bttGender
                                        else -> estradaGender
                                    }
                                    val success = onSave(title, date, category, status, finalGender, selectedSubCats, location, description, selectedAthleteIds, startTime, link)
                                    if (!success) isSaving = false
                                }
                            },
                            modifier = Modifier
                                .height(54.dp)
                                .weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = MidnightBlue
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MidnightBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.races_save_race).uppercase(),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
