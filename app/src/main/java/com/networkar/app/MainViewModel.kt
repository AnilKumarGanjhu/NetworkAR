package com.networkar.app
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.networkar.app.data.*
import com.networkar.app.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(val wifiDbm:Int=-100,val wifiName:String="Wi-Fi",val mobileDbm:Int=-120,val mobileType:String="Mobile",val speed:Double=0.0,val upload:Double=0.0,val ping:Double=0.0,val jitter:Double=0.0,val testing:Boolean=false)
class MainViewModel(a:Application):AndroidViewModel(a){
 private val db=Room.databaseBuilder(a,AppDatabase::class.java,"networkar.db").build();private val w=WifiScanner(a);private val m=MobileNetworkScanner(a);private val t=SpeedTester()
 private val _ui=MutableStateFlow(UiState());val ui=_ui.asStateFlow();val history=db.measurementDao().all()
 fun refresh(){val wi=runCatching{w.current()}.getOrNull();val mo=runCatching{m.current()}.getOrNull();_ui.update{it.copy(wifiDbm=wi?.rssi?:-100,wifiName=wi?.ssid?:"Wi-Fi",mobileDbm=mo?.dbm?:-120,mobileType=mo?.type?:"Mobile")}}
 fun speedTest(){if(_ui.value.testing)return;viewModelScope.launch{_ui.update{it.copy(testing=true)};val r=runCatching{t.test()}.getOrDefault(SpeedResult(0.0,0.0,0.0,0.0));_ui.update{it.copy(speed=r.downloadMbps,upload=r.uploadMbps,ping=r.pingMs,jitter=r.jitterMs,testing=false)};val u=_ui.value;db.measurementDao().insert(Measurement(mode="SPEED",networkName=u.wifiName,networkType="Wi-Fi/Mobile",signalDbm=u.wifiDbm,signalPercent=SignalUtils.percent(u.wifiDbm),quality=SignalUtils.quality(u.wifiDbm),speedMbps=r.downloadMbps,pingMs=r.pingMs,jitterMs=r.jitterMs))}}
 fun sample(){refresh();val u=_ui.value;viewModelScope.launch{db.measurementDao().insert(Measurement(mode="SIGNAL",networkName=u.wifiName,networkType="Wi-Fi",signalDbm=u.wifiDbm,signalPercent=SignalUtils.percent(u.wifiDbm),quality=SignalUtils.quality(u.wifiDbm)))}}
 fun clearHistory(){viewModelScope.launch{db.measurementDao().clear()}}
}
