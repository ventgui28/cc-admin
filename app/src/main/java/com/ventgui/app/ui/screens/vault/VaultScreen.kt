package com.ventgui.app.ui.screens.vault

import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.utils.UserLogger
import com.ventgui.app.data.model.VaultText
import com.ventgui.app.data.utils.BiometricHelper
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ventgui.app.ui.components.*
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R

// Walks up the ContextWrapper chain to find the underlying FragmentActivity.
// Necessary on OPPO ColorOS and other OEM ROMs where LocalContext.current
// returns a wrapped context rather than the Activity directly.
private fun android.content.Context.findActivity(): FragmentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(innerPadding: PaddingValues, onOpenDrawer: () -> Unit) {

    // ── Auth state ────────────────────────────────────────────────────────
    // remember (not rememberSaveable) → resets to false on every navigation
    var isUnlocked by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context.findActivity()   // robust: walks ContextWrapper chain
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Central auth trigger ──────────────────────────────────────────────
    // Always try the biometric prompt.
    val doAuth: () -> Unit = {
        biometricError = ""
        if (activity != null) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Autenticação Necessária",
                subtitle = "Usa a tua impressão digital para aceder ao Cofre",
                onSuccess = { isUnlocked = true },
                onNoBiometrics = { biometricError = "Impressão digital não disponível neste dispositivo" },
                onError = { msg -> if (msg.isNotEmpty()) biometricError = msg }
            )
        } else {
            biometricError = "Não foi possível iniciar a autenticação (actividade indisponível)"
        }
    }

    // Initial auth when the composable first enters composition (every navigation).
    // The 400ms delay is required for OPPO ColorOS (and other OEM ROMs): calling
    // BiometricPrompt before the Activity window is fully RESUMED causes an immediate
    // ERROR_HW_NOT_PRESENT / ERROR_NO_BIOMETRICS on those devices.
    LaunchedEffect(Unit) {
        delay(400)
        doAuth()
    }

    // ── Background lock ───────────────────────────────────────────────────
    // ON_STOP fires when the app is sent to background (home button, task switcher, etc.)
    // ON_RESUME fires when the user returns — wasInBackground guard prevents false-positives
    // from the initial catch-up events the observer receives when first registered.
    DisposableEffect(lifecycleOwner) {
        var wasInBackground = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    isUnlocked = false
                    wasInBackground = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (wasInBackground && !isUnlocked) {
                        wasInBackground = false
                        // Same delay as initial auth: let the window fully resume first
                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            delay(400)
                            doAuth()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Lock screen ───────────────────────────────────────────────────────
    if (!isUnlocked) {
        VaultLockScreen(
            innerPadding = innerPadding,
            onOpenDrawer = onOpenDrawer,
            errorMessage = biometricError,
            onUnlockClick = doAuth
        )
        return
    }

    // ── Vault content (only visible after auth) ───────────────────────────
    var vaultTexts by remember { mutableStateOf<List<VaultText>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val fetchVault = {
        scope.launch {
            try {
                if (!isRefreshing) isLoading = true
                vaultTexts = SupabaseClient.client.from("vault_texts").select().decodeList<VaultText>()
                    .sortedByDescending { it.created_at }
            } catch (e: Exception) {
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchVault() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        PremiumMeshBackground()
        Column(modifier = Modifier.fillMaxSize()) {

            // ── HEADER ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) { Icon(Icons.Rounded.Menu, null, tint = Color.White) }

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

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(CyberCyan.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, CyberCyan.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = stringResource(R.string.nav_vault),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = stringResource(R.string.vault_subtitle),
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    fetchVault()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF80D8FF))
                    }
                } else if (vaultTexts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.Inventory2,
                                null,
                                tint = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.vault_no_items),
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp)
                    ) {
                        itemsIndexed(vaultTexts) { index, text ->
                            StaggeredFadeInItem(index = index) {
                                VaultTextCard(
                                    vaultText = text,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(text.content))
                                    },
                                    onShare = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, text.content)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                    },
                                    onDelete = {
                                        scope.launch {
                                            try {
                                                SupabaseClient.client.from("vault_texts").delete {
                                                    filter { eq("id", text.id!!) }
                                                }
                                                UserLogger.log("Eliminou a nota segura ${text.title}")
                                                vaultTexts = vaultTexts.filter { it.id != text.id }
                                            } catch (e: Exception) {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddVaultTextDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, content ->
                    scope.launch {
                        try {
                            val newText = VaultText(title = title, content = content)
                            val inserted = SupabaseClient.client.from("vault_texts").insert(newText) {
                                select()
                            }.decodeSingle<VaultText>()
                            UserLogger.log("Criou a nota segura $title")
                            vaultTexts = listOf(inserted) + vaultTexts
                            showAddDialog = false
                        } catch (e: Exception) {}
                    }
                }
            )
        }
    }
}

// ── Lock Screen ───────────────────────────────────────────────────────────────
@Composable
private fun VaultLockScreen(
    innerPadding: PaddingValues,
    onOpenDrawer: () -> Unit,
    errorMessage: String,
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        PremiumMeshBackground()

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) { Icon(Icons.Rounded.Menu, null, tint = Color.White) }

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

            Spacer(modifier = Modifier.size(44.dp))
        }

        // Centred lock card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyberCyan.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        CircleShape
                    )
                    .border(1.5.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null, tint = CyberCyan, modifier = Modifier.size(46.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text("Cofre Bloqueado", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Usa a tua impressão digital para aceder às notas seguras.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(36.dp))

            PremiumButton(
                text = "Desbloquear Cofre",
                onClick = onUnlockClick,
                icon = Icons.Rounded.Fingerprint,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
