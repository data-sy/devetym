package com.robin.devetym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robin.devetym.ui.theme.AppScheme

/**
 * 온보딩 화면 (M6 §3-7). VM 없음 — 2단계(인트로 + 데이터수집 동의) → `onComplete(consent)`.
 * 최초 1회 게이트는 M7 셸/seam. (iOS 페이저 → 단순 단계 상태로 관용 이식.)
 */
@Composable
fun OnboardingScreen(onComplete: (Boolean) -> Unit) {
    var page by remember { mutableStateOf(0) }
    OnboardingContent(page = page, onNext = { page = 1 }, onComplete = onComplete)
}

@Composable
fun OnboardingContent(page: Int, onNext: () -> Unit, onComplete: (Boolean) -> Unit) {
    val colors = AppScheme.colors
    val type = AppScheme.type
    Column(
        // M9(시뮬 스모크 발견): 온보딩은 Scaffold 이전 early-return이라 테마 배경이 안 칠해져
        // 다크 기본인데 흰 배경 + 밝은 text 토큰이 저대비로 뜬다 → 최상위에서 bg 명시 칠.
        Modifier.fillMaxSize().background(colors.bg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (page) {
            0 -> {
                Text("📖", style = type.titleHero, color = colors.brand)
                Text("개발 어원 사전", style = type.titleHero, color = colors.text,
                    modifier = Modifier.padding(vertical = 16.dp))
                Text("개발 용어의 어원과 작명 이유를 찾아드려요", style = type.body,
                    color = colors.textDim, textAlign = TextAlign.Center)
                Text("✦ 어원 정보는 AI가 생성하며 부정확할 수 있어요", style = type.caption,
                    color = colors.accentAI, modifier = Modifier.padding(top = 24.dp))
                Button(onClick = onNext, modifier = Modifier.padding(top = 32.dp)) { Text("다음") }
            }
            else -> {
                Text("📊", style = type.titleHero, color = colors.accent)
                Text("데이터 수집 동의", style = type.titleHero, color = colors.text,
                    modifier = Modifier.padding(vertical = 16.dp))
                Text("검색 키워드·결과 유형·API 오류·익명 식별자를 수집해 서비스 개선에 사용해요.\n" +
                    "개인정보는 수집하지 않아요.", style = type.body, color = colors.textDim,
                    textAlign = TextAlign.Center)
                Button(onClick = { onComplete(true) }, modifier = Modifier.padding(top = 32.dp)) { Text("허용") }
                OutlinedButton(onClick = { onComplete(false) }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("허용 안 함")
                }
            }
        }
    }
}
