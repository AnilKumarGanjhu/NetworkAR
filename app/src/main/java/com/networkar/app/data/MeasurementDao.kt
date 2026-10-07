package com.networkar.app.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao interface MeasurementDao { @Insert suspend fun insert(m:Measurement); @Query("SELECT * FROM measurements ORDER BY timestamp DESC") fun all():Flow<List<Measurement>>; @Query("DELETE FROM measurements") suspend fun clear() }
