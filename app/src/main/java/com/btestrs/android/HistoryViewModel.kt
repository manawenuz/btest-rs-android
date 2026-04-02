package com.btestrs.android

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btestrs.android.data.AppDatabase
import com.btestrs.android.data.TestRunDao
import com.btestrs.android.data.TestRunEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class HistoryViewModel : ViewModel() {

    private var dao: TestRunDao? = null
    private var rendererSettings: RendererSettings? = null
    private var deviceId: String? = null

    val runs = MutableStateFlow<List<TestRunEntity>>(emptyList())
    val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectionMode = MutableStateFlow(false)
    val detailIntervals = MutableStateFlow<List<BtestResult>>(emptyList())
    val detailRun = MutableStateFlow<TestRunEntity?>(null)
    val rendererConfig = MutableStateFlow(RendererConfig())
    val statusMessage = MutableStateFlow<String?>(null)
    val isSyncing = MutableStateFlow(false)
    val verifyStatus = MutableStateFlow<String?>(null)

    fun init(context: Context) {
        if (dao != null) return
        dao = AppDatabase.getInstance(context).testRunDao()
        rendererSettings = RendererSettings(context)
        rendererConfig.value = rendererSettings!!.load()
        deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        viewModelScope.launch {
            dao!!.getAllRuns().collect { runs.value = it }
        }
    }

    fun updateRendererConfig(transform: (RendererConfig) -> RendererConfig) {
        val updated = transform(rendererConfig.value)
        rendererConfig.value = updated
        rendererSettings?.save(updated)
    }

    fun setSyncEnabled(enabled: Boolean) {
        updateRendererConfig { it.copy(syncEnabled = enabled) }
        if (enabled) syncUnsynced()
    }

    fun verifyConnection() {
        val cfg = rendererConfig.value
        if (!cfg.isConfigured) {
            verifyStatus.value = "Enter API key first"
            return
        }
        verifyStatus.value = "Verifying..."
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("${cfg.url.trimEnd('/')}/api/auth/me")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Authorization", "Bearer ${cfg.apiKey}")
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    val code = conn.responseCode
                    val body = if (code in 200..299) {
                        conn.inputStream.bufferedReader().readText()
                    } else {
                        conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                    }
                    code to body
                }
                val (code, body) = result
                if (code in 200..299) {
                    verifyStatus.value = "Connected"
                } else {
                    verifyStatus.value = "Failed: HTTP $code"
                }
            } catch (e: Exception) {
                verifyStatus.value = "Failed: ${e.message}"
            }
        }
    }

    /** Sync all unsynced runs to the dashboard. Called when sync is toggled on or after a new test. */
    fun syncUnsynced() {
        val cfg = rendererConfig.value
        if (!cfg.isConfigured || !cfg.syncEnabled) return
        if (isSyncing.value) return

        isSyncing.value = true
        viewModelScope.launch {
            try {
                val unsynced = dao?.getUnsyncedRuns() ?: emptyList()
                if (unsynced.isEmpty()) {
                    isSyncing.value = false
                    return@launch
                }

                statusMessage.value = "Syncing ${unsynced.size} run(s)..."
                var successCount = 0

                for (run in unsynced) {
                    val intervals = dao?.getIntervalsForRun(run.id) ?: continue
                    val json = JsonExporter.exportSingle(run, intervals, deviceId)
                    val result = ResultPoster.postSingle(cfg.url, cfg.apiKey, json)
                    if (result.isSuccess) {
                        dao?.markSynced(listOf(run.id))
                        successCount++
                    }
                }

                statusMessage.value = if (successCount == unsynced.size) {
                    "Synced $successCount run(s)"
                } else {
                    "Synced $successCount/${unsynced.size} (${unsynced.size - successCount} failed)"
                }
            } catch (e: Exception) {
                statusMessage.value = "Sync error: ${e.message}"
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun longPressSelect(id: Long) {
        selectionMode.value = true
        val current = selectedIds.value.toMutableSet()
        current.add(id)
        selectedIds.value = current
    }

    fun toggleSelection(id: Long) {
        val current = selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        selectedIds.value = current
        if (current.isEmpty()) selectionMode.value = false
    }

    fun selectAll() {
        selectedIds.value = runs.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
        selectionMode.value = false
    }

    fun deleteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            dao?.deleteRunsByIds(ids)
            selectedIds.value = emptySet()
            selectionMode.value = false
            if (detailRun.value?.id in ids) {
                detailRun.value = null
                detailIntervals.value = emptyList()
            }
        }
    }

    fun openDetail(run: TestRunEntity) {
        viewModelScope.launch {
            val intervals = dao?.getIntervalsForRun(run.id) ?: return@launch
            detailIntervals.value = intervals.map { iv ->
                BtestResult(
                    intervalSec = iv.intervalSec,
                    direction = iv.direction,
                    speedMbps = iv.speedMbps,
                    bytes = iv.bytes,
                    localCpu = iv.localCpu,
                    remoteCpu = iv.remoteCpu,
                    lost = iv.lost
                )
            }
            detailRun.value = run
        }
    }

    fun closeDetail() {
        detailRun.value = null
        detailIntervals.value = emptyList()
    }

    fun exportCsv(context: Context) {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val selectedRuns = dao?.getRunsByIds(ids) ?: return@launch
            val intervals = dao?.getIntervalsForRuns(ids) ?: return@launch
            val intervalsByRun = intervals.groupBy { it.runId }
            val csv = CsvExporter.export(selectedRuns, intervalsByRun)
            shareCsv(context, csv)
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    private fun shareCsv(context: Context, csv: String) {
        val file = File(context.cacheDir, "btest-export.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export CSV"))
    }
}
