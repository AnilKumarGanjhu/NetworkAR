package com.networkar.app.data
import androidx.room.*
@Database(entities=[Measurement::class],version=1,exportSchema=false)
abstract class AppDatabase:RoomDatabase(){abstract fun measurementDao():MeasurementDao}
