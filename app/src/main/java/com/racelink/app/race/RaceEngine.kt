package com.racelink.app.race

import android.location.Location
import com.racelink.app.bluetooth.BluetoothLink
import com.racelink.app.bluetooth.Message
import com.racelink.app.location.SpeedTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/**
 * Drives the lifecycle of a single race.
 *
 * Phases:
 *   IDLE -> ARMED (both peers ready) -> WAITING_FOR_SYNC (rolling only) ->
 *   STAGED -> TREE -> RUNNING -> FINISHED (or ABORTED)
 *
 * Only the host is authoritative on phase transitions. The host:
 *   - watches both speeds during rolling start
 *   - picks a random tree-arming delay
 *   - publishes the absolute tree-fire wallclock to the guest
 *   - everyone runs the same countdown locally using clock-sync
 *
 * Both phones independently detect their own finish (distance integrated
 * from GPS samples once the green fires) and broadcast their finish time.
 */
class RaceEngine(
    private val link: BluetoothLink,
    private val tracker: SpeedTracker,
    private val clockSync: ClockSync,
    private val scope: CoroutineScope,
) {

    enum class Phase {
        IDLE,
        ARMED,             // both ready, host evaluating start conditions
        STAGED,            // start confirmed, tree about to fire
        TREE,              // ambers counting down to green
        RUNNING,           // green has fired; integrating distance
        FINISHED,
        ABORTED,
    }

    enum class TreeLight { OFF, AMBER1, AMBER2, AMBER3, GREEN, RED }

    data class State(
        val phase: Phase = Phase.IDLE,
        val isHost: Boolean = false,
        val config: RaceConfig = RaceConfig(),
        val selfReady: Boolean = false,
        val peerReady: Boolean = false,
        val selfSpeedMph: Float = 0f,
        val peerSpeedMph: Float = 0f,
        val light: TreeLight = TreeLight.OFF,
        /** Wallclock at which the green light fires (local clock). */
        val greenAtMillis: Long = 0L,
        /** Distance covered by self (ft) since green fired. */
        val selfDistanceFt: Float = 0f,
        val selfElapsedMs: Long = 0L,
        val selfFinishMs: Long? = null,
        val selfFinalMph: Float = 0f,
        val peerFinishMs: Long? = null,
        val peerFinalMph: Float = 0f,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var jobs: List<Job> = emptyList()
    private var distanceJob: Job? = null
    private var lastLoc: Location? = null

    fun start(isHost: Boolean, config: RaceConfig) {
        cancelJobs()
        _state.value = State(isHost = isHost, config = config, phase = Phase.ARMED)

        // Begin clock sync immediately - the rolling-start watcher and the
        // tree-fire scheduler both need a stable peer offset.
        clockSync.start()

        val j1 = link.incoming
            .filterIsInstance<Message.Ready>()
            .onEach { onPeerReady(it.ready) }
            .launchIn(scope)

        val j2 = link.incoming
            .filterIsInstance<Message.SpeedUpdate>()
            .onEach { _state.update { s -> s.copy(peerSpeedMph = it.speedMph) } }
            .launchIn(scope)

        val j3 = link.incoming
            .filterIsInstance<Message.StartTree>()
            .onEach { onStartTree(it.startAtEpochMillis) }
            .launchIn(scope)

        val j4 = link.incoming
            .filterIsInstance<Message.Finish>()
            .onEach { onPeerFinish(it.elapsedMillis, it.finalMph) }
            .launchIn(scope)

        val j5 = link.incoming
            .filterIsInstance<Message.Abort>()
            .onEach { abort("Peer aborted: ${it.reason}", broadcast = false) }
            .launchIn(scope)

        // Stream our own speed up to peer ~5x/sec
        val j6 = scope.launch {
            while (isActive) {
                val mph = tracker.speedMph.value
                _state.update { it.copy(selfSpeedMph = mph) }
                if (link.state.value == BluetoothLink.ConnState.CONNECTED) {
                    link.send(Message.SpeedUpdate(mph, System.currentTimeMillis()))
                }
                delay(200)
            }
        }

        // Host watches for both-ready + speed window, then schedules tree
        val j7 = if (isHost) {
            scope.launch { hostWatcher() }
        } else null

        jobs = listOfNotNull(j1, j2, j3, j4, j5, j6, j7)
    }

    /** User toggles their own ready state. */
    fun setReady(ready: Boolean) {
        _state.update { it.copy(selfReady = ready) }
        link.send(Message.Ready(ready))
    }

    private fun onPeerReady(ready: Boolean) {
        _state.update { it.copy(peerReady = ready) }
    }

    private suspend fun hostWatcher() {
        // Wait for both ready
        while (true) {
            val s = _state.value
            if (s.phase != Phase.ARMED) return
            if (s.selfReady && s.peerReady) break
            delay(100)
        }

        val cfg = _state.value.config
        when (cfg.startType) {
            StartType.STANDING -> {
                // Wait until both are essentially stopped
                while (true) {
                    val s = _state.value
                    if (s.phase != Phase.ARMED) return
                    if (s.selfSpeedMph < 2f && s.peerSpeedMph < 2f) break
                    delay(100)
                }
            }
            StartType.ROLLING -> {
                // Both must be near roll speed and close to each other
                val target = cfg.rollSpeedMph.toFloat()
                val tol = cfg.speedToleranceMph.toFloat()
                while (true) {
                    val s = _state.value
                    if (s.phase != Phase.ARMED) return
                    val a = s.selfSpeedMph
                    val b = s.peerSpeedMph
                    val bothNearTarget = abs(a - target) <= tol && abs(b - target) <= tol
                    val closeToEachOther = abs(a - b) <= tol
                    if (bothNearTarget && closeToEachOther) break
                    delay(100)
                }
            }
        }

        // Wait briefly for clock-sync to converge before committing to a tree time.
        var waited = 0
        while (clockSync.best.value == null && waited < 2000) {
            delay(100)
            waited += 100
        }

        // Stage. Pick a small random delay so neither driver can time it.
        _state.update { it.copy(phase = Phase.STAGED) }
        val delayMs = 1500L + Random.nextLong(0, 1500)
        val greenAt = System.currentTimeMillis() + delayMs
        // Tell peer to schedule the same green time, in *peer* clock terms.
        val peerGreenAt = greenAt + (clockSync.best.value?.offsetMs ?: 0L)
        link.send(Message.StartTree(peerGreenAt))
        runTreeAndRace(greenAt)
    }

    private fun onStartTree(peerGreenAt: Long) {
        if (_state.value.isHost) return
        val localGreenAt = clockSync.peerToLocal(peerGreenAt)
        _state.update { it.copy(phase = Phase.STAGED) }
        scope.launch { runTreeAndRace(localGreenAt) }
    }

    /**
     * Runs the amber-amber-amber-green sequence so green fires exactly at
     * [greenAtLocalMillis], then begins distance integration.
     */
    private suspend fun runTreeAndRace(greenAtLocalMillis: Long) {
        _state.update { it.copy(phase = Phase.TREE, greenAtMillis = greenAtLocalMillis) }

        // 0.5s spacing for sportsman tree -> 3 ambers then green
        val schedule = listOf(
            greenAtLocalMillis - 1500 to TreeLight.AMBER1,
            greenAtLocalMillis - 1000 to TreeLight.AMBER2,
            greenAtLocalMillis - 500  to TreeLight.AMBER3,
            greenAtLocalMillis        to TreeLight.GREEN,
        )
        for ((at, light) in schedule) {
            val wait = at - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            _state.update { it.copy(light = light) }
        }
        _state.update { it.copy(phase = Phase.RUNNING) }
        startDistanceIntegration(greenAtLocalMillis)
    }

    /**
     * Integrate position deltas from GPS samples to estimate distance covered.
     * We use distanceTo() between successive locations rather than naive
     * integration of speed - it survives short GPS outages and is what real
     * timing apps use.
     */
    private fun startDistanceIntegration(greenAtLocalMillis: Long) {
        distanceJob?.cancel()
        lastLoc = null
        distanceJob = scope.launch {
            val cfg = _state.value.config
            val targetFt = cfg.distanceFt.toFloat()
            // Subscribe to live samples
            tracker.sample
                .onEach { sample ->
                    sample ?: return@onEach
                    val loc = sample.location
                    val prev = lastLoc
                    if (prev != null) {
                        val deltaMeters = prev.distanceTo(loc)
                        val deltaFt = deltaMeters * METERS_TO_FT
                        val newDist = _state.value.selfDistanceFt + deltaFt
                        val elapsed = System.currentTimeMillis() - greenAtLocalMillis
                        _state.update { it.copy(selfDistanceFt = newDist, selfElapsedMs = elapsed) }
                        if (newDist >= targetFt && _state.value.selfFinishMs == null) {
                            // Linearly interpolate finish time across this segment
                            val prevDist = newDist - deltaFt
                            val frac = ((targetFt - prevDist) / deltaFt).coerceIn(0f, 1f)
                            val segMs = sample.elapsedRealtimeMillis -
                                (lastSampleElapsed ?: sample.elapsedRealtimeMillis)
                            val finishWall = sample.wallClockMillis - ((1f - frac) * segMs).toLong()
                            val finishElapsed = finishWall - greenAtLocalMillis
                            val mph = if (loc.hasSpeed()) loc.speed * SpeedTracker.MS_TO_MPH else _state.value.selfSpeedMph
                            onSelfFinish(finishElapsed, mph)
                        }
                    }
                    lastSampleElapsed = sample.elapsedRealtimeMillis
                    lastLoc = loc
                }
                .launchIn(this)
        }
    }
    private var lastSampleElapsed: Long? = null

    private fun onSelfFinish(elapsedMs: Long, mph: Float) {
        _state.update { it.copy(selfFinishMs = elapsedMs, selfFinalMph = mph) }
        link.send(Message.Finish(elapsedMs, mph))
        maybeFinish()
    }

    private fun onPeerFinish(elapsedMs: Long, mph: Float) {
        _state.update { it.copy(peerFinishMs = elapsedMs, peerFinalMph = mph) }
        maybeFinish()
    }

    private fun maybeFinish() {
        val s = _state.value
        if (s.selfFinishMs != null && s.peerFinishMs != null && s.phase != Phase.FINISHED) {
            _state.update { it.copy(phase = Phase.FINISHED) }
            distanceJob?.cancel()
        }
    }

    fun abort(reason: String, broadcast: Boolean = true) {
        if (broadcast) link.send(Message.Abort(reason))
        cancelJobs()
        clockSync.stop()
        _state.update { it.copy(phase = Phase.ABORTED, message = reason) }
    }

    fun reset() {
        cancelJobs()
        clockSync.stop()
        _state.value = State()
    }

    private fun cancelJobs() {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        distanceJob?.cancel()
        distanceJob = null
        lastLoc = null
        lastSampleElapsed = null
    }

    companion object {
        const val METERS_TO_FT = 3.28084f
    }
}
