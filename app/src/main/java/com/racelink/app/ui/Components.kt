package com.racelink.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.racelink.app.ui.theme.RaceColors

/**
 * Full-screen vertical gradient — used as the root background of every screen
 * to give the app a consistent atmospheric feel.
 */
@Composable
fun ScreenBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(RaceColors.BgGradientTop, RaceColors.BgGradientBottom)
                )
            ),
        content = content,
    )
}

/** Raised "card" surface with subtle border - matches our motorsport aesthetic. */
@Composable
fun RaceSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(RaceColors.SurfaceRaised)
            .border(1.dp, RaceColors.Outline, shape),
    ) { content() }
}

/** Small uppercase pill, used for statuses and labels. */
@Composable
fun StatusPill(
    text: String,
    color: Color = MaterialTheme.colorScheme.secondary,
    iconDot: Boolean = true,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (iconDot) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

/**
 * A drag-strip Christmas tree bulb. When [on], a soft radial halo is drawn
 * behind the bulb so it appears to glow against the dark frame.
 */
@Composable
fun TreeBulb(on: Boolean, color: Color, size: Dp = 56.dp) {
    val targetAlpha = if (on) 1f else 0.12f
    val alpha by animateFloatAsState(targetAlpha, label = "alpha")
    val haloAlpha by animateFloatAsState(if (on) 0.55f else 0f, label = "halo")
    val core by animateColorAsState(
        if (on) color else color.copy(alpha = 0.22f),
        label = "core",
    )

    Box(
        Modifier
            .size(size * 2.2f)
            .drawBehind {
                if (haloAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = haloAlpha),
                                color.copy(alpha = haloAlpha * 0.4f),
                                Color.Transparent,
                            ),
                            center = Offset(this.size.width / 2f, this.size.height / 2f),
                            radius = this.size.minDimension / 2f,
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Bulb body — gradient sphere look
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(core, core.copy(alpha = alpha * 0.6f)),
                        center = Offset(size.value * 1.2f, size.value * 1.2f),
                    )
                )
                .border(
                    1.dp,
                    Color.Black.copy(alpha = if (on) 0.4f else 0.6f),
                    CircleShape
                ),
        ) {
            // Specular highlight
            Box(
                Modifier
                    .padding(top = size * 0.12f, start = size * 0.18f)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (on) 0.55f else 0.08f)),
            )
        }
    }
}

/** Section header with a colored leading bar - used in Pairing/Config screens. */
@Composable
fun SectionHeader(text: String, accent: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(width = 3.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
