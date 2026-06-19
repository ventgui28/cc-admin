package com.ventgui.app.ui.screens.vault

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ventgui.app.R
import com.ventgui.app.data.model.VaultText
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.ui.components.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch



@Composable
fun VaultTextCard(
    vaultText: VaultText,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    HyperGlassCard(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        color = CyberCyan
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vaultText.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = vaultText.content,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumButton(
                    text = stringResource(R.string.common_copy),
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.ContentCopy
                )
                
                PremiumButton(
                    text = stringResource(R.string.common_share),
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Share,
                    variant = "outline"
                )
            }
        }
    }
}

@Composable
fun AddVaultTextDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    val canSave = title.isNotBlank() && content.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0E1A2B),          // solid dark navy — no transparency issues
            tonalElevation = 0.dp,
            shadowElevation = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // ── Coloured top bar ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(CyberCyan.copy(alpha = 0.15f), Color(0xFF0E3354))
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CyberCyan.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, CyberCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Lock, null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                stringResource(R.string.vault_add_item),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Nota segura · Cofre",
                                color = CyberCyan.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Thin accent line
                HorizontalDivider(
                    thickness = 1.dp,
                    color = CyberCyan.copy(alpha = 0.2f)
                )

                // ── Form fields ───────────────────────────────────────────
                Column(modifier = Modifier.padding(24.dp)) {

                    // Title field
                    Text(
                        text = "TÍTULO",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Ex: Legenda Instagram",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.DriveFileRenameOutline,
                                null,
                                tint = CyberCyan.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = if (title.isNotEmpty()) {
                            {
                                IconButton(onClick = { title = "" }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            cursorColor = CyberCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Content field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "CONTEÚDO",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                        )
                        Text(
                            text = "${content.length} caracteres",
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp, end = 2.dp)
                        )
                    }
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = {
                            Text(
                                stringResource(R.string.vault_item_content_placeholder),
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 14.sp
                            )
                        },
                        trailingIcon = if (content.isNotEmpty()) {
                            {
                                IconButton(onClick = { content = "" }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            cursorColor = CyberCyan
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── Action buttons ────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                stringResource(R.string.common_cancel).uppercase(),
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = { if (canSave) onSave(title, content) },
                            enabled = canSave,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                disabledContainerColor = CyberCyan.copy(alpha = 0.25f)
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Save,
                                null,
                                tint = if (canSave) MidnightBlue else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.common_save).uppercase(),
                                color = if (canSave) MidnightBlue else Color.White.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}


// ── Vault Password Dialog (fallback when no biometrics) ───────────────────────
@Composable
fun VaultPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0E1A2B),
            tonalElevation = 0.dp,
            shadowElevation = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // ── Header ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFFF6B35).copy(alpha = 0.15f), Color(0xFF0E1A2B))
                            ),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFF6B35).copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color(0xFFFF6B35).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Key, null, tint = Color(0xFFFF9A6C), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Verificação de Identidade",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Biometria não disponível · Usa a password",
                                color = Color(0xFFFF9A6C).copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = Color(0xFFFF6B35).copy(alpha = 0.2f))

                // ── Form ──────────────────────────────────────────────────
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "PASSWORD DA CONTA",
                        color = Color(0xFFFF9A6C),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMsg = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        placeholder = {
                            Text(
                                "A tua password de acesso",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Lock, null,
                                tint = Color(0xFFFF9A6C).copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9A6C),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.06f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            cursorColor = Color(0xFFFF9A6C)
                        )
                    )

                    if (errorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline, null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                errorMsg,
                                color = Color(0xFFFF6B6B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── Buttons ───────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (!isLoading) onDismiss() },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color.White.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                "CANCELAR",
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (password.isBlank() || isLoading) return@Button
                                scope.launch {
                                    isLoading = true
                                    errorMsg = ""
                                    try {
                                        val email = SupabaseClient.client.auth
                                            .currentUserOrNull()?.email ?: ""
                                        SupabaseClient.client.auth.signInWith(Email) {
                                            this.email = email
                                            this.password = password
                                        }
                                        onSuccess()
                                    } catch (e: Exception) {
                                        errorMsg = "Password incorreta. Tenta novamente."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = password.isNotBlank() && !isLoading,
                            modifier = Modifier.weight(1.5f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9A6C),
                                disabledContainerColor = Color(0xFFFF9A6C).copy(alpha = 0.25f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF0E1A2B)
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Verified, null,
                                    tint = if (password.isNotBlank()) Color(0xFF0E1A2B)
                                           else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "ENTRAR",
                                    color = if (password.isNotBlank()) Color(0xFF0E1A2B)
                                            else Color.White.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
