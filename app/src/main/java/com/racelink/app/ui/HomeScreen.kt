package com.racelink.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.app.ui.theme.RaceColors

@Composable
fun HomeScreen(
    btReady: Boolean,
    locReady: Boolean,
    nickname: String,
    onPair: () -> Unit,
    onRequestPermissions: () -> Unit,
    onShowTutorial: () -> Unit = {},
    onEditNickname: () -> Unit = {},
    onShowHistory: () -> Unit = {},
) {
    ScreenBackground {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // Hero
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Logo()
                Spacer(Modifier.height(24.dp))
                Text(
                    "RACE LINK",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "HEADS-UP DRAG RACING",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (nickname.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onEditNickname() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            nickname.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.size(6.dp))
                        Icon(
                            Icons.Default.Edit, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Status pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = if (btReady) "BLUETOOTH READY" else "BLUETOOTH OFF",
                    color = if (btReady) RaceColors.Green else MaterialTheme.colorScheme.error,
                )
                StatusPill(
                    text = if (locReady) "GPS READY" else "GPS OFF",
                    color = if (locReady) RaceColors.Green else MaterialTheme.colorScheme.error,
                )
            }

            // Actions
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!btReady || !locReady) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.RadioButtonChecked, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(10.dp))
                        Text("Grant permissions", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = onPair,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Default.FlagCircle, null, Modifier.size(22.dp))
                        Spacer(Modifier.size(10.dp))
                        Text("FIND A RACE", fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    OutlinedButton(
                        onClick = onPair,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Bluetooth, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(10.dp))
                        Text("Wait for incoming challenge")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onShowHistory) {
                        Icon(
                            Icons.Default.EmojiEvents, null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "History",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    TextButton(onClick = onShowTutorial) {
                        Icon(
                            Icons.Default.HelpOutline, null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "How to use",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Logo() {
    Box(
        Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, RaceColors.AccentDeep)
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                    )
                )
        )
        Text(
            "RL",
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
