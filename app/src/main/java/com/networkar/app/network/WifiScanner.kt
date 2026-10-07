package com.networkar.app.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

 data class WifiInfo(val ssid:String,val bssid:String,val rssi:Int,val frequency:Int)

class WifiScanner(private val c:Context){
    private val manager get() = c.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun current():WifiInfo?{
        val i=manager.connectionInfo?:return null
        val s=i.ssid?.trim('"')?.takeIf{it.isNotBlank()&&it!="<unknown ssid>"}? :"Unknown Wi-Fi"
        return WifiInfo(s,i.bssid? :"",i.rssi,i.frequency)
    }

    fun scan():List<WifiInfo>{
        val fine=ContextCompat.checkSelfPermission(c,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        if(!fine || !manager.isWifiEnabled) return emptyList()
        runCatching { manager.startScan() }
        return runCatching { manager.scanResults.map{
            WifiInfo(it.SSID.ifBlank{"Hidden network"},it.BSSID ?: "",it.level,it.frequency)
        }.sortedByDescending{it.rssi} }.getOrDefault(emptyList())
    }
}
