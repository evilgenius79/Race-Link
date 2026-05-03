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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.racelink.app.bluetooth.BluetoothLink
import com.racelink.app.ui.theme.RaceColors

@Composable
fun PairingScreen(
    state: BluetoothLink.ConnState,
    bonded: List<BluetoothLink.DiscoveredDevice>,
    discovered: List<BluetoothLink.DiscoveredDevice>,
    peer: BluetoothLink.DiscoveredDevice?,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onHost: () -> Unit,
    onConnect: (BluetoothLink.DiscoveredDevice) -> Unit,
    onDisconnect: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    // Default to "Race Link only" so the list isn't dominated by car BT
    // accessories. Toggle off to fall back to every paired/discovered device.
    var showAll by rememberSaveable { mutableStateOf(false) }
    val visibleBonded = if (showAll) bonded else bonded.filter { it.verified }
    val visibleDiscovered = if (showAll) discovered else discovered.filter { it.verified }
    val hiddenCount = (bonded.size - visibleBonded.size) + (discovered.size - visibleDiscovered.size)

    ScreenBackground {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            TopBar(title = "PAIR", onBack = onBack)

            // Status header with big readable state
            ConnectionHeader(state = state, peer = peer)

            Spacer(Modifier.height(16.dp))

            if (state == BluetoothLink.ConnState.CONNECTED) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Continue to race setup", style = MaterialTheme.typography.labelLarge) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Disconnect") }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = if (state == BluetoothLink.ConnState.DISCOVERING) onStopScan else onScan,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (state == BluetoothLink.ConnState.DISCOVERING) "Stop" else "Scan")
                    }
                    Button(
                        onClick = onHost,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Wifi, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (state == BluetoothLink.ConnState.LISTENING) "Hosting…" else "Host")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Filter toggle - by default we hide non-Race-Link devices
                // (car infotainment, OBD readers, headphones, etc.). The
                // hidden count nudges the user toward expanding when they
                // can't find their friend's phone.
                FilterToggle(
                    showAll = showAll,
                    hiddenCount = hiddenCount,
                    onToggle = { showAll = it },
                )

                Spacer(Modifier.height(16.dp))

                if (visibleBonded.isNotEmpty()) {
                    SectionHeader(if (showAll) "PAIRED DEVICES" else "PAIRED RACE LINK PHONES")
                    Spacer(Modifier.height(8.dp))
                    RaceSurface(Modifier.fillMaxWidth()) {
                        Column {
                            visibleBonded.forEachIndexed { i, d ->
                                DeviceRow(d, onClick = { onConnect(d) })
                                if (i < visibleBonded.lastIndex) Box(
                                    Modifier.fillMaxWidth().height(1.dp).background(RaceColors.Outline)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                SectionHeader(if (showAll) "NEARBY" else "NEARBY RACE LINK PHONES")
                Spacer(Modifier.height(8.dp))
                if (visibleDiscovered.isEmpty()) {
                    RaceSurface(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                when {
                                    state == BluetoothLink.ConnState.DISCOVERING && !showAll ->
                                        "Searching… make sure the other phone has tapped Host."
                                    state == BluetoothLink.ConnState.DISCOVERING ->
                                        "Searching for nearby cars…"
                                    !showAll ->
                                        "No Race Link phones found. Make sure the other phone has tapped Host, or toggle 'Show all' above."
                                    else -> "Tap Scan to look for nearby cars"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    RaceSurface(Modifier.fillMaxWidth()) {
                        LazyColumn {
                            items(visibleDiscovered) { d ->
                                DeviceRow(d, onClick = { onConnect(d) })
                                Box(Modifier.fillMaxWidth().height(1.dp).background(RaceColors.Outline))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun ConnectionHeader(
    state: BluetoothLink.ConnState,
    peer: BluetoothLink.DiscoveredDevice?,
) {
    val (label, color, busy) = when (state) {
        BluetoothLink.ConnState.IDLE -> Triple("READY", RaceColors.OnSurfaceMuted, false)
        BluetoothLink.ConnState.LISTENING -> Triple("HOSTING", MaterialTheme.colorScheme.secondary, true)
        BluetoothLink.ConnState.DISCOVERING -> Triple("SCANNING", MaterialTheme.colorScheme.secondary, true)
        BluetoothLink.ConnState.CONNECTING -> Triple("CONNECTING", MaterialTheme.colorScheme.secondary, true)
        BluetoothLink.ConnState.CONNECTED -> Triple("CONNECTED", RaceColors.Green, false)
        BluetoothLink.ConnState.ERROR -> Triple("ERROR", MaterialTheme.colorScheme.error, false)
    }

    RaceSurface(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Bluetooth, null, tint = color)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = color, style = MaterialTheme.typography.labelLarge)
                Text(
                    when (state) {
                        BluetoothLink.ConnState.CONNECTED ->
                            peer?.name ?: peer?.address ?: "Peer"
                        BluetoothLink.ConnState.LISTENING -> "Waiting for someone to connect"
                        BluetoothLink.ConnState.DISCOVERING -> "Looking for nearby Race Link phones"
                        else -> "Choose Scan or Host below"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (busy) {
                CircularProgressIndicator(
                    Modifier.size(24.dp),
                    color = color,
                    strokeWidth = 3.dp,
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(d: BluetoothLink.DiscoveredDevice, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Bluetooth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                d.name ?: "Unnamed device",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                d.address,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilterToggle(
    showAll: Boolean,
    hiddenCount: Int,
    onToggle: (Boolean) -> Unit,
) {
    RaceSurface(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (showAll) "Showing all Bluetooth devices" else "Race Link phones only",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (showAll) "Includes car audio, OBD readers, headphones…"
                    else if (hiddenCount > 0) "$hiddenCount other device${if (hiddenCount == 1) "" else "s"} hidden"
                    else "No other devices hidden",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = showAll,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}
