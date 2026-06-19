package com.ventgui.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.ventgui.app.R

// --- PREMIUM DESIGN TOKENS ---
val MidnightBlue = Color(0xFF001220)
val DeepNavy = Color(0xFF000B14)
val CyberCyan = Color(0xFF80D8FF)
val ElectricBlue = Color(0xFF007BFF)
val NeonEmerald = Color(0xFF00E676)
val VividAmber = Color(0xFFFFD600)
val SoftGlass = Color(0xFFFFFFFF).copy(alpha = 0.03f)
val BorderGlass = Color(0xFFFFFFFF).copy(alpha = 0.08f)

@Composable
fun PremiumMeshBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // App background image
        Image(
            painter = painterResource(id = R.drawable.app_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark gradient overlay for UI readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            MidnightBlue.copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}

@Composable
fun HyperGlassCard(
    modifier: Modifier = Modifier,
    color: Color = CyberCyan,
    variant: String = "glass", // "glass" or "solid"
    cornerRadius: Int = 24,
    borderColor: Color? = null,
    borderAlpha: Float = 0.15f,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundBrush = if (variant == "glass") {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF001E35),
                MidnightBlue
            )
        )
    }

    val finalBorderColor = borderColor ?: color
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.2f),
            finalBorderColor.copy(alpha = borderAlpha),
            Color.White.copy(alpha = 0.05f)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (variant == "solid") 20.dp else 0.dp,
                shape = RoundedCornerShape(cornerRadius.dp),
                ambientColor = Color.Transparent,
                spotColor = Color.Transparent
            )
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(cornerRadius.dp)
            )
    ) {
        content()
    }
}

@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    variant: String = "primary", // primary, secondary, outline
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val finalContainerColor = containerColor ?: when(variant) {
        "primary" -> CyberCyan
        "secondary" -> Color.White.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    
    val finalContentColor = contentColor ?: when(variant) {
        "primary" -> MidnightBlue
        else -> Color.White
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "buttonScale")

    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(20.dp),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = finalContainerColor,
            contentColor = finalContentColor,
            disabledContainerColor = finalContainerColor.copy(alpha = 0.3f),
            disabledContentColor = finalContentColor.copy(alpha = 0.5f)
        ),
        border = if (variant == "outline") BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else null,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = finalContentColor, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = text.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PremiumBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    readOnly: Boolean = false,
    isPassword: Boolean = false
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                color = CyberCyan.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp) },
            leadingIcon = leadingIcon?.let { { Icon(it, null, tint = CyberCyan.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) } },
            trailingIcon = trailingIcon?.let { 
                { 
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(it, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                    }
                } 
            },
            shape = RoundedCornerShape(20.dp),
            isError = isError,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                errorBorderColor = Color.Red.copy(alpha = 0.5f)
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun StaggeredFadeInItem(
    index: Int,
    content: @Composable () -> Unit
) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(animationSpec = tween(600, delayMillis = index * 80)) +
                slideInVertically(animationSpec = tween(600, delayMillis = index * 80)) { it / 3 }
    ) {
        content()
    }
}

@Composable
fun GlassSectionTitle(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.5).sp,
            lineHeight = 40.sp
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = CyberCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
