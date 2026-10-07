package com.networkar.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.networkar.app.ar.ArCameraView
import com.networkar.app.network.SignalUtils


// =============================================================
// NETSCOPE COLORS
// =============================================================

private val Background =
    Color(0xFF050A14)

private val SurfaceDark =
    Color(0xFF0C1424)

private val SurfaceLight =
    Color(0xFF111D31)

private val Blue =
    Color(0xFF168CFF)

private val Cyan =
    Color(0xFF19D7FF)

private val Green =
    Color(0xFF19D99A)

private val Purple =
    Color(0xFF805CFF)

private val TextPrimary =
    Color(0xFFF4F8FF)

private val TextSecondary =
    Color(0xFF91A4BF)


// =============================================================
// ACTIVITY
// =============================================================

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

            NetworkARApp(
                vm = viewModel(),
                permissionVersion = permissionVersion,
                requestPermissions = {
                    requestNetworkPermissions()
                }
            )
        }
    }

    private fun requestNetworkPermissions() {

        val permissions =
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

                if (Build.VERSION.SDK_INT >= 33) {

                    add(
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    )
                }
            }

        permissionLauncher.launch(
            permissions.toTypedArray()
        )
    }
}


// =============================================================
// MAIN APP
// =============================================================

@Composable
fun NetworkARApp(
    vm: MainViewModel = viewModel(),
    permissionVersion: Int = 0,
    requestPermissions: () -> Unit = {}
) {

    val ui by vm.ui.collectAsState()

    var selectedTab by
            remember {
                mutableIntStateOf(0)
            }

    LaunchedEffect(Unit) {
        vm.refresh()
    }

    val tabs =
        listOf(
            "Network",
            "Scan",
            "Speed",
            "AR",
            "History"
        )

    val icons =
        listOf(
            Icons.Default.Home,
            Icons.Default.WifiFind,
            Icons.Default.Speed,
            Icons.Default.ViewInAr,
            Icons.Default.History
        )

    MaterialTheme(
        colorScheme =
            androidx.compose.material3.darkColorScheme(
                primary = Blue,
                secondary = Cyan,
                background = Background,
                surface = SurfaceDark,
                onBackground = TextPrimary,
                onSurface = TextPrimary
            )
    ) {

        Scaffold(

            containerColor =
                Background,

            topBar = {

                TopAppBar(

                    title = {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "NetScope",
                                fontSize = 23.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text = " 2.0",
                                fontSize = 23.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                color = Cyan
                            )
                        }
                    },

                    actions = {

                        IconButton(
                            onClick = {
                                vm.refresh()
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Refresh,
                                contentDescription =
                                    "Refresh",
                                tint =
                                    TextPrimary
                            )
                        }
                    },

                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor =
                                Background
                        )
                )
            },

            bottomBar = {

                NavigationBar(

                    modifier =
                        Modifier.navigationBarsPadding(),

                    containerColor =
                        Color(0xFF08111F)
                ) {

                    tabs.forEachIndexed {
                            index,
                            label ->

                        NavigationBarItem(

                            selected =
                                selectedTab == index,

                            onClick = {
                                selectedTab = index
                            },

                            icon = {

                                Icon(
                                    imageVector =
                                        icons[index],
                                    contentDescription =
                                        label
                                )
                            },

                            label = {
                                Text(
                                    label,
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                }
            }
        ) { padding ->

            when (selectedTab) {

                0 ->
                    Dashboard(
                        ui,
                        vm,
                        requestPermissions,
                        Modifier.padding(padding)
                    )

                1 ->
                    WifiAnalyzer(
                        ui,
                        vm,
                        requestPermissions,
                        Modifier.padding(padding)
                    )

                2 ->
                    SpeedScreen(
                        ui,
                        vm,
                        Modifier.padding(padding)
                    )

                3 ->
                    ArScreen(
                        ui,
                        vm,
                        permissionVersion,
                        requestPermissions,
                        Modifier.padding(padding)
                    )

                4 ->
                    HistoryScreen(
                        vm,
                        Modifier.padding(padding)
                    )
            }
        }
    }
}


// =============================================================
// DASHBOARD
// =============================================================

@Composable
private fun Dashboard(
    u: UiState,
    vm: MainViewModel,
    requestPermissions: () -> Unit,
    modifier: Modifier
) {

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize()
                .background(Background),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 20.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        item {

            Text(
                text = "Your Network, In Focus.",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(
                Modifier.height(4.dp)
            )
        }

        item {

            ActiveNetworkCard(u)
        }

        item {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                SignalCard(
                    title = "Wi-Fi",
                    name = u.wifiName,
                    dbm = u.wifiDbm,
                    percent =
                        SignalUtils.percent(
                            u.wifiDbm
                        ),
                    icon = Icons.Default.Wifi,
                    modifier =
                        Modifier.weight(1f)
                )

                SignalCard(
                    title = "Mobile",
                    name = u.mobileOperator,
                    dbm = u.mobileDbm,
                    percent =
                        u.mobileSignalPercent,
                    icon =
                        Icons.Default.CellTower,
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        item {

            InternetCard(u)
        }

        item {

            QuickActions(
                vm,
                requestPermissions
            )
        }
    }
}


// =============================================================
// ACTIVE NETWORK CARD
// =============================================================

@Composable
private fun ActiveNetworkCard(
    u: UiState
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(26.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceLight
            )
    ) {

        Box(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF102C50),
                                Color(0xFF0B182B),
                                Color(0xFF101329)
                            )
                        )
                    )
                    .padding(20.dp)
        ) {

            Column {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconBadge(
                            icon =
                                Icons.Default.NetworkCheck,
                            color =
                                Cyan
                        )

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column {

                            Text(
                                "ACTIVE NETWORK",
                                color =
                                    TextSecondary,
                                fontSize =
                                    11.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                u.activeNetworkName,
                                color =
                                    TextPrimary,
                                fontSize =
                                    19.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    StatusPill(
                        text =
                            if (u.internetValidated)
                                "Connected"
                            else
                                "Offline",
                        active =
                            u.internetValidated
                    )
                }

                Spacer(
                    Modifier.height(18.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(24.dp)
                ) {

                    MiniValue(
                        title = "TYPE",
                        value =
                            u.activeNetworkType
                    )

                    MiniValue(
                        title = "TRANSPORT",
                        value =
                            u.internetTransport
                    )

                    MiniValue(
                        title = "METERED",
                        value =
                            if (u.internetMetered)
                                "Yes"
                            else
                                "No"
                    )
                }
            }
        }
    }
}


// =============================================================
// SIGNAL CARD
// =============================================================

@Composable
private fun SignalCard(
    title: String,
    name: String,
    dbm: Int?,
    percent: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier
) {

    val valid =
        dbm != null &&
            SignalUtils.isValid(dbm)

    Card(

        modifier = modifier,

        shape =
            RoundedCornerShape(22.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceDark
            )
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconBadge(
                    icon = icon,
                    color = Blue
                )

                Spacer(
                    Modifier.width(9.dp)
                )

                Text(
                    title,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        TextPrimary
                )
            }

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                name,
                maxLines = 1,
                color =
                    TextSecondary,
                fontSize =
                    12.sp
            )

            Text(
                if (valid)
                    "$dbm dBm"
                else
                    "Unavailable",
                fontSize =
                    20.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            SignalBar(
                percent =
                    percent
            )

            Spacer(
                Modifier.height(5.dp)
            )

            Text(
                if (valid)
                    "${percent}% • ${
                        SignalUtils.quality(dbm!!)
                    }"
                else
                    "Signal unavailable",
                color =
                    TextSecondary,
                fontSize =
                    11.sp
            )
        }
    }
}


// =============================================================
// INTERNET CARD
// =============================================================

@Composable
private fun InternetCard(
    u: UiState
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(24.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceDark
            )
    ) {

        Column(
            Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconBadge(
                    icon =
                        Icons.Default.Language,
                    color =
                        Green
                )

                Spacer(
                    Modifier.width(12.dp)
                )

                Column {

                    Text(
                        "INTERNET",
                        color =
                            TextSecondary,
                        fontSize =
                            11.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        if (u.internetConnected)
                            "Connected"
                        else
                            "No Internet",
                        color =
                            if (u.internetConnected)
                                Green
                            else
                                Color(0xFFFF667A),
                        fontSize =
                            17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }

            Spacer(
                Modifier.height(16.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                MiniValue(
                    title = "DOWNLOAD",
                    value =
                        "${formatNumber(u.speed)} Mbps"
                )

                MiniValue(
                    title = "UPLOAD",
                    value =
                        "${formatNumber(u.upload)} Mbps"
                )

                MiniValue(
                    title = "PING",
                    value =
                        "${formatNumber(u.ping)} ms"
                )
            }
        }
    }
}


// =============================================================
// QUICK ACTIONS
// =============================================================

@Composable
private fun QuickActions(
    vm: MainViewModel,
    requestPermissions: () -> Unit
) {

    Column {

        Text(
            "Quick Actions",
            fontSize =
                16.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            ActionButton(
                icon =
                    Icons.Default.Wifi,
                text =
                    "Scan Wi-Fi",
                onClick =
                    vm::scanWifi,
                modifier =
                    Modifier.weight(1f)
            )

            ActionButton(
                icon =
                    Icons.Default.Speed,
                text =
                    "Speed Test",
                onClick =
                    vm::speedTest,
                modifier =
                    Modifier.weight(1f)
            )

            ActionButton(
                icon =
                    Icons.Default.ViewInAr,
                text =
                    "AR Scan",
                onClick =
                    requestPermissions,
                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}


// =============================================================
// WIFI ANALYZER
// =============================================================

@Composable
private fun WifiAnalyzer(
    u: UiState,
    vm: MainViewModel,
    requestPermissions: () -> Unit,
    modifier: Modifier
) {

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize()
                .background(Background),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                16.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        item {

            ScreenTitle(
                title = "Wi-Fi Scan",
                subtitle =
                    "Find nearby wireless networks"
            )
        }

        item {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            SurfaceLight
                    )
            ) {

                Column(
                    Modifier.padding(18.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconBadge(
                            icon =
                                Icons.Default.Wifi,
                            color =
                                Cyan
                        )

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                u.wifiName,
                                fontSize =
                                    18.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                "${u.wifiDbm} dBm • ${
                                    SignalUtils.quality(
                                        u.wifiDbm
                                    )
                                }",
                                color =
                                    TextSecondary
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(15.dp)
                    )

                    SignalBar(
                        SignalUtils.percent(
                            u.wifiDbm
                        )
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        if (u.wifiFrequency > 0)
                            "${u.wifiFrequency} MHz"
                        else
                            "Frequency unavailable",
                        color =
                            TextSecondary,
                        fontSize =
                            12.sp
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Button(
                        onClick =
                            vm::scanWifi,
                        enabled =
                            !u.wifiScanning,
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Blue
                            )
                    ) {

                        if (u.wifiScanning) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(18.dp),
                                strokeWidth =
                                    2.dp,
                                color =
                                    Color.White
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )
                        }

                        Text(
                            if (u.wifiScanning)
                                "Scanning..."
                            else
                                "SCAN NEARBY WI-FI"
                        )
                    }

                    OutlinedButton(
                        onClick =
                            requestPermissions,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "CHECK PERMISSIONS"
                        )
                    }
                }
            }
        }

        item {

            Text(
                "Nearby Networks",
                fontSize =
                    17.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        if (u.wifiNetworks.isEmpty()) {

            item {

                EmptyState(
                    icon =
                        Icons.Default.WifiFind,
                    text =
                        "No scan results yet.\nTap Scan Nearby Wi-Fi to discover networks."
                )
            }

        } else {

            items(
                u.wifiNetworks,
                key = {
                    it.bssid
                }
            ) { network ->

                NetworkRow(
                    ssid =
                        network.ssid,
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
// SPEED SCREEN
// =============================================================

@Composable
private fun SpeedScreen(
    u: UiState,
    vm: MainViewModel,
    modifier: Modifier
) {

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize()
                .background(Background),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                16.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        item {

            ScreenTitle(
                title = "Speed Test",
                subtitle =
                    "Measure your actual Internet performance"
            )
        }

        item {

            SpeedGauge(
                speed =
                    u.speed,
                testing =
                    u.testing
            )
        }

        item {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                MetricCard(
                    icon =
                        Icons.Default.ArrowDownward,
                    title =
                        "Download",
                    value =
                        "${formatNumber(u.speed)} Mbps",
                    color =
                        Cyan,
                    modifier =
                        Modifier.weight(1f)
                )

                MetricCard(
                    icon =
                        Icons.Default.ArrowUpward,
                    title =
                        "Upload",
                    value =
                        "${formatNumber(u.upload)} Mbps",
                    color =
                        Purple,
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        item {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                MetricCard(
                    icon =
                        Icons.Default.NetworkCheck,
                    title =
                        "Ping",
                    value =
                        "${formatNumber(u.ping)} ms",
                    color =
                        Green,
                    modifier =
                        Modifier.weight(1f)
                )

                MetricCard(
                    icon =
                        Icons.Default.Speed,
                    title =
                        "Jitter",
                    value =
                        "${formatNumber(u.jitter)} ms",
                    color =
                        Color(0xFFFFC857),
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }

        item {

            Button(
                onClick =
                    vm::speedTest,
                enabled =
                    !u.testing,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                shape =
                    RoundedCornerShape(18.dp)
            ) {

                Text(
                    if (u.testing)
                        "TESTING..."
                    else
                        "START SPEED TEST",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


// =============================================================
// SPEED GAUGE
// =============================================================

@Composable
private fun SpeedGauge(
    speed: Double,
    testing: Boolean
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(28.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceLight
            )
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator(
                    progress = {
                        (speed / 200.0)
                            .coerceIn(
                                0.0,
                                1.0
                            )
                            .toFloat()
                    },
                    modifier =
                        Modifier.size(190.dp),
                    strokeWidth =
                        14.dp,
                    color =
                        Cyan,
                    trackColor =
                        Color(0xFF1B2B40)
                )

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        formatNumber(speed),
                        fontSize =
                            36.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "Mbps",
                        color =
                            TextSecondary
                    )
                }
            }

            Spacer(
                Modifier.height(15.dp)
            )

            Text(
                if (testing)
                    "Testing..."
                else
                    "Ready",
                color =
                    if (testing)
                        Cyan
                    else
                        Green,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// =============================================================
// AR SCREEN
// =============================================================

@Composable
private fun ArScreen(
    u: UiState,
    vm: MainViewModel,
    permissionVersion: Int,
    requestPermissions: () -> Unit,
    modifier: Modifier
) {

    val context =
        LocalContext.current

    val lifecycle =
        LocalLifecycleOwner.current

    var cameraView by
            remember {
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
        modifier =
            modifier.fillMaxSize()
    ) {

        AndroidView(

            factory = {

                ArCameraView(
                    context = context,

                    onSample = {
                            x,
                            y,
                            z,
                            dbm ->

                        vm.arSample(
                            x,
                            y,
                            z,
                            dbm
                        )
                    },

                    onStatus = {
                        vm.arStatus(it)
                    }
                ).also {

                    cameraView = it
                }
            },

            modifier =
                Modifier.fillMaxSize()
        )

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.Bottom
        ) {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(26.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xDD07101C)
                    )
            ) {

                Column(
                    Modifier.padding(18.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconBadge(
                            icon =
                                Icons.Default.ViewInAr,
                            color =
                                Green
                        )

                        Spacer(
                            Modifier.width(10.dp)
                        )

                        Column {

                            Text(
                                "AR ROOM SCAN",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                u.arStatus,
                                color =
                                    TextSecondary,
                                fontSize =
                                    12.sp
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            "Samples: ${u.arSamples}",
                            color =
                                TextSecondary
                        )

                        Text(
                            "Wi-Fi: ${u.wifiDbm} dBm",
                            color =
                                Cyan
                        )
                    }

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick =
                                requestPermissions,
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text("START")
                        }

                        OutlinedButton(
                            onClick = {

                                cameraView?.pauseAr()

                                vm.arStatus(
                                    "Scan paused"
                                )
                            },
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text("PAUSE")
                        }
                    }
                }
            }
        }
    }
}


// =============================================================
// HISTORY
// =============================================================

@Composable
private fun HistoryScreen(
    vm: MainViewModel,
    modifier: Modifier
) {

    val history by
            vm.history.collectAsState(
                initial = emptyList()
            )

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize()
                .background(Background),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                16.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        item {

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        "History",
                        fontSize =
                            27.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        "${history.size} measurements",
                        color =
                            TextSecondary
                    )
                }

                IconButton(
                    onClick =
                        vm::clearHistory
                ) {

                    Icon(
                        Icons.Default.Delete,
                        contentDescription =
                            "Clear history",
                        tint =
                            Color(0xFFFF667A)
                    )
                }
            }
        }

        if (history.isEmpty()) {

            item {

                EmptyState(
                    icon =
                        Icons.Default.History,
                    text =
                        "No measurements yet."
                )
            }

        } else {

            items(
                history,
                key = {
                    it.id
                }
            ) { item ->

                HistoryCard(item)
            }
        }
    }
}


// =============================================================
// HISTORY CARD
// =============================================================

@Composable
private fun HistoryCard(
    item: com.networkar.app.data.Measurement
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceDark
            )
    ) {

        Row(
            Modifier.padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconBadge(
                icon =
                    when (item.mode) {
                        "SPEED" ->
                            Icons.Default.Speed

                        "AR_SCAN" ->
                            Icons.Default.ViewInAr

                        else ->
                            Icons.Default.Wifi
                    },
                color =
                    when (item.mode) {
                        "SPEED" ->
                            Purple

                        "AR_SCAN" ->
                            Green

                        else ->
                            Cyan
                    }
            )

            Spacer(
                Modifier.width(12.dp)
            )

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    item.mode,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "${item.networkName} • ${item.networkType}",
                    color =
                        TextSecondary,
                    fontSize =
                        12.sp
                )

                Text(
                    "${item.signalDbm} dBm • ${item.quality}",
                    color =
                        TextSecondary,
                    fontSize =
                        12.sp
                )

                if (item.mode == "SPEED") {

                    Text(
                        "${formatNumber(item.speedMbps)} Mbps ↓  " +
                                "${formatNumber(item.uploadMbps)} Mbps ↑",
                        color =
                            Cyan,
                        fontSize =
                            12.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                if (item.mode == "AR_SCAN") {

                    Text(
                        "AR: " +
                                "${formatFloat(item.x)}, " +
                                "${formatFloat(item.y)}, " +
                                "${formatFloat(item.z)}",
                        color =
                            Green,
                        fontSize =
                            11.sp
                    )
                }
            }
        }
    }
}


// =============================================================
// COMMON COMPONENTS
// =============================================================

@Composable
private fun ScreenTitle(
    title: String,
    subtitle: String
) {

    Column {

        Text(
            title,
            fontSize =
                27.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            subtitle,
            color =
                TextSecondary,
            fontSize =
                13.sp
        )
    }
}


@Composable
private fun IconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {

    Box(

        modifier =
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    color.copy(
                        alpha = 0.16f
                    )
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier =
                Modifier.size(22.dp)
        )
    }
}


@Composable
private fun StatusPill(
    text: String,
    active: Boolean
) {

    Surface(

        shape =
            RoundedCornerShape(50),

        color =
            if (active)
                Green.copy(alpha = 0.15f)
            else
                Color(0xFFFF667A)
                    .copy(alpha = 0.15f)
    ) {

        Text(
            text,
            modifier =
                Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 5.dp
                ),
            color =
                if (active)
                    Green
                else
                    Color(0xFFFF667A),
            fontSize =
                11.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
private fun MiniValue(
    title: String,
    value: String
) {

    Column {

        Text(
            title,
            color =
                TextSecondary,
            fontSize =
                9.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            value,
            fontSize =
                12.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
private fun SignalBar(
    percent: Int
) {

    val p =
        percent.coerceIn(
            0,
            100
        )

    Box(

        modifier =
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(
                    Color(0xFF1D2B3D)
                )
    ) {

        Box(

            modifier =
                Modifier
                    .fillMaxWidth(
                        p / 100f
                    )
                    .height(7.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Blue,
                                Cyan,
                                Green
                            )
                        )
                    )
        )
    }
}


@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier
) {

    Card(

        modifier = modifier,

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceDark
            ),

        onClick = onClick
    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                icon,
                contentDescription = text,
                tint = Cyan,
                modifier =
                    Modifier.size(23.dp)
            )

            Spacer(
                Modifier.height(7.dp)
            )

            Text(
                text,
                fontSize =
                    10.sp,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}


@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier
) {

    Card(

        modifier = modifier,

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceDark
            )
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Icon(
                icon,
                contentDescription = title,
                tint = color
            )

            Spacer(
                Modifier.height(9.dp)
            )

            Text(
                title,
                color =
                    TextSecondary,
                fontSize =
                    11.sp
            )

            Text(
                value,
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


@Composable
private fun NetworkRow(
    ssid: String,
    rssi: Int,
    frequency: Int
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceDark
            )
    ) {

        Row(
            Modifier.padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconBadge(
                icon =
                    Icons.Default.Wifi,
                color =
                    if (rssi >= -67)
                        Green
                    else if (rssi >= -80)
                        Cyan
                    else
                        TextSecondary
            )

            Spacer(
                Modifier.width(12.dp)
            )

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    ssid,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "$frequency MHz",
                    color =
                        TextSecondary,
                    fontSize =
                        11.sp
                )
            }

            Text(
                "$rssi dBm",
                fontWeight =
                    FontWeight.Bold,
                color =
                    if (rssi >= -67)
                        Green
                    else
                        TextPrimary
            )
        }
    }
}


@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(24.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceDark
            )
    ) {

        Column(

            Modifier
                .fillMaxWidth()
                .padding(35.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                icon,
                contentDescription = null,
                tint =
                    TextSecondary,
                modifier =
                    Modifier.size(42.dp)
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                text,
                color =
                    TextSecondary,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}


// =============================================================
// FORMATTERS
// =============================================================

private fun formatNumber(
    value: Double
): String {

    return if (value == 0.0) {
        "0.0"
    } else {
        String.format(
            "%.1f",
            value
        )
    }
}


private fun formatFloat(
    value: Float?
): String {

    return value?.let {
        String.format(
            "%.2f",
            it
        )
    } ?: "-"
}
