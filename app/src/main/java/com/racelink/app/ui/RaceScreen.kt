package com.racelink.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.app.race.RaceEngine
import com.racelink.app.race.RaceFeedback
import com.racelink.app.race.StartType
import com.racelink.app.ui.theme.RaceColors
import kotlin.math.abs

@Composable
fun RaceScreen(
    state: RaceEngine.State,
    selfName: String,
    peerName: String?,
    onReady: (Boolean) -> Unit,
    onAbort: () -> Unit,
    onDone: () -> Unit,
) {
    // Keep the screen on while we're actively racing - the phone may sit
    // on the dash for several minutes during arming.
    val view = LocalView.current
    val activePhases = setOf(
        RaceEngine.Phase.ARMED, RaceEngine.Phase.STAGED,
        RaceEngine.Phase.TREE, RaceEngine.Phase.RUNNING,
    )
    DisposableEffect(state.phase) {
        view.keepScreenOn = state.phase in activePhases
        onDispose { view.keepScreenOn = false }
    }

    // Audio + haptic for the tree.
    val context = LocalContext.current
    val feedback = remember { RaceFeedback(context) }
    DisposableEffect(Unit) {
        onDispose { feedback.release() }
    }
    LaunchedEffect(state.light) {
        when (state.light) {
            RaceEngine.TreeLight.AMBER1,
            RaceEngine.TreeLight.AMBER2,
            RaceEngine.TreeLight.AMBER3 -> feedback.amber()
            RaceEngine.TreeLight.GREEN -> feedback.green()
            else -> { /* no feedback */ }
        }
    }
    LaunchedEffect(state.phase) {
        if (state.phase == RaceEngine.Phase.FINISHED) feedback.finish()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(RaceColors.BgGradientTop, RaceColors.BgGradientBottom)
                )
            ),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            TopHud(state)
            Spacer(Modifier.height(16.dp))

            // The visual stage swaps based on phase: pre-race we show a big tree,
            // mid-race we show distance + tree, post-race we show results.
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "stage",
            ) { phase ->
                when (phase) {
                    RaceEngine.Phase.RUNNING, RaceEngine.Phase.FINISHED -> {
                        Column {
                            DistancePanel(state)
                            Spacer(Modifier.height(12.dp))
                            TreeFrame(state.light, compact = true)
                        }
                    }
                    else -> TreeFrame(state.light, compact = false)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpeedCard(
                    label = selfName.ifBlank { "YOU" }.uppercase(),
                    mph = state.selfSpeedMph,
                    targetMph = state.config.rollSpeedMph,
                    tolerance = state.config.speedToleranceMph,
                    rolling = state.config.startType == StartType.ROLLING,
                    ready = state.selfReady,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                SpeedCard(
                    label = (peerName ?: "OPP").uppercase(),
                    mph = state.peerSpeedMph,
                    targetMph = state.config.rollSpeedMph,
                    tolerance = state.config.speedToleranceMph,
                    rolling = state.config.startType == StartType.ROLLING,
                    ready = state.peerReady,
                    accent = RaceColors.Blue,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.phase == RaceEngine.Phase.FINISHED) {
                Spacer(Modifier.height(12.dp))
                ResultsCard(state, selfName, peerName)
                if (state.splits.isNotEmpty() || state.reactionMs != null) {
                    Spacer(Modifier.height(8.dp))
                    SplitsCard(state)
                }
            }

            Spacer(Modifier.weight(1f))
            BottomControls(state, onReady, onAbort, onDone)
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Top HUD ─────────────────────────────────────────────────────────────────

@Composable
private fun TopHud(state: RaceEngine.State) {
    val cfg = state.config
    val (label, color) = when (state.phase) {
        RaceEngine.Phase.IDLE -> "READY UP" to RaceColors.OnSurfaceMuted
        RaceEngine.Phase.ARMED -> if (cfg.startType == StartType.ROLLING)
            "CRUISE TO ${cfg.rollSpeedMph} MPH" to MaterialTheme.colorScheme.secondary
        else "STAGE THE CAR" to MaterialTheme.colorScheme.secondary
        RaceEngine.Phase.STAGED -> "STAGED" to MaterialTheme.colorScheme.secondary
        RaceEngine.Phase.TREE -> "GO" to RaceColors.Green
        RaceEngine.Phase.RUNNING -> "RACING" to RaceColors.Green
        RaceEngine.Phase.FINISHED -> "FINISHED" to MaterialTheme.colorScheme.primary
        RaceEngine.Phase.ABORTED -> "ABORTED" to MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusPill(label, color)
        Spacer(Modifier.weight(1f))
        Text(
            "${distanceLabel(cfg.distanceFt)} • ${if (state.isHost) "HOST" else "GUEST"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun distanceLabel(ft: Int) = when (ft) {
    660 -> "1/8 MI"
    1000 -> "1000 FT"
    1320 -> "1/4 MI"
    else -> "$ft FT"
}

// ─── Tree ────────────────────────────────────────────────────────────────────

@Composable
private fun TreeFrame(light: RaceEngine.TreeLight, compact: Boolean) {
    val height = if (compact) 96.dp else 320.dp
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050507), Color(0xFF0E0E15))
                )
            )
            .border(1.dp, RaceColors.Outline, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                TreeBulb(on = isOn(light, RaceEngine.TreeLight.AMBER1, andAfter = true), color = RaceColors.Amber, size = 26.dp)
                TreeBulb(on = isOn(light, RaceEngine.TreeLight.AMBER2, andAfter = true), color = RaceColors.Amber, size = 26.dp)
                TreeBulb(on = isOn(light, RaceEngine.TreeLight.AMBER3, andAfter = true), color = RaceColors.Amber, size = 26.dp)
                TreeBulb(on = light == RaceEngine.TreeLight.GREEN, color = RaceColors.Green, size = 26.dp)
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TreeBulb(on = isOn(light, RaceEngine.TreeLight.AMBER1, andAfter = true), color = RaceColors.Amber, size = 44.dp)
                TreeBulb(on = isOn(light, RaceEngine.TreeLight.AMBER2, andAfter = true), color = RaceColors.Amber, size = 44.dp)
                TreeBulb(on = isOn(light, RaceEngine.TreeLight.AMBER3, andAfter = true), color = RaceColors.Amber, size = 44.dp)
                TreeBulb(on = light == RaceEngine.TreeLight.GREEN, color = RaceColors.Green, size = 52.dp)
            }
        }
    }
}

private fun isOn(current: RaceEngine.TreeLight, target: RaceEngine.TreeLight, andAfter: Boolean): Boolean {
    if (current == target) return true
    if (!andAfter) return false
    val order = listOf(
        RaceEngine.TreeLight.OFF,
        RaceEngine.TreeLight.AMBER1,
        RaceEngine.TreeLight.AMBER2,
        RaceEngine.TreeLight.AMBER3,
        RaceEngine.TreeLight.GREEN,
    )
    val ci = order.indexOf(current)
    val ti = order.indexOf(target)
    return ci >= ti && ci != -1 && ti != -1
}

// ─── Speed cards ─────────────────────────────────────────────────────────────

@Composable
private fun SpeedCard(
    label: String,
    mph: Float,
    targetMph: Int,
    tolerance: Int,
    rolling: Boolean,
    ready: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val inWindow = rolling && abs(mph - targetMph) <= tolerance
    val color by animateColorAsState(
        if (inWindow) RaceColors.Green else Color.White,
        label = "speedColor",
    )
    RaceSurface(modifier) {
        Column(
            Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (ready) RaceColors.Green else RaceColors.OnSurfaceFaint)
                )
                Spacer(Modifier.size(6.dp))
                Text(label, color = accent, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    if (ready) "READY" else "—",
                    color = if (ready) RaceColors.Green else RaceColors.OnSurfaceFaint,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "%.0f".format(mph),
                color = color,
                style = MaterialTheme.typography.displayMedium,
            )
            Text("MPH", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            if (rolling) {
                Spacer(Modifier.height(8.dp))
                SpeedWindowBar(
                    mph = mph,
                    target = targetMph.toFloat(),
                    tolerance = tolerance.toFloat(),
                )
            }
        }
    }
}

/** A horizontal bar visualizing where current mph sits relative to target±tolerance. */
@Composable
private fun SpeedWindowBar(mph: Float, target: Float, tolerance: Float) {
    val span = (tolerance * 4f).coerceAtLeast(8f) // visible range = ±2*tolerance
    val pct = ((mph - (target - span)) / (span * 2f)).coerceIn(0f, 1f)
    val animPct by animateFloatAsState(pct, tween(180), label = "speedPct")

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val w = size.width
        val h = size.height
        val r = h / 2f
        // base track
        drawRoundRect(
            color = RaceColors.SurfaceHigh,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        )
        // window band (target ± tolerance)
        val winLeft = ((target - tolerance) - (target - span)) / (span * 2f) * w
        val winRight = ((target + tolerance) - (target - span)) / (span * 2f) * w
        drawRoundRect(
            color = RaceColors.Green.copy(alpha = 0.25f),
            topLeft = Offset(winLeft, 0f),
            size = androidx.compose.ui.geometry.Size((winRight - winLeft).coerceAtLeast(0f), h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        )
        // marker
        val x = animPct * w
        drawLine(
            color = Color.White,
            start = Offset(x, -2f),
            end = Offset(x, h + 2f),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
        )
    }
}

// ─── Distance panel ──────────────────────────────────────────────────────────

@Composable
private fun DistancePanel(state: RaceEngine.State) {
    val cfg = state.config
    val pct = (state.selfDistanceFt / cfg.distanceFt.toFloat()).coerceIn(0f, 1f)
    val animPct by animateFloatAsState(pct, tween(150), label = "distPct")
    val accent = MaterialTheme.colorScheme.primary

    RaceSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("ELAPSED", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "%.3f".format(state.selfElapsedMs / 1000f),
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DISTANCE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${state.selfDistanceFt.toInt()} / ${cfg.distanceFt} ft",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // Progress bar
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                val w = size.width
                val h = size.height
                val r = h / 2f
                drawRoundRect(
                    color = RaceColors.SurfaceHigh,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(accent, RaceColors.AccentSoft)),
                    size = androidx.compose.ui.geometry.Size(w * animPct, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                )
            }
        }
    }
}

// ─── Results ─────────────────────────────────────────────────────────────────

@Composable
private fun ResultsCard(state: RaceEngine.State, selfName: String, peerName: String?) {
    val self = state.selfFinishMs ?: return
    val peer = state.peerFinishMs ?: return
    val won = self < peer
    val accent = if (won) RaceColors.Green else MaterialTheme.colorScheme.error

    RaceSurface(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (won) "WIN" else "LOSS",
                color = accent,
                fontWeight = FontWeight.Black,
                fontSize = 56.sp,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ResultColumn(
                    label = selfName.ifBlank { "YOU" }.uppercase(),
                    ms = self, mph = state.selfFinalMph,
                    color = MaterialTheme.colorScheme.primary,
                )
                ResultColumn(
                    label = (peerName ?: "OPP").uppercase(),
                    ms = peer, mph = state.peerFinalMph,
                    color = RaceColors.Blue,
                )
            }
            Spacer(Modifier.height(10.dp))
            val margin = (peer - self) / 1000f
            Text(
                "MARGIN ${"%+.3f".format(margin)} s",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Split times + reaction time, shown below the win/loss card after a run. */
@Composable
private fun SplitsCard(state: RaceEngine.State) {
    RaceSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                "RUN DETAIL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            state.reactionMs?.let { rt ->
                SplitRow("REACTION", "%.3f s".format(rt / 1000f))
            }
            // Splits in increasing distance order
            state.splits.entries.sortedBy { it.key }.forEach { (ft, ms) ->
                SplitRow(splitLabel(ft), "%.3f s".format(ms / 1000f))
            }
            // Final ET goes at the bottom for completeness
            state.selfFinishMs?.let { fin ->
                SplitRow(distanceLabel(state.config.distanceFt), "%.3f s".format(fin / 1000f), bold = true)
            }
        }
    }
}

private fun splitLabel(ft: Int): String = when (ft) {
    60 -> "60 FT"
    330 -> "330 FT"
    660 -> "1/8 MI"
    1000 -> "1000 FT"
    1320 -> "1/4 MI"
    else -> "$ft FT"
}

@Composable
private fun SplitRow(label: String, value: String, bold: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = if (bold) Color.White else MaterialTheme.colorScheme.onSurface,
            style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ResultColumn(label: String, ms: Long, mph: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = color, style = MaterialTheme.typography.labelMedium)
        Text(
            "%.3f".format(ms / 1000f),
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            "${"%.0f".format(mph)} MPH",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

// ─── Bottom controls ─────────────────────────────────────────────────────────

@Composable
private fun BottomControls(
    state: RaceEngine.State,
    onReady: (Boolean) -> Unit,
    onAbort: () -> Unit,
    onDone: () -> Unit,
) {
    when (state.phase) {
        RaceEngine.Phase.ARMED -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onReady(!state.selfReady) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = if (state.selfReady) ButtonDefaults.buttonColors(
                        containerColor = RaceColors.Green, contentColor = Color.Black,
                    ) else ButtonDefaults.buttonColors(),
                ) {
                    Icon(if (state.selfReady) Icons.Default.CheckCircle else Icons.Default.Bolt,
                        null, Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(if (state.selfReady) "READY" else "I'M READY",
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                OutlinedButton(
                    onClick = onAbort,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Cancel, null, Modifier.size(20.dp))
                }
            }
        }
        RaceEngine.Phase.FINISHED, RaceEngine.Phase.ABORTED -> {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("DONE", fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
            state.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> {
            OutlinedButton(
                onClick = onAbort,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Cancel, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Abort")
            }
        }
    }
}
