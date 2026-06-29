package com.ventgui.app.ui.screens.team

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.AthleteEquipment
import com.ventgui.app.ui.components.*
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

data class ValidationResult(
    val isValid: Boolean,
    val message: String,
    val calculatedDevelopment: Double
)

fun validateEquipment(
    category: String,
    discipline: String,
    wheelSize: String,
    front: Int,
    rear: Int,
    circunferencia: Double,
    carbon: Boolean,
    rimOver65: Boolean,
    disc: Boolean,
    tt: Boolean
): ValidationResult {
    val catUpper = category.uppercase()
    
    // 1. Validação de Roda em BTT para Sub-11 ou inferior
    if (discipline.uppercase() == "BTT") {
        val isLowerCategory = catUpper.contains("SUB-7") || catUpper.contains("SUB-9") || catUpper.contains("SUB-11")
        if (isLowerCategory && wheelSize == "29") {
            return ValidationResult(
                isValid = false,
                message = "Roda 29\" é expressamente proibida no BTT para ciclistas Sub-11 ou inferiores.",
                calculatedDevelopment = 0.0
            )
        }
        return ValidationResult(
            isValid = true,
            message = "Configuração de BTT em conformidade regulamentar. (Não se aplicam restrições de andamentos em metros nesta vertente).",
            calculatedDevelopment = 0.0
        )
    }

    // 2. Estrada e Pista
    if (rear == 0) return ValidationResult(false, "Número de dentes do carreto inválido.", 0.0)
    val dev = (front.toDouble() / rear.toDouble()) * circunferencia
    
    // Validar limites por categoria e disciplina
    var maxAllowed = 0.0
    var hasLimit = false
    
    if (catUpper.contains("SUB-11")) {
        maxAllowed = 6.41
        hasLimit = true
    } else if (catUpper.contains("SUB-13")) {
        maxAllowed = 7.02
        hasLimit = true
    } else if (catUpper.contains("SUB-15")) {
        if (discipline.uppercase() == "ESTRADA") {
            maxAllowed = 7.32
            hasLimit = true
        } else if (discipline.uppercase() == "PISTA") {
            maxAllowed = 6.55
            hasLimit = true
        }
    } else if (catUpper.contains("SUB-17") || catUpper.contains("CADETES")) {
        if (discipline.uppercase() == "PISTA") {
            maxAllowed = 7.02
            hasLimit = true
        }
    }

    if (hasLimit && dev > maxAllowed) {
        val formattedDev = String.format(java.util.Locale.US, "%.2f", dev)
        return ValidationResult(
            isValid = false,
            message = "Andamento Ilegal: A relação resulta em $formattedDev metros por pedalada. O limite oficial para $category em $discipline é de $maxAllowed metros.",
            calculatedDevelopment = dev
        )
    }

    // Restrições de Material para Sub-15 e Sub-17 (Estrada e Pista)
    val isFormacao = catUpper.contains("SUB-15") || catUpper.contains("SUB-17") || catUpper.contains("CADETES")
    if (isFormacao) {
        if (carbon) return ValidationResult(false, "Rodas de carbono são expressamente proibidas nos escalões de formação (Sub-15/Sub-17) em Estrada e Pista.", dev)
        if (rimOver65) return ValidationResult(false, "Perfil de roda superior a 65mm é proibido para Sub-15/Sub-17 em Estrada/Pista.", dev)
        if (disc) return ValidationResult(false, "Rodas tapadas (lenticulares) são proibidas em Estrada/Pista nos escalões Sub-15 e Sub-17.", dev)
        if (tt) return ValidationResult(false, "Guiadores ou extensores de contrarrelógio são proibidos para Sub-15/Sub-17 em Estrada/Pista.", dev)
    }

    return ValidationResult(
        isValid = true,
        message = "Equipamento em total conformidade com o regulamento oficial da UVP-FPC.",
        calculatedDevelopment = dev
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteEquipmentDialog(
    athlete: Athlete,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var discipline by rememberSaveable { mutableStateOf("Estrada") }
    var wheelSize by rememberSaveable { mutableStateOf("700c") }
    var frontChainring by rememberSaveable { mutableStateOf(46) }
    var rearCog by rememberSaveable { mutableStateOf(16) }
    
    var pneuMedida by rememberSaveable { mutableStateOf("700x25c") }
    var circunferenciaManual by rememberSaveable { mutableStateOf("2.11") }
    
    var carbonWheels by rememberSaveable { mutableStateOf(false) }
    var rimProfileOver65 by rememberSaveable { mutableStateOf(false) }
    var discWheels by rememberSaveable { mutableStateOf(false) }
    var ttHandlebars by rememberSaveable { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var equipmentId by remember { mutableStateOf<String?>(null) }
    
    // Obter circunferência efetiva
    val circunferencia = remember(pneuMedida, circunferenciaManual) {
        if (pneuMedida == "Personalizado") {
            circunferenciaManual.toDoubleOrNull() ?: 2.11
        } else {
            when (pneuMedida) {
                "700x23c" -> 2.10
                "700x25c" -> 2.11
                "700x28c" -> 2.14
                "26x2.00" -> 2.07
                "27.5x2.10" -> 2.18
                "29x2.10" -> 2.29
                else -> 2.11
            }
        }
    }

    // Recarregar equipamento da disciplina selecionada
    LaunchedEffect(discipline, athlete.id) {
        val athleteId = athlete.id
        if (athleteId != null) {
            scope.launch {
                try {
                    isLoading = true
                    val response = SupabaseClient.client.postgrest.from("athlete_equipment")
                        .select {
                            filter {
                                eq("athlete_id", athleteId)
                                eq("discipline", discipline)
                            }
                        }
                    val eqList = response.decodeList<AthleteEquipment>()
                    val existing = eqList.firstOrNull()
                    if (existing != null) {
                        equipmentId = existing.id
                        wheelSize = existing.wheel_size ?: "700c"
                        frontChainring = existing.front_chainring
                        rearCog = existing.rear_cog
                        carbonWheels = existing.carbon_wheels
                        rimProfileOver65 = existing.rim_profile_over_65
                        discWheels = existing.disc_wheels
                        ttHandlebars = existing.tt_handlebars
                    } else {
                        equipmentId = null
                        // Inicializar padrões conforme disciplina
                        if (discipline == "BTT") {
                            wheelSize = "29"
                            pneuMedida = "29x2.10"
                        } else {
                            wheelSize = "700c"
                            pneuMedida = "700x25c"
                        }
                        carbonWheels = false
                        rimProfileOver65 = false
                        discWheels = false
                        ttHandlebars = false
                    }
                } catch (e: Exception) {
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val validation = remember(
        athlete.category, discipline, wheelSize, frontChainring, rearCog,
        circunferencia, carbonWheels, rimProfileOver65, discWheels, ttHandlebars
    ) {
        validateEquipment(
            category = athlete.category,
            discipline = discipline,
            wheelSize = wheelSize,
            front = frontChainring,
            rear = rearCog,
            circunferencia = circunferencia,
            carbon = carbonWheels,
            rimOver65 = rimProfileOver65,
            disc = discWheels,
            tt = ttHandlebars
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = if (validation.isValid) CyberCyan else Color(0xFFFF3B30),
            variant = "solid"
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header do Cockpit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                (if (validation.isValid) CyberCyan else Color(0xFFFF3B30)).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Build,
                            contentDescription = null,
                            tint = if (validation.isValid) CyberCyan else Color(0xFFFF3B30),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "OFICINA DE EQUIPAMENTO",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${athlete.name} (${athlete.category})",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyberCyan)
                    }
                } else {
                    // Seletor de Disciplina (Tabs rápidas)
                    Text(
                        text = "DISCIPLINA",
                        color = CyberCyan.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Estrada", "Pista", "BTT").forEach { discOpt ->
                            val isSelected = discipline == discOpt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                    .border(
                                        1.dp,
                                        if (isSelected) CyberCyan else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { discipline = discOpt }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = discOpt,
                                    color = if (isSelected) CyberCyan else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Roda e Pneu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TAMANHO DA RODA",
                                color = CyberCyan.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val sizes = if (discipline == "BTT") listOf("24", "26", "27.5", "29") else listOf("700c", "24", "26")
                            var sizeExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = sizeExpanded,
                                onExpandedChange = { sizeExpanded = !sizeExpanded }
                            ) {
                                OutlinedTextField(
                                    value = wheelSize,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sizeExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = sizeExpanded,
                                    onDismissRequest = { sizeExpanded = false },
                                    modifier = Modifier.background(MidnightBlue)
                                ) {
                                    sizes.forEach { sz ->
                                        DropdownMenuItem(
                                            text = { Text(sz, color = Color.White, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                wheelSize = sz
                                                sizeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MEDIDA DO PNEU",
                                color = CyberCyan.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val pneus = if (discipline == "BTT") listOf("26x2.00", "27.5x2.10", "29x2.10", "Personalizado") else listOf("700x23c", "700x25c", "700x28c", "Personalizado")
                            var pneuExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = pneuExpanded,
                                onExpandedChange = { pneuExpanded = !pneuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = pneuMedida,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(pneuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = pneuExpanded,
                                    onDismissRequest = { pneuExpanded = false },
                                    modifier = Modifier.background(MidnightBlue)
                                ) {
                                    pneus.forEach { pn ->
                                        DropdownMenuItem(
                                            text = { Text(pn, color = Color.White, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                pneuMedida = pn
                                                pneuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (pneuMedida == "Personalizado") {
                        Spacer(modifier = Modifier.height(12.dp))
                        PremiumTextField(
                            value = circunferenciaManual,
                            onValueChange = { circunferenciaManual = it },
                            label = "CIRCUNFERÊNCIA MANUAL (METROS)",
                            placeholder = "2.11",
                            leadingIcon = Icons.Rounded.SettingsEthernet
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Relação de Transmissão (Prato e Carreto)
                    if (discipline != "BTT") {
                        Text(
                            text = "RELAÇÃO DE TRANSMISSÃO",
                            color = CyberCyan.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Prato (Front)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("PRATO (DENTES)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (frontChainring > 30) frontChainring-- },
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.Remove, null, tint = Color.White)
                                    }
                                    Text(
                                        text = frontChainring.toString(),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    IconButton(
                                        onClick = { if (frontChainring < 60) frontChainring++ },
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.Add, null, tint = Color.White)
                                    }
                                }
                            }

                            // Carreto (Rear)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("CARRETO (DENTES)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (rearCog > 10) rearCog-- },
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.Remove, null, tint = Color.White)
                                    }
                                    Text(
                                        text = rearCog.toString(),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    IconButton(
                                        onClick = { if (rearCog < 34) rearCog++ },
                                        modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    ) {
                                        Icon(Icons.Rounded.Add, null, tint = Color.White)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Painel Dinâmico de Cálculo (Desenvolvimento)
                        val animateColor by animateColorAsState(
                            targetValue = if (validation.isValid) CyberCyan.copy(alpha = 0.15f) else Color(0x33FF3B30),
                            label = "colorAnim"
                        )
                        val animateBorderColor by animateColorAsState(
                            targetValue = if (validation.isValid) CyberCyan else Color(0xFFFF3B30),
                            label = "borderAnim"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(animateColor)
                                .border(1.dp, animateBorderColor, RoundedCornerShape(16.dp))
                                .padding(20.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "DESENVOLVIMENTO ESTIMADO",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val devText = String.format(java.util.Locale.US, "%.2f metros", validation.calculatedDevelopment)
                                Text(
                                    text = devText,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (validation.isValid) "🟢 DENTRO DO LIMITE REGULAMENTAR" else "🔴 EXCEDE O LIMITE LEGAL",
                                    color = if (validation.isValid) CyberCyan else Color(0xFFFF3B30),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Restrições de Material para Sub-15/17
                    val isFormacao = athlete.category.uppercase().contains("SUB-15") || athlete.category.uppercase().contains("SUB-17") || athlete.category.uppercase().contains("CADETES")
                    if (isFormacao && discipline != "BTT") {
                        Text(
                            text = "RESTRIÇÕES DE MATERIAL (SUB-15 / SUB-17)",
                            color = CyberCyan.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MaterialRestritCheckBox("Rodas de Carbono", carbonWheels) { carbonWheels = it }
                            MaterialRestritCheckBox("Rodas com Perfil Superior a 65mm", rimProfileOver65) { rimProfileOver65 = it }
                            MaterialRestritCheckBox("Rodas Tapadas (Lenticulares)", discWheels) { discWheels = it }
                            MaterialRestritCheckBox("Guiador ou Extensor de Contrarrelógio", ttHandlebars) { ttHandlebars = it }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Mensagem de Erro de Validação
                    AnimatedVisibility(
                        visible = !validation.isValid,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x22FF3B30))
                                .border(1.dp, Color(0xFFFF3B30), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = validation.message,
                                color = Color(0xFFFF3B30),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Ações do Formulário
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumButton(
                            text = "CANCELAR",
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            variant = "outline"
                        )
                        PremiumButton(
                            text = if (isSaving) "A GUARDAR..." else "GRAVAR",
                            onClick = {
                                if (!validation.isValid) {
                                    android.widget.Toast.makeText(context, validation.message, android.widget.Toast.LENGTH_LONG).show()
                                    return@PremiumButton
                                }
                                scope.launch {
                                    try {
                                        isSaving = true
                                        val payload = AthleteEquipment(
                                            id = equipmentId,
                                            athlete_id = athlete.id!!,
                                            discipline = discipline,
                                            wheel_size = wheelSize,
                                            front_chainring = frontChainring,
                                            rear_cog = rearCog,
                                            carbon_wheels = carbonWheels,
                                            rim_profile_over_65 = rimProfileOver65,
                                            disc_wheels = discWheels,
                                            tt_handlebars = ttHandlebars,
                                            is_validated = validation.isValid
                                        )
                                        SupabaseClient.client.postgrest.from("athlete_equipment").insert(payload) {
                                            select()
                                        }
                                        android.widget.Toast.makeText(context, "Equipamento gravado e validado com sucesso! 🟢", android.widget.Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Erro ao gravar equipamento: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = validation.isValid && !isSaving,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialRestritCheckBox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkmarkColor = MidnightBlue,
                checkedColor = Color(0xFFFF3B30),
                uncheckedColor = Color.White.copy(alpha = 0.4f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
