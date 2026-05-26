package com.mnemo.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mnemo.ui.theme.Background
import com.mnemo.ui.theme.OnSurface

@Composable
fun GraphScreen(
    onNodeTap: (String) -> Unit,
    vm: GraphViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Text(
            "Mnemo",
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.nodes.isEmpty() -> Text(
                    "No graph data yet — index some screenshots first",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> ForceDirectedCanvas(
                    nodes = state.nodes,
                    edges = state.edges,
                    onNodeTap = onNodeTap
                )
            }
        }
    }
}
