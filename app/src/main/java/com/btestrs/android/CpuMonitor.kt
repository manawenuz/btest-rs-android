package com.btestrs.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.FileReader

class CpuMonitor {

    private data class ProcCpuSnapshot(
        val utime: Long,   // user mode jiffies
        val stime: Long,   // kernel mode jiffies
        val cutime: Long,  // children user mode jiffies
        val cstime: Long   // children kernel mode jiffies
    ) {
        val total get() = utime + stime + cutime + cstime
    }

    /**
     * Monitor CPU usage for a specific process by reading /proc/<pid>/stat.
     * This works on Android even when /proc/stat is restricted.
     * Returns CPU % based on process time vs wall clock elapsed.
     */
    fun monitorProcess(pid: Int, intervalMs: Long = 1000L): Flow<Int> = flow {
        val numCpus = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        var prevSnapshot = readProcStat(pid) ?: return@flow
        var prevUptime = readUptime() ?: return@flow

        while (true) {
            delay(intervalMs)
            val currSnapshot = readProcStat(pid) ?: break
            val currUptime = readUptime() ?: continue

            val procDelta = currSnapshot.total - prevSnapshot.total
            // Uptime is in seconds with centiseconds, convert jiffies (typically 100Hz)
            val uptimeDelta = ((currUptime - prevUptime) * clockTicksPerSecond()).toLong()

            val cpuPercent = if (uptimeDelta > 0) {
                ((procDelta * 100) / uptimeDelta).toInt().coerceIn(0, 100 * numCpus)
            } else {
                0
            }

            emit(cpuPercent.coerceAtMost(100))
            prevSnapshot = currSnapshot
            prevUptime = currUptime
        }
    }.flowOn(Dispatchers.IO)

    private fun readProcStat(pid: Int): ProcCpuSnapshot? {
        return try {
            BufferedReader(FileReader("/proc/$pid/stat")).use { reader ->
                val line = reader.readLine() ?: return null
                // /proc/<pid>/stat format: pid (comm) state fields...
                // Fields after (comm): start at index after last ')'
                val afterComm = line.substringAfterLast(") ")
                val fields = afterComm.split(" ")
                // fields[0]=state, [1]=ppid, ..., [11]=utime, [12]=stime, [13]=cutime, [14]=cstime
                if (fields.size < 15) return null
                ProcCpuSnapshot(
                    utime = fields[11].toLong(),
                    stime = fields[12].toLong(),
                    cutime = fields[13].toLong(),
                    cstime = fields[14].toLong()
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readUptime(): Double? {
        return try {
            BufferedReader(FileReader("/proc/uptime")).use { reader ->
                val line = reader.readLine() ?: return null
                line.split(" ")[0].toDoubleOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun clockTicksPerSecond(): Long = 100L // Standard for Linux/Android (USER_HZ)
}
