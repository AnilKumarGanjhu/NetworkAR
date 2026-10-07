package com.networkar.app.network
import android.content.Context
import android.telephony.TelephonyManager
data class MobileInfo(val type:String,val dbm:Int)
class MobileNetworkScanner(private val c:Context){fun current():MobileInfo{val t=c.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager;val s=t.signalStrength;val d=s?.cellSignalStrengths?.firstOrNull()?.dbm?:-120;val n=when(t.dataNetworkType){TelephonyManager.NETWORK_TYPE_NR->"5G";TelephonyManager.NETWORK_TYPE_LTE->"4G LTE";TelephonyManager.NETWORK_TYPE_HSPAP,TelephonyManager.NETWORK_TYPE_UMTS->"3G";else->"Mobile"};return MobileInfo(n,d)}}
