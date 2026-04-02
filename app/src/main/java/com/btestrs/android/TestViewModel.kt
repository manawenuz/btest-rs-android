package com.btestrs.android

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun init(context: Context) {
        if (credentialStore != null) return
        val store = CredentialStore(context)
        credentialStore = store
        savedCredentials.value = store.loadAll()

        // Restore last used credentials
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

        // Save credentials if checkbox is checked
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
                // Start CPU monitoring once we have the PID (on first output)
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
