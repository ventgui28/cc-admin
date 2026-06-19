package com.ventgui.app.ui.screens.profile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ventgui.app.R
import com.ventgui.app.data.model.Profile
import com.ventgui.app.ui.components.*

@Composable
fun ProfileStatItem(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CyberCyan, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ProfileCategoryHeader(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ProfileOptionItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String, 
    hasToggle: Boolean = false, 
    toggleState: Boolean = false,
    onToggleChange: (Boolean) -> Unit = {},
    isExternal: Boolean = false, 
    onClick: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        
        if (hasToggle) {
            Switch(
                checked = toggleState,
                onCheckedChange = onToggleChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyberCyan)
            )
        } else if (isExternal) {
            Icon(Icons.Rounded.OpenInNew, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
        } else {
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun MFASetupDialog(onDismiss: () -> Unit, onEnroll: () -> Unit, secret: String?, onVerify: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    
    LaunchedEffect(Unit) { onEnroll() }

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(modifier = Modifier.fillMaxWidth(), color = CyberCyan, variant = "solid") {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.profile_dialog_mfa_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (secret == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    Text(stringResource(R.string.profile_dialog_mfa_step1), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(secret))
                        },
                        color = Color.Black.copy(alpha = 0.3f), 
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(secret, color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ContentCopy, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(R.string.profile_dialog_mfa_step2), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumTextField(value = code, onValueChange = { if (it.length <= 6) code = it }, label = stringResource(R.string.profile_dialog_mfa_code_label), placeholder = "000000")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = onDismiss,
                        variant = "outline",
                        modifier = Modifier.weight(1f)
                    )
                    PremiumButton(
                        text = stringResource(R.string.profile_dialog_mfa_verify), 
                        onClick = { onVerify(code) }, 
                        modifier = Modifier.weight(1.5f),
                        containerColor = if (code.length == 6) CyberCyan else Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(profile: Profile?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(profile?.full_name ?: "") }
    var phone by rememberSaveable { mutableStateOf(profile?.phone ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(modifier = Modifier.fillMaxWidth(), color = CyberCyan, variant = "solid") {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.profile_dialog_edit_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(24.dp))
                PremiumTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.profile_dialog_edit_name), placeholder = stringResource(R.string.profile_dialog_edit_name_placeholder))
                Spacer(modifier = Modifier.height(16.dp))
                PremiumTextField(value = phone, onValueChange = { phone = it }, label = stringResource(R.string.profile_dialog_edit_phone), placeholder = stringResource(R.string.profile_dialog_edit_phone_placeholder))
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = onDismiss,
                        variant = "outline",
                        modifier = Modifier.weight(1f)
                    )
                    PremiumButton(text = stringResource(R.string.common_save), onClick = { onSave(name, phone) }, modifier = Modifier.weight(1.5f))
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    
    val passwordErrors = buildList {
        if (password.length < 8) add("Mínimo 8 caracteres")
        if (!password.any { it.isUpperCase() }) add("1 letra maiúscula")
        if (!password.any { it.isLowerCase() }) add("1 letra minúscula")
        if (!password.any { it.isDigit() }) add("1 número")
    }
    val isPasswordValid = passwordErrors.isEmpty()
    val passwordsMatch = password == confirmPassword && password.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        HyperGlassCard(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFF5252), variant = "solid") {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(stringResource(R.string.profile_dialog_pwd_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(24.dp))
                PremiumTextField(value = password, onValueChange = { password = it }, label = stringResource(R.string.profile_dialog_pwd_new), placeholder = "••••••••", isPassword = true)
                
                // Password strength indicator
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    if (passwordErrors.isNotEmpty()) {
                        Text(
                            text = "Falta: ${passwordErrors.joinToString(", ")}",
                            color = Color(0xFFFFAB40),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else {
                        Text(
                            text = "✓ Senha forte",
                            color = Color(0xFF69F0AE),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                PremiumTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = stringResource(R.string.profile_dialog_pwd_confirm), placeholder = "••••••••", isPassword = true)
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = onDismiss,
                        variant = "outline",
                        modifier = Modifier.weight(1f)
                    )
                    PremiumButton(
                        text = stringResource(R.string.profile_dialog_pwd_update), 
                        onClick = { if (passwordsMatch && isPasswordValid) onSave(password) }, 
                        modifier = Modifier.weight(1.5f),
                        containerColor = if (passwordsMatch && isPasswordValid) Color(0xFFFF5252) else Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}



