package com.networkar.app.network
import android.content.Context
import android.net.wifi.WifiManager
data class WifiInfo(val ssid:String,val bssid:String,val rssi:Int,val frequency:Int)
class WifiScanner(private val c:Context){fun current():WifiInfo?{val w=c.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager;val i=w.connectionInfo?:return null;val s=i.ssid?.trim('"')?.takeIf{it.isNotBlank()&&it!="<unknown ssid>"}?:"Unknown Wi-Fi";return WifiInfo(s,i.bssid?:"",i.rssi,i.frequency)}}
