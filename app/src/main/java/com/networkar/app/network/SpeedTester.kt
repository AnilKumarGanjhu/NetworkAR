package com.networkar.app.network
import java.net.InetAddress
import java.net.URL
import kotlin.math.abs
import kotlin.system.measureTimeMillis
data class SpeedResult(val downloadMbps:Double,val uploadMbps:Double,val pingMs:Double,val jitterMs:Double)
class SpeedTester{suspend fun test():SpeedResult{val p=mutableListOf<Long>();repeat(3){p+=measureTimeMillis{InetAddress.getByName("1.1.1.1").isReachable(1500)}};val ping=p.average();val jitter=p.zipWithNext().map{abs(it.first-it.second).toDouble()}.average();val bytes=runCatching{URL("https://speed.cloudflare.com/__down?bytes=3000000").openStream().use{it.readBytes().size}}.getOrDefault(0);val down=if(bytes>0)bytes*8.0/1_000_000.0/2.0 else 0.0;return SpeedResult(down,0.0,ping,jitter)}}
