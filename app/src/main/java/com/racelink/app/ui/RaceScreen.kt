package com.racelink.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.app.race.RaceEngine

@Composable
fun RaceScreen(
    state: RaceEngine.State,
    onReady: (Boolean) -> Unit,
    onAbort: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
    ) {
        TopHud(state)
        Spacer(Modifier.height(16.dp))
        TreeLights(state)
        Spacer(Modifier.height(16.dp))
        SpeedBlock(state)
        Spacer(Modifier.weight(1f))
        BottomControls(state, onReady, onAbort, onDone)
    }
}

@Composable
private fun TopHud(state: RaceEngine.State) {
    val cfg = state.config
    val phaseLabel = when (state.phase) {
        RaceEngine.Phase.IDLE -> "—"
        RaceEngine.Phase.ARMED -> if (cfg.startType.name == "ROLLING")
            "Cruise to ${cfg.rollSpeedMph} mph (±${cfg.speedToleranceMph})"
        else "Come to a stop"
        RaceEngine.Phase.STAGED -> "Staged"
        RaceEngine.Phase.TREE -> "Tree"
        RaceEngine.Phase.RUNNING -> "Racing"
        RaceEngine.Phase.FINISHED -> "Finished"
        RaceEngine.Phase.ABORTED -> "Aborted"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            phaseLabel,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${state.config.distanceFt} ft  •  ${if (state.isHost) "Host" else "Guest"}",
            color = Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun TreeLights(state: RaceEngine.State) {
    val active = state.light
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111111)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Bulb(on = active == RaceEngine.TreeLight.AMBER1 || active == RaceEngine.TreeLight.AMBER2 ||
                active == RaceEngine.TreeLight.AMBER3 || active == RaceEngine.TreeLight.GREEN, color = Color(0xFFFFC107))
            Bulb(on = active == RaceEngine.TreeLight.AMBER2 || active == RaceEngine.TreeLight.AMBER3 ||
                active == RaceEngine.TreeLight.GREEN, color = Color(0xFFFFC107))
            Bulb(on = active == RaceEngine.TreeLight.AMBER3 || active == RaceEngine.TreeLight.GREEN, color = Color(0xFFFFC107))
            Bulb(on = active == RaceEngine.TreeLight.GREEN, color = Color(0xFF4CAF50))
            Bulb(on = active == RaceEngine.TreeLight.RED, color = Color(0xFFF44336))
        }
    }
}

@Composable
private fun Bulb(on: Boolean, color: Color) {
    val animated by animateColorAsState(
        targetValue = if (on) color else color.copy(alpha = 0.15f),
        label = "bulb",
    )
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(animated),
    )
}

@Composable
private fun SpeedBlock(state: RaceEngine.State) {
    val cfg = state.config
    val target = cfg.rollSpeedMph
    val tol = cfg.speedToleranceMph
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpeedCard("YOU", state.selfSpeedMph, target, tol, Modifier.weight(1f))
        SpeedCard("OPP", state.peerSpeedMph, target, tol, Modifier.weight(1f))
    }
    if (state.phase == RaceEngine.Phase.RUNNING || state.phase == RaceEngine.Phase.FINISHED) {
        Spacer(Modifier.height(12.dp))
        Column {
            Text(
                "Distance: ${"%.0f".format(state.selfDistanceFt)} / ${cfg.distanceFt} ft",
                color = Color.White, fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = { (state.selfDistanceFt / cfg.distanceFt.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Elapsed: ${"%.3f".format(state.selfElapsedMs / 1000f)} s",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
    if (state.phase == RaceEngine.Phase.FINISHED) {
        Spacer(Modifier.height(8.dp))
        ResultsBlock(state)
    }
}

@Composable
private fun SpeedCard(label: String, mph: Float, target: Int, tol: Int, modifier: Modifier = Modifier) {
    val inWindow = kotlin.math.abs(mph - target) <= tol
    val color = if (inWindow) Color(0xFF4CAF50) else Color.White
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1F))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
        Text(
            "%.0f".format(mph),
            color = color,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
        )
        Text("mph", color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
private fun ResultsBlock(state: RaceEngine.State) {
    val self = state.selfFinishMs ?: return
    val peer = state.peerFinishMs ?: return
    val won = self < peer
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (won) Color(0xFF1B3A1B) else Color(0xFF3A1B1B))
            .padding(16.dp),
    ) {
        Text(
            if (won) "WIN" else "LOSS",
            color = if (won) Color(0xFF8BC34A) else Color(0xFFEF5350),
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You: ${"%.3f".format(self / 1000f)} s @ ${"%.0f".format(state.selfFinalMph)} mph",
            color = Color.White, fontFamily = FontFamily.Monospace,
        )
        Text(
            "Opp: ${"%.3f".format(peer / 1000f)} s @ ${"%.0f".format(state.peerFinalMph)} mph",
            color = Color.White, fontFamily = FontFamily.Monospace,
        )
        val margin = (peer - self) / 1000f
        Text(
            "Margin: ${"%+.3f".format(margin)} s",
            color = Color.White.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
        )
    }
}

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
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.selfReady) "Cancel ready" else "I'm ready") }
                OutlinedButton(onClick = onAbort, modifier = Modifier.width(120.dp)) { Text("Abort") }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "You: ${if (state.selfReady) "READY" else "—"}     Opp: ${if (state.peerReady) "READY" else "—"}",
                color = Color.White,
            )
        }
        RaceEngine.Phase.FINISHED, RaceEngine.Phase.ABORTED -> {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            state.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color.White.copy(alpha = 0.7f))
            }
        }
        else -> {
            OutlinedButton(onClick = onAbort, modifier = Modifier.fillMaxWidth()) { Text("Abort") }
        }
    }
}
