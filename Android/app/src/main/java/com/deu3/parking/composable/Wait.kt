package com.deu3.parking.composable

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

@Composable
fun WaitScreen(
    countdown: Int,
    onTick: () -> Unit,
    onFinish: () -> Unit
) {
    var internalCount by remember { mutableStateOf(countdown) }

    LaunchedEffect(internalCount) {
        if (internalCount > 0) {
            kotlinx.coroutines.delay(1000)
            internalCount--
            onTick()
        } else {
            onFinish()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("2차 촬영까지 대기", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))
            Text("${internalCount / 60}분 ${internalCount % 60}초", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
