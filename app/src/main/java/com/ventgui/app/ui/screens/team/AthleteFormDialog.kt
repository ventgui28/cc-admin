package com.ventgui.app.ui.screens.team

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.ui.components.*
import androidx.compose.ui.layout.ContentScale
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteFormDialog(athlete: Athlete?, onDismiss: () -> Unit, onSave: (Athlete) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(athlete?.name ?: "") }
    var photoUrl by rememberSaveable { mutableStateOf(athlete?.photo_url ?: "") }
    
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val mimeType = context.contentResolver.getType(uri)
                    if (mimeType == null || !mimeType.startsWith("image/")) {
                        android.widget.Toast.makeText(context, "Apenas ficheiros de imagem são permitidos.", android.widget.Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val maxSize = 5 * 1024 * 1024 // 5MB
                        if (bytes.size > maxSize) {
                            android.widget.Toast.makeText(context, "Imagem demasiado grande. Máximo: 5MB.", android.widget.Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        android.widget.Toast.makeText(context, "A carregar foto...", android.widget.Toast.LENGTH_SHORT).show()
                        val fileName = "athlete_${System.currentTimeMillis()}_avatar.jpg"
                        SupabaseClient.client.storage.from("avatars").upload(fileName, bytes) {
                            upsert = true
                        }
                        val publicUrl = SupabaseClient.client.storage.from("avatars").publicUrl(fileName)
                        photoUrl = publicUrl
                        android.widget.Toast.makeText(context, "Foto carregada com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Erro ao carregar foto.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var gender by rememberSaveable {
        mutableStateOf(
            if (athlete?.category?.contains("Feminino", ignoreCase = true) == true) "Feminino" else "Masculino"
        )
    }
    var escalaoType by rememberSaveable {
        mutableStateOf(
            if (athlete?.category?.contains("Sub-17", ignoreCase = true) == true) "Cadetes" else "Escolas"
        )
    }
    
    val subCategories = remember(gender, escalaoType) {
        if (gender == "Masculino") {
            if (escalaoType == "Escolas") {
                listOf("Sub-7 Masculino", "Sub-9 Masculino", "Sub-11 Masculino", "Sub-13 Masculino", "Sub-15 Masculino")
            } else {
                listOf("Sub-17")
            }
        } else {
            if (escalaoType == "Escolas") {
                listOf("Sub-7 Feminino", "Sub-9 Feminino", "Sub-11 Feminino", "Sub-13 Feminino", "Sub-15 Feminino")
            } else {
                listOf("Sub-17 Feminino")
            }
        }
    }

    var cat by rememberSaveable { mutableStateOf(athlete?.category ?: "Sub-7 Masculino") }
    var license by rememberSaveable { mutableStateOf(athlete?.license_number ?: "") }
    var birth by rememberSaveable { mutableStateOf(athlete?.birth_date ?: "") }
    var phone by rememberSaveable { mutableStateOf(athlete?.phone ?: "") }
    var status by rememberSaveable { mutableStateOf(athlete?.status ?: "active") }

    var genderExpanded by rememberSaveable { mutableStateOf(false) }
    var escalaoTypeExpanded by rememberSaveable { mutableStateOf(false) }
    var subCategoryExpanded by rememberSaveable { mutableStateOf(false) }
    var statusExpanded by rememberSaveable { mutableStateOf(false) }

    val statuses = listOf("active", "injured")

    LaunchedEffect(gender, escalaoType) {
        if (cat !in subCategories) {
            cat = subCategories.firstOrNull() ?: ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            color = CyberCyan,
            variant = "solid"
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(CyberCyan.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (athlete == null) Icons.Rounded.PersonAdd else Icons.Rounded.Edit, null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(if (athlete == null) stringResource(R.string.team_add_athlete) else stringResource(R.string.team_edit_athlete), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .border(2.dp, CyberCyan, CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            if (photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(44.dp))
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(CyberCyan, CircleShape)
                                .border(1.dp, MidnightBlue, CircleShape)
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CameraAlt, null, tint = MidnightBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                FormSectionTitle(stringResource(R.string.team_section_basic).uppercase())
                PremiumTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.profile_full_name).uppercase(), placeholder = stringResource(R.string.team_athlete_name_placeholder), leadingIcon = Icons.Rounded.Badge)
                Spacer(modifier = Modifier.height(20.dp))
                
                // Gender Selection
                Text("GÉNERO", color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
                    OutlinedTextField(value = gender, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(genderExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = formFieldColors(), shape = RoundedCornerShape(16.dp))
                    ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }, modifier = Modifier.background(MidnightBlue)) {
                        listOf("Masculino", "Feminino").forEach { DropdownMenuItem(text = { Text(it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { gender = it; genderExpanded = false }) }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // Escalão Type Selection
                Text("TIPO DE ESCALÃO", color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                ExposedDropdownMenuBox(expanded = escalaoTypeExpanded, onExpandedChange = { escalaoTypeExpanded = !escalaoTypeExpanded }) {
                    OutlinedTextField(value = escalaoType, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(escalaoTypeExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = formFieldColors(), shape = RoundedCornerShape(16.dp))
                    ExposedDropdownMenu(expanded = escalaoTypeExpanded, onDismissRequest = { escalaoTypeExpanded = false }, modifier = Modifier.background(MidnightBlue)) {
                        listOf("Escolas", "Cadetes").forEach { DropdownMenuItem(text = { Text(it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { escalaoType = it; escalaoTypeExpanded = false }) }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // Specific Category (actual category saved in DB)
                Text(stringResource(R.string.team_athlete_category).uppercase(), color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                ExposedDropdownMenuBox(expanded = subCategoryExpanded, onExpandedChange = { subCategoryExpanded = !subCategoryExpanded }) {
                    OutlinedTextField(value = cat, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subCategoryExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = formFieldColors(), shape = RoundedCornerShape(16.dp))
                    ExposedDropdownMenu(expanded = subCategoryExpanded, onDismissRequest = { subCategoryExpanded = false }, modifier = Modifier.background(MidnightBlue)) {
                        subCategories.forEach { DropdownMenuItem(text = { Text(it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { cat = it; subCategoryExpanded = false }) }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(stringResource(R.string.common_status).uppercase(), color = CyberCyan.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 8.dp))
                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }) {
                    val statusLabel = when (status) {
                        "active" -> stringResource(R.string.team_status_active)
                        "injured" -> stringResource(R.string.team_status_injured)
                        else -> status
                    }
                    OutlinedTextField(value = statusLabel, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), colors = formFieldColors(), shape = RoundedCornerShape(16.dp))
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }, modifier = Modifier.background(MidnightBlue)) {
                        statuses.forEach { statKey ->
                            val label = when (statKey) {
                                "active" -> stringResource(R.string.team_status_active)
                                "injured" -> stringResource(R.string.team_status_injured)
                                else -> statKey
                            }
                            DropdownMenuItem(text = { Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, onClick = { status = statKey; statusExpanded = false })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                PremiumTextField(value = birth, onValueChange = { birth = it }, label = (stringResource(R.string.team_athlete_birth).uppercase() + " (AAAA-MM-DD)"), placeholder = "2000-01-01", leadingIcon = Icons.Rounded.Cake)
                Spacer(modifier = Modifier.height(20.dp))
                PremiumTextField(value = license, onValueChange = { license = it }, label = stringResource(R.string.team_athlete_license).uppercase(), placeholder = "123456", leadingIcon = Icons.Rounded.AssignmentInd)
                Spacer(modifier = Modifier.height(20.dp))
                PremiumTextField(value = phone, onValueChange = { phone = it }, label = "TELEFONE", placeholder = "912345678", leadingIcon = Icons.Rounded.Phone)
                
                Spacer(modifier = Modifier.height(40.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(56.dp)) { Text(stringResource(R.string.common_cancel).uppercase(), color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Black) }
                    PremiumButton(
                        text = stringResource(R.string.common_save).uppercase(),
                        onClick = { 
                            onSave(Athlete(
                                id = athlete?.id,
                                name = name,
                                category = cat,
                                photo_url = photoUrl,
                                status = status,
                                license_number = license,
                                birth_date = birth,
                                phone = if (phone.isBlank()) null else phone
                            ))
                        },
                        modifier = Modifier.weight(1.5f),
                        containerColor = if (name.isNotBlank()) CyberCyan else Color.White.copy(alpha = 0.1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun FormSectionTitle(t: String) { Text(t, color = Color(0xFF80D8FF).copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)) }

@Composable
fun formFieldColors() = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF80D8FF), unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedContainerColor = Color.White.copy(alpha = 0.05f), unfocusedContainerColor = Color.White.copy(alpha = 0.05f), focusedLabelColor = Color(0xFF80D8FF))
