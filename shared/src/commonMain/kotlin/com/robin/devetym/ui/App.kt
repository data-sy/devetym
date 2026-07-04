package com.robin.devetym.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.robin.devetym.Greeting
import org.koin.mp.KoinPlatform

/** 공유 Compose 진입 화면(commonMain). M0: Koin으로 Greeting 해석 → 화면에 그림. */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val greeting = remember { KoinPlatform.getKoin().get<Greeting>() }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = greeting.greet())
            }
        }
    }
}
