package com.networkar.app.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoGsm
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int
)

data class MobileNetwork(
    val operator: String,
    val type: String,
    val signalDbm: Int?,
    val registered: Boolean
)

data class InternetInfo(
    val connected: Boolean,
    val validated: Boolean,
    val transport: String,
    val metered: Boolean
)

data class NetworkStatus(
    val wifi: WifiNetwork?,
    val wifiNetworks: List<WifiNetwork>,
    val mobile: MobileNetwork?,
    val internet: InternetInfo
)

class NetworkScanner(private val context: Context) {

    private val appContext = context.applicationContext

    private val wifiManager: WifiManager by lazy {
        appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val connectivityManager: ConnectivityManager by lazy {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
    }

    private val telephonyManager: TelephonyManager by lazy {
        appContext.getSystemService(Context.TELEPHONY_SERVICE)
            as TelephonyManager
    }

    /*
     * Currently connected Wi-Fi
     */
    fun currentWifi(): WifiNetwork? {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return null
        }

        return runCatching {
            val info = wifiManager.connectionInfo ?: return null

            val ssid = info.ssid
                ?.trim('"')
                ?.takeIf {
                    it.isNotBlank() && it != "<unknown ssid>"
                }
                ?: "Unknown Wi-Fi"

            WifiNetwork(
                ssid = ssid,
                bssid = info.bssid ?: "",
                rssi = info.rssi,
                frequency = info.frequency
            )
        }.getOrNull()
    }

    /*
     * Nearby Wi-Fi networks
     */
    fun scanWifi(): List<WifiNetwork> {

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return emptyList()
        }

        if (!wifiManager.isWifiEnabled) {
            return emptyList()
        }

        return runCatching {

            /*
             * Android may throttle this request.
             * The returned scan results can therefore be
             * the most recently available results.
             */
            runCatching {
                wifiManager.startScan()
            }

            wifiManager.scanResults
                .map { result ->

                    WifiNetwork(
                        ssid = result.SSID.ifBlank {
                            "Hidden network"
                        },
                        bssid = result.BSSID ?: "",
                        rssi = result.level,
                        frequency = result.frequency
                    )
                }
                .sortedByDescending {
                    it.rssi
                }

        }.getOrDefault(emptyList())
    }

    /*
     * Current mobile network.
     *
     * This does NOT scan all cellular towers.
     * It reports the cellular network currently
     * associated with the device.
     */
    fun currentMobile(): MobileNetwork? {

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return null
        }

        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            return null
        }

        return runCatching {

            val operator = telephonyManager.networkOperatorName
                .takeIf { it.isNotBlank() }
                ?: "Unknown operator"

            val cells = telephonyManager.allCellInfo

            val registeredCell = cells
                ?.firstOrNull { it.isRegistered }

            val type = when (registeredCell) {

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

            val signalDbm = when (registeredCell) {

                is CellInfoNr -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        registeredCell.cellSignalStrength.dbm
                    } else {
                        null
                    }
                }

                is CellInfoLte -> {
                    registeredCell.cellSignalStrength.dbm
                }

                else -> {
                    null
                }
            }

            MobileNetwork(
                operator = operator,
                type = type,
                signalDbm = signalDbm,
                registered = registeredCell != null
            )

        }.getOrNull()
    }

    /*
     * Internet connectivity.
     */
    fun internetStatus(): InternetInfo {

        val network = connectivityManager.activeNetwork

        if (network == null) {
            return InternetInfo(
                connected = false,
                validated = false,
                transport = "NONE",
                metered = false
            )
        }

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)

        if (capabilities == null) {
            return InternetInfo(
                connected = false,
                validated = false,
                transport = "UNKNOWN",
                metered = false
            )
        }

        val transport = when {

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

            else -> {
                "Other"
            }
        }

        val hasInternet =
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )

        val validated =
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )

        return InternetInfo(
            connected = hasInternet,
            validated = validated,
            transport = transport,
            metered = !capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_METERED
            )
        )
    }

    /*
     * Collect everything.
     */
    fun getStatus(): NetworkStatus {

        return NetworkStatus(
            wifi = currentWifi(),
            wifiNetworks = scanWifi(),
            mobile = currentMobile(),
            internet = internetStatus()
        )
    }

    private fun getNetworkTypeFallback(): String {

        return when (telephonyManager.networkType) {

            TelephonyManager.NETWORK_TYPE_NR ->
                "5G NR"

            TelephonyManager.NETWORK_TYPE_LTE ->
                "4G LTE"

            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA ->
                "3G"

            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_GPRS ->
                "2G"

            else ->
                "Unknown"
        }
    }

    private fun hasPermission(permission: String): Boolean {

        return ContextCompat.checkSelfPermission(
            appContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}