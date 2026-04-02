package com.btestrs.android

data class BtestResult(
    val intervalSec: Int,
    val direction: String, // "TX" or "RX"
    val speedMbps: Double,
    val bytes: Long,
    val localCpu: Int? = null,
    val remoteCpu: Int? = null,
    val lost: Long? = null
)

data class BtestSummary(
    val peer: String,
    val proto: String,
    val dir: String,
    val durationSec: Int,
    val txAvgMbps: Double,
    val rxAvgMbps: Double,
    val txBytes: Long,
    val rxBytes: Long,
    val lost: Long
)
