package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Animated glossy iOS bottom navigation squircle icon with spring scale physics,
 * active neon tint glow, and top specular glass reflection.
 */
@Composable
fun AnimatedIosNavBadge(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    activeColor: Color,
    inactiveBg: Color,
    inactiveTint: Color,
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.09f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "nav_icon_scale"
    )
    val shadowElev by animateDpAsState(
        targetValue = if (selected) 5.dp else 0.dp,
        label = "nav_icon_shadow"
    )
    val topBg by animateColorAsState(
        targetValue = if (selected) {
            Color(
                red = (activeColor.red + 0.16f).coerceAtMost(1f),
                green = (activeColor.green + 0.16f).coerceAtMost(1f),
                blue = (activeColor.blue + 0.16f).coerceAtMost(1f),
                alpha = 1f
            )
        } else {
            inactiveBg.copy(alpha = 0.48f)
        },
        label = "nav_icon_top_bg"
    )
    val bottomBg by animateColorAsState(
        targetValue = if (selected) {
            Color(
                red = (activeColor.red * 0.82f).coerceAtLeast(0f),
                green = (activeColor.green * 0.82f).coerceAtLeast(0f),
                blue = (activeColor.blue * 0.82f).coerceAtLeast(0f),
                alpha = 1f
            )
        } else {
            inactiveBg.copy(alpha = 0.72f)
        },
        label = "nav_icon_bottom_bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else inactiveTint,
        label = "nav_icon_tint"
    )

    val shape = RoundedCornerShape(size * 0.30f)
    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .shadow(elevation = shadowElev, shape = shape, clip = false)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topBg, bottomBg)))
            .border(
                width = if (selected) 0.9.dp else 0.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (selected) 0.65f else 0.25f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        startY = 0f,
                        endY = this.size.height * 0.48f
                    ),
                    topLeft = Offset(this.size.width * 0.06f, this.size.height * 0.04f),
                    size = Size(this.size.width * 0.88f, this.size.height * 0.44f),
                    cornerRadius = CornerRadius(this.size.width * 0.24f, this.size.width * 0.24f)
                )
            }
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Sleek BMW M-inspired 3-color precision telemetry hairline bar (Emerald / Azure Cobalt / Crimson).
 */
@Composable
fun BmwPrecisionStripe(
    modifier: Modifier = Modifier,
    height: Dp = 2.5.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .height(height)
                .background(Color(0xFF10B981))
        )
        Box(
            modifier = Modifier
                .weight(0.9f)
                .height(height)
                .background(Color(0xFF0284C7))
        )
        Box(
            modifier = Modifier
                .weight(0.7f)
                .height(height)
                .background(Color(0xFFDC2626))
        )
    }
}

/**
 * Precision wide-tracked uppercase geometric display badge (BMW cockpit typography aesthetic).
 */
@Composable
fun BmwPrecisionBadge(
    label: String,
    accentColor: Color = Color(0xFF0B6B50),
    textColor: Color = Color.White,
    bgColor: Color = Color.White.copy(alpha = 0.14f),
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.24f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                Box(modifier = Modifier.width(3.dp).height(8.dp).background(Color(0xFF34D399), RoundedCornerShape(1.dp)))
                Box(modifier = Modifier.width(3.dp).height(8.dp).background(Color(0xFF38BDF8), RoundedCornerShape(1.dp)))
                Box(modifier = Modifier.width(3.dp).height(8.dp).background(Color(0xFFF87171), RoundedCornerShape(1.dp)))
            }
            Text(
                text = label.uppercase(),
                fontSize = 9.5.sp,
                letterSpacing = 1.15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                maxLines = 1
            )
        }
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
    val scale by animateFloatAsState(
        targetValue = if (active) 1.07f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "calc_icon_scale"
    )
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
            .scale(scale)
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
 * Renders the user-provided logo inside an iOS glossy squircle frame.
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

/**
 * Glossy iOS WhatsApp Bill icon badge featuring a vivid WhatsApp emerald squircle,
 * crisp chat bubble, and mini receipt/bill lines inside.
 */
@Composable
fun IosWhatsAppBillIcon(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4ADE80),
                        Color(0xFF25D366),
                        Color(0xFF128C7E)
                    )
                )
            )
            .border(
                width = 0.85.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.14f))
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Specular upper sheen
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.48f
                ),
                topLeft = Offset(w * 0.06f, h * 0.04f),
                size = Size(w * 0.88f, h * 0.44f),
                cornerRadius = CornerRadius(w * 0.22f, w * 0.22f)
            )

            // Crisp white chat bubble body with bottom-left tail
            val bubblePath = androidx.compose.ui.graphics.Path().apply {
                val r = w * 0.28f
                val cx = w * 0.50f
                val cy = h * 0.48f
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        cx - r,
                        cy - r,
                        cx + r,
                        cy + r
                    )
                )
                moveTo(cx - r * 0.62f, cy + r * 0.56f)
                lineTo(cx - r * 0.95f, cy + r * 0.98f)
                lineTo(cx - r * 0.18f, cy + r * 0.86f)
                close()
            }
            drawPath(bubblePath, color = Color.White)

            // Mini receipt card inside bubble
            val docW = w * 0.24f
            val docH = h * 0.28f
            val docL = w * 0.50f - docW / 2f
            val docT = h * 0.48f - docH / 2f
            drawRoundRect(
                color = Color(0xFF0B6B50),
                topLeft = Offset(docL, docT),
                size = Size(docW, docH),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
            // White bill lines
            drawLine(
                color = Color.White,
                start = Offset(docL + docW * 0.20f, docT + docH * 0.34f),
                end = Offset(docL + docW * 0.80f, docT + docH * 0.34f),
                strokeWidth = w * 0.045f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFDE047),
                start = Offset(docL + docW * 0.20f, docT + docH * 0.64f),
                end = Offset(docL + docW * 0.68f, docT + docH * 0.64f),
                strokeWidth = w * 0.045f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
