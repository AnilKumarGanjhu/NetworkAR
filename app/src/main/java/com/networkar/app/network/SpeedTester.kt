package com.networkar.app.network

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.math.abs
import kotlin.system.measureTimeMillis

 data class SpeedResult(val downloadMbps:Double,val uploadMbps:Double,val pingMs:Double,val jitterMs:Double)

class SpeedTester{
    suspend fun test():SpeedResult{
        val p=mutableListOf<Long>()
        repeat(4){
            p += measureTimeMillis { runCatching { InetAddress.getByName("1.1.1.1").isReachable(1500) } }
        }
        val ping=p.average()
        val jitter=p.zipWithNext().map{abs(it.first-it.second).toDouble()}.average()

        val downloadBytes=runCatching{
            URL("https://speed.cloudflare.com/__down?bytes=5000000").openStream().use{it.readBytes().size}
        }.getOrDefault(0)
        val downloadMbps=if(downloadBytes>0) downloadBytes*8.0/1_000_000.0/2.5 else 0.0

        val uploadBytes=2_000_000
        val payload=ByteArray(uploadBytes)
        val start=System.nanoTime()
        val uploaded=runCatching{
            val c=URL("https://speed.cloudflare.com/__up").openConnection() as HttpURLConnection
            c.requestMethod="POST"
            c.doOutput=true
            c.setRequestProperty("Content-Type","application/octet-stream")
            c.setFixedLengthStreamingMode(uploadBytes)
            c.outputStream.use{it.write(payload)}
            c.responseCode in 200..299
        }.getOrDefault(false)
        val elapsed=(System.nanoTime()-start)/1_000_000_000.0
        val uploadMbps=if(uploaded && elapsed>0) uploadBytes*8.0/1_000_000.0/elapsed else 0.0

        return SpeedResult(downloadMbps,uploadMbps,ping,jitter)
    }
}
