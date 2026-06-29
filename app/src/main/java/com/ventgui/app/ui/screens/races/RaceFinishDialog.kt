package com.ventgui.app.ui.screens.races

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.data.model.RaceSubStage
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.ui.components.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

data class SubStageResultInput(
    val position: String = "",
    val time: String = "",
    val penaltiesSeconds: Int = 0,
    val pedagogicalStatus: String? = null
)

fun getDorsalChipColors(dorsalStr: String?, category: String): Pair<Color, Color> {
    if (dorsalStr.isNullOrBlank() || dorsalStr == "?") {
        return Pair(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.4f)) // cinza
    }
    val number = dorsalStr.toIntOrNull() ?: return Pair(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.4f))
    
    val cat = category.uppercase()
    val isSub13_15 = cat.contains("SUB-13") || cat.contains("SUB-15")
    val isSub17Masc = cat.contains("SUB-17") && !cat.contains("FEM") && !cat.contains("ROS")
    val isSub17Fem = cat.contains("SUB-17") && (cat.contains("FEM") || cat.contains("ROS"))
    
    return when {
        // Laranja (Série 801+) para Sub-13/Sub-15
        isSub13_15 && number >= 801 -> Pair(Color(0xFFFF9800).copy(alpha = 0.2f), Color(0xFFFF9800))
        // Verde (Série 701+) para Sub-17 Masc
        isSub17Masc && number >= 701 -> Pair(Color(0xFF4CAF50).copy(alpha = 0.2f), Color(0xFF4CAF50))
        // Rosa/Verde (Série 650/900) para Sub-17 Fem
        isSub17Fem && (number >= 650) -> Pair(Color(0xFFE91E63).copy(alpha = 0.2f), Color(0xFFE91E63))
        else -> Pair(Color.White.copy(alpha = 0.1f), Color.White) // padrão neutro
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceFinishDialog(
    race: Race,
    results: List<Pair<RaceResult, Athlete>>,
    onDismiss: () -> Unit,
    onSave: (List<RaceResult>, Int?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var activeTab by remember { mutableStateOf("Geral") } // "Geral" ou ID do SubStage
    
    var activeAthleteForPositionPicker by remember { mutableStateOf<String?>(null) }
    var activeAthleteForTimePicker by remember { mutableStateOf<String?>(null) }
    var activeTeamForPositionPicker by remember { mutableStateOf(false) }
    var teamPositionInput by remember { mutableStateOf(race.team_classification?.toString() ?: "") }
    
    // Edição inline de dorsais
    var editingAthleteDorsalId by remember { mutableStateOf<String?>(null) }
    var tempDorsalInput by remember { mutableStateOf("") }

    // Map para os resultados gerais (Chave: AthleteID)
    val generalInputs = remember {
        mutableStateMapOf<String, Triple<String, String, String>>().apply {
            results.forEach { (res, athlete) ->
                val status = when (res.time) {
                    "DNF", "DNS", "DSQ", "OTL" -> res.time
                    else -> "Normal"
                }
                val posStr = if (status == "Normal") res.position?.toString() ?: "" else ""
                val timeStr = if (status == "Normal") res.time ?: "" else ""
                put(athlete.id!!, Triple(posStr, timeStr, status))
            }
        }
    }
    
    // Dorsais locais para edição inline (Chave: AthleteID)
    val athleteDorsais = remember {
        mutableStateMapOf<String, String>().apply {
            results.forEach { (res, athlete) ->
                put(athlete.id!!, res.bib_number ?: "?")
            }
        }
    }

    // Map para os resultados de sub-provas (Chave: "athleteId_subStageId")
    val subStagesInputs = remember { mutableStateMapOf<String, SubStageResultInput>() }
    var isLoadingSubStagesData by remember { mutableStateOf(false) }

    // Carregar dados de sub-provas se a corrida possuir
    val subStages = race.sub_stages ?: emptyList()
    LaunchedEffect(race.id) {
        val raceId = race.id
        if (raceId != null && subStages.isNotEmpty()) {
            scope.launch {
                try {
                    isLoadingSubStagesData = true
                    val response = SupabaseClient.client.postgrest.from("race_results")
                        .select {
                            filter {
                                eq("race_id", raceId)
                            }
                        }
                    val existingList = response.decodeList<RaceResult>().filter { it.race_sub_stage_id != null }
                    existingList.forEach { r ->
                        val key = "${r.athlete_id}_${r.race_sub_stage_id}"
                        subStagesInputs[key] = SubStageResultInput(
                            position = r.position?.toString() ?: "",
                            time = r.time ?: "",
                            penaltiesSeconds = r.penalty_seconds,
                            pedagogicalStatus = r.pedagogical_status
                        )
                    }
                } catch (e: Exception) {
                } finally {
                    isLoadingSubStagesData = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            color = CyberCyan,
            variant = "solid"
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxHeight()
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(CyberCyan.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "CONCLUIR PROVA",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = race.title,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs para Encontro de Escolas
                if (race.race_format == "Encontro de Escolas" && subStages.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Aba Geral
                        val isGeralSelected = activeTab == "Geral"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isGeralSelected) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isGeralSelected) CyberCyan else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { activeTab = "Geral" }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("GERAL & DORSAL", color = if (isGeralSelected) CyberCyan else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        // Abas de sub-provas
                        subStages.forEach { stage ->
                            val isSelected = activeTab == stage.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { activeTab = stage.id!! }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(stage.name, color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Corpo do formulário (rolável)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isLoadingSubStagesData) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CyberCyan)
                        }
                    } else if (activeTab == "Geral") {
                        // --- ABA GERAL: Classificação da equipa, posição geral, tempo geral e dorsais ---
                        Text(
                            text = "CLASSIFICAÇÃO DA EQUIPA",
                            color = CyberCyan.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = teamPositionInput,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Posição Geral da Equipa", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                                        placeholder = { Text("Sem Classificação (Vazio)", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Transparent)
                                            .clickable { activeTeamForPositionPicker = true }
                                    )
                                }

                                if (teamPositionInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = { teamPositionInput = "" },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Rounded.Clear, null, tint = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }

                        Text(
                            text = "CLASSIFICAÇÃO GERAL DOS ATLETAS",
                            color = CyberCyan.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                        )

                        results.forEach { (_, athlete) ->
                            val athleteId = athlete.id!!
                            val currentInput = generalInputs[athleteId] ?: Triple("", "", "Normal")
                            val bibNum = athleteDorsais[athleteId] ?: "?"

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = athlete.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // Chip do Dorsal BTT regulamentar
                                    val chipColors = getDorsalChipColors(bibNum, athlete.category)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(chipColors.first)
                                            .border(1.dp, chipColors.second, RoundedCornerShape(8.dp))
                                            .clickable {
                                                editingAthleteDorsalId = athleteId
                                                tempDorsalInput = if (bibNum == "?") "" else bibNum
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "DORSAL: $bibNum",
                                            color = chipColors.second,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = currentInput.first,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = currentInput.third == "Normal",
                                            label = { Text("Posição", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                                            placeholder = { Text("Selecionar", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = CyberCyan,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        if (currentInput.third == "Normal") {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color.Transparent)
                                                    .clickable { activeAthleteForPositionPicker = athleteId }
                                            )
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1.3f)) {
                                        OutlinedTextField(
                                            value = currentInput.second,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = currentInput.third == "Normal",
                                            label = { Text("Tempo Geral", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                                            placeholder = { Text("Selecionar", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = CyberCyan,
                                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        if (currentInput.third == "Normal") {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(Color.Transparent)
                                                    .clickable { activeAthleteForTimePicker = athleteId }
                                            )
                                        }
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Normal", "OTL", "DNF", "DNS", "DSQ").forEach { option ->
                                        val isSelected = currentInput.third == option
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f))
                                                .border(1.dp, if (isSelected) CyberCyan else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    generalInputs[athleteId] = Triple(
                                                        if (option == "Normal") currentInput.first else "",
                                                        if (option == "Normal") currentInput.second else "",
                                                        option
                                                    )
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option,
                                                color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- ABA SUB-PROVA: Lançamento de exercício individual ---
                        val currentSubStage = subStages.find { it.id == activeTab }
                        if (currentSubStage != null) {
                            Text(
                                text = "EXERCÍCIO: ${currentSubStage.name.uppercase()}",
                                color = CyberCyan.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                            )
                            
                            results.forEach { (_, athlete) ->
                                val athleteId = athlete.id!!
                                val key = "${athleteId}_${currentSubStage.id}"
                                val currentStageInput = subStagesInputs[key] ?: SubStageResultInput()
                                val isGincana = currentSubStage.stage_type == "gincana"
                                
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.02f))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = athlete.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (isGincana) {
                                        // Gincana: Faltas e Classificação Pedagógica
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        val isPedagogic = athlete.category.uppercase().contains("SUB-7") || athlete.category.uppercase().contains("SUB-9")
                                        val faltas = currentStageInput.penaltiesSeconds / 10
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("FALTAS (+10s cada)", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            if (faltas > 0) {
                                                                val newPen = (faltas - 1) * 10
                                                                val newPed = if (isPedagogic) {
                                                                    if (newPen <= 30) "Completou com êxito" else "Continua o seu processo de formação ciclista"
                                                                } else null
                                                                subStagesInputs[key] = currentStageInput.copy(penaltiesSeconds = newPen, pedagogicalStatus = newPed)
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                                                    ) {
                                                        Icon(Icons.Rounded.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                    Text(
                                                        text = faltas.toString(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        modifier = Modifier.padding(horizontal = 16.dp)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            val newPen = (faltas + 1) * 10
                                                            val newPed = if (isPedagogic) {
                                                                if (newPen <= 30) "Completou com êxito" else "Continua o seu processo de formação ciclista"
                                                            } else null
                                                            subStagesInputs[key] = currentStageInput.copy(penaltiesSeconds = newPen, pedagogicalStatus = newPed)
                                                        },
                                                        modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                                                    ) {
                                                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                            
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("PENALIZAÇÃO", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                Text(
                                                    text = "${currentStageInput.penaltiesSeconds} segundos",
                                                    color = if (currentStageInput.penaltiesSeconds > 30) Color(0xFFFF9800) else CyberCyan,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                        
                                        if (isPedagogic) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            val statusPed = currentStageInput.pedagogicalStatus ?: "Completou com êxito"
                                            val isSuccess = statusPed == "Completou com êxito"
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSuccess) CyberCyan.copy(alpha = 0.1f) else Color(0x22FF9800))
                                                    .border(1.dp, if (isSuccess) CyberCyan else Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (isSuccess) Icons.Rounded.CheckCircleOutline else Icons.Rounded.Info,
                                                        contentDescription = null,
                                                        tint = if (isSuccess) CyberCyan else Color(0xFFFF9800),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isSuccess) "Completou com êxito" else "Continua formação ciclista",
                                                        color = if (isSuccess) CyberCyan else Color(0xFFFF9800),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // Linha / Outro: Posição e tempo
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = currentStageInput.position,
                                                onValueChange = {
                                                    if (it.all { c -> c.isDigit() }) {
                                                        subStagesInputs[key] = currentStageInput.copy(position = it)
                                                    }
                                                },
                                                label = { Text("Posição", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                                                placeholder = { Text("Ex: 1", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = CyberCyan,
                                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            
                                            OutlinedTextField(
                                                value = currentStageInput.time,
                                                onValueChange = {
                                                    subStagesInputs[key] = currentStageInput.copy(time = it)
                                                },
                                                label = { Text("Tempo", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                                                placeholder = { Text("Ex: 00:12:45", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1.5f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = CyberCyan,
                                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer de Ações
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    PremiumButton(
                        text = "CANCELAR",
                        onClick = onDismiss,
                        variant = "outline",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    PremiumButton(
                        text = "CONCLUIR",
                        onClick = {
                            // Montar a lista integrada contendo resultados Gerais e de Sub-provas
                            val finalResultsList = mutableListOf<RaceResult>()
                            
                            // 1. Adicionar resultados Gerais
                            results.forEach { (res, athlete) ->
                                val athleteId = athlete.id!!
                                val input = generalInputs[athleteId] ?: Triple("", "", "Normal")
                                val status = input.third
                                val bibNum = athleteDorsais[athleteId]
                                
                                val generalRes = if (status != "Normal") {
                                    res.copy(
                                        position = null,
                                        time = status,
                                        bib_number = if (bibNum == "?") null else bibNum
                                    )
                                } else {
                                    res.copy(
                                        position = input.first.toIntOrNull(),
                                        time = if (input.second.isBlank()) null else input.second,
                                        bib_number = if (bibNum == "?") null else bibNum
                                    )
                                }
                                finalResultsList.add(generalRes)
                            }
                            
                            // 2. Adicionar resultados de Sub-provas (Encontro de Escolas)
                            if (race.race_format == "Encontro de Escolas" && subStages.isNotEmpty()) {
                                results.forEach { (_, athlete) ->
                                    val athleteId = athlete.id!!
                                    subStages.forEach { stage ->
                                        val key = "${athleteId}_${stage.id}"
                                        val input = subStagesInputs[key]
                                        
                                        if (input != null) {
                                            // Calcular pontos da equipa baseados na classificação pedagógica para Sub-7/Sub-9
                                            val isPedagogic = athlete.category.uppercase().contains("SUB-7") || athlete.category.uppercase().contains("SUB-9")
                                            val points = if (isPedagogic && stage.stage_type == "gincana") {
                                                if (input.penaltiesSeconds <= 30) 1 else 2 // Completou êxito (1pt) vs Continua formação (2pt)
                                            } else null
                                            
                                            val subStageRes = RaceResult(
                                                id = null, // Deixamos nulo para upsert baseado na constraint UNIQUE
                                                race_id = race.id!!,
                                                athlete_id = athleteId,
                                                race_sub_stage_id = stage.id!!,
                                                position = input.position.toIntOrNull(),
                                                time = input.time.ifBlank { null },
                                                penalty_seconds = input.penaltiesSeconds,
                                                pedagogical_status = input.pedagogicalStatus,
                                                team_points = points,
                                                bib_number = athleteDorsais[athleteId].let { if (it == "?") null else it }
                                            )
                                            finalResultsList.add(subStageRes)
                                        }
                                    }
                                }
                            }
                            
                            onSave(finalResultsList, teamPositionInput.toIntOrNull())
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }

    // Picker de Classificação de Equipa
    if (activeTeamForPositionPicker) {
        val currentVal = teamPositionInput.toIntOrNull() ?: 1
        NumberScrollPickerDialog(
            title = "Selecionar Posição da Equipa",
            range = 1..150,
            initialValue = currentVal,
            onDismiss = { activeTeamForPositionPicker = false },
            onConfirm = { pickedVal ->
                teamPositionInput = pickedVal.toString()
                activeTeamForPositionPicker = false
            }
        )
    }

    // Picker de Posição de Atleta Geral
    if (activeAthleteForPositionPicker != null) {
        val athId = activeAthleteForPositionPicker!!
        val currentVal = generalInputs[athId]?.first?.toIntOrNull() ?: 1
        NumberScrollPickerDialog(
            title = "Selecionar Posição",
            range = 1..150,
            initialValue = currentVal,
            onDismiss = { activeAthleteForPositionPicker = null },
            onConfirm = { pickedVal ->
                val curr = generalInputs[athId] ?: Triple("", "", "Normal")
                generalInputs[athId] = Triple(pickedVal.toString(), curr.second, curr.third)
                activeAthleteForPositionPicker = null
            }
        )
    }

    // Picker de Tempo Geral de Atleta
    if (activeAthleteForTimePicker != null) {
        val athId = activeAthleteForTimePicker!!
        val currentVal = generalInputs[athId]?.second ?: "00:00:00"
        TimeScrollPickerDialog(
            initialTime = currentVal,
            onDismiss = { activeAthleteForTimePicker = null },
            onConfirm = { pickedTime ->
                val curr = generalInputs[athId] ?: Triple("", "", "Normal")
                generalInputs[athId] = Triple(curr.first, pickedTime, curr.third)
                activeAthleteForTimePicker = null
            }
        )
    }

    // Diálogo simples para Edição Inline de Dorsal
    if (editingAthleteDorsalId != null) {
        val athId = editingAthleteDorsalId!!
        val athleteName = results.find { it.second.id == athId }?.second?.name ?: "Atleta"
        
        Dialog(onDismissRequest = { editingAthleteDorsalId = null }) {
            HyperGlassCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = CyberCyan,
                variant = "solid"
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EDITAR DORSAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(athleteName, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = tempDorsalInput,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() }) {
                                tempDorsalInput = it
                            }
                        },
                        label = { Text("Número do Dorsal", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                        placeholder = { Text("Ex: 812", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Badge, null, tint = CyberCyan, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PremiumButton(
                            text = "CANCELAR",
                            onClick = { editingAthleteDorsalId = null },
                            modifier = Modifier.weight(1f),
                            variant = "outline"
                        )
                        PremiumButton(
                            text = "SALVAR",
                            onClick = {
                                athleteDorsais[athId] = tempDorsalInput.ifBlank { "?" }
                                editingAthleteDorsalId = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
