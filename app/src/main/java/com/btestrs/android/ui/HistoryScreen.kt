package com.btestrs.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btestrs.android.HistoryViewModel
import com.btestrs.android.data.TestRunEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val runs by viewModel.runs.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val detailRun by viewModel.detailRun.collectAsState()
    val detailIntervals by viewModel.detailIntervals.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    // Auto-dismiss status after 3 seconds
    LaunchedEffect(statusMessage) {
        if (statusMessage != null && !isSyncing) {
            delay(3000)
            viewModel.clearStatusMessage()
        }
    }

    // If detail view is open, show it
    detailRun?.let { run ->
        ResultDetailScreen(
            run = run,
            intervals = detailIntervals,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Selection action bar
        if (selectionMode) {
            ActionBar(
                selectedCount = selectedIds.size,
                totalCount = runs.size,
                onSelectAll = { viewModel.selectAll() },
                onClearSelection = { viewModel.clearSelection() },
                onExportCsv = { viewModel.exportCsv(context) },
                onDelete = { viewModel.deleteSelected() }
            )
        }

        // Status message
        statusMessage?.let { status ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.startsWith("Error") || status.startsWith("Failed") || status.contains("failed"))
                        MaterialTheme.colorScheme.error else Color.Gray
                )
            }
        }

        // Renderer settings (collapsible)
        RendererSettingsCard(viewModel)

        if (runs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No test history yet", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text("Run a test to see results here", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(runs, key = { it.id }) { run ->
                    RunCard(
                        run = run,
                        isSelected = run.id in selectedIds,
                        selectionMode = selectionMode,
                        onTap = {
                            if (selectionMode) {
                                viewModel.toggleSelection(run.id)
                            } else {
                                viewModel.openDetail(run)
                            }
                        },
                        onLongPress = {
                            viewModel.longPressSelect(run.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RendererSettingsCard(viewModel: HistoryViewModel) {
    val rendererConfig by viewModel.rendererConfig.collectAsState()
    val verifyStatus by viewModel.verifyStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Web Dashboard",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (rendererConfig.syncEnabled && rendererConfig.isConfigured) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text("\u2713", color = RxGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    if (expanded) "\u25B2" else "\u25BC",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = rendererConfig.url,
                    onValueChange = { viewModel.updateRendererConfig { c -> c.copy(url = it) } },
                    label = { Text("Dashboard URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = rendererConfig.apiKey,
                    onValueChange = { viewModel.updateRendererConfig { c -> c.copy(apiKey = it) } },
                    label = { Text("API Key") },
                    placeholder = { Text("btk_...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(Modifier.height(8.dp))

                // Verify button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.verifyConnection() },
                        enabled = rendererConfig.isConfigured,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Verify")
                    }

                    verifyStatus?.let { status ->
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                status == "Connected" -> RxGreen
                                status == "Verifying..." -> Color.Gray
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Sync toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            "Upload new results automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = rendererConfig.syncEnabled,
                        onCheckedChange = { viewModel.setSyncEnabled(it) },
                        enabled = rendererConfig.isConfigured
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onExportCsv: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClearSelection) {
                Text("\u2715")
            }
            Text(
                "$selectedCount selected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Row {
            if (selectedCount < totalCount) {
                TextButton(onClick = onSelectAll) { Text("All") }
            }
            IconButton(onClick = onExportCsv) {
                Icon(Icons.Default.Share, contentDescription = "Export CSV")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RunCard(
    run: TestRunEntity,
    isSelected: Boolean,
    selectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onTap() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        run.server,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${run.protocol} ${run.direction}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        if (run.synced) {
                            Text(
                                "\u2601",
                                fontSize = 12.sp,
                                color = RxGreen
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (run.txAvgMbps > 0) {
                            Row {
                                Text("TX ", color = TxBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f", run.txAvgMbps), fontSize = 13.sp)
                            }
                        }
                        if (run.rxAvgMbps > 0) {
                            Row {
                                Text("RX ", color = RxGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(String.format("%.1f", run.rxAvgMbps), fontSize = 13.sp)
                            }
                        }
                    }
                    Text(
                        dateFormat.format(Date(run.timestamp)),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
