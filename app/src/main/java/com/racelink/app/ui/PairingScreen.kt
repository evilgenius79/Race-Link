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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.app.bluetooth.BluetoothLink

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
    Column(
        Modifier.fillMaxSize().padding(16.dp),
    ) {
        Text("Pair with another driver", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            when (state) {
                BluetoothLink.ConnState.IDLE -> "Idle"
                BluetoothLink.ConnState.LISTENING -> "Waiting for incoming connection..."
                BluetoothLink.ConnState.DISCOVERING -> "Scanning for nearby cars..."
                BluetoothLink.ConnState.CONNECTING -> "Connecting..."
                BluetoothLink.ConnState.CONNECTED -> "Connected to ${peer?.name ?: peer?.address ?: "peer"}"
                BluetoothLink.ConnState.ERROR -> "Error - try again"
            },
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(16.dp))

        if (state == BluetoothLink.ConnState.CONNECTED) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continue to race setup")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text("Disconnect")
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = if (state == BluetoothLink.ConnState.DISCOVERING) onStopScan else onScan,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state == BluetoothLink.ConnState.DISCOVERING) "Stop scan" else "Scan")
                }
                Button(onClick = onHost, modifier = Modifier.weight(1f)) {
                    Text(if (state == BluetoothLink.ConnState.LISTENING) "Hosting..." else "Host")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (state == BluetoothLink.ConnState.CONNECTING ||
                state == BluetoothLink.ConnState.LISTENING ||
                state == BluetoothLink.ConnState.DISCOVERING
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                Spacer(Modifier.height(16.dp))
            }

            if (bonded.isNotEmpty()) {
                Text("Paired devices", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        bonded.forEachIndexed { i, d ->
                            DeviceRow(d, onClick = { onConnect(d) })
                            if (i < bonded.size - 1) Divider()
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Nearby", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            if (discovered.isEmpty()) {
                Text("No devices yet. Tap Scan.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(discovered) { d ->
                        DeviceRow(d, onClick = { onConnect(d) })
                        Divider()
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

@Composable
private fun DeviceRow(d: BluetoothLink.DiscoveredDevice, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                .padding(8.dp)
        ) { Text("BT", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp) }
        Spacer(Modifier.height(0.dp))
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(d.name ?: "(unnamed)", fontWeight = FontWeight.SemiBold)
            Text(d.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text("Connect", color = MaterialTheme.colorScheme.primary)
    }
}
