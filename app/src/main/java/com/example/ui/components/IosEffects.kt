package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.formatInr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

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
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Shared animated glossy iOS squircle container used by custom-crafted navigation & dashboard icons.
 */
@Composable
private fun PremiumSquircleShell(
    size: Dp,
    active: Boolean,
    activeColors: List<Color>,
    inactiveColors: List<Color> = listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B)),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (active) 1.09f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "premium_squircle_scale"
    )
    val shadowElev by animateDpAsState(
        targetValue = if (active) 6.dp else 1.5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "premium_squircle_shadow"
    )
    val topColor by animateColorAsState(
        targetValue = if (active) activeColors[0] else inactiveColors[0],
        label = "premium_squircle_top"
    )
    val midColor by animateColorAsState(
        targetValue = if (active) activeColors[1] else inactiveColors[1],
        label = "premium_squircle_mid"
    )
    val bottomColor by animateColorAsState(
        targetValue = if (active) activeColors[2] else inactiveColors[2],
        label = "premium_squircle_bot"
    )
    val shape = RoundedCornerShape(size * 0.30f)

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .shadow(elevation = shadowElev, shape = shape, clip = false)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topColor, midColor, bottomColor)))
            .border(
                width = if (active) 0.95.dp else 0.55.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (active) 0.70f else 0.28f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Upper liquid-glass specular sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (active) 0.32f else 0.14f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = this.size.height * 0.48f
                ),
                topLeft = Offset(this.size.width * 0.06f, this.size.height * 0.04f),
                size = Size(this.size.width * 0.88f, this.size.height * 0.44f),
                cornerRadius = CornerRadius(this.size.width * 0.24f, this.size.width * 0.24f)
            )
        }
        content()
    }
}

/**
 * Custom luxury iOS Home icon: architectural pitched villa roof, soft pearl facade,
 * glowing golden doorway arch, and emerald glass sheen.
 */
@Composable
fun PremiumHomeIcon(
    size: Dp = 36.dp,
    active: Boolean = true,
    primaryColor: Color = Color(0xFF0B6B50),
    modifier: Modifier = Modifier
) {
    val topTone = Color(
        red = (primaryColor.red + 0.18f).coerceAtMost(1f),
        green = (primaryColor.green + 0.24f).coerceAtMost(1f),
        blue = (primaryColor.blue + 0.16f).coerceAtMost(1f),
        alpha = 1f
    )
    val deepTone = Color(
        red = (primaryColor.red * 0.68f).coerceAtLeast(0f),
        green = (primaryColor.green * 0.74f).coerceAtLeast(0f),
        blue = (primaryColor.blue * 0.76f).coerceAtLeast(0f),
        alpha = 1f
    )
    PremiumSquircleShell(
        size = size,
        active = active,
        activeColors = listOf(topTone, primaryColor, deepTone),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val houseColor = if (active) Color.White else Color.White.copy(alpha = 0.86f)

            // Chimney accent
            drawRoundRect(
                color = houseColor.copy(alpha = 0.82f),
                topLeft = Offset(w * 0.64f, h * 0.24f),
                size = Size(w * 0.10f, h * 0.18f),
                cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
            )

            // Main house facade body
            val bodyPath = Path().apply {
                moveTo(w * 0.26f, h * 0.47f)
                lineTo(w * 0.50f, h * 0.27f)
                lineTo(w * 0.74f, h * 0.47f)
                lineTo(w * 0.74f, h * 0.74f)
                quadraticTo(w * 0.74f, h * 0.79f, w * 0.69f, h * 0.79f)
                lineTo(w * 0.31f, h * 0.79f)
                quadraticTo(w * 0.26f, h * 0.79f, w * 0.26f, h * 0.74f)
                close()
            }
            drawPath(bodyPath, color = houseColor)

            // Crisp overhanging architectural roof chevron
            val roofPath = Path().apply {
                moveTo(w * 0.18f, h * 0.49f)
                lineTo(w * 0.50f, h * 0.22f)
                lineTo(w * 0.82f, h * 0.49f)
            }
            drawPath(
                path = roofPath,
                color = if (active) Color(0xFFFDE68A) else Color.White,
                style = Stroke(
                    width = w * 0.085f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Illuminated golden arched doorway
            val doorW = w * 0.17f
            val doorH = h * 0.24f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (active) {
                        listOf(Color(0xFFF59E0B), Color(0xFF065F46))
                    } else {
                        listOf(Color(0xFF475569), Color(0xFF1E293B))
                    }
                ),
                topLeft = Offset(w * 0.50f - doorW / 2f, h * 0.55f),
                size = Size(doorW, doorH),
                cornerRadius = CornerRadius(doorW * 0.48f, doorW * 0.48f)
            )

            // Tiny warm window dot
            drawCircle(
                color = if (active) Color(0xFF34D399) else Color(0xFF94A3B8),
                radius = w * 0.042f,
                center = Offset(w * 0.50f, h * 0.42f)
            )
        }
    }
}

/**
 * Custom luxury iOS User / Customers icon: sapphire-azure glass squircle with dual sculpted
 * user portraits and luminous emerald online status gem.
 */
@Composable
fun PremiumUserIcon(
    size: Dp = 36.dp,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    PremiumSquircleShell(
        size = size,
        active = active,
        activeColors = listOf(Color(0xFF38BDF8), Color(0xFF007AFF), Color(0xFF1D4ED8)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Subtle secondary background user silhouette (right)
            drawCircle(
                color = Color.White.copy(alpha = if (active) 0.55f else 0.36f),
                radius = w * 0.095f,
                center = Offset(w * 0.67f, h * 0.38f)
            )
            val rightBody = Path().apply {
                moveTo(w * 0.56f, h * 0.72f)
                quadraticTo(w * 0.67f, h * 0.50f, w * 0.81f, h * 0.72f)
                quadraticTo(w * 0.76f, h * 0.77f, w * 0.60f, h * 0.77f)
                close()
            }
            drawPath(rightBody, color = Color.White.copy(alpha = if (active) 0.50f else 0.32f))

            // Primary foreground user head & sculpted shoulders
            val fgColor = if (active) Color.White else Color.White.copy(alpha = 0.86f)
            drawCircle(
                color = fgColor,
                radius = w * 0.145f,
                center = Offset(w * 0.44f, h * 0.35f)
            )
            val mainBody = Path().apply {
                moveTo(w * 0.21f, h * 0.76f)
                quadraticTo(w * 0.22f, h * 0.53f, w * 0.44f, h * 0.53f)
                quadraticTo(w * 0.66f, h * 0.53f, w * 0.67f, h * 0.76f)
                quadraticTo(w * 0.67f, h * 0.80f, w * 0.62f, h * 0.80f)
                lineTo(w * 0.26f, h * 0.80f)
                quadraticTo(w * 0.21f, h * 0.80f, w * 0.21f, h * 0.76f)
                close()
            }
            drawPath(mainBody, color = fgColor)

            // Verified emerald glowing status gem on bottom-right
            if (active) {
                drawCircle(
                    color = Color(0xFF1D4ED8),
                    radius = w * 0.105f,
                    center = Offset(w * 0.74f, h * 0.73f)
                )
                drawCircle(
                    color = Color(0xFF4ADE80),
                    radius = w * 0.072f,
                    center = Offset(w * 0.74f, h * 0.73f)
                )
            }
        }
    }
}

/**
 * Custom luxury iOS Bill / Invoice icon: royal-indigo glass squircle framing a crisp white
 * invoice sheet with emerald header stripe, golden total bar, and micro 1D barcode footer.
 */
@Composable
fun PremiumBillIcon(
    size: Dp = 36.dp,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    PremiumSquircleShell(
        size = size,
        active = active,
        activeColors = listOf(Color(0xFF818CF8), Color(0xFF5856D6), Color(0xFF3730A3)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            val docL = w * 0.24f
            val docT = h * 0.17f
            val docW = w * 0.52f
            val docH = h * 0.66f

            // Crisp white invoice card
            drawRoundRect(
                color = if (active) Color.White else Color.White.copy(alpha = 0.88f),
                topLeft = Offset(docL, docT),
                size = Size(docW, docH),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )

            // Emerald header strip on receipt
            drawRoundRect(
                color = if (active) Color(0xFF10B981) else Color(0xFF64748B),
                topLeft = Offset(docL + docW * 0.12f, docT + docH * 0.11f),
                size = Size(docW * 0.76f, docH * 0.14f),
                cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
            )

            // Invoice line items
            val line1Y = docT + docH * 0.39f
            val line2Y = docT + docH * 0.54f
            drawLine(
                color = Color(0xFF4F46E5).copy(alpha = 0.65f),
                start = Offset(docL + docW * 0.14f, line1Y),
                end = Offset(docL + docW * 0.84f, line1Y),
                strokeWidth = w * 0.048f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFF59E0B),
                start = Offset(docL + docW * 0.14f, line2Y),
                end = Offset(docL + docW * 0.64f, line2Y),
                strokeWidth = w * 0.052f,
                cap = StrokeCap.Round
            )

            // Micro 1D barcode footer bars
            val barTop = docT + docH * 0.70f
            val barBot = docT + docH * 0.86f
            val barXs = listOf(0.16f, 0.26f, 0.34f, 0.45f, 0.56f, 0.64f, 0.75f, 0.84f)
            barXs.forEachIndexed { i, frac ->
                val x = docL + docW * frac
                drawLine(
                    color = Color(0xFF1E293B).copy(alpha = 0.82f),
                    start = Offset(x, barTop),
                    end = Offset(x, barBot),
                    strokeWidth = if (i % 3 == 0) w * 0.034f else w * 0.020f,
                    cap = StrokeCap.Butt
                )
            }
        }
    }
}

/**
 * Custom luxury iOS Pay / Settlement icon: vibrant emerald squircle with a gleaming golden-rimmed
 * medallion coin bearing a precision-drawn ₹ Rupee monogram.
 */
@Composable
fun PremiumPayIcon(
    size: Dp = 36.dp,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    PremiumSquircleShell(
        size = size,
        active = active,
        activeColors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A), Color(0xFF047857)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.50f
            val cy = h * 0.50f

            // Outer golden coin rim
            drawCircle(
                brush = Brush.linearGradient(
                    colors = if (active) {
                        listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
                    } else {
                        listOf(Color.White.copy(alpha = 0.85f), Color(0xFFCBD5E1))
                    }
                ),
                radius = w * 0.32f,
                center = Offset(cx, cy)
            )
            // Inner crisp white medallion face
            drawCircle(
                color = if (active) Color(0xFFFFFFFF) else Color.White.copy(alpha = 0.88f),
                radius = w * 0.26f,
                center = Offset(cx, cy)
            )

            // Crisp precision ₹ (Rupee) symbol inside coin
            val strokeColor = if (active) Color(0xFF047857) else Color(0xFF334155)
            val sw = w * 0.060f
            val left = w * 0.37f
            val right = w * 0.63f
            val topY = h * 0.355f
            val midY = h * 0.455f

            // Top bar & middle bar of ₹
            drawLine(
                color = strokeColor,
                start = Offset(left, topY),
                end = Offset(right, topY),
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor,
                start = Offset(left, midY),
                end = Offset(right, midY),
                strokeWidth = sw * 0.88f,
                cap = StrokeCap.Round
            )

            // Curved bowl + diagonal leg of ₹
            val rupeeCurve = Path().apply {
                moveTo(w * 0.47f, topY)
                quadraticTo(w * 0.61f, topY + h * 0.02f, w * 0.56f, h * 0.51f)
                quadraticTo(w * 0.52f, h * 0.56f, w * 0.39f, h * 0.555f)
                lineTo(w * 0.59f, h * 0.695f)
            }
            drawPath(
                path = rupeeCurve,
                color = strokeColor,
                style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Custom luxury iOS History / Ledger Archive icon: amethyst-plum glass squircle featuring a
 * Swiss chronograph dial with sweeping metallic rewind arc and luminous gold-white hands.
 */
@Composable
fun PremiumHistoryIcon(
    size: Dp = 36.dp,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    PremiumSquircleShell(
        size = size,
        active = active,
        activeColors = listOf(Color(0xFFC084FC), Color(0xFF9333EA), Color(0xFF581C87)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.51f
            val cy = h * 0.51f
            val dialR = w * 0.28f

            // Subtle dark sapphire-plum inner watch face
            drawCircle(
                color = Color(0xFF1E1035).copy(alpha = if (active) 0.55f else 0.35f),
                radius = dialR,
                center = Offset(cx, cy)
            )

            // Precision sweeping rewind arc (285 degrees)
            val arcRect = Rect(cx - dialR, cy - dialR, cx + dialR, cy + dialR)
            drawArc(
                color = if (active) Color.White else Color.White.copy(alpha = 0.85f),
                startAngle = -140f,
                sweepAngle = 295f,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(width = w * 0.068f, cap = StrokeCap.Round)
            )

            // Crisp rewind arrowhead on top-left of arc
            val arrowPath = Path().apply {
                moveTo(w * 0.22f, h * 0.26f)
                lineTo(w * 0.25f, h * 0.41f)
                lineTo(w * 0.39f, h * 0.34f)
            }
            drawPath(
                path = arrowPath,
                color = if (active) Color(0xFFFDE68A) else Color.White,
                style = Stroke(width = w * 0.060f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Hour & minute chronograph hands
            drawLine(
                color = Color.White,
                start = Offset(cx, cy),
                end = Offset(cx, cy - dialR * 0.58f),
                strokeWidth = w * 0.055f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = if (active) Color(0xFFFDE047) else Color.White.copy(alpha = 0.85f),
                start = Offset(cx, cy),
                end = Offset(cx + dialR * 0.52f, cy + dialR * 0.26f),
                strokeWidth = w * 0.050f,
                cap = StrokeCap.Round
            )
            // Center jewel pivot
            drawCircle(
                color = if (active) Color(0xFF38BDF8) else Color.White,
                radius = w * 0.045f,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Custom luxury iOS Settings icon: brushed titanium squircle with precision-machined 8-tooth
 * gear and glowing emerald center jewel.
 */
@Composable
fun PremiumSettingsIcon(
    size: Dp = 36.dp,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    PremiumSquircleShell(
        size = size,
        active = active,
        activeColors = listOf(Color(0xFF94A3B8), Color(0xFF475569), Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.50f
            val cy = h * 0.50f
            val gearColor = if (active) Color.White else Color.White.copy(alpha = 0.85f)

            // 8 precision gear teeth
            val outerR = w * 0.31f
            val innerR = w * 0.19f
            for (i in 0 until 8) {
                val angleRad = Math.toRadians(i * 45.0)
                val sx = cx + (innerR * cos(angleRad)).toFloat()
                val sy = cy + (innerR * sin(angleRad)).toFloat()
                val ex = cx + (outerR * cos(angleRad)).toFloat()
                val ey = cy + (outerR * sin(angleRad)).toFloat()
                drawLine(
                    color = gearColor,
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = w * 0.088f,
                    cap = StrokeCap.Round
                )
            }
            // Gear ring
            drawCircle(
                color = gearColor,
                radius = w * 0.23f,
                center = Offset(cx, cy)
            )
            // Inner dark hub
            drawCircle(
                color = Color(0xFF0F172A),
                radius = w * 0.125f,
                center = Offset(cx, cy)
            )
            // Emerald precision jewel core
            drawCircle(
                color = if (active) Color(0xFF34D399) else Color(0xFF94A3B8),
                radius = w * 0.055f,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Custom luxury iOS Electricity / WBSEDCL icon: radiant amber-gold squircle with a sculpted
 * high-voltage lightning bolt and subtle electric halo.
 */
@Composable
fun PremiumElectricIcon(
    size: Dp = 36.dp,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    PremiumSquircleShell(
        size = size,
        active = active,
        activeColors = listOf(Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309)),
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Soft energy aura
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = w * 0.28f,
                center = Offset(w * 0.50f, h * 0.50f)
            )

            // Crisp sculpted lightning bolt
            val bolt = Path().apply {
                moveTo(w * 0.56f, h * 0.16f)
                lineTo(w * 0.29f, h * 0.53f)
                lineTo(w * 0.49f, h * 0.53f)
                lineTo(w * 0.43f, h * 0.84f)
                lineTo(w * 0.72f, h * 0.45f)
                lineTo(w * 0.52f, h * 0.45f)
                close()
            }
            drawPath(bolt, color = Color.White)
        }
    }
}

/**
 * Staggered spring entrance reveal for premium dashboard cards and screen sections.
 */
@Composable
fun StaggeredPremiumReveal(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val slideAnim = remember { Animatable(22f) }

    LaunchedEffect(Unit) {
        val staggerMs = (index.coerceIn(0, 6)) * 42L
        if (staggerMs > 0L) delay(staggerMs)
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
        launch {
            slideAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = alphaAnim.value
            translationY = slideAnim.value
        }
    ) {
        content()
    }
}

/**
 * Breathing live telemetry dot with an expanding soft radial pulse halo.
 */
@Composable
fun PulsingLiveDot(
    color: Color,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "live_dot_pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(size * 1.9f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(color.copy(alpha = pulseAlpha))
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Animated BMW 3-color precision telemetry hairline with a sleek traveling light glint.
 */
@Composable
fun AnimatedBmwPrecisionStripe(
    modifier: Modifier = Modifier,
    height: Dp = 2.5.dp
) {
    val shimmerTransition = rememberInfiniteTransition(label = "bmw_stripe_shimmer")
    val shimmerPos by shimmerTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bmw_shimmer_x"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
    ) {
        BmwPrecisionStripe(height = height)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * shimmerPos
            val glintW = size.width * 0.26f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.65f),
                        Color.Transparent
                    ),
                    startX = cx - glintW / 2f,
                    endX = cx + glintW / 2f
                )
            )
        }
    }
}

/**
 * Smoothly animated INR currency counter for KPI hero figures when filters or data update.
 */
@Composable
fun AnimatedCurrencyText(
    amount: Double,
    style: TextStyle,
    color: Color,
    prefix: String = "",
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    val animatedVal by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "animated_inr_value"
    )
    Text(
        text = prefix + formatInr(animatedVal.toDouble()),
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

