package com.racelink.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.app.race.RaceConfig
import com.racelink.app.race.StartType

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

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Set up the race", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Text("Start type", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = startType == StartType.STANDING,
                onClick = { startType = StartType.STANDING },
                label = { Text("Standing") },
            )
            FilterChip(
                selected = startType == StartType.ROLLING,
                onClick = { startType = StartType.ROLLING },
                label = { Text("Rolling") },
            )
        }

        if (startType == StartType.ROLLING) {
            Text("Roll speed: $rollSpeed mph", fontWeight = FontWeight.SemiBold)
            Slider(
                value = rollSpeed.toFloat(),
                onValueChange = { rollSpeed = it.toInt() },
                valueRange = 20f..80f,
                steps = 60 - 1,
            )

            Text("Sync tolerance: ±$tolerance mph", fontWeight = FontWeight.SemiBold)
            Slider(
                value = tolerance.toFloat(),
                onValueChange = { tolerance = it.toInt() },
                valueRange = 1f..10f,
                steps = 9 - 1,
            )
        }

        Text("Distance", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = distance == RaceConfig.EIGHTH_MILE_FT,
                onClick = { distance = RaceConfig.EIGHTH_MILE_FT },
                label = { Text("1/8 mile") },
            )
            FilterChip(
                selected = distance == RaceConfig.QUARTER_MILE_FT,
                onClick = { distance = RaceConfig.QUARTER_MILE_FT },
                label = { Text("1/4 mile") },
            )
            FilterChip(
                selected = distance == 1000,
                onClick = { distance = 1000 },
                label = { Text("1000 ft") },
            )
        }

        Spacer(Modifier.height(8.dp))

        if (pendingResponse) {
            Text("Waiting for the other driver to accept...", color = MaterialTheme.colorScheme.secondary)
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel request")
            }
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
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Send race request") }
        }

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
