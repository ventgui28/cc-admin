package com.ventgui.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.model.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import com.ventgui.app.ui.components.*
import androidx.compose.ui.res.stringResource
import com.ventgui.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sessionStatus by SupabaseClient.client.auth.sessionStatus.collectAsStateWithLifecycle()
    val user = (sessionStatus as? io.github.jan.supabase.auth.status.SessionStatus.Authenticated)?.session?.user
    
    var currentSlide by rememberSaveable { mutableStateOf(0) }
    var fullName by rememberSaveable { mutableStateOf("") }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    if (user == null) {
        Box(modifier = Modifier.fillMaxSize().background(MidnightBlue), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CyberCyan)
        }
        return
    }

    val slides = listOf(
        OnboardingSlide(
            title = stringResource(R.string.onboarding_slide1_title),
            description = stringResource(R.string.onboarding_slide1_desc),
            icon = Icons.Rounded.RocketLaunch,
            color = CyberCyan
        ),
        OnboardingSlide(
            title = stringResource(R.string.onboarding_slide2_title),
            description = stringResource(R.string.onboarding_slide2_desc),
            icon = Icons.Rounded.Dashboard,
            color = NeonEmerald
        ),
        OnboardingSlide(
            title = stringResource(R.string.onboarding_slide3_title),
            description = stringResource(R.string.onboarding_slide3_desc),
            icon = Icons.Rounded.AutoAwesome,
            color = VividAmber
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlue)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(Brush.radialGradient(colors = listOf(CyberCyan.copy(alpha = 0.1f), Color.Transparent)))
        )

        // Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalSteps = slides.size + 1
            repeat(totalSteps) { step ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (step <= currentSlide) CyberCyan
                            else Color.White.copy(alpha = 0.1f)
                        )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { if (targetState > initialState) it else -it })
                        .togetherWith(fadeOut() + slideOutHorizontally { if (targetState > initialState) -it else it })
                },
                label = "slide_anim"
            ) { slideIdx ->
                if (slideIdx < slides.size) {
                    val slide = slides[slideIdx]
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Brush.radialGradient(colors = listOf(slide.color.copy(alpha = 0.15f), Color.Transparent)))
                            )
                            Icon(
                                slide.icon,
                                contentDescription = null,
                                tint = slide.color,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Text(
                            text = slide.title,
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-1.5).sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = slide.description,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.onboarding_welcome_back),
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-1.5).sp
                        )
                        Text(
                            text = stringResource(R.string.onboarding_how_to_address),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        PremiumTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = stringResource(R.string.profile_dialog_edit_name),
                            placeholder = stringResource(R.string.onboarding_name_placeholder),
                            leadingIcon = Icons.Rounded.Person
                        )
                    }
                }
            }
        }

        // Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp, start = 32.dp, end = 32.dp)
        ) {
            PremiumButton(
                text = if (currentSlide < slides.size) stringResource(R.string.onboarding_next) else stringResource(R.string.onboarding_start_now),
                onClick = {
                    if (currentSlide < slides.size) {
                        currentSlide++
                    } else if (fullName.isNotBlank()) {
                        scope.launch {
                            isSaving = true
                            try {
                                val profile = Profile(id = user.id, full_name = fullName.trim(), role = "editor")
                                SupabaseClient.client.postgrest.from("profiles").upsert(profile)
                                onFinish()
                            } catch (e: Exception) {
                                onFinish()
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                containerColor = if (currentSlide < slides.size || fullName.isNotBlank()) CyberCyan else Color.White.copy(alpha = 0.05f),
                contentColor = if (currentSlide < slides.size || fullName.isNotBlank()) MidnightBlue else Color.White.copy(alpha = 0.2f)
            )
        }
    }
}

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)
