package com.ventgui.app.ui.screens.team

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.R
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.JoinedRaceResult
import com.ventgui.app.data.model.Race
import com.ventgui.app.ui.components.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog

fun checkEmdStatus(emdValidade: String?): String {
    if (emdValidade.isNullOrBlank()) return "EXPIRADO"
    return try {
        val validade = java.time.LocalDate.parse(emdValidade)
        val hoje = java.time.LocalDate.now()
        val trintaDiasDepois = hoje.plusDays(30)
        when {
            validade.isBefore(hoje) -> "EXPIRADO"
            validade.isBefore(trintaDiasDepois) -> "AVISO"
            else -> "VALIDO"
        }
    } catch (e: Exception) {
        "EXPIRADO"
    }
}

@Composable
fun AthleteDetailsScreen(
    innerPadding: PaddingValues,
    athlete: Athlete?,
    onBack: () -> Unit
) {
    if (athlete == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nenhum atleta selecionado.", color = Color.White)
        }
        return
    }

    val scope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<Pair<JoinedRaceResult, Race>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }
    var showEquipmentDialog by remember { mutableStateOf(false) }

    if (showEnlargedPhoto && !athlete.photo_url.isNullOrBlank()) {
        Dialog(onDismissRequest = { showEnlargedPhoto = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.95f))
                    .border(2.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = athlete.photo_url,
                    contentDescription = "Foto ampliada de ${athlete.name}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showEnlargedPhoto = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Fechar",
                        tint = Color.White
                    )
                }
            }
        }
    }

    LaunchedEffect(athlete.id) {
        val athleteId = athlete.id
        if (athleteId != null) {
            scope.launch {
                try {
                    isLoading = true
                    val response = SupabaseClient.client.postgrest.from("race_results")
                        .select(Columns.raw("*, races(*)")) { filter { eq("athlete_id", athleteId) } }
                    val list = response.decodeList<JoinedRaceResult>()
                    results = list.map { res -> 
                        res to (res.races ?: Race(title = "Desconhecida", date = "-", category = "-")) 
                    }.sortedByDescending { pair -> pair.second.date }
                } catch (e: Exception) {
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PremiumMeshBackground()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .padding(top = innerPadding.calculateTopPadding())
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- TOP HEADER ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White)
                        }

                        // BRAND LOGO
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("C", color = MidnightBlue, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CANTANHEDE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text("CYCLING HUB", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }

                        // Placeholder
                        Box(modifier = Modifier.size(44.dp))
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Athlete Header Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                                .border(2.dp, CyberCyan.copy(alpha = 0.3f), CircleShape)
                                .then(
                                    if (!athlete.photo_url.isNullOrBlank()) {
                                        Modifier.clickable { showEnlargedPhoto = true }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!athlete.photo_url.isNullOrBlank()) {
                                AsyncImage(
                                    model = athlete.photo_url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Rounded.AccountCircle, null, tint = CyberCyan, modifier = Modifier.size(44.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(athlete.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = CyberCyan.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        text = athlete.category.uppercase(),
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        letterSpacing = 1.sp
                                    )
                                }
                                if (!athlete.license_number.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LIC: ${athlete.license_number}",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // Banner do EMD (Exame Médico-Desportivo)
                    val emdStatus = remember(athlete.emd_validade) { checkEmdStatus(athlete.emd_validade) }
                    if (emdStatus == "EXPIRADO") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x22FF3B30))
                                .border(1.dp, Color(0xFFFF3B30), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFF3B30))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("IMPEDIDO DE CORRER", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("O Exame Médico-Desportivo (EMD) expirou em ${athlete.emd_validade ?: "data desconhecida"}.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                            }
                        }
                    } else if (emdStatus == "AVISO") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x22FFCC00))
                                .border(1.dp, Color(0xFFFFCC00), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Info, null, tint = Color(0xFFFFCC00))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("EMD PERTO DO FIM", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("A validade do Exame Médico termina a ${athlete.emd_validade}.", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Stats Cards Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AthleteStatCard(Modifier.weight(1f), "PROVAS", if (isLoading) "..." else results.size.toString(), Icons.Rounded.DirectionsBike)
                        AthleteStatCard(Modifier.weight(1f), "PÓDIOS", if (isLoading) "..." else results.count { (it.first.position ?: 99) <= 3 }.toString(), Icons.Rounded.EmojiEvents, Color(0xFFFFD600))
                        AthleteStatCard(Modifier.weight(1f), "VITÓRIAS", if (isLoading) "..." else results.count { it.first.position == 1 }.toString(), Icons.Rounded.WorkspacePremium, Color(0xFF00E676))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Technical Details Card
                    HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("DADOS TÉCNICOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                val displayCat = if (athlete.category.isNullOrBlank()) "--" else athlete.category
                                AthleteDetailItem(Icons.Rounded.DirectionsBike, "CATEGORIA", displayCat)
                                val displayBirth = if (athlete.birth_date.isNullOrBlank()) "--" else athlete.birth_date
                                AthleteDetailItem(Icons.Rounded.Cake, "NASCIMENTO", displayBirth)
                                val displayLicense = if (athlete.license_number.isNullOrBlank()) "--" else athlete.license_number
                                AthleteDetailItem(Icons.Rounded.AssignmentInd, "LICENÇA", displayLicense)
                                val displayPhone = if (athlete.phone.isNullOrBlank()) "--" else athlete.phone
                                AthleteDetailItem(Icons.Rounded.Phone, "TELEFONE", displayPhone)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Cartão de Saúde & Encarregado
                    val context = LocalContext.current
                    HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("SAÚDE & AUTORIZAÇÃO PARENTAL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                val encarregadoNome = athlete.encarregado_educacao_nome ?: "--"
                                AthleteDetailItem(Icons.Rounded.Person, "ENCARREGADO", encarregadoNome)
                                val encarregadoContacto = athlete.encarregado_educacao_contacto ?: "--"
                                AthleteDetailItem(Icons.Rounded.Phone, "CONTACTO", encarregadoContacto)
                                val emdVal = athlete.emd_validade ?: "--"
                                AthleteDetailItem(Icons.Rounded.Event, "VALIDADE EMD", emdVal)
                                val termoStatus = if (athlete.termo_responsabilidade_assinado == true) "Entregue e Assinado" else "Pendente / Não Entregue"
                                AthleteDetailItem(Icons.Rounded.Assignment, "AUTORIZAÇÃO", termoStatus)
                            }
                            
                            if (athlete.termo_responsabilidade_assinado == true && !athlete.termo_responsabilidade_url.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                PremiumButton(
                                    text = "VER TERMO DE RESPONSABILIDADE",
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(athlete.termo_responsabilidade_url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "Erro ao abrir o termo.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = "outline"
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Ações de Equipamento
                    PremiumButton(
                        text = "GERIR EQUIPAMENTO DA BIKE",
                        onClick = {
                            showEquipmentDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = "solid"
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("HISTÓRICO RECENTE", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyberCyan)
                    }
                }
            } else if (results.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Sem registos competitivos.", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                    }
                }
            } else {
                items(results) { pair ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                        RaceHistoryItem(pair.first, pair.second)
                    }
                }
            }
        }
        
        if (showEquipmentDialog) {
            AthleteEquipmentDialog(
                athlete = athlete,
                onDismiss = { showEquipmentDialog = false }
            )
        }
    }
}

@Composable
private fun AthleteDetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
