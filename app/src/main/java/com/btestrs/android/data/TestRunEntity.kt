package com.btestrs.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_runs")
data class TestRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val server: String,
    val protocol: String,
    val direction: String,
    val durationSec: Int,
    val txAvgMbps: Double,
    val rxAvgMbps: Double,
    val txBytes: Long,
    val rxBytes: Long,
    val lost: Long,
    val synced: Boolean = false
)
