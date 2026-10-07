package com.networkar.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.networkar.app.network.SignalUtils

// -------------------------------------------------------------
// IMPORTANT:
// Keep this import according to your actual ArCameraView package.
// -------------------------------------------------------------
// import com.networkar.app.ar.ArCameraView


class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            MaterialTheme {

                NetworkARApp()
            }
        }
    }
}


// =============================================================
// MAIN APP
// =============================================================

@Composable
fun NetworkARApp(
    vm: MainViewModel = viewModel()
) {

    val context =
        LocalContext.current

    val ui by vm.ui.collectAsState()

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    var permissionVersion by remember {
        mutableIntStateOf(0)
    }

    val requiredPermissions =
        remember {

            buildList {

                add(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )

                add(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

                add(
                    Manifest.permission.CAMERA
                )

                add(
                    Manifest.permission.READ_PHONE_STATE
                )

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU
                ) {
                    add(
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    )
                }
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionVersion++
        }

    LaunchedEffect(
        permissionVersion
    ) {

        vm.refresh()
    }

    LaunchedEffect(Unit) {

        val missing =
            requiredPermissions.filter {

                ContextCompat.checkSelfPermission(
                    context,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (missing.isNotEmpty()) {

            permissionLauncher.launch(
                missing.toTypedArray()
            )

        } else {

            vm.refresh()
        }
    }

    Scaffold(

        bottomBar = {

            NavigationBar {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                    },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = {
                        Text("Home")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                    },
                    icon = {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Speed"
                        )
                    },
                    label = {
                        Text("Speed")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                    },
                    icon = {
                        Icon(
                            Icons.Default.WifiFind,
                            contentDescription = "Analyzer"
                        )
                    },
                    label = {
                        Text("Analyzer")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                    },
                    icon = {
                        Icon(
                            Icons.Default.NetworkCheck,
                            contentDescription = "AR Scan"
                        )
                    },
                    label = {
                        Text("AR")
                    }
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                    },
                    icon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History"
                        )
                    },
                    label = {
                        Text("History")
                    }
                )
            }
        }

    ) { paddingValues ->

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {

            when (selectedTab) {

                0 -> {

                    HomeScreen(
                        ui = ui,
                        onRefresh = {
                            vm.refresh()
                        },
                        onSpeedTest = {
                            vm.speedTest()
                        },
                        onRequestPermissions = {

                            val missing =
                                requiredPermissions.filter {

                                    ContextCompat.checkSelfPermission(
                                        context,
                                        it
                                    ) != PackageManager.PERMISSION_GRANTED
                                }

                            if (missing.isNotEmpty()) {

                                permissionLauncher.launch(
                                    missing.toTypedArray()
                                )
                            }
                        }
                    )
                }

                1 -> {

                    SpeedScreen(
                        ui = ui,
                        onSpeedTest = {
                            vm.speedTest()
                        }
                    )
                }

                2 -> {

                    AnalyzerScreen(
                        ui = ui,
                        onRefresh = {
                            vm.refresh()
                        },
                        onScan = {
                            vm.scanWifi()
                        }
                    )
                }

                3 -> {

                    ArScreen(
                        ui = ui,
                        onSample = { x, y, z, dbm ->

                            vm.arSample(
                                x = x,
                                y = y,
                                z = z,
                                dbm = dbm
                            )
                        },
                        onStatus = {
                            vm.arStatus(it)
                        },
                        onReset = {
                            vm.resetArCount()
                        }
                    )
                }

                4 -> {

                    HistoryScreen(
                        vm = vm
                    )
                }
            }
        }
    }
}


// =============================================================
// HOME SCREEN
// =============================================================

@Composable
private fun HomeScreen(
    ui: UiState,
    onRefresh: () -> Unit,
    onSpeedTest: () -> Unit,
    onRequestPermissions: () -> Unit
) {

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        item {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "NetScope",
                        style =
                            MaterialTheme.typography.headlineMedium,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text = "Network & Signal Monitor",
                        style =
                            MaterialTheme.typography.bodyMedium
                    )
                }

                IconButton(
                    onClick = onRefresh
                ) {

                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        }

        item {

            NetworkStatusCard(
                ui = ui
            )
        }

        item {

            WifiCard(
                ui = ui
            )
        }

        item {

            MobileCard(
                ui = ui
            )
        }

        item {

            InternetCard(
                ui = ui
            )
        }

        item {

            SpeedSummaryCard(
                ui = ui,
                onSpeedTest = onSpeedTest
            )
        }

        item {

            PermissionCard(
                onRequestPermissions =
                    onRequestPermissions
            )
        }
    }
}


// =============================================================
// NETWORK STATUS CARD
// =============================================================

@Composable
private fun NetworkStatusCard(
    ui: UiState
) {

    val connected =
        ui.internetConnected

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.NetworkCheck,
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Text(
                    text = "Network Status",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    if (connected)
                        "Internet Connected"
                    else
                        "Internet Not Connected",
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text =
                    "Active: ${ui.activeNetworkName}"
            )

            Text(
                text =
                    "Type: ${ui.activeNetworkType}"
            )

            Text(
                text =
                    "Transport: ${ui.internetTransport}"
            )
        }
    }
}


// =============================================================
// WIFI CARD
// =============================================================

@Composable
private fun WifiCard(
    ui: UiState
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Text(
                    text = "Wi-Fi",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    ui.wifiName,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    SignalUtils.summary(
                        ui.wifiDbm
                    )
            )

            if (ui.wifiFrequency > 0) {

                Text(
                    text =
                        "Frequency: ${ui.wifiFrequency} MHz"
                )
            }

            if (ui.wifiBssid.isNotBlank()) {

                Text(
                    text =
                        "BSSID: ${ui.wifiBssid}"
                )
            }
        }
    }
}


// =============================================================
// MOBILE CARD
// =============================================================

@Composable
private fun MobileCard(
    ui: UiState
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.CellTower,
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Text(
                    text = "Mobile Network",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    ui.mobileOperator,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "Network: ${ui.mobileType}"
            )

            Text(
                text =
                    "Signal: ${
                        ui.mobileDbm?.let {
                            SignalUtils.formatDbm(it)
                        } ?: "Unavailable"
                    }"
            )

            Text(
                text =
                    "Signal: ${ui.mobileSignalPercent}%"
            )

            Text(
                text =
                    "Quality: ${ui.mobileQuality}"
            )

            Text(
                text =
                    if (ui.mobileRegistered)
                        "Registered cell: Yes"
                    else
                        "Registered cell: No"
            )
        }
    }
}


// =============================================================
// INTERNET CARD
// =============================================================

@Composable
private fun InternetCard(
    ui: UiState
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.Language,
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Text(
                    text = "Internet Connectivity",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    if (ui.internetConnected)
                        "Connected"
                    else
                        "Disconnected",
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    if (ui.internetValidated)
                        "Internet validation: Passed"
                    else
                        "Internet validation: Not verified"
            )

            Text(
                text =
                    "Transport: ${ui.internetTransport}"
            )

            Text(
                text =
                    if (ui.internetMetered)
                        "Metered connection"
                    else
                        "Unmetered connection"
            )
        }
    }
}


// =============================================================
// SPEED SUMMARY
// =============================================================

@Composable
private fun SpeedSummaryCard(
    ui: UiState,
    onSpeedTest: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = "Speed Test",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                MetricValue(
                    title = "Download",
                    value =
                        "%.2f Mbps".format(
                            ui.speed
                        )
                )

                MetricValue(
                    title = "Upload",
                    value =
                        "%.2f Mbps".format(
                            ui.upload
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                MetricValue(
                    title = "Ping",
                    value =
                        "%.1f ms".format(
                            ui.ping
                        )
                )

                MetricValue(
                    title = "Jitter",
                    value =
                        "%.1f ms".format(
                            ui.jitter
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Button(
                onClick = onSpeedTest,
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !ui.testing
            ) {

                if (ui.testing) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text("Testing...")

                } else {

                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text("Run Speed Test")
                }
            }
        }
    }
}


// =============================================================
// PERMISSION CARD
// =============================================================

@Composable
private fun PermissionCard(
    onRequestPermissions: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Text(
                    text = "Permissions",
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "Wi-Fi scanning, mobile network information and AR scanning require the appropriate Android permissions."
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            TextButton(
                onClick =
                    onRequestPermissions
            ) {

                Text(
                    text = "Request Permissions"
                )
            }
        }
    }
}


// =============================================================
// SPEED SCREEN
// =============================================================

@Composable
private fun SpeedScreen(
    ui: UiState,
    onSpeedTest: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)
    ) {

        Text(
            text = "Internet Speed",
            style =
                MaterialTheme.typography.headlineMedium,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(20.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Download",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    text =
                        "%.2f Mbps".format(
                            ui.speed
                        ),
                    style =
                        MaterialTheme.typography.displaySmall,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            SmallMetricCard(
                modifier =
                    Modifier.weight(1f),
                title = "Upload",
                value =
                    "%.2f Mbps".format(
                        ui.upload
                    )
            )

            SmallMetricCard(
                modifier =
                    Modifier.weight(1f),
                title = "Ping",
                value =
                    "%.1f ms".format(
                        ui.ping
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        SmallMetricCard(
            modifier =
                Modifier.fillMaxWidth(),
            title = "Jitter",
            value =
                "%.1f ms".format(
                    ui.jitter
                )
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Button(
            onClick = onSpeedTest,
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !ui.testing
        ) {

            if (ui.testing) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text("Testing...")

            } else {

                Icon(
                    Icons.Default.Speed,
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text("Start Speed Test")
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Connection",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Network: ${ui.activeNetworkName}"
                )

                Text(
                    text =
                        "Type: ${ui.activeNetworkType}"
                )

                Text(
                    text =
                        "Internet: ${
                            if (ui.internetValidated)
                                "Validated"
                            else
                                "Not validated"
                        }"
                )
            }
        }
    }
}


// =============================================================
// ANALYZER SCREEN
// =============================================================

@Composable
private fun AnalyzerScreen(
    ui: UiState,
    onRefresh: () -> Unit,
    onScan: () -> Unit
) {

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        item {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Network Analyzer",
                    style =
                        MaterialTheme.typography.headlineMedium,
                    fontWeight =
                        FontWeight.Bold
                )

                IconButton(
                    onClick = onRefresh
                ) {

                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        }

        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Connected Wi-Fi",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            ui.wifiName
                    )

                    Text(
                        text =
                            SignalUtils.summary(
                                ui.wifiDbm
                            )
                    )

                    if (ui.wifiFrequency > 0) {

                        Text(
                            text =
                                "Frequency: ${ui.wifiFrequency} MHz"
                        )
                    }
                }
            }
        }

        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Mobile Network",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            ui.mobileOperator
                    )

                    Text(
                        text =
                            "Type: ${ui.mobileType}"
                    )

                    Text(
                        text =
                            "Signal: ${
                                ui.mobileDbm?.let {
                                    SignalUtils.formatDbm(it)
                                } ?: "Unavailable"
                            }"
                    )

                    Text(
                        text =
                            "Quality: ${ui.mobileQuality}"
                    )
                }
            }
        }

        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Internet",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            if (ui.internetConnected)
                                "Connected"
                            else
                                "Disconnected"
                    )

                    Text(
                        text =
                            "Transport: ${ui.internetTransport}"
                    )

                    Text(
                        text =
                            if (ui.internetValidated)
                                "Validated"
                            else
                                "Not validated"
                    )
                }
            }
        }

        item {

            Button(
                onClick = onScan,
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !ui.wifiScanning
            ) {

                if (ui.wifiScanning) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text("Scanning...")

                } else {

                    Icon(
                        Icons.Default.WifiFind,
                        contentDescription = null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )

                    Text("Scan Nearby Wi-Fi")
                }
            }
        }

        item {

            Text(
                text =
                    "Nearby Wi-Fi Networks (${ui.wifiNetworks.size})",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }

        if (ui.wifiNetworks.isEmpty()) {

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            "No nearby Wi-Fi networks found.",
                        modifier =
                            Modifier.padding(16.dp)
                    )
                }
            }

        } else {

            items(
                items =
                    ui.wifiNetworks,
                key = {
                    it.bssid
                }
            ) { network ->

                WifiNetworkItem(
                    ssid =
                        network.ssid,
                    bssid =
                        network.bssid,
                    rssi =
                        network.rssi,
                    frequency =
                        network.frequency
                )
            }
        }
    }
}


// =============================================================
// WIFI NETWORK ITEM
// =============================================================

@Composable
private fun WifiNetworkItem(
    ssid: String,
    bssid: String,
    rssi: Int,
    frequency: Int
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.NetworkWifi,
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )

                Column {

                    Text(
                        text = ssid,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            SignalUtils.summary(
                                rssi
                            )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text =
                    "Frequency: ${frequency} MHz"
            )

            Text(
                text =
                    "BSSID: $bssid"
            )
        }
    }
}


// =============================================================
// AR SCREEN
// =============================================================

@Composable
private fun ArScreen(
    ui: UiState,
    onSample: (
        Float,
        Float,
        Float,
        Int
    ) -> Unit,
    onStatus: (String) -> Unit,
    onReset: () -> Unit
) {

    val context =
        LocalContext.current

    val lifecycleOwner =
        androidx.lifecycle.compose.LocalLifecycleOwner.current

    val cameraGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column {

                Text(
                    text = "AR Signal Scanner",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        ui.arStatus
                )
            }

            TextButton(
                onClick = onReset
            ) {

                Text("Reset")
            }
        }

        if (!cameraGranted) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment =
                    Alignment.Center
            ) {

                Card(
                    modifier =
                        Modifier.padding(24.dp)
                ) {

                    Column(
                        modifier =
                            Modifier.padding(20.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier =
                                Modifier.size(40.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )

                        Text(
                            text =
                                "Camera permission is required for AR scanning.",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

        } else {

            /*
             * ArCameraView is kept as the existing AR engine.
             *
             * The view should call:
             *
             * onSample(x, y, z, dbm)
             *
             * whenever a signal sample is captured.
             */

            AndroidViewArContainer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                onSample =
                    onSample,
                onStatus =
                    onStatus
            )
        }

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Samples",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "${ui.arSamples}"
                    )
                }

                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text = "Signal",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            SignalUtils.formatDbm(
                                ui.wifiDbm
                            )
                    )
                }
            }
        }
    }
}


// =============================================================
// AR CAMERA CONTAINER
// =============================================================

@Composable
private fun AndroidViewArContainer(
    modifier: Modifier,
    onSample: (
        Float,
        Float,
        Float,
        Int
    ) -> Unit,
    onStatus: (String) -> Unit
) {

    /*
     * IMPORTANT:
     *
     * Replace the ArCameraView import/package below if your
     * existing ArCameraView is located somewhere else.
     *
     * The existing ArCameraView API is expected to expose:
     *
     *   ArCameraView(context)
     *   pause()
     *   resume()
     *   close()
     *
     * and callbacks:
     *
     *   arSampleListener
     *   statusListener
     *
     * If your existing class uses different callback names,
     * keep those names from your existing ArCameraView.kt.
     */

    val context =
        LocalContext.current

    val lifecycleOwner =
        androidx.lifecycle.compose.LocalLifecycleOwner.current

    val arView =
        remember {

            /*
             * Existing project class.
             *
             * If Android Studio reports unresolved reference,
             * check the package/import of ArCameraView.kt.
             */
            com.networkar.app.ar.ArCameraView(
                context
            )
        }

    DisposableEffect(
        lifecycleOwner,
        arView
    ) {

        val observer =
            object :
                androidx.lifecycle.DefaultLifecycleObserver {

                override fun onResume(
                    owner:
                        androidx.lifecycle.LifecycleOwner
                ) {

                    arView.resume()
                }

                override fun onPause(
                    owner:
                        androidx.lifecycle.LifecycleOwner
                ) {

                    arView.pause()
                }

                override fun onDestroy(
                    owner:
                        androidx.lifecycle.LifecycleOwner
                ) {

                    arView.close()
                }
            }

        lifecycleOwner.lifecycle.addObserver(
            observer
        )

        onDispose {

            lifecycleOwner.lifecycle.removeObserver(
                observer
            )

            arView.close()
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,

        factory = {

            arView
        },

        update = {

            /*
             * Keep the existing AR view updated here.
             */
        }
    )
}


// =============================================================
// HISTORY SCREEN
// =============================================================

@Composable
private fun HistoryScreen(
    vm: MainViewModel
) {

    val history by
        vm.history.collectAsState(
            initial = emptyList()
        )

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 8.dp,
                        top = 16.dp,
                        bottom = 8.dp
                    ),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "History",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.Bold
            )

            IconButton(
                onClick = {
                    vm.clearHistory()
                },
                enabled =
                    history.isNotEmpty()
            ) {

                Icon(
                    Icons.Default.Delete,
                    contentDescription =
                        "Clear history"
                )
            }
        }

        if (history.isEmpty()) {

            Box(
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier =
                            Modifier.size(48.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text =
                            "No measurements yet."
                    )
                }
            }

        } else {

            LazyColumn(
                modifier =
                    Modifier.fillMaxSize(),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                items(
                    items = history,
                    key = {
                        it.id
                    }
                ) { measurement ->

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(14.dp)
                        ) {

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Text(
                                    text =
                                        measurement.mode,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    text =
                                        measurement.networkType
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    measurement.networkName
                            )

                            Text(
                                text =
                                    "Signal: ${
                                        SignalUtils.formatDbm(
                                            measurement.signalDbm
                                        )
                                    }"
                            )

                            Text(
                                text =
                                    "Quality: ${measurement.quality}"
                            )

                            if (
                                measurement.mode ==
                                "SPEED"
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )

                                Text(
                                    text =
                                        "Download: ${
                                            "%.2f".format(
                                                measurement.speedMbps
                                            )
                                        } Mbps"
                                )

                                Text(
                                    text =
                                        "Upload: ${
                                            "%.2f".format(
                                                measurement.uploadMbps
                                            )
                                        } Mbps"
                                )

                                Text(
                                    text =
                                        "Ping: ${
                                            "%.1f".format(
                                                measurement.pingMs
                                            )
                                        } ms"
                                )

                                Text(
                                    text =
                                        "Jitter: ${
                                            "%.1f".format(
                                                measurement.jitterMs
                                            )
                                        } ms"
                                )
                            }

                            if (
                                measurement.mode ==
                                "AR_SCAN"
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )

                                Text(
                                    text =
                                        "Position: " +
                                            "X=${"%.2f".format(measurement.x)} " +
                                            "Y=${"%.2f".format(measurement.y)} " +
                                            "Z=${"%.2f".format(measurement.z)}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// =============================================================
// METRIC COMPONENTS
// =============================================================

@Composable
private fun MetricValue(
    title: String,
    value: String
) {

    Column {

        Text(
            text = title,
            style =
                MaterialTheme.typography.bodySmall
        )

        Text(
            text = value,
            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
private fun SmallMetricCard(
    modifier: Modifier,
    title: String,
    value: String
) {

    Card(
        modifier = modifier
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodySmall
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text = value,
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
