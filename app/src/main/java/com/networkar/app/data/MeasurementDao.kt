package com.networkar.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {

    /**
     * Insert a new measurement.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: Measurement)

    /**
     * Insert multiple measurements.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(
        measurements: List<Measurement>
    )

    /**
     * Get complete measurement history.
     * Latest measurements first.
     */
    @Query(
        """
        SELECT *
        FROM measurements
        ORDER BY timestamp DESC
        """
    )
    fun getAll(): Flow<List<Measurement>>

    /**
     * Get latest measurements.
     */
    @Query(
        """
        SELECT *
        FROM measurements
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    suspend fun getLatest(
        limit: Int
    ): List<Measurement>

    /**
     * Get measurements by mode.
     *
     * Examples:
     * SAMPLE
     * SPEED
     * AR_SCAN
     */
    @Query(
        """
        SELECT *
        FROM measurements
        WHERE mode = :mode
        ORDER BY timestamp DESC
        """
    )
    fun getByMode(
        mode: String
    ): Flow<List<Measurement>>

    /**
     * Get AR room scan history.
     */
    @Query(
        """
        SELECT *
        FROM measurements
        WHERE mode = 'AR_SCAN'
        ORDER BY timestamp ASC
        """
    )
    fun getArScanHistory(): Flow<List<Measurement>>

    /**
     * Get speed-test history.
     */
    @Query(
        """
        SELECT *
        FROM measurements
        WHERE mode = 'SPEED'
        ORDER BY timestamp DESC
        """
    )
    fun getSpeedHistory(): Flow<List<Measurement>>

    /**
     * Delete one measurement.
     */
    @Delete
    suspend fun delete(
        measurement: Measurement
    )

    /**
     * Delete complete history.
     */
    @Query("DELETE FROM measurements")
    suspend fun deleteAll()

    /**
     * Count all measurements.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM measurements
        """
    )
    suspend fun count(): Int
}
