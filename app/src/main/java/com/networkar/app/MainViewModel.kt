package com.networkar.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.networkar.app.data.AppDatabase
import com.networkar.app.data.Measurement
import com.networkar.app.network.InternetInfo
import com.networkar.app.network.MobileNetwork
import com.networkar.app.network.NetworkScanner
import com.networkar.app.network.NetworkStatus
import com.networkar.app.network.SignalUtils
import com.networkar.app.network.SpeedResult
import com.networkar.app.network.SpeedTester
import com.networkar.app.network.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class UiState(

    // ---------------------------------------------------------
    // Wi-Fi
    // ---------------------------------------------------------

    val wifiDbm: Int = -100,

    val wifiName: String = "Wi-Fi",

    val wifiFrequency: Int = 0,

    val wifiBssid: String = "",

    // ---------------------------------------------------------
    // Mobile 4G / 5G
    // ---------------------------------------------------------

    val mobileDbm: Int? = null,

    val mobileType: String = "Mobile",

    val mobileOperator: String = "Unknown operator",

    val mobileRegistered: Boolean = false,

    val mobileSignalPercent: Int = 0,

    val mobileQuality: String = "UNAVAILABLE",

    // ---------------------------------------------------------
    // Internet
    // ---------------------------------------------------------

    val internetConnected: Boolean = false,

    val internetValidated: Boolean = false,

    val internetTransport: String = "NONE",

    val internetMetered: Boolean = false,

    // ---------------------------------------------------------
    // Active network
    // ---------------------------------------------------------

    val activeNetworkName: String = "No network",

    val activeNetworkType: String = "NONE",

    // ---------------------------------------------------------
    // Speed test
    // ---------------------------------------------------------

    val speed: Double = 0.0,

    val upload: Double = 0.0,

    val ping: Double = 0.0,

    val jitter: Double = 0.0,

    val testing: Boolean = false,

    // ---------------------------------------------------------
    // Wi-Fi scanner
    // ---------------------------------------------------------

    val wifiScanning: Boolean = false,

    val wifiNetworks: List<WifiNetwork> = emptyList(),

    // ---------------------------------------------------------
    // AR Scanner
    // ---------------------------------------------------------

    val arSamples: Int = 0,

    val arStatus: String = "Ready"
)


class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    // ---------------------------------------------------------
    // Room database
    // ---------------------------------------------------------

    private val db =
        AppDatabase.getInstance(application)

    private val dao =
        db.measurementDao()

    // ---------------------------------------------------------
    // Network scanner
    // ---------------------------------------------------------

    private val networkScanner =
        NetworkScanner(application)

    // ---------------------------------------------------------
    // Speed tester
    // ---------------------------------------------------------

    private val speedTester =
        SpeedTester()

    // ---------------------------------------------------------
    // UI state
    // ---------------------------------------------------------

    private val _ui =
        MutableStateFlow(UiState())

    val ui =
        _ui.asStateFlow()

    // ---------------------------------------------------------
    // Room history
    // ---------------------------------------------------------

    val history =
        dao.getAll()


    // =========================================================
    // NETWORK REFRESH
    // =========================================================

    /**
     * Refreshes:
     *
     * - Current Wi-Fi
     * - Mobile 4G/5G
     * - Internet connectivity
     * - Active network
     *
     * Does NOT perform nearby Wi-Fi scan.
     */
    fun refresh() {

        viewModelScope.launch {

            val status =
                withContext(Dispatchers.IO) {

                    runCatching {
                        networkScanner.getStatus()
                    }.getOrElse {

                        NetworkStatus(
                            wifi = null,
                            wifiNetworks = emptyList(),
                            mobile = null,
                            internet = InternetInfo(
                                connected = false,
                                validated = false,
                                transport = "NONE",
                                metered = false
                            )
                        )
                    }
                }

            applyNetworkStatus(status)
        }
    }


    // =========================================================
    // APPLY NETWORK STATUS
    // =========================================================

    private fun applyNetworkStatus(
        status: NetworkStatus
    ) {

        val wifi =
            status.wifi

        val mobile =
            status.mobile

        val internet =
            status.internet

        val activeName =
            getActiveNetworkName(
                wifi = wifi,
                mobile = mobile,
                internet = internet
            )

        val activeType =
            getActiveNetworkType(
                wifi = wifi,
                mobile = mobile,
                internet = internet
            )

        _ui.update {

            it.copy(

                // -------------------------------------------------
                // Wi-Fi
                // -------------------------------------------------

                wifiDbm =
                    wifi?.rssi ?: -100,

                wifiName =
                    wifi?.ssid ?: "Wi-Fi",

                wifiFrequency =
                    wifi?.frequency ?: 0,

                wifiBssid =
                    wifi?.bssid ?: "",

                // -------------------------------------------------
                // Mobile
                // -------------------------------------------------

                mobileDbm =
                    mobile?.signalDbm,

                mobileType =
                    mobile?.type ?: "Mobile",

                mobileOperator =
                    mobile?.operator ?: "Unknown operator",

                mobileRegistered =
                    mobile?.registered ?: false,

                mobileSignalPercent =
                    mobile
                        ?.signalDbm
                        ?.let {
                            if (SignalUtils.isValid(it)) {
                                SignalUtils.percent(it)
                            } else {
                                0
                            }
                        }
                        ?: 0,

                mobileQuality =
                    mobile
                        ?.signalDbm
                        ?.let {
                            if (SignalUtils.isValid(it)) {
                                SignalUtils.quality(it)
                            } else {
                                "UNAVAILABLE"
                            }
                        }
                        ?: "UNAVAILABLE",

                // -------------------------------------------------
                // Internet
                // -------------------------------------------------

                internetConnected =
                    internet.connected,

                internetValidated =
                    internet.validated,

                internetTransport =
                    internet.transport,

                internetMetered =
                    internet.metered,

                // -------------------------------------------------
                // Active network
                // -------------------------------------------------

                activeNetworkName =
                    activeName,

                activeNetworkType =
                    activeType
            )
        }
    }


    // =========================================================
    // ACTIVE NETWORK NAME
    // =========================================================

    private fun getActiveNetworkName(
        wifi: WifiNetwork?,
        mobile: MobileNetwork?,
        internet: InternetInfo
    ): String {

        return when (internet.transport) {

            "Wi-Fi" -> {

                wifi
                    ?.ssid
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Wi-Fi"
            }

            "Mobile Data" -> {

                mobile
                    ?.operator
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Mobile Data"
            }

            "Ethernet" -> {
                "Ethernet"
            }

            "VPN" -> {
                "VPN"
            }

            else -> {

                when {

                    wifi != null ->
                        wifi.ssid

                    mobile != null ->
                        mobile.operator

                    else ->
                        "No network"
                }
            }
        }
    }


    // =========================================================
    // ACTIVE NETWORK TYPE
    // =========================================================

    private fun getActiveNetworkType(
        wifi: WifiNetwork?,
        mobile: MobileNetwork?,
        internet: InternetInfo
    ): String {

        return when (internet.transport) {

            "Wi-Fi" -> {
                "Wi-Fi"
            }

            "Mobile Data" -> {

                mobile
                    ?.type
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Mobile"
            }

            "Ethernet" -> {
                "Ethernet"
            }

            "VPN" -> {
                "VPN"
            }

            else -> {

                when {

                    wifi != null ->
                        "Wi-Fi"

                    mobile != null ->
                        mobile.type

                    else ->
                        "NONE"
                }
            }
        }
    }


    // =========================================================
    // WIFI SCAN
    // =========================================================

    /**
     * Performs nearby Wi-Fi scan.
     */
    fun scanWifi() {

        if (_ui.value.wifiScanning) {
            return
        }

        viewModelScope.launch {

            _ui.update {
                it.copy(
                    wifiScanning = true
                )
            }

            try {

                val results =
                    withContext(Dispatchers.IO) {

                        runCatching {
                            networkScanner.scanWifi()
                        }.getOrDefault(
                            emptyList()
                        )
                    }

                _ui.update {

                    it.copy(
                        wifiNetworks = results
                    )
                }

            } finally {

                _ui.update {

                    it.copy(
                        wifiScanning = false
                    )
                }
            }
        }
    }


    // =========================================================
    // SPEED TEST
    // =========================================================

    /**
     * Runs:
     *
     * - Ping
     * - Jitter
     * - Download
     * - Upload
     *
     * and stores the result in Room.
     */
    fun speedTest() {

        if (_ui.value.testing) {
            return
        }

        viewModelScope.launch {

            _ui.update {
                it.copy(
                    testing = true
                )
            }

            try {

                val result =
                    withContext(Dispatchers.IO) {

                        runCatching {
                            speedTester.test()
                        }.getOrDefault(
                            SpeedResult(
                                downloadMbps = 0.0,
                                uploadMbps = 0.0,
                                pingMs = 0.0,
                                jitterMs = 0.0
                            )
                        )
                    }

                _ui.update {

                    it.copy(

                        speed =
                            result.downloadMbps,

                        upload =
                            result.uploadMbps,

                        ping =
                            result.pingMs,

                        jitter =
                            result.jitterMs
                    )
                }

                saveSpeedMeasurement(
                    result
                )

            } finally {

                _ui.update {

                    it.copy(
                        testing = false
                    )
                }
            }
        }
    }


    // =========================================================
    // SAVE SPEED TEST RESULT
    // =========================================================

    private suspend fun saveSpeedMeasurement(
        result: SpeedResult
    ) {

        val state =
            _ui.value

        val useWifi =
            state.internetTransport == "Wi-Fi" &&
                state.wifiName != "Wi-Fi"

        val networkName =
            if (useWifi) {
                state.wifiName
            } else {
                state.mobileOperator
            }

        val networkType =
            if (useWifi) {
                "Wi-Fi"
            } else {
                state.mobileType
            }

        val signalDbm =
            if (useWifi) {
                state.wifiDbm
            } else {
                state.mobileDbm ?: -120
            }

        val signalPercent =
            if (SignalUtils.isValid(signalDbm)) {
                SignalUtils.percent(signalDbm)
            } else {
                0
            }

        val quality =
            if (SignalUtils.isValid(signalDbm)) {
                SignalUtils.quality(signalDbm)
            } else {
                "UNAVAILABLE"
            }

        withContext(Dispatchers.IO) {

            dao.insert(

                Measurement(

                    mode = "SPEED",

                    networkName =
                        networkName,

                    networkType =
                        networkType,

                    signalDbm =
                        signalDbm,

                    signalPercent =
                        signalPercent,

                    quality =
                        quality,

                    speedMbps =
                        result.downloadMbps,

                    uploadMbps =
                        result.uploadMbps,

                    pingMs =
                        result.pingMs.toLong(),

                    jitterMs =
                        result.jitterMs.toLong()
                )
            )
        }
    }


    // =========================================================
    // NORMAL SIGNAL SAMPLE
    // =========================================================

    /**
     * Saves current Wi-Fi/mobile signal into Room.
     */
    fun sample() {

        viewModelScope.launch {

            val status =
                withContext(Dispatchers.IO) {

                    runCatching {
                        networkScanner.getStatus()
                    }.getOrNull()
                }

            if (status == null) {
                return@launch
            }

            applyNetworkStatus(status)

            val useWifi =
                status.internet.transport == "Wi-Fi" &&
                    status.wifi != null

            val signalDbm =
                if (useWifi) {

                    status.wifi?.rssi
                        ?: -100

                } else {

                    status.mobile?.signalDbm
                        ?: -120
                }

            val networkName =
                if (useWifi) {

                    status.wifi?.ssid
                        ?: "Wi-Fi"

                } else {

                    status.mobile?.operator
                        ?: "Mobile"
                }

            val networkType =
                if (useWifi) {

                    "Wi-Fi"

                } else {

                    status.mobile?.type
                        ?: "Mobile"
                }

            val signalPercent =
                if (SignalUtils.isValid(signalDbm)) {

                    SignalUtils.percent(
                        signalDbm
                    )

                } else {
                    0
                }

            val quality =
                if (SignalUtils.isValid(signalDbm)) {

                    SignalUtils.quality(
                        signalDbm
                    )

                } else {
                    "UNAVAILABLE"
                }

            withContext(Dispatchers.IO) {

                dao.insert(

                    Measurement(

                        mode = "SIGNAL",

                        networkName =
                            networkName,

                        networkType =
                            networkType,

                        signalDbm =
                            signalDbm,

                        signalPercent =
                            signalPercent,

                        quality =
                            quality
                    )
                }
            }
        }
    }


    // =========================================================
    // AR SAMPLE
    // =========================================================

    /**
     * Stores one AR signal measurement.
     *
     * x/y/z represent AR world position.
     */
    fun arSample(
        x: Float,
        y: Float,
        z: Float,
        dbm: Int
    ) {

        val quality =
            if (SignalUtils.isValid(dbm)) {

                SignalUtils.quality(dbm)

            } else {
                "UNAVAILABLE"
            }

        val signalPercent =
            if (SignalUtils.isValid(dbm)) {

                SignalUtils.percent(dbm)

            } else {
                0
            }

        val state =
            _ui.value

        val nextCount =
            state.arSamples + 1

        _ui.update {

            it.copy(

                wifiDbm =
                    dbm,

                arSamples =
                    nextCount,

                arStatus =
                    "Scanning • $nextCount points"
            )
        }

        viewModelScope.launch {

            withContext(Dispatchers.IO) {

                dao.insert(

                    Measurement(

                        mode = "AR_SCAN",

                        networkName =
                            state.wifiName,

                        networkType =
                            "Wi-Fi",

                        signalDbm =
                            dbm,

                        signalPercent =
                            signalPercent,

                        quality =
                            quality,

                        x = x,

                        y = y,

                        z = z
                    )
                )
            }
        }
    }


    // =========================================================
    // AR STATUS
    // =========================================================

    fun arStatus(
        status: String
    ) {

        _ui.update {

            it.copy(
                arStatus = status
            )
        }
    }


    // =========================================================
    // RESET AR
    // =========================================================

    fun resetArCount() {

        _ui.update {

            it.copy(

                arSamples = 0,

                arStatus = "Ready"
            )
        }
    }


    // =========================================================
    // CLEAR HISTORY
    // =========================================================

    fun clearHistory() {

        viewModelScope.launch {

            withContext(Dispatchers.IO) {

                dao.deleteAll()
            }

            resetArCount()
        }
    }


    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onCleared() {

        super.onCleared()

        // AppDatabase singleton ko yahan close nahi karna.
        // Room database application lifetime tak available rahega.
    }
}
