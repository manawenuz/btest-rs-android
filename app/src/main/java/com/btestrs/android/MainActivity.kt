package com.btestrs.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btestrs.android.ui.BtestTheme
import com.btestrs.android.ui.HistoryScreen
import com.btestrs.android.ui.TestScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BtestTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                val testViewModel: TestViewModel = viewModel()
                val historyViewModel: HistoryViewModel = viewModel()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "btest",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                label = { Text("Test") },
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                label = { Text("History") },
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                ) {
                    val detailRun by historyViewModel.detailRun.collectAsState()
                    val selectionMode by historyViewModel.selectionMode.collectAsState()

                    // Handle back: close detail or exit selection first
                    BackHandler(enabled = detailRun != null || selectionMode) {
                        if (detailRun != null) {
                            historyViewModel.closeDetail()
                        } else if (selectionMode) {
                            historyViewModel.clearSelection()
                        }
                    }

                    val title = when {
                        selectedTab == 0 -> "btest"
                        detailRun != null -> "Result"
                        else -> "History"
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = { Text(title) },
                                navigationIcon = {
                                    if (detailRun != null) {
                                        IconButton(onClick = { historyViewModel.closeDetail() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    } else {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        when (selectedTab) {
                            0 -> TestScreen(testViewModel, Modifier.padding(innerPadding))
                            1 -> HistoryScreen(historyViewModel, Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}
