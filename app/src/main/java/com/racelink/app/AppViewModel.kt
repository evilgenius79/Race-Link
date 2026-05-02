package com.racelink.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.racelink.app.bluetooth.BluetoothLink
import com.racelink.app.bluetooth.Message
import com.racelink.app.location.SpeedTracker
import com.racelink.app.race.ClockSync
import com.racelink.app.race.RaceConfig
import com.racelink.app.race.RaceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = AppPrefs(app)
    val link = BluetoothLink(app).also { it.selfNickname = prefs.nickname }
    val tracker = SpeedTracker(app)
    private val clockSync = ClockSync(link, viewModelScope)
    val engine = RaceEngine(link, tracker, clockSync, viewModelScope)

    private val _nickname = MutableStateFlow(prefs.nickname)
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    fun setNickname(name: String) {
        val trimmed = name.trim().take(20)
        prefs.nickname = trimmed
        link.selfNickname = trimmed
        _nickname.value = trimmed
    }

    /** Pending inbound race request (we are the guest), waiting for accept/deny. */
    private val _pendingInbound = MutableStateFlow<RaceConfig?>(null)
    val pendingInbound: StateFlow<RaceConfig?> = _pendingInbound.asStateFlow()

    /** Whether we sent a request and are waiting for the peer's response. */
    private val _outboundPending = MutableStateFlow(false)
    val outboundPending: StateFlow<Boolean> = _outboundPending.asStateFlow()

    /** Last config the user filled in - reused next time. */
    private val _lastConfig = MutableStateFlow(RaceConfig())
    val lastConfig: StateFlow<RaceConfig> = _lastConfig.asStateFlow()

    /** Live banner messages (errors, info). */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        // Inbound race requests
        link.incoming
            .filterIsInstance<Message.RaceRequest>()
            .onEach { _pendingInbound.value = it.config }
            .launchIn(viewModelScope)

        // Inbound race responses to our outbound request
        link.incoming
            .filterIsInstance<Message.RaceResponse>()
            .onEach { resp ->
                _outboundPending.value = false
                if (resp.accepted) {
                    tracker.start()
                    engine.start(isHost = true, config = _lastConfig.value)
                } else {
                    _toast.value = "Race declined"
                }
            }
            .launchIn(viewModelScope)

        link.errors
            .onEach { _toast.value = it }
            .launchIn(viewModelScope)
    }

    fun startTrackerIfPossible() {
        if (tracker.hasPermission()) tracker.start()
    }

    fun sendRaceRequest(config: RaceConfig) {
        _lastConfig.value = config
        _outboundPending.value = true
        link.send(Message.RaceRequest(config))
    }

    fun acceptInbound() {
        val cfg = _pendingInbound.value ?: return
        _pendingInbound.value = null
        link.send(Message.RaceResponse(true))
        tracker.start()
        engine.start(isHost = false, config = cfg)
    }

    fun declineInbound() {
        _pendingInbound.value = null
        link.send(Message.RaceResponse(false))
    }

    fun cancelOutbound() {
        _outboundPending.value = false
    }

    fun clearToast() { _toast.value = null }

    fun resetRace() {
        engine.reset()
        tracker.stop()
    }

    override fun onCleared() {
        super.onCleared()
        engine.reset()
        tracker.stop()
        link.shutdown()
    }
}
