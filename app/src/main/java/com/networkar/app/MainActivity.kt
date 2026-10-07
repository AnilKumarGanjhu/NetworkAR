package com.networkar.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.networkar.app.ar.ArCameraView
import com.networkar.app.network.SignalUtils

class MainActivity : ComponentActivity() {

    private var permissionVersion by mutableIntStateOf(0)

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionVersion++
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val version = permissionVersion

            NetworkARApp(
                vm = viewModel(),
                permissionVersion = version,
                requestPermissions = {
                    requestNetworkPermissions()
                }
            )
        }
    }

    private fun requestNetworkPermissions() {

        val permissions = buildList {

            // Wi-Fi scanning
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)

            // AR camera
            add(Manifest.permission.CAMERA)

            // Mobile network information
            add(Manifest.permission.READ_PHONE_STATE)

            // Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        permissionLauncher.launch(
            permissions.toTypedArray()
        )
    }
}


@Composable
fun NetworkARApp(
    vm: MainViewModel = viewModel(),
    permissionVersion: Int = 0,
    requestPermissions: () -> Unit = {}
) {

    val u by vm.ui.collectAsState()

    var tab by remember {
        mutableIntStateOf(0)
    }

    /*
     * Refresh network information when the app starts
     * and whenever permissions change.
     */
    LaunchedEffect(permissionVersion) {
        vm.refresh()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C5CFF),
            secondary = Color(0xFF20D6A7),
            background = Color(0xFF090B10),
            surface = Color(0xFF131722)
        )
    ) {

        Scaffold(

            bottomBar = {

                NavigationBar {

                    listOf(
                        "Home",
                        "Speed",
                        "Analyzer",
                        "AR Scan",
                        "History"
                    ).forEachIndexed { index, name ->

                        NavigationBarItem(

                            selected = tab == index,

                            onClick = {
                                tab = index
                            },

                            icon = {

                                Icon(
                                    imageVector = when (index) {

                                        0 -> Icons.Default.Home

                                        1 -> Icons.Default.Speed

                                        2 -> Icons.Default.Wifi

                                        3 -> Icons.Default.ViewInAr

                                        else -> Icons.Default.History
                                    },

                                    contentDescription = name
                                )
                            },

                            label = {
                                Text(name)
                            }
                        )
                    }
                }
            }

        ) { padding ->

            when (tab) {

                0 -> Home(
                    u = u,
                    v = vm,
                    requestPermissions = requestPermissions,
                    m = Modifier.padding(padding)
                )

                1 -> Speed(
                    u = u,
                    v = vm,
                    m = Modifier.padding(padding)
                )

                2 -> Analyzer(
                    u = u,
                    v = vm,
                    requestPermissions = requestPermissions,
                    m = Modifier.padding(padding)
                )

                3 -> Ar(
                    u = u,
                    v = vm,
                    permissionVersion = permissionVersion,
                    requestPermissions = requestPermissions,
                    m = Modifier.padding(padding)
                )

                4 -> History(
                    v = vm,
                    m = Modifier.padding(padding)
                )
            }
        }
    }
}


@Composable
fun BoxCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}


/*
 * HOME
 */

@Composable
fun Home(
    u: UiState,
    v: MainViewModel,
    requestPermissions: () -> Unit,
    m: Modifier
) {

    Column(
        modifier = m
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Text(
            text = "NetScope",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Wi-Fi + 4G/5G + Internet + AR room signal scanner",
            color = Color.Gray
        )

        /*
         * CURRENT CONNECTION
         */

        BoxCard {

            Text(
                text = "CURRENT CONNECTION",
                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = u.wifiName,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "${u.wifiDbm} dBm • " +
                    SignalUtils.quality(u.wifiDbm),

                style = MaterialTheme.typography.titleLarge
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    "${u.mobileType}: " +
                    "${u.mobileDbm} dBm"
            )

            Text(
                text =
                    "Frequency: " +
                    if (u.wifiFrequency > 0)
                        "${u.wifiFrequency} MHz"
                    else
                        "Unavailable",

                color = Color.Gray
            )
        }


        /*
         * INTERNET
         */

        BoxCard {

            Text(
                text = "INTERNET",
                color = Color.Gray
            )

            Text(
                text = "${u.speed} Mbps",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Ping ${u.ping} ms • " +
                    "Jitter ${u.jitter} ms"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    v.speedTest()
                },

                enabled = !u.testing,

                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    if (u.testing)
                        "TESTING..."
                    else
                        "START SPEED TEST"
                )
            }
        }


        /*
         * NETWORK PERMISSION
         */

        BoxCard {

            Text(
                text = "NETWORK ACCESS",
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Wi-Fi scanning, mobile 4G/5G information " +
                    "and AR camera require permissions.",

                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = requestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    "ALLOW NETWORK & CAMERA ACCESS"
                )
            }
        }


        /*
         * AR ROOM SCAN
         */

        BoxCard {

            Text(
                text = "ROOM SIGNAL SCAN",
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Use the phone camera and ARCore to walk " +
                    "through the room and record signal points.",

                color = Color.Gray
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = requestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    "START AR ROOM SCAN"
                )
            }
        }
    }
}


/*
 * SPEED TEST
 */

@Composable
fun Speed(
    u: UiState,
    v: MainViewModel,
    m: Modifier
) {

    Column(
        modifier = m.padding(18.dp)
    ) {

        Text(
            text = "Speed Test",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        BoxCard {

            Text(
                text = "DOWNLOAD",
                color = Color.Gray
            )

            Text(
                text = "${u.speed} Mbps",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "UPLOAD ${u.upload} Mbps"
            )

            Text(
                text = "PING ${u.ping} ms"
            )

            Text(
                text = "JITTER ${u.jitter} ms"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    v.speedTest()
                },

                enabled = !u.testing,

                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    if (u.testing)
                        "Testing..."
                    else
                        "Run Test"
                )
            }
        }
    }
}


/*
 * WI-FI + MOBILE ANALYZER
 */

@Composable
fun Analyzer(
    u: UiState,
    v: MainViewModel,
    requestPermissions: () -> Unit,
    m: Modifier
) {

    Column(
        modifier = m.padding(18.dp)
    ) {

        Text(
            text = "Network Analyzer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )


        /*
         * CONNECTED WI-FI
         */

        BoxCard {

            Text(
                text = "CONNECTED WI-FI",
                color = Color.Gray
            )

            Text(
                text = u.wifiName,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "RSSI: ${u.wifiDbm} dBm"
            )

            Text(
                text =
                    "Quality: " +
                    SignalUtils.quality(u.wifiDbm)
            )

            Text(
                text =
                    "Frequency: " +
                    if (u.wifiFrequency > 0)
                        "${u.wifiFrequency} MHz"
                    else
                        "Unavailable"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    v.scanWifi()
                },

                enabled = !u.wifiScanning,

                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    if (u.wifiScanning)
                        "SCANNING..."
                    else
                        "SCAN NEARBY WI-FI"
                )
            }

            OutlinedButton(
                onClick = requestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    "CHECK PERMISSIONS"
                )
            }
        }


        /*
         * MOBILE 4G / 5G
         */

        BoxCard {

            Text(
                text = "MOBILE NETWORK",
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Network: ${u.mobileType}"
            )

            Text(
                text = "Signal: ${u.mobileDbm} dBm"
            )

            Text(
                text =
                    "This shows the current cellular " +
                    "connection of the phone.",

                color = Color.Gray
            )
        }


        /*
         * WI-FI VS MOBILE
         */

        BoxCard {

            Text(
                text = "WI-FI VS MOBILE",
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Wi-Fi: ${u.wifiDbm} dBm"
            )

            Text(
                text =
                    "${u.mobileType}: " +
                    "${u.mobileDbm} dBm"
            )

            Text(
                text =
                    if (u.wifiDbm > u.mobileDbm)
                        "Current signal: Wi-Fi"
                    else
                        "Current signal: Mobile"
            )
        }


        /*
         * NEARBY WI-FI
         */

        Text(
            text = "Nearby Wi-Fi Networks",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (u.wifiNetworks.isEmpty()) {

            Text(
                text =
                    "No scan results yet. Tap Scan Nearby Wi-Fi " +
                    "and allow Location/Nearby devices.",

                color = Color.Gray,

                modifier = Modifier.padding(
                    vertical = 12.dp
                )
            )

        } else {

            /*
             * Important:
             * Don't put a LazyColumn inside another scrolling
             * Column. The analyzer itself is therefore using
             * LazyColumn for the network list.
             */

            LazyColumn(
                modifier = Modifier.fillMaxHeight()
            ) {

                items(
                    items = u.wifiNetworks,
                    key = {
                        it.bssid
                    }
                ) { network ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = network.ssid,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text =
                                        "${network.frequency} MHz • " +
                                        network.bssid,

                                    color = Color.Gray
                                )
                            }

                            Text(
                                text = "${network.rssi} dBm",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
 * AR SCAN
 */

@Composable
fun Ar(
    u: UiState,
    v: MainViewModel,
    permissionVersion: Int,
    requestPermissions: () -> Unit,
    m: Modifier
) {

    val context = LocalContext.current

    val lifecycle =
        androidx.lifecycle.compose.LocalLifecycleOwner.current

    var cameraView by remember {
        mutableStateOf<ArCameraView?>(null)
    }


    DisposableEffect(
        lifecycle,
        permissionVersion
    ) {

        val observer =
            object : DefaultLifecycleObserver {

                override fun onResume(
                    owner: LifecycleOwner
                ) {

                    cameraView?.resumeAr()
                }

                override fun onPause(
                    owner: LifecycleOwner
                ) {

                    cameraView?.pauseAr()
                }
            }

        lifecycle.lifecycle.addObserver(
            observer
        )

        onDispose {

            lifecycle.lifecycle.removeObserver(
                observer
            )

            cameraView?.closeAr()
        }
    }


    Box(
        modifier = m.fillMaxSize()
    ) {

        AndroidView(

            factory = {

                ArCameraView(

                    context,

                    onSample = {
                            x,
                            y,
                            z,
                            dbm ->

                        v.arSample(
                            x,
                            y,
                            z,
                            dbm
                        )
                    },

                    onStatus = {
                        v.arStatus(it)
                    }

                ).also {
                    cameraView = it
                }
            },

            modifier = Modifier.fillMaxSize()
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            verticalArrangement =
                Arrangement.Bottom
        ) {

            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.Black.copy(
                                alpha = .72f
                            )
                    ),

                shape =
                    RoundedCornerShape(22.dp)
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "AR ROOM SCAN",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = u.arStatus,
                        color = Color.White
                    )

                    Text(
                        text =
                            "Samples: ${u.arSamples} • " +
                            "Wi-Fi: ${u.wifiDbm} dBm",

                        color = Color.LightGray
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = requestPermissions,

                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                "START / RETRY"
                            )
                        }

                        OutlinedButton(

                            onClick = {

                                cameraView?.pauseAr()

                                v.arStatus(
                                    "Scan paused"
                                )
                            },

                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                "PAUSE"
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
 * HISTORY
 */

@Composable
fun History(
    v: MainViewModel,
    m: Modifier
) {

    val history by v.history.collectAsState(
        initial = emptyList()
    )

    Column(
        modifier = m.padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),

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

            TextButton(
                onClick = {
                    v.clearHistory()
                }
            ) {

                Text("Clear")
            }
        }


        if (history.isEmpty()) {

            Text(
                text = "No measurements yet."
            )

        } else {

            LazyColumn {

                items(
                    items = history,
                    key = {
                        it.id
                    }
                ) { item ->

                    BoxCard {

                        Text(
                            text = item.mode,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "${item.networkName} • " +
                                "${item.signalDbm} dBm • " +
                                item.quality
                        )

                        if (item.mode == "AR_SCAN") {

                            Text(
                                text =
                                    "Position: " +
                                    "%.2f".format(item.x) +
                                    ", " +
                                    "%.2f".format(item.y) +
                                    ", " +
                                    "%.2f".format(item.z)
                            )
                        }

                        if (item.speedMbps > 0) {

                            Text(
                                text =
                                    "${item.speedMbps} Mbps • " +
                                    "ping ${item.pingMs} ms • " +
                                    "upload ${item.uploadMbps} Mbps"
                            )
                        }
                    }
                }
            }
        }
    }
}
