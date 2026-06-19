package com.ventgui.app.ui.screens.profile

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.model.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import com.ventgui.app.data.utils.UserLog
import com.ventgui.app.data.utils.UserLogger
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import com.ventgui.app.ui.components.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.mfa.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    onNavigateToAuditLog: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val user = SupabaseClient.client.auth.currentUserOrNull()
    var profile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showChangePasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showMFASetupDialog by rememberSaveable { mutableStateOf(false) }
    var mfaFactorId by rememberSaveable { mutableStateOf<String?>(null) }
    var mfaSecret by rememberSaveable { mutableStateOf<String?>(null) }

    
    val context = androidx.compose.ui.platform.LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    // Validate MIME type
                    val mimeType = context.contentResolver.getType(uri)
                    if (mimeType == null || !mimeType.startsWith("image/")) {
                        android.widget.Toast.makeText(context, "Apenas ficheiros de imagem são permitidos.", android.widget.Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        // Validate file size (max 5MB)
                        val maxSize = 5 * 1024 * 1024 // 5MB
                        if (bytes.size > maxSize) {
                            android.widget.Toast.makeText(context, "Imagem demasiado grande. Máximo: 5MB.", android.widget.Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        
                        android.widget.Toast.makeText(context, context.getString(R.string.profile_uploading_avatar), android.widget.Toast.LENGTH_SHORT).show()
                        // Use auth.uid as filename prefix (matches storage policy)
                        val fileName = "${user?.id}_avatar.jpg"
                        SupabaseClient.client.storage.from("avatars").upload(fileName, bytes) {
                            upsert = true
                        }
                        val publicUrl = SupabaseClient.client.storage.from("avatars").publicUrl(fileName)
                        
                        val updated = profile?.copy(avatar_url = publicUrl) ?: Profile(id = user?.id, avatar_url = publicUrl)
                        SupabaseClient.client.postgrest.from("profiles").upsert(updated)
                        UserLogger.log("Atualizou a foto de perfil")
                        profile = updated
                        android.widget.Toast.makeText(context, context.getString(R.string.profile_avatar_updated), android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Erro ao atualizar avatar. Tenta novamente.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val formatDate = { dateStr: String? ->
        try {
            if (dateStr == null) "Jan 2024"
            else {
                val date = java.time.OffsetDateTime.parse(dateStr)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale("pt", "PT"))
                date.format(formatter)
            }
        } catch (e: Exception) { "Jan 2024" }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    val fetchProfile = {
        scope.launch {
            if (user != null) {
                try {
                    if (!isRefreshing) isLoading = true
                    profile = SupabaseClient.client.postgrest.from("profiles")
                        .select { filter { eq("id", user.id) } }
                        .decodeSingleOrNull<Profile>()
                } catch (e: Exception) { 
                } finally {
                    isLoading = false
                    isRefreshing = false
                }
            } else {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(user?.id) {
        fetchProfile()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PremiumMeshBackground()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                fetchProfile()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 120.dp)
            ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- HEADER ---
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) { Icon(Icons.Rounded.ChevronLeft, null, tint = Color.White) }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Text("C", color = MidnightBlue, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CANTANHEDE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text("CYCLING HUB", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }

                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(
                                onClick = { android.widget.Toast.makeText(context, context.getString(R.string.profile_no_new_notifications), android.widget.Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            ) { Icon(Icons.Rounded.NotificationsNone, null, tint = Color.White) }
                            Box(modifier = Modifier.padding(4.dp).size(10.dp).background(CyberCyan, CircleShape).border(2.dp, MidnightBlue, CircleShape))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(stringResource(R.string.profile_title), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.profile_subtitle), color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- MAIN PROFILE CARD ---
                    HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier.size(80.dp).clip(CircleShape).border(2.dp, CyberCyan, CircleShape)
                                ) {
                                    AsyncImage(
                                        model = profile?.avatar_url ?: "https://ui-avatars.com/api/?name=${profile?.full_name ?: user?.email}&background=002B5B&color=fff",
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable {
                                        imagePicker.launch("image/*")
                                    },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Rounded.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
                            }
                            
                            Spacer(modifier = Modifier.width(20.dp))
                            
                            Column {
                                Text(profile?.full_name ?: stringResource(R.string.profile_default_user), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Surface(color = CyberCyan.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Icon(Icons.Rounded.RemoveRedEye, null, tint = CyberCyan, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(profile?.role?.uppercase() ?: "EDITOR", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(user?.email ?: "", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(profile?.phone ?: stringResource(R.string.common_not_set), color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- STATS ROW ---
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileStatItem(Icons.Rounded.AssignmentInd, stringResource(R.string.profile_role_label), profile?.role?.uppercase() ?: "EDITOR", Modifier.weight(1f))
                        ProfileStatItem(Icons.Rounded.Groups, stringResource(R.string.profile_access_label), stringResource(R.string.profile_access_full), Modifier.weight(1f))
                        ProfileStatItem(Icons.Rounded.CalendarMonth, stringResource(R.string.profile_member_since), formatDate(profile?.created_at), Modifier.weight(1.2f))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- ACCOUNT SECTION ---
                    ProfileCategoryHeader(stringResource(R.string.profile_category_account))
                    HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            ProfileOptionItem(Icons.Rounded.Person, stringResource(R.string.profile_personal_info), stringResource(R.string.profile_personal_info_desc), onClick = { showEditProfileDialog = true })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            ProfileOptionItem(Icons.Rounded.Lock, stringResource(R.string.profile_change_password), stringResource(R.string.profile_change_password_desc), onClick = { showChangePasswordDialog = true })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            
                            val isMFAEnabled = (user?.factors?.size ?: 0) > 0
                            ProfileOptionItem(
                                Icons.Rounded.Shield, 
                                stringResource(R.string.profile_mfa), 
                                if (isMFAEnabled) stringResource(R.string.profile_mfa_enabled) else stringResource(R.string.profile_mfa_recommended), 
                                hasToggle = true, 
                                toggleState = isMFAEnabled, 
                                onToggleChange = { if (!isMFAEnabled) showMFASetupDialog = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- PREFERENCES SECTION ---
                    ProfileCategoryHeader(stringResource(R.string.profile_category_preferences))
                    HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            val currentLang = profile?.language ?: "Português"
                            ProfileOptionItem(Icons.Rounded.Language, stringResource(R.string.profile_language), currentLang, onClick = { 
                                val nextLang = if (currentLang == "English") "Português" else "English"
                                scope.launch {
                                    try {
                                        val updated = profile?.copy(language = nextLang) ?: Profile(id = user?.id, language = nextLang)
                                        SupabaseClient.client.postgrest.from("profiles").upsert(updated)
                                        UserLogger.log("Alterou o idioma", "Novo idioma: $nextLang")
                                        profile = updated
                                    } catch (e: Exception) {}
                                }
                            })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            val currentTheme = profile?.theme ?: "Elite / Hyper-Glass"
                            ProfileOptionItem(Icons.Rounded.LightMode, stringResource(R.string.profile_theme), currentTheme, onClick = { 
                                val nextTheme = if (currentTheme.contains("Elite")) "Standard Dark" else "Elite / Hyper-Glass"
                                scope.launch {
                                    try {
                                        val updated = profile?.copy(theme = nextTheme) ?: Profile(id = user?.id, theme = nextTheme)
                                        SupabaseClient.client.postgrest.from("profiles").upsert(updated)
                                        UserLogger.log("Alterou o tema", "Novo tema: $nextTheme")
                                        profile = updated
                                    } catch (e: Exception) {}
                                }
                            })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            ProfileOptionItem(Icons.Rounded.Notifications, stringResource(R.string.profile_notif_title), stringResource(R.string.profile_notif_desc), onClick = { 
                                try {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.login_success), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ADMINISTRATION SECTION ---
                    ProfileCategoryHeader(stringResource(R.string.profile_category_administration))
                    HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            val adminToast = { android.widget.Toast.makeText(context, context.getString(R.string.profile_toast_soon), android.widget.Toast.LENGTH_SHORT).show() }
                            ProfileOptionItem(Icons.Rounded.Groups, stringResource(R.string.profile_team_mgmt), stringResource(R.string.profile_team_mgmt_desc), onClick = adminToast)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            ProfileOptionItem(Icons.Rounded.PersonAdd, stringResource(R.string.profile_roles_perms), stringResource(R.string.profile_roles_perms_desc), onClick = adminToast)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            ProfileOptionItem(Icons.Rounded.Description, stringResource(R.string.profile_audit_log), stringResource(R.string.profile_audit_log_desc), onClick = onNavigateToAuditLog)
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            ProfileOptionItem(Icons.Rounded.AccountTree, stringResource(R.string.profile_integrations), stringResource(R.string.profile_integrations_desc), onClick = adminToast)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SUPPORT SECTION ---
                    ProfileCategoryHeader(stringResource(R.string.profile_category_support))
                    HyperGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            val openUrl = { url: String -> 
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.profile_toast_browser_error), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            ProfileOptionItem(Icons.Rounded.Help, stringResource(R.string.profile_help_center), stringResource(R.string.profile_help_center_desc), isExternal = true, onClick = { openUrl("https://cantanhedecycling.pt/ajuda") })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            ProfileOptionItem(Icons.Rounded.Feedback, stringResource(R.string.profile_send_feedback), stringResource(R.string.profile_send_feedback_desc), isExternal = true, onClick = { openUrl("mailto:support@cantanhedecycling.pt") })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            ProfileOptionItem(Icons.Rounded.Info, stringResource(R.string.profile_app_version), "v2.5.0-PRO")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- LOGOUT BUTTON ---
                    HyperGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                try { 
                                    UserLogger.log("Terminou sessão")
                                    SupabaseClient.client.auth.signOut() 
                                } catch (e: Exception) {}
                                onLogout()
                            }
                        },
                        borderColor = Color(0xFFFF5252),
                        borderAlpha = 0.3f,
                        cornerRadius = 16
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Logout, null, tint = Color(0xFFFF5252))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(R.string.profile_logout), color = Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.profile_logout_desc), color = Color(0xFFFF5252).copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        }

        if (showEditProfileDialog) {
            EditProfileDialog(
                profile = profile,
                onDismiss = { showEditProfileDialog = false },
                onSave = { newName, newPhone ->
                    scope.launch {
                        try {
                            val updated = profile?.copy(full_name = newName, phone = newPhone) ?: Profile(id = user?.id, full_name = newName, phone = newPhone)
                            SupabaseClient.client.postgrest.from("profiles").upsert(updated)
                            UserLogger.log("Atualizou o perfil", "Nome: $newName, Telefone: $newPhone")
                            profile = updated
                            showEditProfileDialog = false
                            android.widget.Toast.makeText(context, context.getString(R.string.profile_avatar_updated), android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Erro ao guardar perfil. Tenta novamente.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                onDismiss = { showChangePasswordDialog = false },
                onSave = { newPassword ->
                    scope.launch {
                        try {
                            SupabaseClient.client.auth.updateUser { password = newPassword }
                            UserLogger.log("Alterou a palavra-passe")
                            showChangePasswordDialog = false
                            android.widget.Toast.makeText(context, context.getString(R.string.profile_avatar_updated), android.widget.Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Erro ao alterar senha. Tenta novamente.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        if (showMFASetupDialog) {
            MFASetupDialog(
                onDismiss = { showMFASetupDialog = false },
                onEnroll = { 
                    scope.launch {
                        try {
                            val factor = SupabaseClient.client.auth.mfa.enroll(FactorType.TOTP)
                            mfaFactorId = factor.id
                            // Correct access for 3.x
                            mfaSecret = factor.data.secret
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Erro ao configurar MFA. Tenta novamente.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                },
                secret = mfaSecret,
                onVerify = { code ->
                    scope.launch {
                        try {
                            if (mfaFactorId != null) {
                                // In Supabase 3.x syntax
                                val challenge = SupabaseClient.client.auth.mfa.createChallenge(mfaFactorId!!)
                                SupabaseClient.client.auth.mfa.verifyChallenge(mfaFactorId!!, challenge.id, code)
                                UserLogger.log("Ativou a autenticação de 2 fatores (MFA)")
                                showMFASetupDialog = false
                                android.widget.Toast.makeText(context, context.getString(R.string.profile_mfa_success), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Código MFA inválido. Tenta novamente.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }
}
