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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btestrs.android.BtestResult
import com.btestrs.android.data.TestRunEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResultDetailScreen(
    run: TestRunEntity,
    intervals: List<BtestResult>,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM d yyyy, HH:mm:ss", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Run info header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(run.server, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(dateFormat.format(Date(run.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(run.protocol, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(run.direction, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("${run.durationSec}s", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        // Live speed display (final values)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (run.txAvgMbps > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TX", color = TxBlue, fontWeight = FontWeight.Bold)
                    Text(
                        String.format("%.1f", run.txAvgMbps),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TxBlue
                    )
                    Text("Mbps avg", color = Color.Gray)
                }
            }
            if (run.rxAvgMbps > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RX", color = RxGreen, fontWeight = FontWeight.Bold)
                    Text(
                        String.format("%.1f", run.rxAvgMbps),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = RxGreen
                    )
                    Text("Mbps avg", color = Color.Gray)
                }
            }
        }

        // Graph
        if (intervals.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    StatRow("Avg TX", String.format("%.2f Mbps", txIntervals.map { it.speedMbps }.average()))
                    StatRow("TX bytes", formatBytes(txIntervals.sumOf { it.bytes }))
                }
                if (rxIntervals.isNotEmpty()) {
                    StatRow("Avg RX", String.format("%.2f Mbps", rxIntervals.map { it.speedMbps }.average()))
                    StatRow("RX bytes", formatBytes(rxIntervals.sumOf { it.bytes }))
                }

                val lastCpu = intervals.lastOrNull { it.remoteCpu != null && it.remoteCpu > 0 }
                lastCpu?.let {
                    StatRow("CPU (local/remote)", "${it.localCpu ?: 0}% / ${it.remoteCpu ?: 0}%")
                }

                val lastLost = intervals.lastOrNull { it.lost != null }
                lastLost?.let {
                    StatRow("Lost packets", it.lost.toString())
                }

                StatRow("Duration", "${run.durationSec}s")

                Spacer(Modifier.height(8.dp))
                Text("Final Results", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                StatRow("TX avg", String.format("%.2f Mbps", run.txAvgMbps))
                StatRow("RX avg", String.format("%.2f Mbps", run.rxAvgMbps))
                StatRow("Total TX", formatBytes(run.txBytes))
                StatRow("Total RX", formatBytes(run.rxBytes))
                StatRow("Lost", run.lost.toString())
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
