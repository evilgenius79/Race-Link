package com.racelink.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.racelink.app.race.RaceConfig
import com.racelink.app.race.StartType

@Composable
fun InboundRequestDialog(
    config: RaceConfig,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        confirmButton = { TextButton(onClick = onAccept) { Text("Accept") } },
        dismissButton = { TextButton(onClick = onDecline) { Text("Decline") } },
        title = { Text("Race request") },
        text = {
            Column {
                Text(
                    if (config.startType == StartType.ROLLING)
                        "Rolling start at ${config.rollSpeedMph} mph (±${config.speedToleranceMph})"
                    else "Standing start"
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                Text("${config.distanceFt} ft")
            }
        },
    )
}
