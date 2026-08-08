package com.robin.devetym

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * #19 「앱 평가하기」 무반응 수정 — 리뷰 딥링크 상수 형태 고정. 실행 동작(`UIApplication.openURL` 실호출)은
 * 실기기 오라클로 넘기고, 여기선 **오타·Apple ID 드리프트를 빌드 실패로 만든다**.
 *
 * 이 단언이 필요한 이유: 딥링크는 틀려도 예외가 안 난다 — 잘못된 ID면 App Store가 "항목을 찾을 수 없음"을
 * 조용히 띄우고 앱은 알 방법이 없다. 종전 프롬프트 API의 무반응과 **증상이 같은 실패 모드**라,
 * 범주를 바꿨어도 값이 틀리면 3회차 결함이 4회차로 이어진다.
 */
class AppStoreReviewUrlTest {

    @Test
    fun test_리뷰URL_형태() {
        val url = Constants.appStoreReviewUrl
        assertTrue(
            url.startsWith("https://apps.apple.com/"),
            "App Store 호스트가 아님 — 리뷰 딥링크가 엉뚱한 곳을 연다: $url",
        )
        assertTrue(
            url.contains("6790429958"),
            "Apple ID 드리프트 — 2026-07-27 게시 실측값(ROADMAP §M9)과 불일치: $url",
        )
        assertTrue(
            url.contains("action=write-review"),
            "write-review 파라미터 부재 — 리뷰 작성 시트가 아니라 앱 소개 페이지만 열린다: $url",
        )
    }
}
