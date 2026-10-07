package com.networkar.app
import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.networkar.app.network.SignalUtils

class MainActivity:ComponentActivity(){private val req=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){};override fun onCreate(b:Bundle?){super.onCreate(b);req.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.READ_PHONE_STATE,Manifest.permission.CAMERA));setContent{NetworkARApp()}}}
@Composable fun NetworkARApp(vm:MainViewModel=viewModel()){val u by vm.ui.collectAsState();var tab by remember{mutableIntStateOf(0)};LaunchedEffect(Unit){vm.refresh()};MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF7C5CFF),secondary=Color(0xFF20D6A7),background=Color(0xFF090B10),surface=Color(0xFF131722))){Scaffold(bottomBar={NavigationBar{listOf("Home","Speed","Analyzer","AR","History").forEachIndexed{i,n->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Icon(if(i==0)Icons.Default.Home else if(i==4)Icons.Default.History else if(i==3)Icons.Default.ViewInAr else Icons.Default.Speed,n)},label={Text(n)})}}}){p->when(tab){0->Home(u,vm,Modifier.padding(p));1->Speed(u,vm,Modifier.padding(p));2->Analyzer(u,Modifier.padding(p));3->Ar(u,Modifier.padding(p));4->History(vm,Modifier.padding(p))}}}}
@Composable fun BoxCard(c:@Composable ColumnScope.()->Unit){Card(shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth().padding(vertical=7.dp)){Column(Modifier.padding(18.dp),content=c)}}
@Composable fun Home(u:UiState,v:MainViewModel,m:Modifier)=Column(m.verticalScroll(rememberScrollState()).padding(18.dp)){Text("NetworkAR",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold);Text("Network Analyzer + AR Heatmap",color=Color.Gray);BoxCard{Text("Wi-Fi: ${u.wifiName}");Text("${u.wifiDbm} dBm • ${SignalUtils.quality(u.wifiDbm)}");Text("${u.mobileType}: ${u.mobileDbm} dBm")};BoxCard{Text("INTERNET",color=Color.Gray);Text("${u.speed} Mbps",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Ping ${u.ping} ms • Jitter ${u.jitter} ms");Button(onClick={v.speedTest()},enabled=!u.testing,modifier=Modifier.fillMaxWidth()){Text(if(u.testing)"TESTING..." else "START SPEED TEST")}};BoxCard{Text("SIGNAL MODE");Text("Save signal points while moving around your room.");Button(onClick={v.sample()},modifier=Modifier.fillMaxWidth()){Text("SAVE CURRENT SIGNAL")}}}
@Composable fun Speed(u:UiState,v:MainViewModel,m:Modifier)=Column(m.padding(18.dp)){Text("Speed Test",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);BoxCard{Text("DOWNLOAD");Text("${u.speed} Mbps",style=MaterialTheme.typography.headlineLarge);Text("UPLOAD ${u.upload} Mbps");Text("PING ${u.ping} ms");Text("JITTER ${u.jitter} ms");Button(onClick={v.speedTest()},enabled=!u.testing){Text(if(u.testing)"Testing..." else "Run Test")}}}
@Composable fun Analyzer(u:UiState,m:Modifier)=Column(m.padding(18.dp)){Text("Wi-Fi Analyzer",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);BoxCard{Text(u.wifiName,fontWeight=FontWeight.Bold);Text("RSSI: ${u.wifiDbm} dBm");Text("Quality: ${SignalUtils.quality(u.wifiDbm)}")};BoxCard{Text("Wi-Fi vs Mobile",fontWeight=FontWeight.Bold);Text("Wi-Fi: ${u.wifiDbm} dBm");Text("${u.mobileType}: ${u.mobileDbm} dBm");Text(if(u.wifiDbm>u.mobileDbm)"Current signal: Wi-Fi" else "Current signal: Mobile")}}
@Composable fun Ar(u:UiState,m:Modifier)=Column(m.padding(18.dp)){Text("AR Signal Heatmap",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);BoxCard{Text("🟢 STRONG   🟡 MEDIUM   🔴 WEAK");Text("ARCore + heatmap engine foundation included.");Text("Move through the room and save measurements to build your map.");Button(onClick={}){Text("START AR SIGNAL MODE")}}}
@Composable fun History(v:MainViewModel,m:Modifier){val h by v.history.collectAsState(initial=emptyList());Column(m.padding(18.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("History",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);TextButton(onClick={v.clearHistory()}){Text("Clear")}};if(h.isEmpty())Text("No measurements yet.") else Column(Modifier.verticalScroll(rememberScrollState())){h.forEach{BoxCard{Text(it.mode,fontWeight=FontWeight.Bold);Text("${it.networkName} • ${it.signalDbm} dBm • ${it.quality}");if(it.speedMbps>0)Text("${it.speedMbps} Mbps • ping ${it.pingMs} ms")}}}}}
