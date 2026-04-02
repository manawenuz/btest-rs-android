package com.btestrs.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TestRunDao {

    @Insert
    suspend fun insertRun(run: TestRunEntity): Long

    @Insert
    suspend fun insertIntervals(intervals: List<TestIntervalEntity>)

    @Transaction
    suspend fun insertRunWithIntervals(run: TestRunEntity, intervals: List<TestIntervalEntity>) {
        val runId = insertRun(run)
        insertIntervals(intervals.map { it.copy(runId = runId) })
    }

    @Query("SELECT * FROM test_runs ORDER BY timestamp DESC")
    fun getAllRuns(): Flow<List<TestRunEntity>>

    @Query("SELECT * FROM test_intervals WHERE runId = :runId ORDER BY intervalSec, direction")
    suspend fun getIntervalsForRun(runId: Long): List<TestIntervalEntity>

    @Query("SELECT * FROM test_intervals WHERE runId IN (:runIds) ORDER BY runId, intervalSec, direction")
    suspend fun getIntervalsForRuns(runIds: List<Long>): List<TestIntervalEntity>

    @Query("SELECT * FROM test_runs WHERE id IN (:ids)")
    suspend fun getRunsByIds(ids: List<Long>): List<TestRunEntity>

    @Query("DELETE FROM test_runs WHERE id IN (:ids)")
    suspend fun deleteRunsByIds(ids: List<Long>)

    @Query("SELECT * FROM test_runs WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedRuns(): List<TestRunEntity>

    @Query("UPDATE test_runs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
