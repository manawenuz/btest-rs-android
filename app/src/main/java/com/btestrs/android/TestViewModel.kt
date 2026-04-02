package com.btestrs.android

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btestrs.android.data.AppDatabase
import com.btestrs.android.data.TestIntervalEntity
import com.btestrs.android.data.TestRunDao
import com.btestrs.android.data.TestRunEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TestViewModel : ViewModel() {

    val config = MutableStateFlow(BtestConfig())
    val isRunning = MutableStateFlow(false)
    val intervals = MutableStateFlow<List<BtestResult>>(emptyList())
    val summary = MutableStateFlow<BtestSummary?>(null)
    val error = MutableStateFlow<String?>(null)
    val localCpu = MutableStateFlow(0)
    val savedCredentials = MutableStateFlow<List<SavedCredential>>(emptyList())
    val saveCredentials = MutableStateFlow(true)

    private var runner: BtestRunner? = null
    private var job: Job? = null
    private var cpuJob: Job? = null
    private val cpuMonitor = CpuMonitor()
    private var credentialStore: CredentialStore? = null
    private var dao: TestRunDao? = null
    private var rendererSettings: RendererSettings? = null
    private var deviceId: String? = null

    fun init(context: Context) {
        if (credentialStore != null) return
        val store = CredentialStore(context)
        credentialStore = store
        dao = AppDatabase.getInstance(context).testRunDao()
        rendererSettings = RendererSettings(context)
        deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        savedCredentials.value = store.loadAll()

        store.loadLast()?.let { last ->
            config.value = config.value.copy(
                host = last.host,
                username = last.username,
                password = last.password
            )
        }
    }

    fun start(context: Context) {
        if (isRunning.value) return

        if (saveCredentials.value) {
            val cfg = config.value
            credentialStore?.save(SavedCredential(cfg.host, cfg.username, cfg.password))
            savedCredentials.value = credentialStore?.loadAll() ?: emptyList()
        }

        intervals.value = emptyList()
        summary.value = null
        error.value = null
        localCpu.value = 0
        isRunning.value = true

        val btestRunner = BtestRunner(context)
        runner = btestRunner

        job = viewModelScope.launch {
            btestRunner.run(config.value).collect { output ->
                if (cpuJob == null) {
                    btestRunner.pid?.let { pid ->
                        cpuJob = viewModelScope.launch {
                            cpuMonitor.monitorProcess(pid).collect { cpu ->
                                localCpu.value = cpu
                            }
                        }
                    }
                }

                when (output) {
                    is BtestOutput.Interval -> {
                        intervals.value = intervals.value + output.result
                    }
                    is BtestOutput.Summary -> {
                        summary.value = output.summary
                        saveRun(output.summary)
                    }
                    is BtestOutput.Error -> {
                        error.value = output.message
                    }
                }
            }
            isRunning.value = false
            cpuJob?.cancel()
        }
    }

    private fun saveRun(s: BtestSummary) {
        val cfg = config.value
        val currentIntervals = intervals.value
        viewModelScope.launch {
            try {
                val run = TestRunEntity(
                    timestamp = System.currentTimeMillis(),
                    server = cfg.host,
                    protocol = s.proto,
                    direction = s.dir,
                    durationSec = s.durationSec,
                    txAvgMbps = s.txAvgMbps,
                    rxAvgMbps = s.rxAvgMbps,
                    txBytes = s.txBytes,
                    rxBytes = s.rxBytes,
                    lost = s.lost
                )
                val intervalEntities = currentIntervals.map { iv ->
                    TestIntervalEntity(
                        runId = 0,
                        intervalSec = iv.intervalSec,
                        direction = iv.direction,
                        speedMbps = iv.speedMbps,
                        bytes = iv.bytes,
                        localCpu = iv.localCpu,
                        remoteCpu = iv.remoteCpu,
                        lost = iv.lost
                    )
                }
                dao?.insertRunWithIntervals(run, intervalEntities)
                // Auto-sync if enabled
                syncNewRun()
            } catch (_: Exception) {
                // Don't crash the app if DB write fails
            }
        }
    }

    private fun syncNewRun() {
        val cfg = rendererSettings?.load() ?: return
        if (!cfg.isConfigured || !cfg.syncEnabled) return
        viewModelScope.launch {
            try {
                val unsynced = dao?.getUnsyncedRuns() ?: return@launch
                for (run in unsynced) {
                    val intervals = dao?.getIntervalsForRun(run.id) ?: continue
                    val json = JsonExporter.exportSingle(run, intervals, deviceId)
                    val result = ResultPoster.postSingle(cfg.url, cfg.apiKey, json)
                    if (result.isSuccess) {
                        val remoteId = ResultPoster.parseRemoteId(result.getOrNull())
                        dao?.markSynced(run.id, remoteId)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun stop() {
        runner?.stop()
        job?.cancel()
        cpuJob?.cancel()
        cpuJob = null
        isRunning.value = false
    }

    fun selectCredential(credential: SavedCredential) {
        config.value = config.value.copy(
            host = credential.host,
            username = credential.username,
            password = credential.password
        )
    }

    fun deleteCredential(credential: SavedCredential) {
        credentialStore?.delete(credential)
        savedCredentials.value = credentialStore?.loadAll() ?: emptyList()
    }

    fun updateConfig(transform: (BtestConfig) -> BtestConfig) {
        config.value = transform(config.value)
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
