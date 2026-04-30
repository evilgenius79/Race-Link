package com.racelink.app.race

enum class StartType { STANDING, ROLLING }

/**
 * Settings the user picks before starting a race. Synchronized between
 * peers so both phones use the exact same rules.
 *
 * - rollSpeedMph: target speed for rolling start (ignored when STANDING)
 * - speedToleranceMph: how close the two cars must be before the tree fires
 * - distanceFt: race length (1320 = 1/4 mile, 660 = 1/8 mile, etc.)
 */
data class RaceConfig(
    val startType: StartType = StartType.ROLLING,
    val rollSpeedMph: Int = 40,
    val speedToleranceMph: Int = 3,
    val distanceFt: Int = 1320,
) {
    companion object {
        const val QUARTER_MILE_FT = 1320
        const val EIGHTH_MILE_FT = 660
    }
}
