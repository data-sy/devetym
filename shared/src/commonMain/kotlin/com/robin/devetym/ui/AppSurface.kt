package com.robin.devetym.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.robin.devetym.ui.theme.AppScheme

/**
 * 루트 배경·인셋 정본 (M9-후속 셸 재설계 §2-B) — Scaffold 안/밖 화면이 같은 규율을 받는다.
 * 배경을 전면에 칠하고(온보딩 수동 도색 패치 §1-5 대체) 상단·좌우 safeDrawing(상태바·노치)을
 * 패딩+consume — 라이선스 노치 깔림(3-6) 해소, 상속받는 Scaffold는 top 인셋 0을 본다.
 *
 * bottom은 의도적으로 제외: iOS 관례대로 NavigationBar가 홈 인디케이터 아래로 surface를 흘리며
 * 자기 인셋을 소유한다(전면 소유 시 바가 인디케이터 위에 떠 시각 회귀). ime(키보드)는 §2-C 스텝 3 소관.
 */
@Composable
fun AppSurface(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppScheme.colors.bg)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
    ) { content() }
}
