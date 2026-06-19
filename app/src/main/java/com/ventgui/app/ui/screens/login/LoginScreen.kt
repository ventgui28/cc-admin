package com.ventgui.app.ui.screens.login

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R
import com.ventgui.app.data.utils.UserLogger
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.ventgui.app.ui.components.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    val infiniteTransition = rememberInfiniteTransition(label = "bikeGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val bikeScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bikeScale"
    )
    
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val lockoutTimeRemaining by authViewModel.lockoutTimeRemaining.collectAsStateWithLifecycle()
    var secondsRemaining by remember { mutableStateOf(0L) }
    LaunchedEffect(lockoutTimeRemaining) {
        if (lockoutTimeRemaining > 0) {
            while (true) {
                val rem = authViewModel.getSecondsRemaining()
                secondsRemaining = rem
                if (rem <= 0) break
                kotlinx.coroutines.delay(1000)
            }
        } else {
            secondsRemaining = 0
        }
    }
    val isLockedOut = secondsRemaining > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlue)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(500.dp)
                .offset(x = (-150).dp, y = (-150).dp)
                .background(Brush.radialGradient(colors = listOf(CyberCyan.copy(alpha = 0.15f), Color.Transparent)))
        )
        
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 150.dp)
                .background(Brush.radialGradient(colors = listOf(ElectricBlue.copy(alpha = 0.1f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Section
            StaggeredFadeInItem(index = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(32.dp, RoundedCornerShape(32.dp), spotColor = CyberCyan.copy(alpha = glowAlpha))
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, CyberCyan.copy(alpha = glowAlpha), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsBike,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .graphicsLayer {
                                    scaleX = bikeScale
                                    scaleY = bikeScale
                                },
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "Cantanhede Cycling",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "HUB ADMINISTRATIVO",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Premium Login Form
            StaggeredFadeInItem(index = 1) {
                HyperGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = CyberCyan
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        var isSignUp by rememberSaveable { mutableStateOf(false) }

                        Text(
                            text = if (isSignUp) stringResource(R.string.login_create_account) else stringResource(R.string.login_welcome_back),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (isSignUp) stringResource(R.string.login_start_journey) else stringResource(R.string.login_enter_credentials),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Email Field
                        PremiumTextField(
                            value = email,
                            onValueChange = { email = it; errorMsg = null },
                            label = stringResource(R.string.login_email),
                            placeholder = stringResource(R.string.login_email_placeholder),
                            leadingIcon = Icons.Rounded.Email
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Password Field
                        Column {
                            Text(
                                text = stringResource(R.string.login_password).uppercase(),
                                color = CyberCyan.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it; errorMsg = null },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("••••••••", color = Color.White.copy(alpha = 0.2f)) },
                                leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = CyberCyan.copy(alpha = 0.5f)) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.4f)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                                )
                            )
                        }

                        if (errorMsg != null) {
                            Text(
                                text = errorMsg!!,
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Password complexity validation
                        val passwordErrors = if (isSignUp) {
                            buildList {
                                if (password.length < 8) add("Mínimo 8 caracteres")
                                if (!password.any { it.isUpperCase() }) add("1 letra maiúscula")
                                if (!password.any { it.isLowerCase() }) add("1 letra minúscula")
                                if (!password.any { it.isDigit() }) add("1 número")
                            }
                        } else emptyList()
                        // Password strength indicator (signup only)
                        if (isSignUp && password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (passwordErrors.isNotEmpty()) {
                                Text(
                                    text = "Falta: ${passwordErrors.joinToString(", ")}",
                                    color = Color(0xFFFFAB40),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "✓ Senha forte",
                                    color = Color(0xFF69F0AE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
                                )
                            }
                        }
                          val isPasswordValid = !isSignUp || passwordErrors.isEmpty()
                        val isButtonEnabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() && isPasswordValid && !isLockedOut

                        // Submit Button
                        val buttonText = if (isLockedOut) {
                            "Bloqueado (${secondsRemaining}s)"
                        } else if (isSignUp) {
                            stringResource(R.string.login_btn_signup)
                        } else {
                            stringResource(R.string.login_btn_signin)
                        }

                        PremiumButton(
                            text = buttonText,
                            onClick = {
                                isLoading = true
                                errorMsg = null
                                if (isSignUp) {
                                    authViewModel.signUp(email, password) { err ->
                                        isLoading = false
                                        if (err == null) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Conta criada! Verifica o teu e-mail para ativar.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                            onSignUpSuccess()
                                        } else {
                                            errorMsg = err
                                        }
                                    }
                                } else {
                                    authViewModel.login(email, password, rememberMe, context) { err ->
                                        isLoading = false
                                        if (err == null) {
                                            onLoginSuccess()
                                        } else {
                                            errorMsg = err
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = if (isButtonEnabled) CyberCyan else Color.White.copy(alpha = 0.1f),
                            contentColor = if (isButtonEnabled) MidnightBlue else Color.White.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Toggle Button
                        TextButton(onClick = { isSignUp = !isSignUp }) {
                            Text(
                                text = if (isSignUp) stringResource(R.string.login_have_account, stringResource(R.string.login_link_signin)).uppercase() else stringResource(R.string.login_no_account, stringResource(R.string.login_link_signup)).uppercase(),
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
