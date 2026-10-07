package com.networkar.app.network

object SignalUtils {

    /**
     * Converts a dBm signal value into an approximate
     * 0-100 percentage.
     *
     * -100 dBm or lower = 0%
     * -60 dBm or higher = 100%
     */
    fun percent(dbm: Int): Int {
        if (dbm <= -100) return 0
        if (dbm >= -60) return 100

        return ((dbm + 100) * 100 / 40)
            .coerceIn(0, 100)
    }

    /**
     * Converts dBm into a simple signal quality.
     */
    fun quality(dbm: Int): String {
        return when {
            dbm >= -60 -> "STRONG"
            dbm >= -75 -> "MEDIUM"
            else -> "WEAK"
        }
    }

    /**
     * User-friendly signal description.
     */
    fun description(dbm: Int): String {
        return when {
            dbm >= -60 -> "Excellent"
            dbm >= -67 -> "Very Good"
            dbm >= -75 -> "Good"
            dbm >= -85 -> "Fair"
            dbm >= -95 -> "Poor"
            else -> "Very Poor"
        }
    }

    /**
     * Checks whether a dBm value looks like a valid
     * cellular/Wi-Fi signal value.
     */
    fun isValid(dbm: Int): Boolean {
        return dbm in -120..-1
    }

    /**
     * Formats a dBm value for display.
     */
    fun formatDbm(dbm: Int?): String {
        return if (dbm == null || !isValid(dbm)) {
            "Unavailable"
        } else {
            "$dbm dBm"
        }
    }

    /**
     * Formats signal information for UI.
     */
    fun summary(dbm: Int?): String {
        if (dbm == null || !isValid(dbm)) {
            return "Signal unavailable"
        }

        return "$dbm dBm • ${quality(dbm)}"
    }
}
