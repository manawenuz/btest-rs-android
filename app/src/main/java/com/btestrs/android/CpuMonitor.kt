package com.btestrs.android

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.FileReader

class CpuMonitor {

    /**
     * Monitor CPU usage for a specific process by reading /proc/<pid>/stat.
     * Uses SystemClock.elapsedRealtime() as time base since /proc/uptime
     * is permission-denied for app processes on modern Android.
     *
     * Returns CPU % (0-100) of the monitored process.
     */
    fun monitorProcess(pid: Int, intervalMs: Long = 1000L): Flow<Int> = flow {
        val numCpus = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val hz = 100L // USER_HZ on Linux/Android

        var prevJiffies = readProcessJiffies(pid) ?: return@flow
        var prevTimeMs = SystemClock.elapsedRealtime()

        while (true) {
            delay(intervalMs)

            val currJiffies = readProcessJiffies(pid) ?: break
            val currTimeMs = SystemClock.elapsedRealtime()

            val jiffiesDelta = currJiffies - prevJiffies
            val timeDeltaMs = currTimeMs - prevTimeMs

            // Convert wall-clock ms to jiffies: (ms / 1000) * hz
            val wallJiffies = (timeDeltaMs * hz) / 1000L

            val cpuPercent = if (wallJiffies > 0) {
                // Process jiffies / (wall jiffies * numCpus) * 100
                // Simplified: (jiffiesDelta * 100) / (wallJiffies * numCpus)
                ((jiffiesDelta * 100L) / (wallJiffies * numCpus)).toInt().coerceIn(0, 100)
            } else {
                0
            }

            emit(cpuPercent)
            prevJiffies = currJiffies
            prevTimeMs = currTimeMs
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Read total CPU jiffies (utime + stime + cutime + cstime) for a process.
     * Reads /proc/<pid>/stat which is accessible for child processes.
     */
    private fun readProcessJiffies(pid: Int): Long? {
        return try {
            BufferedReader(FileReader("/proc/$pid/stat")).use { reader ->
                val line = reader.readLine() ?: return null
                // Format: pid (comm) state fields...
                // Fields after closing ')': indexed from 0
                // [11]=utime [12]=stime [13]=cutime [14]=cstime
                val afterComm = line.substringAfterLast(") ")
                val fields = afterComm.split(" ")
                if (fields.size < 15) return null
                val utime = fields[11].toLongOrNull() ?: return null
                val stime = fields[12].toLongOrNull() ?: return null
                val cutime = fields[13].toLongOrNull() ?: return null
                val cstime = fields[14].toLongOrNull() ?: return null
                utime + stime + cutime + cstime
            }
        } catch (_: Exception) {
            null
        }
    }
}
