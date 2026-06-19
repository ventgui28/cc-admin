package com.ventgui.app.ui.screens.races

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.ui.components.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceFinishDialog(
    race: Race,
    results: List<Pair<RaceResult, Athlete>>,
    onDismiss: () -> Unit,
    onSave: (List<RaceResult>) -> Unit
) {
    var activeAthleteForPositionPicker by remember { mutableStateOf<String?>(null) }
    var activeAthleteForTimePicker by remember { mutableStateOf<String?>(null) }

    // Map to keep track of user inputs (Position, Time, Status)
    val inputs = remember {
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

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = CyberCyan,
            variant = "solid"
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(CyberCyan.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Concluir Prova",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = race.title,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Removed from here as per user request to edit on details page

                Text(
                    text = "CLASSIFICAÇÃO DOS ATLETAS",
                    color = CyberCyan.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )

                if (results.isEmpty()) {
                    Text(
                        text = "Nenhum atleta participante inscrito nesta prova.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 16.dp)
                    )
                } else {
                    results.forEach { (_, athlete) ->
                        val athleteId = athlete.id!!
                        val currentInput = inputs[athleteId] ?: Triple("", "", "Normal")

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = athlete.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Position Input (Custom Scrollable Picker Overlay)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                ) {
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
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            disabledTextColor = Color.White.copy(alpha = 0.3f),
                                            disabledBorderColor = Color.White.copy(alpha = 0.05f)
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

                                // Time Input (Custom Scrollable Picker Overlay)
                                Box(
                                    modifier = Modifier
                                        .weight(1.5f)
                                ) {
                                    OutlinedTextField(
                                        value = currentInput.second,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = currentInput.third == "Normal",
                                        label = { Text("Tempo", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                                        placeholder = { Text("Selecionar", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                            disabledTextColor = Color.White.copy(alpha = 0.3f),
                                            disabledBorderColor = Color.White.copy(alpha = 0.05f)
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
                            
                            // Choice Chips for Status
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                                inputs[athleteId] = Triple(
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
                }

                Spacer(modifier = Modifier.height(32.dp))

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
                            val updatedList = results.map { (res, athlete) ->
                                val input = inputs[athlete.id!!] ?: Triple("", "", "Normal")
                                val status = input.third
                                if (status != "Normal") {
                                    res.copy(
                                        position = null,
                                        time = status
                                    )
                                } else {
                                    res.copy(
                                        position = input.first.toIntOrNull(),
                                        time = if (input.second.isBlank()) null else input.second
                                    )
                                }
                            }
                            onSave(updatedList)
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }

    if (activeAthleteForPositionPicker != null) {
        val athId = activeAthleteForPositionPicker!!
        val currentVal = inputs[athId]?.first?.toIntOrNull() ?: 1
        NumberScrollPickerDialog(
            title = "Selecionar Posição",
            range = 1..150,
            initialValue = currentVal,
            onDismiss = { activeAthleteForPositionPicker = null },
            onConfirm = { pickedVal ->
                val curr = inputs[athId] ?: Triple("", "", "Normal")
                inputs[athId] = Triple(pickedVal.toString(), curr.second, curr.third)
                activeAthleteForPositionPicker = null
            }
        )
    }

    if (activeAthleteForTimePicker != null) {
        val athId = activeAthleteForTimePicker!!
        val currentVal = inputs[athId]?.second ?: "00:00:00"
        TimeScrollPickerDialog(
            initialTime = currentVal,
            onDismiss = { activeAthleteForTimePicker = null },
            onConfirm = { pickedTime ->
                val curr = inputs[athId] ?: Triple("", "", "Normal")
                inputs[athId] = Triple(curr.first, pickedTime, curr.third)
                activeAthleteForTimePicker = null
            }
        )
    }
}
