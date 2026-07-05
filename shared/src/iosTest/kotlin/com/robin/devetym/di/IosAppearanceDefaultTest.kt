package com.robin.devetym.di

import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * M9 §3-3(v) — iOS 외관 키부재 기본다크(최고위험 seam 로직, `[AI]`가 사람 없이 폐쇄).
 *
 * `IosSeams.kt:56-59`: `integerForKey`는 키 부재 시 0(=시스템)을 반환하는 함정이라, 신규 iOS 설치 첫 실행이
 * 다크가 아니라 **시스템으로 에러 없이 조용히 발산**한다. `readMode()`가 `objectForKey==null → 2(다크)`로
 * 이를 막는다. 실기기에서도 눈에 안 띌 수 있는 로직이라 네이티브 테스트로 못박는다.
 */
class IosAppearanceDefaultTest {

    @AfterTest fun teardown() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey("appearance_mode")
    }

    @Test
    fun test_ios_외관_키부재_기본다크() {
        // 키 부재 상태 강제(신규 설치 첫 실행 재현)
        NSUserDefaults.standardUserDefaults.removeObjectForKey("appearance_mode")

        val store = UserDefaultsAppearanceStore()
        assertEquals(
            2, store.mode.value,
            "iOS 키부재 첫 실행이 다크(2)가 아님 — integerForKey 0(시스템) 발산 함정 재발",
        )
    }

    @Test
    fun test_ios_외관_set_persist_왕복() {
        val store = UserDefaultsAppearanceStore()
        store.set(1)   // 라이트로 설정
        assertEquals(1, store.mode.value, "set 후 emit 값 불일치")
        // 새 인스턴스가 영속값을 읽는지(objectForKey 존재 → integerForKey 경로)
        assertEquals(1, UserDefaultsAppearanceStore().mode.value, "NSUserDefaults 영속 왕복 실패")
    }
}
