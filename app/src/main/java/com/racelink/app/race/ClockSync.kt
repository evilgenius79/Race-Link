package com.racelink.app.race

import com.racelink.app.bluetooth.BluetoothLink
import com.racelink.app.bluetooth.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Tiny NTP-style clock sync over the BT link.
 *
 * Each ping/pong round measures the offset between local and peer wallclocks,
 * and the round-trip latency. We keep the best (lowest-RTT) sample of the
 * last few rounds. The race engine uses this to pick a future T0 that both
 * phones can fire on simultaneously.
 *
 *   t1 = local send time
 *   t2 = peer receive time
 *   t3 = peer send time (reply)
 *   t4 = local receive time
 *   offset = ((t2 - t1) + (t3 - t4)) / 2
 *   rtt    =  (t4 - t1) - (t3 - t2)
 */
class ClockSync(
    private val link: BluetoothLink,
    private val scope: CoroutineScope,
) {

    data class Sample(val offsetMs: Long, val rttMs: Long)

    private val _best = MutableStateFlow<Sample?>(null)
    val best: StateFlow<Sample?> = _best.asStateFlow()

    private var job: Job? = null
    private val recent = ArrayDeque<Sample>()

    init {
        // Server-side: respond to pings.
        link.incoming
            .filterIsInstance<Message.Ping>()
            .onEach { p ->
                val t2 = System.currentTimeMillis()
                val t3 = System.currentTimeMillis()
                link.send(Message.Pong(p.t1, t2, t3))
            }
            .launchIn(scope)

        // Client-side: handle pongs.
        link.incoming
            .filterIsInstance<Message.Pong>()
            .onEach { pong ->
                val t4 = System.currentTimeMillis()
                val rtt = (t4 - pong.t1) - (pong.t3 - pong.t2)
                val offset = ((pong.t2 - pong.t1) + (pong.t3 - t4)) / 2
                if (rtt >= 0) record(Sample(offset, rtt))
            }
            .launchIn(scope)
    }

    /** Start periodic ping rounds. Idempotent. */
    fun start(periodMs: Long = 1000L) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                link.send(Message.Ping(System.currentTimeMillis()))
                delay(periodMs)
            }
        }
    }

    fun stop() { job?.cancel(); job = null }

    private fun record(s: Sample) {
        recent.addLast(s)
        while (recent.size > 8) recent.removeFirst()
        // pick the lowest-RTT sample as our best estimate
        _best.value = recent.minByOrNull { it.rttMs }
    }

    /** Convert a peer wallclock time into the local wallclock equivalent. */
    fun peerToLocal(peerMillis: Long): Long {
        val off = _best.value?.offsetMs ?: 0L
        // peer = local + offset  =>  local = peer - offset
        return peerMillis - off
    }
}
