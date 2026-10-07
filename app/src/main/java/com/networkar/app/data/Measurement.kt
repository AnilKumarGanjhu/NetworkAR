package com.networkar.app.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName="measurements")
data class Measurement(@PrimaryKey(autoGenerate=true) val id:Long=0,val timestamp:Long=System.currentTimeMillis(),val mode:String,val networkName:String,val networkType:String,val signalDbm:Int,val signalPercent:Int,val quality:String,val speedMbps:Double=0.0,val uploadMbps:Double=0.0,val pingMs:Double=0.0,val jitterMs:Double=0.0,val x:Float=0f,val y:Float=0f,val z:Float=0f)
