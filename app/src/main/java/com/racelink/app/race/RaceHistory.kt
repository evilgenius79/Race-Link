package com.racelink.app.race

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Single completed race, ready to persist.
 *
 * Persistence is a JSON-lines file in the app's filesDir - simple, no
 * dependencies, easy to inspect/back-up. If we outgrow it (thousands of
 * races, queries by best ET, etc.) we can swap in Room later without any
 * schema migration headaches.
 */
data class RaceResult(
    val timestampMs: Long,
    val peerName: String?,
    val startType: StartType,
    val rollSpeedMph: Int,
    val distanceFt: Int,
    val selfEtMs: Long,
    val selfMph: Float,
    val peerEtMs: Long,
    val peerMph: Float,
    val reactionMs: Long?,
    val won: Boolean,
    val splits: Map<Int, Long>,
) {
    fun toJson(): JSONObject {
        val splitsArr = JSONArray()
        splits.entries.sortedBy { it.key }.forEach { (ft, ms) ->
            splitsArr.put(JSONObject().put("ft", ft).put("ms", ms))
        }
        return JSONObject()
            .put("ts", timestampMs)
            .put("peer", peerName ?: JSONObject.NULL)
            .put("startType", startType.name)
            .put("roll", rollSpeedMph)
            .put("dist", distanceFt)
            .put("selfEt", selfEtMs)
            .put("selfMph", selfMph.toDouble())
            .put("peerEt", peerEtMs)
            .put("peerMph", peerMph.toDouble())
            .put("rt", reactionMs ?: JSONObject.NULL)
            .put("won", won)
            .put("splits", splitsArr)
    }

    companion object {
        fun fromJson(line: String): RaceResult? = runCatching {
            val o = JSONObject(line)
            val splits = mutableMapOf<Int, Long>()
            val arr = o.optJSONArray("splits")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    splits[s.getInt("ft")] = s.getLong("ms")
                }
            }
            RaceResult(
                timestampMs = o.optLong("ts"),
                peerName = o.optString("peer").takeIf { it.isNotBlank() && it != "null" },
                startType = runCatching { StartType.valueOf(o.optString("startType")) }
                    .getOrDefault(StartType.ROLLING),
                rollSpeedMph = o.optInt("roll"),
                distanceFt = o.optInt("dist"),
                selfEtMs = o.optLong("selfEt"),
                selfMph = o.optDouble("selfMph").toFloat(),
                peerEtMs = o.optLong("peerEt"),
                peerMph = o.optDouble("peerMph").toFloat(),
                reactionMs = o.opt("rt").let { if (it == null || it == JSONObject.NULL) null else (it as Number).toLong() },
                won = o.optBoolean("won"),
                splits = splits,
            )
        }.getOrNull()
    }
}

/**
 * All disk I/O is forced onto the IO dispatcher; callers should use these
 * suspend forms rather than blocking. The class itself is internally
 * synchronized so concurrent writers are safe.
 */
class RaceHistoryStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val lock = Any()

    suspend fun save(result: RaceResult): Unit = withContext(Dispatchers.IO) {
        synchronized(lock) {
            file.appendText(result.toJson().toString() + "\n")
        }
    }

    suspend fun loadAll(): List<RaceResult> = withContext(Dispatchers.IO) {
        synchronized(lock) {
            if (!file.exists()) emptyList()
            else file.readLines()
                .mapNotNull { RaceResult.fromJson(it) }
                .sortedByDescending { it.timestampMs }
        }
    }

    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        synchronized(lock) {
            runCatching { file.delete() }
        }
    }

    private companion object {
        const val FILE_NAME = "race_history.jsonl"
    }
}
