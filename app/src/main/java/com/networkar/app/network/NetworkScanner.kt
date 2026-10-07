package com.networkar.app.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Represents a Wi-Fi network.
 */
data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int
)

/**
 * Represents the currently registered mobile network.
 */
data class MobileNetwork(
    val operator: String,
    val type: String,
    val signalDbm: Int?,
    val registered: Boolean
)

/**
 * Represents Internet connectivity.
 */
data class InternetInfo(
    val connected: Boolean,
    val validated: Boolean,
    val transport: String,
    val metered: Boolean
)

/**
 * Complete network status.
 */
data class NetworkStatus(
    val wifi: WifiNetwork?,
    val wifiNetworks: List<WifiNetwork>,
    val mobile: MobileNetwork?,
    val internet: InternetInfo
)

/**
 * Central network scanner for NetScope.
 *
 * Handles:
 *
 * - Current Wi-Fi
 * - Nearby Wi-Fi
 * - 2G / 3G / 4G / 5G
 * - Mobile signal
 * - Operator
 * - Internet connectivity
 */
class NetworkScanner(
    context: Context
) {

    private val appContext =
        context.applicationContext

    private val wifiManager: WifiManager by lazy {

        appContext.getSystemService(
            Context.WIFI_SERVICE
        ) as WifiManager
    }

    private val connectivityManager:
        ConnectivityManager by lazy {

        appContext.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
    }

    private val telephonyManager:
        TelephonyManager by lazy {

        appContext.getSystemService(
            Context.TELEPHONY_SERVICE
        ) as TelephonyManager
    }

    // ---------------------------------------------------------------------
    // CURRENT WI-FI
    // ---------------------------------------------------------------------

    /**
     * Returns the Wi-Fi connection currently used
     * by the device.
     */
    fun currentWifi(): WifiNetwork? {

        if (!hasWifiPermission()) {
            return null
        }

        return runCatching {

            val info =
                wifiManager.connectionInfo
                    ?: return@runCatching null

            val ssid =
                info.ssid
                    ?.trim('"')
                    ?.takeIf {
                        it.isNotBlank() &&
                            it != "<unknown ssid>"
                    }
                    ?: "Unknown Wi-Fi"

            val bssid =
                info.bssid
                    ?.takeIf {
                        it.isNotBlank() &&
                            it != "02:00:00:00:00:00"
                    }
                    ?: ""

            val rssi =
                info.rssi

            val frequency =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    info.frequency
                } else {
                    0
                }

            WifiNetwork(
                ssid = ssid,
                bssid = bssid,
                rssi = rssi,
                frequency = frequency
            )

        }.getOrNull()
    }

    // ---------------------------------------------------------------------
    // NEARBY WI-FI
    // ---------------------------------------------------------------------

    /**
     * Scans nearby Wi-Fi networks.
     *
     * Android may throttle scan requests.
     * Therefore scanResults can contain the most
     * recently available results.
     */
    fun scanWifi(): List<WifiNetwork> {

        if (!hasWifiPermission()) {
            return emptyList()
        }

        if (!wifiManager.isWifiEnabled) {
            return emptyList()
        }

        return runCatching {

            /*
             * Start a fresh scan when possible.
             *
             * startScan() is deprecated on newer Android
             * versions and may be throttled, so failure
             * here should not prevent reading existing
             * scan results.
             */
            runCatching {
                @Suppress("DEPRECATION")
                wifiManager.startScan()
            }

            @Suppress("DEPRECATION")
            wifiManager.scanResults
                .mapNotNull { result ->

                    val ssid =
                        result.SSID
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "Hidden network"

                    val bssid =
                        result.BSSID
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: return@mapNotNull null

                    WifiNetwork(
                        ssid = ssid,
                        bssid = bssid,
                        rssi = result.level,
                        frequency = result.frequency
                    )
                }
                .distinctBy {
                    it.bssid
                }
                .sortedByDescending {
                    it.rssi
                }

        }.getOrDefault(emptyList())
    }

    // ---------------------------------------------------------------------
    // CURRENT MOBILE NETWORK
    // ---------------------------------------------------------------------

    /**
     * Returns information about the currently registered
     * cellular network.
     *
     * This does NOT scan all cellular towers.
     */
    fun currentMobile(): MobileNetwork? {

        if (!hasPermission(
                Manifest.permission.READ_PHONE_STATE
            )
        ) {
            return null
        }

        return runCatching {

            val operator =
                runCatching {
                    telephonyManager.networkOperatorName
                }.getOrNull()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Unknown operator"

            val cells =
                runCatching {
                    telephonyManager.allCellInfo
                }.getOrNull()
                    ?: emptyList()

            /*
             * Prefer the registered cell.
             */
            val registeredCell =
                cells.firstOrNull {
                    it.isRegistered
                }

            /*
             * If Android doesn't provide allCellInfo,
             * use the network type reported by TelephonyManager.
             */
            val type =
                detectMobileType(
                    registeredCell
                )

            val signalDbm =
                extractSignalDbm(
                    registeredCell
                )

            MobileNetwork(
                operator = operator,
                type = type,
                signalDbm = signalDbm,
                registered = registeredCell != null
            )

        }.getOrNull()
    }

    // ---------------------------------------------------------------------
    // MOBILE TYPE
    // ---------------------------------------------------------------------

    /**
     * Detects the cellular technology.
     */
    private fun detectMobileType(
        cell: CellInfo?
    ): String {

        return when (cell) {

            is CellInfoNr -> {
                "5G NR"
            }

            is CellInfoLte -> {
                "4G LTE"
            }

            is CellInfoWcdma -> {
                "3G WCDMA"
            }

            is CellInfoGsm -> {
                "2G GSM"
            }

            else -> {
                getNetworkTypeFallback()
            }
        }
    }

    // ---------------------------------------------------------------------
    // MOBILE SIGNAL
    // ---------------------------------------------------------------------

    /**
     * Extracts signal dBm from the registered cell.
     */
    private fun extractSignalDbm(
        cell: CellInfo?
    ): Int? {

        return when (cell) {

            is CellInfoNr -> {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {
                    runCatching {
                        cell.cellSignalStrength.dbm
                    }.getOrNull()
                } else {
                    null
                }
            }

            is CellInfoLte -> {

                runCatching {
                    cell.cellSignalStrength.dbm
                }.getOrNull()
            }

            is CellInfoWcdma -> {

                runCatching {
                    cell.cellSignalStrength.dbm
                }.getOrNull()
            }

            is CellInfoGsm -> {

                runCatching {
                    cell.cellSignalStrength.dbm
                }.getOrNull()
            }

            else -> null
        }
    }

    // ---------------------------------------------------------------------
    // NETWORK TYPE FALLBACK
    // ---------------------------------------------------------------------

    /**
     * Fallback cellular technology detection.
     */
    private fun getNetworkTypeFallback(): String {

        return runCatching {

            when (telephonyManager.dataNetworkType) {

                TelephonyManager.NETWORK_TYPE_NR ->
                    "5G NR"

                TelephonyManager.NETWORK_TYPE_LTE ->
                    "4G LTE"

                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS ->
                    "3G"

                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_GSM ->
                    "2G"

                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_EVDO_B ->
                    "CDMA"

                TelephonyManager.NETWORK_TYPE_IWLAN ->
                    "Wi-Fi Calling"

                TelephonyManager.NETWORK_TYPE_UNKNOWN ->
                    "Unknown"

                else ->
                    "Mobile"
            }

        }.getOrDefault("Mobile")
    }

    // ---------------------------------------------------------------------
    // INTERNET STATUS
    // ---------------------------------------------------------------------

    /**
     * Returns the current active network and Internet
     * validation state.
     */
    fun internetStatus(): InternetInfo {

        val network =
            connectivityManager.activeNetwork

        if (network == null) {

            return InternetInfo(
                connected = false,
                validated = false,
                transport = "NONE",
                metered = false
            )
        }

        val capabilities =
            connectivityManager
                .getNetworkCapabilities(network)

        if (capabilities == null) {

            return InternetInfo(
                connected = false,
                validated = false,
                transport = "UNKNOWN",
                metered = false
            )
        }

        val transport =
            getTransportName(
                capabilities
            )

        val hasInternet =
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )

        val validated =
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )

        val metered =
            !capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_METERED
            )

        return InternetInfo(
            connected = hasInternet,
            validated = validated,
            transport = transport,
            metered = metered
        )
    }

    // ---------------------------------------------------------------------
    // TRANSPORT
    // ---------------------------------------------------------------------

    /**
     * Converts Android network transports into
     * user-friendly names.
     */
    private fun getTransportName(
        capabilities: NetworkCapabilities
    ): String {

        return when {

            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) -> {
                "Wi-Fi"
            }

            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR
            ) -> {
                "Mobile Data"
            }

            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_ETHERNET
            ) -> {
                "Ethernet"
            }

            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_VPN
            ) -> {
                "VPN"
            }

            capabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_BLUETOOTH
            ) -> {
                "Bluetooth"
            }

            else -> {
                "Other"
            }
        }
    }

    // ---------------------------------------------------------------------
    // COMPLETE STATUS
    // ---------------------------------------------------------------------

    /**
     * Returns complete network information.
     *
     * Note:
     * This method intentionally does NOT start a Wi-Fi scan
     * automatically. Nearby scanning should be triggered
     * explicitly by the user.
     */
    fun getStatus(): NetworkStatus {

        return NetworkStatus(
            wifi = currentWifi(),
            wifiNetworks = emptyList(),
            mobile = currentMobile(),
            internet = internetStatus()
        )
    }

    /**
     * Returns complete status including the latest
     * nearby Wi-Fi scan.
     */
    fun getStatusWithWifiScan(): NetworkStatus {

        return NetworkStatus(
            wifi = currentWifi(),
            wifiNetworks = scanWifi(),
            mobile = currentMobile(),
            internet = internetStatus()
        )
    }

    // ---------------------------------------------------------------------
    // PERMISSIONS
    // ---------------------------------------------------------------------

    /**
     * Wi-Fi permission handling.
     *
     * Android 13+:
     * NEARBY_WIFI_DEVICES is required for Wi-Fi device
     * operations when applicable.
     *
     * Location permission is also checked because
     * Android Wi-Fi scanning behavior varies by OS/device
     * and some devices still require Location services/
     * permission for scan results.
     */
    private fun hasWifiPermission(): Boolean {

        val nearbyWifiGranted =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                hasPermission(
                    Manifest.permission.NEARBY_WIFI_DEVICES
                )
            } else {
                true
            }

        val locationGranted =
            hasPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ||
                hasPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

        return nearbyWifiGranted || locationGranted
    }

    /**
     * Generic runtime permission check.
     */
    private fun hasPermission(
        permission: String
    ): Boolean {

        return ContextCompat.checkSelfPermission(
            appContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
