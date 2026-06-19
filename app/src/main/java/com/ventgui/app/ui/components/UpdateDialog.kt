package com.ventgui.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ventgui.app.R
import com.ventgui.app.data.utils.AppUpdateInfo
import com.ventgui.app.data.utils.UpdateManager
import com.ventgui.app.data.utils.UpdateState
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FormattedReleaseNotes(notes: String) {
    val lines = notes.split(Regex("[\n|]")).map { it.trim() }.filter { it.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        lines.forEach { line ->
            val cleanLine = line.trim().removePrefix("-").trim().removePrefix("*").trim()
            val emoji = when {
                cleanLine.contains("fix", ignoreCase = true) || cleanLine.contains("corri", ignoreCase = true) || cleanLine.contains("resolv", ignoreCase = true) -> "🛠️"
                cleanLine.contains("adiciona", ignoreCase = true) || cleanLine.contains("novo", ignoreCase = true) || cleanLine.contains("new", ignoreCase = true) || cleanLine.contains("cria", ignoreCase = true) || cleanLine.contains("painel", ignoreCase = true) || cleanLine.contains("ecrã", ignoreCase = true) -> "✨"
                cleanLine.contains("perf", ignoreCase = true) || cleanLine.contains("melho", ignoreCase = true) || cleanLine.contains("rapid", ignoreCase = true) || cleanLine.contains("otim", ignoreCase = true) -> "🚀"
                cleanLine.contains("segura", ignoreCase = true) || cleanLine.contains("vault", ignoreCase = true) || cleanLine.contains("cofre", ignoreCase = true) || cleanLine.contains("mfa", ignoreCase = true) || cleanLine.contains("senha", ignoreCase = true) -> "🔒"
                cleanLine.contains("idioma", ignoreCase = true) || cleanLine.contains("lingua", ignoreCase = true) || cleanLine.contains("tradu", ignoreCase = true) || cleanLine.contains("localiz", ignoreCase = true) -> "🌍"
                else -> "⚡"
            }
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = emoji,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 10.dp, top = 1.dp)
                )
                Text(
                    text = cleanLine,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = {
            if (state is UpdateState.UpdateAvailable && !state.info.is_mandatory) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = state !is UpdateState.Downloading,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            HyperGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = when (state) {
                    is UpdateState.Error -> Color(0xFFFF5252)
                    is UpdateState.ReadyToInstall -> NeonEmerald
                    else -> CyberCyan
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing Icon Header
                    val headerIcon = when (state) {
                        is UpdateState.Error -> Icons.Rounded.Error
                        is UpdateState.ReadyToInstall -> Icons.Rounded.CheckCircle
                        is UpdateState.Downloading -> Icons.Rounded.Downloading
                        else -> Icons.Rounded.RocketLaunch
                    }
                    val headerColor = when (state) {
                        is UpdateState.Error -> Color(0xFFFF5252)
                        is UpdateState.ReadyToInstall -> NeonEmerald
                        else -> CyberCyan
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(headerColor.copy(alpha = 0.1f), CircleShape)
                            .border(1.dp, headerColor.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Brush.radialGradient(colors = listOf(headerColor.copy(alpha = 0.2f), Color.Transparent)))
                        )
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = headerColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = when (state) {
                            is UpdateState.Error -> stringResource(R.string.update_dialog_error_title)
                            is UpdateState.ReadyToInstall -> stringResource(R.string.update_dialog_ready_title)
                            is UpdateState.Downloading -> stringResource(R.string.update_dialog_downloading)
                            else -> stringResource(R.string.update_dialog_title)
                        },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (state) {
                        is UpdateState.UpdateAvailable -> {
                            val info = state.info
                            Text(
                                text = stringResource(R.string.update_dialog_desc, info.version_name),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            if (!info.release_notes.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .heightIn(max = 180.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = stringResource(R.string.update_dialog_news),
                                            color = CyberCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        FormattedReleaseNotes(info.release_notes)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!info.is_mandatory) {
                                    PremiumButton(
                                        text = stringResource(R.string.update_dialog_later),
                                        onClick = onDismiss,
                                        modifier = Modifier.weight(1f),
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        contentColor = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                                PremiumButton(
                                    text = stringResource(R.string.update_dialog_now),
                                    onClick = {
                                        scope.launch {
                                            UpdateManager.downloadAndInstallUpdate(context, info)
                                        }
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    containerColor = CyberCyan,
                                    contentColor = MidnightBlue
                                )
                            }
                        }

                        is UpdateState.Downloading -> {
                            val progress = state.progress
                            val percentage = (progress * 100).toInt()

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = CyberCyan,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "$percentage%",
                                color = CyberCyan,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                        is UpdateState.ReadyToInstall -> {
                            Text(
                                text = stringResource(R.string.update_dialog_ready_desc),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            PremiumButton(
                                text = stringResource(R.string.update_dialog_install_now),
                                onClick = {
                                    UpdateManager.installApk(context, state.apkFile)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = NeonEmerald,
                                contentColor = MidnightBlue
                            )
                        }

                        is UpdateState.Error -> {
                            Text(
                                text = state.message,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            PremiumButton(
                                text = stringResource(R.string.common_close),
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = Color.White.copy(alpha = 0.05f),
                                contentColor = Color.White
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
