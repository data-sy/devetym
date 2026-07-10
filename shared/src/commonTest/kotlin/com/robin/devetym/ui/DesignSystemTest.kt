package com.robin.devetym.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.robin.devetym.data.local.toEntity
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import com.robin.devetym.model.TermResult
import com.robin.devetym.ui.theme.AppFonts
import com.robin.devetym.ui.theme.DarkColors
import com.robin.devetym.ui.theme.LightColors
import com.robin.devetym.ui.theme.appTypography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * M6 슬라이스 §6 — 순수 헬퍼(색/타이포/에러메시지/상대시간/상태표시매핑/카테고리색/북마크교차조회).
 * 4축 실행(네이티브 포함). Compose 화면 렌더 자체는 이 테스트 범위 밖(검증 천장 §0).
 */
class DesignSystemTest {

    @Test
    fun test_색상토큰_정본일치() {
        // Assets.xcassets/Theme/*.colorset 정본(대표값).
        assertEquals(Color(0xFF3F7A00), LightColors.accent)
        assertEquals(Color(0xFFC8F060), DarkColors.accent)   // 라임(다크)
        assertEquals(Color(0xFFFAFAFA), LightColors.bg)
        assertEquals(Color(0xFF0A0A0A), DarkColors.bg)
        assertEquals(Color(0xFFECECEC), DarkColors.text)
    }

    @Test
    fun test_타이포토큰_패밀리매핑() {
        val fonts = AppFonts(FontFamily.Default, FontFamily.Monospace, FontFamily.Serif, FontFamily.Default)
        val t = appTypography(fonts)
        assertEquals(FontFamily.Monospace, t.codeBody.fontFamily)   // 영문 코드=code
        assertEquals(FontFamily.Serif, t.titleHero.fontFamily)      // 시그니처=serif
        assertEquals(FontFamily.Default, t.body.fontFamily)         // 한글 본문=시스템
        assertEquals(28.sp, t.titleHero.fontSize)                   // 대표 size
        assertEquals(FontWeight.Medium, t.codeBody.fontWeight)      // 대표 weight
    }

    @Test
    fun test_errorKind_메시지_전수() {
        assertEquals("응답이 지연되고 있어요. 잠시 후 다시 시도해주세요", errorMessage(ErrorKind.Timeout))
        assertEquals("인터넷 연결을 확인해주세요", errorMessage(ErrorKind.Network))
        assertEquals("오늘 사용량을 모두 사용했어요", errorMessage(ErrorKind.DailyLimitExceeded))
        assertEquals("결과를 불러오지 못했어요", errorMessage(ErrorKind.InvalidResponse))
        assertEquals("문제가 발생했어요", errorMessage(ErrorKind.Unknown))
    }

    @Test
    fun test_relativeTime_경계() {
        val now = 1_000_000_000_000L
        val min = 60_000L; val hour = 60 * min; val day = 24 * hour
        assertEquals("방금 전", relativeTimeLabel(now, now - 30_000))
        assertEquals("5분 전", relativeTimeLabel(now, now - 5 * min))
        assertEquals("3시간 전", relativeTimeLabel(now, now - 3 * hour))   // 1~23h 구간(diff 기반)
        assertEquals("어제", relativeTimeLabel(now, now - day))
        assertEquals("3일 전", relativeTimeLabel(now, now - 3 * day))
    }

    @Test
    fun test_detailState_표시매핑() {
        assertEquals("loading", detailPresentation(DetailUiState.Loading).kind)
        val found = DetailUiState.Result(TermResult.Found(te("mutex"), Source.AI))
        assertEquals("found", detailPresentation(found).kind)
        assertEquals(DetailIcon.None, detailPresentation(found).icon)
        val notDev = DetailUiState.Result(TermResult.NotDevTerm)
        assertEquals(DetailIcon.Question, detailPresentation(notDev).icon)
        val typo = DetailUiState.Result(TermResult.PossibleTypo("react"))
        assertEquals(DetailIcon.Lightbulb, detailPresentation(typo).icon)
        assertEquals("react을(를) 찾으셨나요?", detailPresentation(typo).message)
        val error = DetailUiState.Error(ErrorKind.Network)
        assertEquals(DetailIcon.Error, detailPresentation(error).icon)
        assertEquals("인터넷 연결을 확인해주세요", detailPresentation(error).message)
    }

    @Test
    fun test_categoryColor_클램프() {
        assertEquals(DarkColors.accent, categoryColor("동시성", DarkColors))   // 정본 6종
        assertEquals(DarkColors.accent, categoryColor("존재안함카테고리", DarkColors)) // 범위밖→accent(크래시 없음)
    }

    @Test
    fun test_isBookmarked_교차조회() {
        // 저장 로우 keyword는 정규화값(소문자). 라우트/원문은 대소문자 섞임 가능.
        val bookmarks = listOf(
            te("oauth").toEntity(Source.AI, createdAt = 1L, isBookmarked = true, seenAt = 1L),
        )
        assertTrue(isBookmarkedFor(bookmarks, "OAuth"))   // 번들 원문 대소문자 섞임 → 정규화 매치
        assertTrue(isBookmarkedFor(bookmarks, "oauth"))   // AI 정규화값 그대로
        assertTrue(isBookmarkedFor(bookmarks, "  OAUTH  ")) // trim+lowercase
        assertFalse(isBookmarkedFor(bookmarks, "react"))  // 미북마크
    }

    private fun te(keyword: String) = TermEntry(keyword, listOf("별칭"), "동시성", "s", "e", "n")
}
