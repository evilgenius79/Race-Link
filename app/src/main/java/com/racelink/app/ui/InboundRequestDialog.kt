package com.racelink.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.racelink.app.race.RaceConfig
import com.racelink.app.race.StartType
import com.racelink.app.ui.theme.RaceColors

@Composable
fun InboundRequestDialog(
    config: RaceConfig,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Dialog(onDismissRequest = onDecline) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, RaceColors.Outline, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.FlagCircle, null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            "RACE REQUEST",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            "Another driver wants to race",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Detail rows
                DetailRow(
                    "START",
                    if (config.startType == StartType.ROLLING)
                        "Rolling at ${config.rollSpeedMph} mph"
                    else "Standing"
                )
                if (config.startType == StartType.ROLLING) {
                    DetailRow("TOLERANCE", "± ${config.speedToleranceMph} mph")
                }
                DetailRow(
                    "DISTANCE",
                    when (config.distanceFt) {
                        660 -> "1/8 mile"
                        1000 -> "1000 ft"
                        1320 -> "1/4 mile"
                        else -> "${config.distanceFt} ft"
                    }
                )

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("Decline") }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RaceColors.Green, contentColor = Color.Black,
                        ),
                    ) {
                        Text("ACCEPT", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}
