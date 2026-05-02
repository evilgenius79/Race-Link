package com.racelink.app.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.racelink.app.race.RaceResult
import com.racelink.app.race.StartType
import com.racelink.app.ui.theme.RaceColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    results: List<RaceResult>,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }

    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text("HISTORY", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Spacer(Modifier.weight(1f))
                if (results.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(
                            Icons.Default.DeleteSweep, "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (results.isEmpty()) {
                EmptyState()
            } else {
                BestStrip(results)
                Spacer(Modifier.height(12.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results) { r -> ResultRow(r) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all history?") },
            text = { Text("This deletes every saved race on this phone. Cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onClear(); confirmClear = false }) {
                    Text("CLEAR", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.EmojiEvents,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("No races yet", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Run your first race and it'll show up here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BestStrip(results: List<RaceResult>) {
    // Group by distance, pick the best ET in each.
    val byDist = results.groupBy { it.distanceFt }
    val rows = byDist.entries
        .sortedBy { it.key }
        .mapNotNull { (dist, list) ->
            list.minByOrNull { it.selfEtMs }?.let { dist to it.selfEtMs }
        }
    if (rows.isEmpty()) return
    RaceSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "PERSONAL BESTS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))
            rows.forEach { (dist, ms) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        distanceShort(dist),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "%.3f s".format(ms / 1000f),
                        color = RaceColors.Green,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

private val DATE_FMT = SimpleDateFormat("MMM d  h:mma", Locale.getDefault())

@Composable
private fun ResultRow(r: RaceResult) {
    RaceSurface(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Win/Loss indicator
            val color = if (r.won) RaceColors.Green else MaterialTheme.colorScheme.error
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (r.won) "W" else "L",
                    color = color,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "vs ${r.peerName ?: "Driver"}  •  ${distanceShort(r.distanceFt)}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${if (r.startType == StartType.ROLLING) "Rolling ${r.rollSpeedMph} mph" else "Standing"}  •  ${DATE_FMT.format(Date(r.timestampMs))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.3f".format(r.selfEtMs / 1000f),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    "${"%.0f".format(r.selfMph)} MPH",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun distanceShort(ft: Int): String = when (ft) {
    660 -> "1/8 mile"
    1000 -> "1000 ft"
    1320 -> "1/4 mile"
    else -> "$ft ft"
}
