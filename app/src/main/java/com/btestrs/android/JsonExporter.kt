package com.btestrs.android

import com.btestrs.android.data.TestIntervalEntity
import com.btestrs.android.data.TestRunEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object JsonExporter {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun runToJson(
        run: TestRunEntity,
        intervals: List<TestIntervalEntity>,
        deviceId: String? = null
    ): JSONObject {
        val obj = JSONObject()
        obj.put("timestamp", isoFormat.format(Date(run.timestamp)))
        obj.put("server", run.server)
        obj.put("protocol", run.protocol)
        obj.put("direction", run.direction)
        obj.put("duration_sec", run.durationSec)
        obj.put("tx_avg_mbps", run.txAvgMbps)
        obj.put("rx_avg_mbps", run.rxAvgMbps)
        obj.put("tx_bytes", run.txBytes)
        obj.put("rx_bytes", run.rxBytes)
        obj.put("lost", run.lost)
        if (deviceId != null) {
            obj.put("device_id", deviceId)
        }

        val intArray = JSONArray()
        intervals.forEach { iv ->
            val ivObj = JSONObject()
            ivObj.put("sec", iv.intervalSec)
            ivObj.put("dir", iv.direction)
            ivObj.put("speed_mbps", iv.speedMbps)
            ivObj.put("bytes", iv.bytes)
            ivObj.put("local_cpu", iv.localCpu ?: JSONObject.NULL)
            ivObj.put("remote_cpu", iv.remoteCpu ?: JSONObject.NULL)
            ivObj.put("lost", iv.lost ?: JSONObject.NULL)
            intArray.put(ivObj)
        }
        obj.put("intervals", intArray)
        return obj
    }

    /** For legacy CSV-style export (wraps multiple runs) */
    fun export(
        runs: List<TestRunEntity>,
        intervalsByRun: Map<Long, List<TestIntervalEntity>>
    ): String {
        val root = JSONObject()
        root.put("app", "btest-android")
        root.put("version", "1.0")
        root.put("exported_at", isoFormat.format(Date()))

        val runsArray = JSONArray()
        for (run in runs) {
            runsArray.put(runToJson(run, intervalsByRun[run.id] ?: emptyList()))
        }
        root.put("runs", runsArray)
        return root.toString()
    }

    /** Single run JSON string for POST /api/results */
    fun exportSingle(run: TestRunEntity, intervals: List<TestIntervalEntity>, deviceId: String? = null): String {
        return runToJson(run, intervals, deviceId).toString()
    }

    /** Batch JSON for POST /api/results/batch (gzip-compressed) */
    fun exportBatchCompressed(
        runs: List<TestRunEntity>,
        intervalsByRun: Map<Long, List<TestIntervalEntity>>,
        deviceId: String? = null
    ): ByteArray {
        val root = JSONObject()
        val runsArray = JSONArray()
        for (run in runs) {
            runsArray.put(runToJson(run, intervalsByRun[run.id] ?: emptyList(), deviceId))
        }
        root.put("runs", runsArray)

        val json = root.toString()
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(baos).use { gz ->
            gz.write(json.toByteArray(Charsets.UTF_8))
        }
        return baos.toByteArray()
    }

    fun exportCompressed(
        runs: List<TestRunEntity>,
        intervalsByRun: Map<Long, List<TestIntervalEntity>>
    ): ByteArray {
        val json = export(runs, intervalsByRun)
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(baos).use { gz ->
            gz.write(json.toByteArray(Charsets.UTF_8))
        }
        return baos.toByteArray()
    }
}
