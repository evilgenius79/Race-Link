package com.racelink.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.racelink.app.race.RaceEngine
import com.racelink.app.ui.HomeScreen
import com.racelink.app.ui.InboundRequestDialog
import com.racelink.app.ui.PairingScreen
import com.racelink.app.ui.RaceConfigScreen
import com.racelink.app.ui.RaceScreen
import com.racelink.app.ui.TutorialScreen
import com.racelink.app.ui.theme.RaceLinkTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RaceLinkTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(vm)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.link.shutdown()
        vm.tracker.stop()
    }
}

private enum class Screen { TUTORIAL, HOME, PAIRING, CONFIG, RACE }

@Composable
private fun AppRoot(vm: AppViewModel) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    var screen by rememberSaveable {
        mutableStateOf(if (prefs.tutorialSeen) Screen.HOME else Screen.TUTORIAL)
    }

    var hasBt by remember { mutableStateOf(vm.link.hasConnectPerm() && vm.link.hasScanPerm()) }
    var hasLoc by remember { mutableStateOf(vm.tracker.hasPermission()) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasBt = vm.link.hasConnectPerm() && vm.link.hasScanPerm()
        hasLoc = vm.tracker.hasPermission()
        if (hasLoc) vm.startTrackerIfPossible()
        result.entries.firstOrNull { !it.value }?.let {
            Toast.makeText(context, "Permission denied: ${it.key}", Toast.LENGTH_SHORT).show()
        }
    }

    val askPerms: () -> Unit = {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += Manifest.permission.BLUETOOTH_SCAN
            perms += Manifest.permission.BLUETOOTH_CONNECT
        }
        permLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(Unit) { vm.startTrackerIfPossible() }

    val toast by vm.toast.collectAsState()
    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.clearToast()
        }
    }

    val pendingInbound by vm.pendingInbound.collectAsState()
    val outboundPending by vm.outboundPending.collectAsState()
    val raceState by vm.engine.state.collectAsState()
    val linkState by vm.link.state.collectAsState()
    val discovered by vm.link.discovered.collectAsState()
    val peer by vm.link.peer.collectAsState()
    val lastConfig by vm.lastConfig.collectAsState()

    // Auto-jump to race screen as soon as the engine is armed
    LaunchedEffect(raceState.phase) {
        if (raceState.phase != RaceEngine.Phase.IDLE && screen != Screen.RACE) {
            screen = Screen.RACE
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (screen) {
            Screen.TUTORIAL -> TutorialScreen(
                onFinish = {
                    prefs.tutorialSeen = true
                    screen = Screen.HOME
                },
            )
            Screen.HOME -> HomeScreen(
                btReady = hasBt,
                locReady = hasLoc,
                onPair = { screen = Screen.PAIRING },
                onRequestPermissions = askPerms,
                onShowTutorial = { screen = Screen.TUTORIAL },
            )
            Screen.PAIRING -> PairingScreen(
                state = linkState,
                bonded = remember(hasBt) { vm.link.bondedDevices() },
                discovered = discovered,
                peer = peer,
                onScan = { vm.link.startDiscovery() },
                onStopScan = { vm.link.stopDiscovery() },
                onHost = { vm.link.host() },
                onConnect = { vm.link.connect(it.address) },
                onDisconnect = { vm.link.disconnect() },
                onContinue = { screen = Screen.CONFIG },
                onBack = { screen = Screen.HOME },
            )
            Screen.CONFIG -> RaceConfigScreen(
                initial = lastConfig,
                pendingResponse = outboundPending,
                onSend = { vm.sendRaceRequest(it) },
                onCancel = { vm.cancelOutbound() },
                onBack = { screen = Screen.PAIRING },
            )
            Screen.RACE -> RaceScreen(
                state = raceState,
                onReady = { vm.engine.setReady(it) },
                onAbort = { vm.engine.abort("user aborted") },
                onDone = {
                    vm.resetRace()
                    screen = Screen.PAIRING
                },
            )
        }

        pendingInbound?.let { cfg ->
            InboundRequestDialog(
                config = cfg,
                onAccept = { vm.acceptInbound() },
                onDecline = { vm.declineInbound() },
            )
        }
    }
}
