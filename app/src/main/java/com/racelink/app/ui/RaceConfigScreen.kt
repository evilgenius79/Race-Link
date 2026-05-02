package com.racelink.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.app.race.RaceConfig
import com.racelink.app.race.StartType
import com.racelink.app.ui.theme.RaceColors

@Composable
fun RaceConfigScreen(
    initial: RaceConfig,
    pendingResponse: Boolean,
    onSend: (RaceConfig) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) {
    var startType by remember { mutableStateOf(initial.startType) }
    var rollSpeed by remember { mutableIntStateOf(initial.rollSpeedMph) }
    var tolerance by remember { mutableIntStateOf(initial.speedToleranceMph) }
    var distance by remember { mutableIntStateOf(initial.distanceFt) }

    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Text("RACE SETUP", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Start type
                Column {
                    SectionHeader("START TYPE")
                    Spacer(Modifier.height(8.dp))
                    Segmented(
                        options = listOf("STANDING" to StartType.STANDING, "ROLLING" to StartType.ROLLING),
                        selected = startType,
                        onSelect = { startType = it },
                    )
                }

                if (startType == StartType.ROLLING) {
                    SliderRow(
                        label = "ROLL SPEED",
                        value = rollSpeed,
                        suffix = "mph",
                        range = 20f..80f,
                        onChange = { rollSpeed = it },
                    )
                    SliderRow(
                        label = "SYNC TOLERANCE",
                        value = tolerance,
                        suffix = "± mph",
                        range = 1f..10f,
                        onChange = { tolerance = it },
                    )
                }

                Column {
                    SectionHeader("DISTANCE")
                    Spacer(Modifier.height(8.dp))
                    Segmented(
                        options = listOf(
                            "1/8 MI" to RaceConfig.EIGHTH_MILE_FT,
                            "1000 FT" to 1000,
                            "1/4 MI" to RaceConfig.QUARTER_MILE_FT,
                        ),
                        selected = distance,
                        onSelect = { distance = it },
                    )
                }

                // Summary card
                RaceSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("RACE PREVIEW", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            buildString {
                                append(if (startType == StartType.ROLLING) "Rolling start at $rollSpeed mph (±$tolerance)" else "Standing start")
                                append("  •  ")
                                append(distanceLabel(distance))
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }

            // Bottom action
            if (pendingResponse) {
                RaceSurface(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "WAITING FOR DRIVER",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            "Sent the race request. The other driver decides next.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Cancel request") }
            } else {
                Button(
                    onClick = {
                        onSend(
                            RaceConfig(
                                startType = startType,
                                rollSpeedMph = rollSpeed,
                                speedToleranceMph = tolerance,
                                distanceFt = distance,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Send, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Text("SEND CHALLENGE", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun distanceLabel(ft: Int): String = when (ft) {
    660 -> "1/8 mile"
    1000 -> "1000 ft"
    1320 -> "1/4 mile"
    else -> "$ft ft"
}

@Composable
private fun <T> Segmented(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, RaceColors.Outline, RoundedCornerShape(14.dp)),
    ) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "segbg",
            )
            val fg by animateColorAsState(
                if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segfg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = fg, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Int,
    suffix: String,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Int) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(label)
            Spacer(Modifier.weight(1f))
            Text(
                "$value $suffix",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range,
            steps = ((range.endInclusive - range.start).toInt() - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

