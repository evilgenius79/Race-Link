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
import com.racelink.app.race.RaceHistoryStore
import com.racelink.app.race.RaceResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = AppPrefs(app)
    val link = BluetoothLink(app).also { it.selfNickname = prefs.nickname }
    val tracker = SpeedTracker(app)
    private val clockSync = ClockSync(link, viewModelScope)
    val engine = RaceEngine(link, tracker, clockSync, viewModelScope)
    val history = RaceHistoryStore(app)

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
                    onRaceStarting()
                    engine.start(isHost = true, config = _lastConfig.value)
                } else {
                    _toast.value = "Race declined"
                }
            }
            .launchIn(viewModelScope)

        link.errors
            .onEach { _toast.value = it }
            .launchIn(viewModelScope)

        // Service lifecycle: keep the foreground service alive while a race
        // is being arranged or in progress; stop it once the engine is idle.
        // Android 14+ throws SecurityException if you start a location-typed
        // FGS without ACCESS_FINE_LOCATION granted, so gate the start.
        engine.state
            .distinctUntilChangedBy { it.phase }
            .onEach { s ->
                val active = s.phase in ACTIVE_PHASES
                if (active && tracker.hasPermission()) {
                    RaceForegroundService.start(app)
                } else {
                    RaceForegroundService.stop(app)
                }
            }
            .launchIn(viewModelScope)

        // Save a result the first time the engine reports both finish times.
        engine.state
            .distinctUntilChangedBy { it.selfFinishMs to it.peerFinishMs }
            .onEach { s ->
                if (s.phase == RaceEngine.Phase.FINISHED && s.selfFinishMs != null && s.peerFinishMs != null) {
                    persistResult(s)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onRaceStarting() {
        if (tracker.hasPermission()) tracker.start()
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
        onRaceStarting()
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

    private val _history = MutableStateFlow<List<RaceResult>>(emptyList())
    val historyResults: StateFlow<List<RaceResult>> = _history.asStateFlow()

    fun refreshHistory() {
        viewModelScope.launch { _history.value = history.loadAll() }
    }

    fun clearHistory() {
        viewModelScope.launch {
            history.clear()
            _history.value = emptyList()
        }
    }

    private fun persistResult(s: RaceEngine.State) {
        val selfMs = s.selfFinishMs ?: return
        val peerMs = s.peerFinishMs ?: return
        // Defensive dedupe in case the flow re-emits.
        if (lastSavedFinish == selfMs) return
        lastSavedFinish = selfMs
        viewModelScope.launch {
            history.save(
                RaceResult(
                    timestampMs = System.currentTimeMillis(),
                    peerName = link.peerNickname.value,
                    startType = s.config.startType,
                    rollSpeedMph = s.config.rollSpeedMph,
                    distanceFt = s.config.distanceFt,
                    selfEtMs = selfMs,
                    selfMph = s.selfFinalMph,
                    peerEtMs = peerMs,
                    peerMph = s.peerFinalMph,
                    reactionMs = s.reactionMs,
                    won = selfMs < peerMs,
                    splits = s.splits,
                )
            )
        }
    }
    private var lastSavedFinish: Long? = null

    override fun onCleared() {
        super.onCleared()
        engine.reset()
        tracker.stop()
        link.shutdown()
        RaceForegroundService.stop(getApplication())
    }

    private companion object {
        val ACTIVE_PHASES = setOf(
            RaceEngine.Phase.ARMED,
            RaceEngine.Phase.STAGED,
            RaceEngine.Phase.TREE,
            RaceEngine.Phase.RUNNING,
        )
    }
}
