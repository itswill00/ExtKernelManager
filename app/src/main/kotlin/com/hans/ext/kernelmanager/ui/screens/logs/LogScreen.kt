package com.hans.ext.kernelmanager.ui.screens.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(viewModel: LogViewModel = viewModel()) {
    val logs by viewModel.logs.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startLogging()
        onDispose { viewModel.stopLogging() }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Kernel Logs (dmesg)") },
                actions = {
                    Button(onClick = { viewModel.clearLogs() }) {
                        Text("Clear")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .padding(8.dp)
        ) {
            items(logs) { line ->
                Text(
                    text = line,
                    color = Color.Green, // Terminal style
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
