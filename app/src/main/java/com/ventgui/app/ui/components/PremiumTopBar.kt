package com.ventgui.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PremiumTopBar(
    onLeftClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBackArrow: Boolean = false,
    leftIcon: ImageVector? = null,
    onRightClick: (() -> Unit)? = null,
    rightIcon: ImageVector? = null,
    hasRightBadge: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Action Button
        IconButton(
            onClick = onLeftClick,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = leftIcon ?: if (showBackArrow) Icons.Rounded.ChevronLeft else Icons.Rounded.Menu,
                contentDescription = if (showBackArrow) "Voltar" else "Menu",
                tint = Color.White
            )
        }

        // Center Brand Logo & Typography
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = MidnightBlue,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CANTANHEDE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "CYCLING HUB",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Right Action Button (or placeholder to balance the row)
        if (onRightClick != null && rightIcon != null) {
            Box {
                IconButton(
                    onClick = onRightClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = rightIcon,
                        contentDescription = "Ação",
                        tint = Color.White
                    )
                }
                if (hasRightBadge) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(CyberCyan, CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                    )
                }
            }
        } else {
            // Invisible placeholder of the same size to ensure perfect centering of the logo
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}
