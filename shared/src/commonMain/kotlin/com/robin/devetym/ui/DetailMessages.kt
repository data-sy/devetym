package com.robin.devetym.ui

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
