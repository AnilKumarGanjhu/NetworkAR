package com.networkar.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.networkar.app.ar.ArCameraView
import com.networkar.app.network.SignalUtils

class MainActivity:ComponentActivity(){
    private var permissionVersion by mutableIntStateOf(0)
    private val permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        permissionVersion++
    }

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        setContent{
            val version=permissionVersion
            NetworkARApp(
                vm=viewModel(),
                permissionVersion=version,
                requestPermissions={requestNetworkPermissions()}
            )
        }
    }

    private fun requestNetworkPermissions(){
        val permissions=buildList{
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.READ_PHONE_STATE)
            if(Build.VERSION.SDK_INT>=33) add("android.permission.NEARBY_WIFI_DEVICES")
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@Composable
fun NetworkARApp(
    vm:MainViewModel=viewModel(),
    permissionVersion:Int=0,
    requestPermissions:()->Unit={}
){
    val u by vm.ui.collectAsState()
    var tab by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){vm.refresh()}
    MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF7C5CFF),secondary=Color(0xFF20D6A7),background=Color(0xFF090B10),surface=Color(0xFF131722))){
        Scaffold(bottomBar={
            NavigationBar{
                listOf("Home","Speed","Analyzer","AR Scan","History").forEachIndexed{i,n->
                    NavigationBarItem(selected=tab==i,onClick={tab=i},icon={
                        Icon(when(i){0->Icons.Default.Home;1->Icons.Default.Speed;2->Icons.Default.Wifi;3->Icons.Default.ViewInAr;else->Icons.Default.History},n)
                    },label={Text(n)})
                }
            }
        }){p->
            when(tab){
                0->Home(u,vm,requestPermissions,Modifier.padding(p))
                1->Speed(u,vm,Modifier.padding(p))
                2->Analyzer(u,vm,requestPermissions,Modifier.padding(p))
                3->Ar(u,vm,permissionVersion,requestPermissions,Modifier.padding(p))
                4->History(vm,Modifier.padding(p))
            }
        }
    }
}

@Composable fun BoxCard(c:@Composable ColumnScope.()->Unit){
    Card(shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth().padding(vertical=7.dp)){
        Column(Modifier.padding(18.dp),content=c)
    }
}

@Composable fun Home(u:UiState,v:MainViewModel,requestPermissions:()->Unit,m:Modifier)=Column(m.verticalScroll(rememberScrollState()).padding(18.dp)){
    Text("NetScope",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
    Text("Wi-Fi + 4G/5G + AR room signal scanner",color=Color.Gray)
    BoxCard{
        Text("CURRENT CONNECTION",color=Color.Gray)
        Text(u.wifiName,fontWeight=FontWeight.Bold)
        Text("${u.wifiDbm} dBm • ${SignalUtils.quality(u.wifiDbm)}",style=MaterialTheme.typography.titleLarge)
        Text("${u.mobileType}: ${u.mobileDbm} dBm")
        Text("Frequency: ${if(u.wifiFrequency>0) "${u.wifiFrequency} MHz" else "Unavailable"}",color=Color.Gray)
    }
    BoxCard{
        Text("INTERNET",color=Color.Gray)
        Text("${u.speed} Mbps",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Text("Ping ${u.ping} ms • Jitter ${u.jitter} ms")
        Button(onClick={v.speedTest()},enabled=!u.testing,modifier=Modifier.fillMaxWidth()){
            Text(if(u.testing)"TESTING..." else "START SPEED TEST")
        }
    }
    BoxCard{
        Text("ROOM SIGNAL SCAN",fontWeight=FontWeight.Bold)
        Text("Use the phone camera and ARCore to walk through the room and record signal points.",color=Color.Gray)
        Button(onClick={requestPermissions,modifier=Modifier.fillMaxWidth()}){Text("ALLOW CAMERA & NETWORK ACCESS")}
    }
}

@Composable fun Speed(u:UiState,v:MainViewModel,m:Modifier)=Column(m.padding(18.dp)){
    Text("Speed Test",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
    BoxCard{
        Text("DOWNLOAD",color=Color.Gray);Text("${u.speed} Mbps",style=MaterialTheme.typography.headlineLarge)
        Text("UPLOAD ${u.upload} Mbps");Text("PING ${u.ping} ms");Text("JITTER ${u.jitter} ms")
        Button(onClick={v.speedTest()},enabled=!u.testing,modifier=Modifier.fillMaxWidth()){Text(if(u.testing)"Testing..." else "Run Test")}
    }
}

@Composable fun Analyzer(u:UiState,v:MainViewModel,requestPermissions:()->Unit,m:Modifier)=Column(m.padding(18.dp)){
    Text("Wi-Fi Analyzer",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
    BoxCard{
        Text("CONNECTED NETWORK",color=Color.Gray);Text(u.wifiName,fontWeight=FontWeight.Bold)
        Text("RSSI: ${u.wifiDbm} dBm");Text("Quality: ${SignalUtils.quality(u.wifiDbm)}")
        Text("Frequency: ${if(u.wifiFrequency>0) "${u.wifiFrequency} MHz" else "Unavailable"}")
        Button(onClick={v.scanWifi()},enabled=!u.wifiScanning,modifier=Modifier.fillMaxWidth()){Text(if(u.wifiScanning)"Scanning..." else "SCAN NEARBY WI-FI")}
        OutlinedButton(onClick=requestPermissions,modifier=Modifier.fillMaxWidth()){Text("CHECK PERMISSIONS")}
    }
    BoxCard{
        Text("Wi-Fi vs Mobile",fontWeight=FontWeight.Bold)
        Text("Wi-Fi: ${u.wifiDbm} dBm")
        Text("${u.mobileType}: ${u.mobileDbm} dBm")
        Text(if(u.wifiDbm>u.mobileDbm)"Current signal: Wi-Fi" else "Current signal: Mobile")
    }
    Text("Nearby networks",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
    if(u.wifiNetworks.isEmpty()) Text("No scan results yet. Tap Scan Nearby Wi-Fi and allow Location/Nearby devices.",color=Color.Gray,modifier=Modifier.padding(vertical=12.dp))
    else LazyColumn(modifier=Modifier.fillMaxHeight()){
        items(u.wifiNetworks,key={it.bssid}){n->
            Card(modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)){
                Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween){
                    Column(Modifier.weight(1f)){Text(n.ssid,fontWeight=FontWeight.Bold);Text("${n.frequency} MHz • ${n.bssid}",color=Color.Gray)}
                    Text("${n.rssi} dBm",fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable fun Ar(u:UiState,v:MainViewModel,permissionVersion:Int,requestPermissions:()->Unit,m:Modifier){
    val context=LocalContext.current
    val lifecycle= androidx.lifecycle.compose.LocalLifecycleOwner.current
    var cameraView by remember { mutableStateOf<ArCameraView?>(null) }
    DisposableEffect(lifecycle,permissionVersion){
        val observer=object:DefaultLifecycleObserver{
            override fun onResume(owner:LifecycleOwner){cameraView?.resumeAr()}
            override fun onPause(owner:LifecycleOwner){cameraView?.pauseAr()}
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose{lifecycle.lifecycle.removeObserver(observer);cameraView?.closeAr()}
    }
    Box(m.fillMaxSize()){
        AndroidView(factory={
            ArCameraView(context,onSample={x,y,z,dbm->v.arSample(x,y,z,dbm)},onStatus={v.arStatus(it)}).also{cameraView=it}
        },modifier=Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.Bottom){
            Card(colors=CardDefaults.cardColors(containerColor=Color.Black.copy(alpha=.72f)),shape=RoundedCornerShape(22.dp)){
                Column(Modifier.padding(16.dp)){
                    Text("AR ROOM SCAN",fontWeight=FontWeight.Bold)
                    Text(u.arStatus,color=Color.White)
                    Text("Samples: ${u.arSamples}  •  Wi-Fi: ${u.wifiDbm} dBm",color=Color.LightGray)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Button(onClick=requestPermissions,modifier=Modifier.weight(1f)){Text("START / RETRY")}
                        OutlinedButton(onClick={cameraView?.pauseAr();v.arStatus("Scan paused")},modifier=Modifier.weight(1f)){Text("PAUSE")}
                    }
                }
            }
        }
    }
}

@Composable fun History(v:MainViewModel,m:Modifier){
    val h by v.history.collectAsState(initial=emptyList())
    Column(m.padding(18.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text("History",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            TextButton(onClick={v.clearHistory()}){Text("Clear")}
        }
        if(h.isEmpty()) Text("No measurements yet.") else LazyColumn{
            items(h,key={it.id}){item->
                BoxCard{
                    Text(item.mode,fontWeight=FontWeight.Bold)
                    Text("${item.networkName} • ${item.signalDbm} dBm • ${item.quality}")
                    if(item.mode=="AR_SCAN") Text("Position: ${"%.2f".format(item.x)}, ${"%.2f".format(item.y)}, ${"%.2f".format(item.z)}")
                    if(item.speedMbps>0)Text("${item.speedMbps} Mbps • ping ${item.pingMs} ms • upload ${item.uploadMbps} Mbps")
                }
            }
        }
    }
}
