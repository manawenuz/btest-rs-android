package com.btestrs.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btestrs.android.BtestConfig
import com.btestrs.android.TestViewModel

@Composable
fun TestScreen(viewModel: TestViewModel, modifier: Modifier = Modifier) {
    val config by viewModel.config.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val intervals by viewModel.intervals.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "btest",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Config form
        ConfigSection(config, isRunning, viewModel)

        // Start/Stop button
        Button(
            onClick = { if (isRunning) viewModel.stop() else viewModel.start(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = if (isRunning) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(if (isRunning) "Stop" else "Start Test", fontSize = 18.sp)
        }

        // Error
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        // Live speed display
        if (intervals.isNotEmpty()) {
            LiveSpeedDisplay(intervals)
        }

        // Graph
        if (intervals.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("TX", color = TxBlue, fontWeight = FontWeight.Bold)
                        Text("RX", color = RxGreen, fontWeight = FontWeight.Bold)
                        Text("Mbps", color = Color.Gray)
                    }
                    Spacer(Modifier.height(8.dp))
                    SpeedGraph(intervals)
                }
            }
        }

        // Stats
        if (intervals.isNotEmpty()) {
            StatsSection(intervals, summary)
        }
    }
}

@Composable
private fun ConfigSection(
    config: BtestConfig,
    isRunning: Boolean,
    viewModel: TestViewModel
) {
    OutlinedTextField(
        value = config.host,
        onValueChange = { viewModel.updateConfig { c -> c.copy(host = it) } },
        label = { Text("Server") },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isRunning,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = config.username,
            onValueChange = { viewModel.updateConfig { c -> c.copy(username = it) } },
            label = { Text("User") },
            modifier = Modifier.weight(1f),
            enabled = !isRunning,
            singleLine = true
        )
        OutlinedTextField(
            value = config.password,
            onValueChange = { viewModel.updateConfig { c -> c.copy(password = it) } },
            label = { Text("Password") },
            modifier = Modifier.weight(1f),
            enabled = !isRunning,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
    }

    // Protocol toggle
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Protocol:", style = MaterialTheme.typography.bodyMedium)
        BtestConfig.Protocol.entries.forEach { proto ->
            FilterChip(
                selected = config.protocol == proto,
                onClick = { viewModel.updateConfig { c -> c.copy(protocol = proto) } },
                label = { Text(proto.name) },
                enabled = !isRunning
            )
        }
    }

    // Direction toggle
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Direction:", style = MaterialTheme.typography.bodyMedium)
        BtestConfig.Direction.entries.forEach { dir ->
            FilterChip(
                selected = config.direction == dir,
                onClick = { viewModel.updateConfig { c -> c.copy(direction = dir) } },
                label = {
                    Text(
                        when (dir) {
                            BtestConfig.Direction.SEND -> "Send"
                            BtestConfig.Direction.RECEIVE -> "Receive"
                            BtestConfig.Direction.BOTH -> "Both"
                        }
                    )
                },
                enabled = !isRunning
            )
        }
    }

    // Duration slider
    Column {
        Text(
            "Duration: ${config.duration}s",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = config.duration.toFloat(),
            onValueChange = { viewModel.updateConfig { c -> c.copy(duration = it.toInt()) } },
            valueRange = 10f..120f,
            steps = 21,
            enabled = !isRunning
        )
    }
}

@Composable
private fun LiveSpeedDisplay(intervals: List<com.btestrs.android.BtestResult>) {
    val lastTx = intervals.lastOrNull { it.direction == "TX" }
    val lastRx = intervals.lastOrNull { it.direction == "RX" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        lastTx?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TX", color = TxBlue, fontWeight = FontWeight.Bold)
                Text(
                    String.format("%.1f", it.speedMbps),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TxBlue
                )
                Text("Mbps", color = Color.Gray)
            }
        }
        lastRx?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RX", color = RxGreen, fontWeight = FontWeight.Bold)
                Text(
                    String.format("%.1f", it.speedMbps),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = RxGreen
                )
                Text("Mbps", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun StatsSection(
    intervals: List<com.btestrs.android.BtestResult>,
    summary: com.btestrs.android.BtestSummary?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Statistics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

            val txIntervals = intervals.filter { it.direction == "TX" }
            val rxIntervals = intervals.filter { it.direction == "RX" }

            if (txIntervals.isNotEmpty()) {
                val avgTx = txIntervals.map { it.speedMbps }.average()
                val totalTxBytes = txIntervals.sumOf { it.bytes }
                StatRow("Avg TX", String.format("%.2f Mbps", avgTx))
                StatRow("TX bytes", formatBytes(totalTxBytes))
            }

            if (rxIntervals.isNotEmpty()) {
                val avgRx = rxIntervals.map { it.speedMbps }.average()
                val totalRxBytes = rxIntervals.sumOf { it.bytes }
                StatRow("Avg RX", String.format("%.2f Mbps", avgRx))
                StatRow("RX bytes", formatBytes(totalRxBytes))
            }

            // CPU from latest interval
            val lastWithCpu = intervals.lastOrNull { it.localCpu != null }
            lastWithCpu?.let {
                StatRow("CPU (local/remote)", "${it.localCpu}% / ${it.remoteCpu ?: 0}%")
            }

            // Lost packets
            val lastWithLost = intervals.lastOrNull { it.lost != null }
            lastWithLost?.let {
                StatRow("Lost packets", it.lost.toString())
            }

            // Duration
            val maxInterval = intervals.maxOfOrNull { it.intervalSec } ?: 0
            StatRow("Elapsed", "${maxInterval}s")

            // Summary
            summary?.let { s ->
                Spacer(Modifier.height(8.dp))
                Text("Final Results", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                StatRow("TX avg", String.format("%.2f Mbps", s.txAvgMbps))
                StatRow("RX avg", String.format("%.2f Mbps", s.rxAvgMbps))
                StatRow("Total TX", formatBytes(s.txBytes))
                StatRow("Total RX", formatBytes(s.rxBytes))
                StatRow("Lost", s.lost.toString())
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format("%.2f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
