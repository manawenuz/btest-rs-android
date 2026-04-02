package com.btestrs.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.FileReader

class CpuMonitor {

    private data class CpuSnapshot(
        val user: Long,
        val nice: Long,
        val system: Long,
        val idle: Long,
        val iowait: Long,
        val irq: Long,
        val softirq: Long,
        val steal: Long
    ) {
        val totalBusy get() = user + nice + system + irq + softirq + steal
        val total get() = totalBusy + idle + iowait
    }

    fun monitor(intervalMs: Long = 1000L): Flow<Int> = flow {
        var prev = readCpuSnapshot() ?: return@flow

        while (true) {
            delay(intervalMs)
            val curr = readCpuSnapshot() ?: continue

            val totalDiff = curr.total - prev.total
            val busyDiff = curr.totalBusy - prev.totalBusy

            val cpuPercent = if (totalDiff > 0) {
                ((busyDiff * 100) / totalDiff).toInt().coerceIn(0, 100)
            } else {
                0
            }

            emit(cpuPercent)
            prev = curr
        }
    }.flowOn(Dispatchers.IO)

    private fun readCpuSnapshot(): CpuSnapshot? {
        return try {
            BufferedReader(FileReader("/proc/stat")).use { reader ->
                val line = reader.readLine() ?: return null
                if (!line.startsWith("cpu ")) return null
                val parts = line.substringAfter("cpu ").trim().split("\\s+".toRegex())
                if (parts.size < 8) return null
                CpuSnapshot(
                    user = parts[0].toLong(),
                    nice = parts[1].toLong(),
                    system = parts[2].toLong(),
                    idle = parts[3].toLong(),
                    iowait = parts[4].toLong(),
                    irq = parts[5].toLong(),
                    softirq = parts[6].toLong(),
                    steal = parts[7].toLong()
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
