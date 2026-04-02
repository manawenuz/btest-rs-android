package com.btestrs.android.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_intervals",
    foreignKeys = [ForeignKey(
        entity = TestRunEntity::class,
        parentColumns = ["id"],
        childColumns = ["runId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("runId")]
)
data class TestIntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val intervalSec: Int,
    val direction: String,
    val speedMbps: Double,
    val bytes: Long,
    val localCpu: Int?,
    val remoteCpu: Int?,
    val lost: Long?
)
