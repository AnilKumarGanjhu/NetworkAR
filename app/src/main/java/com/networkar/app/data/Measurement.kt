package com.networkar.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class Measurement(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Measurement type:
    // SAMPLE, SPEED, AR_SCAN
    val mode: String,

    // Network information
    val networkName: String,
    val networkType: String,

    // Signal information
    val signalDbm: Int,
    val signalPercent: Int,
    val quality: String,

    // Speed test information
    val speedMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val pingMs: Long = 0L,
    val jitterMs: Long = 0L,

    // AR room-scan position
    val x: Float? = null,
    val y: Float? = null,
    val z: Float? = null,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
)
