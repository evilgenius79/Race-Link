package com.racelink.app.bluetooth

import com.racelink.app.race.RaceConfig
import com.racelink.app.race.StartType
import org.json.JSONObject

/**
 * Wire protocol: each message is a single line of JSON terminated by '\n'.
 * Field "t" identifies the message type. Remaining fields depend on the type.
 *
 * The protocol is intentionally tiny so it stays readable on either side of
 * the link - decoding is hand-rolled below.
 */
sealed class Message {
    abstract fun toJson(): JSONObject

    fun encode(): String = toJson().toString() + "\n"

    /** First message after connect. Lets the peer know who we are. */
    data class Hello(val name: String, val appVersion: String) : Message() {
        override fun toJson() = JSONObject()
            .put("t", "hello")
            .put("name", name)
            .put("v", appVersion)
    }

    /** A peer requests a race with the given config. Receiver may accept/deny. */
    data class RaceRequest(val config: RaceConfig) : Message() {
        override fun toJson() = JSONObject()
            .put("t", "req")
            .put("startType", config.startType.name)
            .put("rollSpeedMph", config.rollSpeedMph)
            .put("speedToleranceMph", config.speedToleranceMph)
            .put("distanceFt", config.distanceFt)
    }

    data class RaceResponse(val accepted: Boolean) : Message() {
        override fun toJson() = JSONObject().put("t", "resp").put("ok", accepted)
    }

    /** Periodic clock-sync ping. t1 = sender wallclock at send. */
    data class Ping(val t1: Long) : Message() {
        override fun toJson() = JSONObject().put("t", "ping").put("t1", t1)
    }

    /**
     * Reply to a ping. Includes the original t1 so the requester can compute
     * round-trip and clock offset (t2/t3 are the responder's receive/send times).
     */
    data class Pong(val t1: Long, val t2: Long, val t3: Long) : Message() {
        override fun toJson() = JSONObject()
            .put("t", "pong")
            .put("t1", t1).put("t2", t2).put("t3", t3)
    }

    /** Live speed update. speedMph in mph; ts is sender wallclock. */
    data class SpeedUpdate(val speedMph: Float, val ts: Long) : Message() {
        override fun toJson() = JSONObject()
            .put("t", "spd").put("mph", speedMph.toDouble()).put("ts", ts)
    }

    /** "I'm armed and ready". */
    data class Ready(val ready: Boolean) : Message() {
        override fun toJson() = JSONObject().put("t", "ready").put("ok", ready)
    }

    /**
     * Authoritative tree-start message from the host. startAtEpochMillis is
     * the wallclock time (host clock) at which the green light should fire.
     * Both sides count from there using their synced offset.
     */
    data class StartTree(val startAtEpochMillis: Long) : Message() {
        override fun toJson() = JSONObject()
            .put("t", "start").put("at", startAtEpochMillis)
    }

    /** Sender finished. elapsedMillis from green-light fire. */
    data class Finish(val elapsedMillis: Long, val finalMph: Float) : Message() {
        override fun toJson() = JSONObject()
            .put("t", "fin").put("ms", elapsedMillis).put("mph", finalMph.toDouble())
    }

    /** Sender bailed (false start, lost signal, manual abort). */
    data class Abort(val reason: String) : Message() {
        override fun toJson() = JSONObject().put("t", "abort").put("r", reason)
    }

    companion object {
        fun decode(line: String): Message? {
            val o = runCatching { JSONObject(line) }.getOrNull() ?: return null
            return when (o.optString("t")) {
                "hello" -> Hello(
                    // Cap nickname length so a malicious peer can't push huge
                    // strings into our UI / persisted history.
                    o.optString("name").take(32),
                    o.optString("v").take(32),
                )
                "req" -> RaceRequest(
                    RaceConfig(
                        // Wrap in runCatching so a peer sending an unknown
                        // start type can't crash our read loop.
                        startType = runCatching {
                            StartType.valueOf(o.optString("startType", "ROLLING"))
                        }.getOrDefault(StartType.ROLLING),
                        rollSpeedMph = o.optInt("rollSpeedMph", 40).coerceIn(10, 200),
                        speedToleranceMph = o.optInt("speedToleranceMph", 3).coerceIn(1, 20),
                        distanceFt = o.optInt("distanceFt", 1320).coerceIn(100, 5280),
                    )
                )
                "resp" -> RaceResponse(o.optBoolean("ok"))
                "ping" -> Ping(o.optLong("t1"))
                "pong" -> Pong(o.optLong("t1"), o.optLong("t2"), o.optLong("t3"))
                "spd" -> SpeedUpdate(o.optDouble("mph").toFloat(), o.optLong("ts"))
                "ready" -> Ready(o.optBoolean("ok"))
                "start" -> StartTree(o.optLong("at"))
                "fin" -> Finish(o.optLong("ms"), o.optDouble("mph").toFloat())
                "abort" -> Abort(o.optString("r"))
                else -> null
            }
        }
    }
}
