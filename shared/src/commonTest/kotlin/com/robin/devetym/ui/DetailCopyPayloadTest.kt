package com.robin.devetym.ui

import com.robin.devetym.model.TermEntry
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * M9-후속 셸 재설계 §2-E — 복사 페이로드 조립 계약. UX-4(어원만 복사돼 오인)의 수정을 포맷 전문으로 고정:
 * 키워드 + "어원"/"왜 이 이름인가" 라벨 + 각 본문, 빈 줄 구분.
 */
class DetailCopyPayloadTest {

    private val entry = TermEntry(
        keyword = "mutex",
        category = "CS 일반",
        summary = "상호 배제 잠금",
        etymology = "mutual exclusion의 축약",
        namingReason = "서로(mutual) 배제(exclusion)한다는 의미의 합성",
    )

    @Test
    fun test_복사페이로드_포맷_전문() {
        assertEquals(
            "mutex\n\n어원\nmutual exclusion의 축약\n\n왜 이 이름인가\n서로(mutual) 배제(exclusion)한다는 의미의 합성",
            detailCopyPayload(entry),
            "§2-E 포맷(키워드+어원+왜 이 이름인가, 라벨 포함) 불일치 — UX-4 오인 수정 회귀",
        )
    }

    @Test
    fun test_공유페이로드_전문_출처포함() {
        // 라운드 2 — 공유는 복사와 달리 요약 포함 전문 + 출처 표기(공유 관례, 복사엔 출처 미부착).
        assertEquals(
            "mutex\n상호 배제 잠금\n\n어원\nmutual exclusion의 축약\n\n왜 이 이름인가\n서로(mutual) 배제(exclusion)한다는 의미의 합성\n\n— DevEtym",
            detailSharePayload(entry),
            "공유 페이로드(요약+전문+출처) 불일치 — 라운드 2 '요약 한 줄만 공유' 회귀",
        )
    }
}
