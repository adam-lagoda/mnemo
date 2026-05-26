package com.mnemo.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mnemo.ui.theme.Background

@Composable
fun GraphScreen(
    onNodeTap: (String) -> Unit,
    vm: GraphViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
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
