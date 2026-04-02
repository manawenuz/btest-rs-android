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

    private var runner: BtestRunner? = null
    private var job: Job? = null
    private var cpuJob: Job? = null
    private val cpuMonitor = CpuMonitor()

    fun start(context: Context) {
        if (isRunning.value) return

        intervals.value = emptyList()
        summary.value = null
        error.value = null
        localCpu.value = 0
        isRunning.value = true

        val btestRunner = BtestRunner(context)
        runner = btestRunner

        cpuJob = viewModelScope.launch {
            cpuMonitor.monitor().collect { cpu ->
                localCpu.value = cpu
            }
        }

        job = viewModelScope.launch {
            btestRunner.run(config.value).collect { output ->
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
        isRunning.value = false
    }

    fun updateConfig(transform: (BtestConfig) -> BtestConfig) {
        config.value = transform(config.value)
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
