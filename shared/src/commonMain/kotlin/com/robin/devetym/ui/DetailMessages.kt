package com.robin.devetym.ui

import com.robin.devetym.model.TermEntry

/**
 * `ErrorKind`(M5) → 사용자 한글 메시지 순수 매핑 (M6 §3-9-3). `when` 전수·`else` 없음(DR-3 canary —
 * 새 ErrorKind 추가 시 컴파일 실패). §6 `test_errorKind_메시지_전수` 네이티브 실측.
 */
fun errorMessage(kind: ErrorKind): String = when (kind) {
    ErrorKind.Timeout -> "응답이 지연되고 있어요. 잠시 후 다시 시도해주세요"
    ErrorKind.Network -> "인터넷 연결을 확인해주세요"
    ErrorKind.DailyLimitExceeded -> "오늘 사용량을 모두 사용했어요"
    ErrorKind.InvalidResponse -> "결과를 불러오지 못했어요"
    ErrorKind.Unknown -> "문제가 발생했어요"
}

/**
 * 로딩 안내 문구 교차 (M9-후속 UX-3) — 단일 고정 문구가 길게 느껴진다는 실기기 피드백.
 * 차분한 안내형 2문구를 ~3초 간격 크로스페이드로 순환. tick은 0부터 증가(음수 없음).
 */
val LOADING_PHRASES = listOf("AI가 어원을 찾고 있어요", "잠시만 기다려 주세요")

fun loadingPhrase(tick: Int): String = LOADING_PHRASES[tick % LOADING_PHRASES.size]

/**
 * 상세 복사 페이로드 조립 (M9-후속 셸 재설계 §2-E) — 순수 함수(commonTest 대상). 종전 어원 단일 필드 복사가
 * 버튼 위치("왜 이 이름인가" 밑) 탓에 오인됐다(UX-4) — 키워드+두 섹션을 라벨과 함께 전부 담는다.
 */
fun detailCopyPayload(entry: TermEntry): String =
    "${entry.keyword}\n\n어원\n${entry.etymology}\n\n왜 이 이름인가\n${entry.namingReason}"

/**
 * 경과 diff 기반 상대시간 라벨 (M6 §3-6). 캘린더/타임존/DST 비의존 — `now - epochMillis`만 사용.
 * `<1분`="방금 전" / `<60분`="N분 전" / `<24시간`="N시간 전" / `<48시간`="어제" / 그이상="N일 전".
 */
fun relativeTimeLabel(now: Long, epochMillis: Long): String {
    val diff = (now - epochMillis).coerceAtLeast(0)
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "방금 전"
        diff < hour -> "${diff / minute}분 전"
        diff < day -> "${diff / hour}시간 전"
        diff < 2 * day -> "어제"
        else -> "${diff / day}일 전"
    }
}
