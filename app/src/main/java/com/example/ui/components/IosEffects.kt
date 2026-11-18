package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Premium glossy iOS-style rounded squircle icon tile with multi-stop depth gradient,
 * top specular glass sheen, and soft ambient elevation.
 */
@Composable
fun IosSquircleIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = Color.White,
    containerColor: Color = Color(0xFF0B6B50),
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    val radius = size * 0.28f
    val shape = RoundedCornerShape(radius)
    val lighterTop = Color(
        red = (containerColor.red + 0.18f).coerceAtMost(1f),
        green = (containerColor.green + 0.18f).coerceAtMost(1f),
        blue = (containerColor.blue + 0.18f).coerceAtMost(1f),
        alpha = 1f
    )
    val deeperBottom = Color(
        red = (containerColor.red * 0.80f).coerceAtLeast(0f),
        green = (containerColor.green * 0.80f).coerceAtLeast(0f),
        blue = (containerColor.blue * 0.80f).coerceAtLeast(0f),
        alpha = 1f
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(lighterTop, containerColor, deeperBottom)
                )
            )
            .border(
                width = 0.85.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.58f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle glossy upper glass sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.26f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = this.size.height * 0.52f
                ),
                topLeft = Offset(this.size.width * 0.06f, this.size.height * 0.04f),
                size = Size(this.size.width * 0.88f, this.size.height * 0.46f),
                cornerRadius = CornerRadius(this.size.width * 0.24f, this.size.width * 0.24f)
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Custom high-craft iOS Calculator squircle icon featuring an illuminated dark emerald LCD screen,
 * crisp tactile keypad grid, and vivid amber/emerald operator badge.
 */
@Composable
fun PremiumCalculatorIcon(
    size: Dp = 36.dp,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.28f)
    val outerBrush = if (active) {
        Brush.verticalGradient(
            listOf(Color(0xFFFFB340), Color(0xFFFF8A00), Color(0xFFD96200))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B))
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = if (active) 4.dp else 1.dp, shape = shape, clip = false)
            .clip(shape)
            .background(outerBrush)
            .border(
                width = 0.85.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.14f))
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val pad = w * 0.17f

            // Gloss sheen on top edge
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.44f
                ),
                topLeft = Offset(w * 0.06f, h * 0.04f),
                size = Size(w * 0.88f, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
            )

            // Dark calculator body inset
            val bodyLeft = pad
            val bodyTop = pad
            val bodyW = w - pad * 2f
            val bodyH = h - pad * 2f

            // Top LCD screen pill
            val lcdH = bodyH * 0.26f
            drawRoundRect(
                color = Color(0xFF0F2922),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, lcdH),
                cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
            )
            // Illuminated LCD readout accent dot/bar
            drawRoundRect(
                color = Color(0xFF4ADE80),
                topLeft = Offset(bodyLeft + bodyW * 0.46f, bodyTop + lcdH * 0.34f),
                size = Size(bodyW * 0.42f, lcdH * 0.32f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )

            // 3x3 key matrix below LCD
            val gridTop = bodyTop + lcdH + bodyH * 0.10f
            val gridH = bodyH - (gridTop - bodyTop)
            val cols = 3
            val rows = 3
            val gap = w * 0.055f
            val keyW = (bodyW - gap * (cols - 1)) / cols
            val keyH = (gridH - gap * (rows - 1)) / rows

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val isEqualsKey = (r == 2 && c == 2)
                    val isOpCol = (c == 2 && !isEqualsKey)
                    // Make bottom-right "=" pill stand out
                    val kx = bodyLeft + c * (keyW + gap)
                    val ky = gridTop + r * (keyH + gap)
                    val keyColor = when {
                        isEqualsKey -> Color(0xFF10B981)
                        isOpCol -> Color(0xFFFEF3C7)
                        else -> Color.White.copy(alpha = 0.94f)
                    }
                    drawRoundRect(
                        color = keyColor,
                        topLeft = Offset(kx, ky),
                        size = Size(keyW, keyH),
                        cornerRadius = CornerRadius(keyW * 0.32f, keyW * 0.32f)
                    )
                }
            }
        }
    }
}

/**
 * Renders the user-provided 2nd logo inside an iOS glossy squircle frame.
 */
@Composable
fun IosAppLogoMark(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.25f)
    Surface(
        shape = shape,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f)),
        color = Color.Transparent,
        modifier = modifier.size(size)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_asha_logo),
            contentDescription = "ASHA App Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(shape)
        )
    }
}
