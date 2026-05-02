package com.racelink.app.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID

/**
 * High-level Bluetooth link.
 *
 * Both phones run the app, so either can be host or guest. The host opens a
 * listening RFCOMM socket on a fixed UUID; the guest discovers and connects.
 * Once a socket is established the side ceases to matter - both can send and
 * receive any [Message].
 */
class BluetoothLink(private val appContext: Context) {

    enum class ConnState { IDLE, LISTENING, DISCOVERING, CONNECTING, CONNECTED, ERROR }

    data class DiscoveredDevice(val name: String?, val address: String)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(ConnState.IDLE)
    val state: StateFlow<ConnState> = _state.asStateFlow()

    private val _discovered = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discovered: StateFlow<List<DiscoveredDevice>> = _discovered.asStateFlow()

    private val _peer = MutableStateFlow<DiscoveredDevice?>(null)
    val peer: StateFlow<DiscoveredDevice?> = _peer.asStateFlow()

    /** Peer-supplied nickname, set when we receive their Hello after connect. */
    private val _peerNickname = MutableStateFlow<String?>(null)
    val peerNickname: StateFlow<String?> = _peerNickname.asStateFlow()

    /** Our own nickname - sent in Hello as soon as a socket comes up. */
    var selfNickname: String = ""

    private val _incoming = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incoming: SharedFlow<Message> = _incoming.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var socket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var output: OutputStream? = null
    private var ioJob: Job? = null
    private var receiver: BroadcastReceiver? = null

    /**
     * Whether the most recent connect was the host or the guest, plus the
     * peer address (only meaningful when we were the guest). Used by
     * [tryReconnect] to put us back the way we were after a transient drop.
     */
    private var lastWasHost: Boolean = false
    private var lastConnectedAddress: String? = null

    /**
     * Messages that were sent while we had no active socket. These are
     * replayed once a new socket comes up - critical for the Finish
     * message, which the peer needs to see to complete the race.
     */
    private val pendingMessages = mutableListOf<Message>()

    private val adapter: BluetoothAdapter? by lazy {
        val mgr = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        mgr?.adapter
    }

    val isBluetoothOn: Boolean get() = adapter?.isEnabled == true

    fun hasConnectPerm(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasScanPerm(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Pre-31 needs LOCATION for discovery.
            return ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        return ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun bondedDevices(): List<DiscoveredDevice> {
        if (!hasConnectPerm()) return emptyList()
        return adapter?.bondedDevices.orEmpty().map {
            DiscoveredDevice(it.name, it.address)
        }
    }

    /**
     * Begin classic discovery. Results stream into [discovered]. Caller must
     * have acquired BLUETOOTH_SCAN (31+) or LOCATION (<=30).
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val a = adapter ?: return
        if (!hasScanPerm() || !hasConnectPerm()) {
            scope.launch { _errors.emit("Missing Bluetooth permission") }
            return
        }
        _discovered.value = emptyList()
        unregisterReceiverIfAny()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        @Suppress("DEPRECATION")
                        val dev: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        dev ?: return
                        val name = try { dev.name } catch (_: SecurityException) { null }
                        val item = DiscoveredDevice(name, dev.address)
                        _discovered.update { cur ->
                            if (cur.any { it.address == item.address }) cur else cur + item
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        if (_state.value == ConnState.DISCOVERING) _state.value = ConnState.IDLE
                    }
                }
            }
        }
        // BT discovery uses system-protected broadcasts so this receiver
        // doesn't need to be exported. Android 14+ throws SecurityException
        // unless the export state is declared explicitly.
        ContextCompat.registerReceiver(
            appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (a.isDiscovering) a.cancelDiscovery()
        _state.value = ConnState.DISCOVERING
        a.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (hasConnectPerm()) adapter?.cancelDiscovery()
        unregisterReceiverIfAny()
        if (_state.value == ConnState.DISCOVERING) _state.value = ConnState.IDLE
    }

    private fun unregisterReceiverIfAny() {
        receiver?.let {
            try { appContext.unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
        }
        receiver = null
    }

    /** Listen for an inbound RFCOMM connection (host role). */
    @SuppressLint("MissingPermission")
    fun host() {
        val a = adapter ?: return
        if (!hasConnectPerm()) {
            scope.launch { _errors.emit("Missing BLUETOOTH_CONNECT") }
            return
        }
        closeSocket()
        lastWasHost = true
        _state.value = ConnState.LISTENING
        scope.launch {
            try {
                val ss = a.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                serverSocket = ss
                val s = ss.accept()
                runCatching { ss.close() }
                serverSocket = null
                handleSocket(s)
            } catch (e: IOException) {
                Log.w(TAG, "host accept failed", e)
                _state.value = ConnState.ERROR
                _errors.emit("Host failed: ${e.message ?: "io error"}")
            }
        }
    }

    /** Connect outbound to a discovered/bonded device (guest role). */
    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val a = adapter ?: return
        if (!hasConnectPerm()) {
            scope.launch { _errors.emit("Missing BLUETOOTH_CONNECT") }
            return
        }
        closeSocket()
        lastWasHost = false
        lastConnectedAddress = address
        _state.value = ConnState.CONNECTING
        scope.launch {
            try {
                if (a.isDiscovering) a.cancelDiscovery()
                val device = a.getRemoteDevice(address)
                val s = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                s.connect()
                handleSocket(s)
            } catch (e: IOException) {
                Log.w(TAG, "connect failed", e)
                _state.value = ConnState.ERROR
                _errors.emit("Connect failed: ${e.message ?: "io error"}")
            }
        }
    }

    /**
     * Re-establish the link after a transient drop. Returns false if we have
     * no record of how we were connected (e.g. before the user has ever
     * paired). Caller is expected to retry with backoff if needed.
     */
    fun tryReconnect(): Boolean {
        if (lastWasHost) {
            host(); return true
        }
        val addr = lastConnectedAddress ?: return false
        connect(addr); return true
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleSocket(s: BluetoothSocket) {
        socket = s
        output = s.outputStream
        _state.value = ConnState.CONNECTED
        val remote = s.remoteDevice
        val name = try { remote.name } catch (_: SecurityException) { null }
        _peer.value = DiscoveredDevice(name, remote.address)
        _peerNickname.value = null
        // Track address so a reconnect can target the same peer even if we
        // were the host the first time (peer initiated connect).
        lastConnectedAddress = remote.address
        ioJob = scope.launch { readLoop(s) }
        // Greet the peer with our nickname so they can show it in the UI.
        send(Message.Hello(selfNickname.ifBlank { "Driver" }, APP_VERSION))
        // Replay anything that was queued while we were disconnected
        // (e.g. a Finish message that completed the race during an outage).
        val toReplay = synchronized(pendingMessages) {
            val copy = pendingMessages.toList()
            pendingMessages.clear()
            copy
        }
        toReplay.forEach { send(it) }
    }

    private suspend fun readLoop(s: BluetoothSocket) {
        try {
            val reader = BufferedReader(InputStreamReader(s.inputStream, Charsets.UTF_8))
            val sb = StringBuilder()
            while (true) {
                // Hand-rolled line read so we can enforce a hard upper bound.
                // BufferedReader.readLine() will happily allocate gigabytes if
                // the peer never sends a newline, which is a trivial DoS.
                sb.setLength(0)
                while (true) {
                    val ch = reader.read()
                    if (ch == -1) {
                        if (sb.isEmpty()) return
                        break
                    }
                    if (ch == '\n'.code) break
                    if (ch == '\r'.code) continue
                    sb.append(ch.toChar())
                    if (sb.length > MAX_LINE_BYTES) {
                        // Drop the connection rather than emit a partial line
                        // or trust a peer that's behaving badly.
                        Log.w(TAG, "peer sent oversized line, disconnecting")
                        return
                    }
                }
                val msg = Message.decode(sb.toString()) ?: continue
                if (msg is Message.Hello) {
                    _peerNickname.value = msg.name.ifBlank { null }
                }
                _incoming.emit(msg)
            }
        } catch (e: IOException) {
            Log.i(TAG, "read loop ended")
        } finally {
            disconnect()
        }
    }

    fun send(msg: Message) {
        val out = output
        if (out == null) {
            // Queue messages that the peer truly needs to see eventually -
            // dropping a Finish would orphan a race result. Speed updates
            // and pings are time-sensitive and pointless to replay later.
            if (msg.isDurable()) synchronized(pendingMessages) { pendingMessages.add(msg) }
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    out.write(msg.encode().toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            } catch (e: IOException) {
                _errors.emit("Send failed: ${e.message ?: "io"}")
                if (msg.isDurable()) synchronized(pendingMessages) { pendingMessages.add(msg) }
                disconnect()
            }
        }
    }

    private fun Message.isDurable(): Boolean = when (this) {
        is Message.Finish, is Message.RaceResponse, is Message.Abort -> true
        else -> false
    }

    fun disconnect() {
        closeSocket()
        if (_state.value != ConnState.IDLE) _state.value = ConnState.IDLE
        _peer.value = null
    }

    private fun closeSocket() {
        ioJob?.cancel(); ioJob = null
        runCatching { output?.close() }; output = null
        runCatching { socket?.close() }; socket = null
        runCatching { serverSocket?.close() }; serverSocket = null
    }

    fun shutdown() {
        stopDiscovery()
        disconnect()
        scope.cancel()
    }

    companion object {
        private const val TAG = "BluetoothLink"
        private const val APP_VERSION = "0.2.0"
        /** Hard cap on a single line of JSON. Real messages are a few hundred bytes. */
        private const val MAX_LINE_BYTES = 4096
        // SPP-style UUID, but app-specific so we don't collide with audio devices.
        // If you fork the app and want to talk only to your own builds, change this.
        val SERVICE_UUID: UUID = UUID.fromString("8b6e3c8a-1f44-4f5c-9c5c-1e2bd2e3ab51")
        const val SERVICE_NAME = "RaceLink"
    }
}
