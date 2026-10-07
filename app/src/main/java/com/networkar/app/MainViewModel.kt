package com.networkar.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.networkar.app.data.*
import com.networkar.app.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class UiState(
    val wifiDbm:Int=-100,
    val wifiName:String="Wi-Fi",
    val wifiFrequency:Int=0,
    val wifiBssid:String="",
    val mobileDbm:Int=-120,
    val mobileType:String="Mobile",
    val speed:Double=0.0,
    val upload:Double=0.0,
    val ping:Double=0.0,
    val jitter:Double=0.0,
    val testing:Boolean=false,
    val wifiScanning:Boolean=false,
    val wifiNetworks:List<WifiInfo> = emptyList(),
    val arSamples:Int=0,
    val arStatus:String="Ready"
)

class MainViewModel(a:Application):AndroidViewModel(a){
    private val db= Room.databaseBuilder(a,AppDatabase::class.java,"networkar.db").fallbackToDestructiveMigration().build()
    private val w= WifiScanner(a)
    private val m= MobileNetworkScanner(a)
    private val t= SpeedTester()
    private val _ui= MutableStateFlow(UiState())
    val ui=_ui.asStateFlow()
    val history=db.measurementDao().all()

    fun refresh(){
        val wi=runCatching{w.current()}.getOrNull()
        val mo=runCatching{m.current()}.getOrNull()
        _ui.update{it.copy(
            wifiDbm=wi?.rssi?:-100,
            wifiName=wi?.ssid?:"Wi-Fi",
            wifiFrequency=wi?.frequency?:0,
            wifiBssid=wi?.bssid?:"",
            mobileDbm=mo?.dbm?:-120,
            mobileType=mo?.type?:"Mobile"
        )}
    }

    fun scanWifi(){
        if(_ui.value.wifiScanning)return
        viewModelScope.launch{
            _ui.update{it.copy(wifiScanning=true)}
            val results=runCatching{w.scan()}.getOrDefault(emptyList())
            _ui.update{it.copy(wifiNetworks=results,wifiScanning=false)}
        }
    }

    fun speedTest(){
        if(_ui.value.testing)return
        viewModelScope.launch{
            _ui.update{it.copy(testing=true)}
            val r=runCatching{t.test()}.getOrDefault(SpeedResult(0.0,0.0,0.0,0.0))
            _ui.update{it.copy(speed=r.downloadMbps,upload=r.uploadMbps,ping=r.pingMs,jitter=r.jitterMs,testing=false)}
            val u=_ui.value
            db.measurementDao().insert(Measurement(mode="SPEED",networkName=u.wifiName,networkType="Wi-Fi/Mobile",signalDbm=u.wifiDbm,signalPercent=SignalUtils.percent(u.wifiDbm),quality=SignalUtils.quality(u.wifiDbm),speedMbps=r.downloadMbps,uploadMbps=r.uploadMbps,pingMs=r.pingMs,jitterMs=r.jitterMs))
        }
    }

    fun sample(){
        refresh()
        val u=_ui.value
        viewModelScope.launch{db.measurementDao().insert(Measurement(mode="SIGNAL",networkName=u.wifiName,networkType="Wi-Fi",signalDbm=u.wifiDbm,signalPercent=SignalUtils.percent(u.wifiDbm),quality=SignalUtils.quality(u.wifiDbm)))}
    }

    fun arSample(x:Float,y:Float,z:Float,dbm:Int){
        val quality=SignalUtils.quality(dbm)
        val u=_ui.value
        _ui.update{it.copy(wifiDbm=dbm,arSamples=it.arSamples+1,arStatus="Scanning • ${it.arSamples+1} points")}
        viewModelScope.launch{
            db.measurementDao().insert(Measurement(mode="AR_SCAN",networkName=u.wifiName,networkType="Wi-Fi",signalDbm=dbm,signalPercent=SignalUtils.percent(dbm),quality=quality,x=x,y=y,z=z))
        }
    }

    fun arStatus(status:String){_ui.update{it.copy(arStatus=status)}}

    fun resetArCount(){_ui.update{it.copy(arSamples=0,arStatus="Ready")}}

    fun clearHistory(){viewModelScope.launch{db.measurementDao().clear();resetArCount()}}
}
