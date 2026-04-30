package com.racelink.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    btReady: Boolean,
    locReady: Boolean,
    onPair: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("RACE LINK", fontSize = 44.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Heads-up drag racing", fontSize = 16.sp)
        Spacer(Modifier.height(48.dp))

        if (!btReady || !locReady) {
            Text(
                "Need permissions: " +
                    listOfNotNull(
                        if (!btReady) "Bluetooth" else null,
                        if (!locReady) "Location" else null,
                    ).joinToString(" + "),
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("Grant permissions")
            }
        } else {
            Button(
                onClick = onPair,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Find a car to race", fontSize = 18.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onPair, modifier = Modifier.fillMaxWidth()) {
                Text("Wait for incoming challenge")
            }
        }
    }
}
