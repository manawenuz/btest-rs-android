package com.btestrs.android

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.coroutines.coroutineContext

sealed class BtestOutput {
    data class Interval(val result: BtestResult) : BtestOutput()
    data class Summary(val summary: BtestSummary) : BtestOutput()
    data class Error(val message: String) : BtestOutput()
}

class BtestRunner(private val context: Context) {

    private var process: Process? = null

    private val binaryPath: String
        get() = "${context.applicationInfo.nativeLibraryDir}/libbtest.so"

    fun run(config: BtestConfig): Flow<BtestOutput> = flow {
        val args = config.toCommandArgs(binaryPath)

        val pb = ProcessBuilder(args)
            .redirectErrorStream(true)
            .directory(context.filesDir)

        try {
            val proc = pb.start()
            process = proc

            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            var line: String?

            while (coroutineContext.isActive) {
                line = reader.readLine() ?: break
                val output = parseLine(line)
                if (output != null) {
                    emit(output)
                }
            }

            val exitCode = proc.waitFor()
            if (exitCode != 0 && coroutineContext.isActive) {
                emit(BtestOutput.Error("Process exited with code $exitCode"))
            }
        } catch (e: Exception) {
            if (coroutineContext.isActive) {
                emit(BtestOutput.Error(e.message ?: "Unknown error"))
            }
        } finally {
            process = null
        }
    }.flowOn(Dispatchers.IO)

    fun stop() {
        process?.destroyForcibly()
        process = null
    }

    companion object {
        private val INTERVAL_REGEX = Regex(
            """\[\s*(\d+)]\s+(TX|RX)\s+([\d.]+)\s+(Gbps|Mbps|Kbps|bps)\s+\((\d+) bytes\)(?:\s+cpu:\s+(\d+)%/(\d+)%)?(?:\s+lost:\s+(\d+))?"""
        )

        private val SUMMARY_REGEX = Regex(
            """TEST_END\s+peer=(\S+)\s+proto=(\S+)\s+dir=(\S+)\s+duration=(\d+)s\s+tx_avg=([\d.]+)Mbps\s+rx_avg=([\d.]+)Mbps\s+tx_bytes=(\d+)\s+rx_bytes=(\d+)\s+lost=(\d+)"""
        )

        private fun parseLine(line: String): BtestOutput? {
            INTERVAL_REGEX.find(line)?.let { match ->
                val (sec, dir, speed, unit, bytes, localCpu, remoteCpu, lost) = match.destructured
                val speedMbps = toMbps(speed.toDouble(), unit)
                return BtestOutput.Interval(
                    BtestResult(
                        intervalSec = sec.toInt(),
                        direction = dir,
                        speedMbps = speedMbps,
                        bytes = bytes.toLong(),
                        localCpu = localCpu.toIntOrNull(),
                        remoteCpu = remoteCpu.toIntOrNull(),
                        lost = lost.toLongOrNull()
                    )
                )
            }

            SUMMARY_REGEX.find(line)?.let { match ->
                val (peer, proto, dir, duration, txAvg, rxAvg, txBytes, rxBytes, lost) = match.destructured
                return BtestOutput.Summary(
                    BtestSummary(
                        peer = peer,
                        proto = proto,
                        dir = dir,
                        durationSec = duration.toInt(),
                        txAvgMbps = txAvg.toDouble(),
                        rxAvgMbps = rxAvg.toDouble(),
                        txBytes = txBytes.toLong(),
                        rxBytes = rxBytes.toLong(),
                        lost = lost.toLong()
                    )
                )
            }

            return null
        }

        private fun toMbps(value: Double, unit: String): Double = when (unit) {
            "Gbps" -> value * 1000.0
            "Mbps" -> value
            "Kbps" -> value / 1000.0
            "bps" -> value / 1_000_000.0
            else -> value
        }
    }
}
