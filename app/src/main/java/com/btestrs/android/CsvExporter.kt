package com.btestrs.android

import com.btestrs.android.data.TestIntervalEntity
import com.btestrs.android.data.TestRunEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CsvExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun export(
        runs: List<TestRunEntity>,
        intervalsByRun: Map<Long, List<TestIntervalEntity>>
    ): String {
        val sb = StringBuilder()

        sb.appendLine("# btest-android export")
        sb.appendLine("# exported: ${isoFormat.format(Date())}")
        sb.appendLine("# runs: ${runs.size}")
        sb.appendLine("#")
        sb.appendLine("# SECTION: runs")
        sb.appendLine("run_id,timestamp,server,protocol,direction,duration_sec,tx_avg_mbps,rx_avg_mbps,tx_bytes,rx_bytes,lost")
        for (run in runs) {
            sb.appendLine("${run.id},${isoFormat.format(Date(run.timestamp))},${run.server},${run.protocol},${run.direction},${run.durationSec},${run.txAvgMbps},${run.rxAvgMbps},${run.txBytes},${run.rxBytes},${run.lost}")
        }

        sb.appendLine("#")
        sb.appendLine("# SECTION: intervals")
        sb.appendLine("run_id,interval_sec,direction,speed_mbps,bytes,local_cpu,remote_cpu,lost")
        for (run in runs) {
            intervalsByRun[run.id]?.forEach { iv ->
                sb.appendLine("${run.id},${iv.intervalSec},${iv.direction},${iv.speedMbps},${iv.bytes},${iv.localCpu ?: ""},${iv.remoteCpu ?: ""},${iv.lost ?: ""}")
            }
        }

        return sb.toString()
    }
}
