package com.robin.devetym.di

import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * M9-후속 셸 재설계 §2-F — iOS 동의 토글 키부재 기본 true. `IosAppearanceDefaultTest`와 같은 함정:
 * `boolForKey`는 키 부재 시 false를 반환하므로, 신규 설치 첫 실행이 기본 true(현행 UI 기본)가 아니라
 * **꺼짐으로 조용히 발산**한다. `objectForKey==null → true` 판정이 이를 막는다.
 */
class IosConsentDefaultTest {

    @AfterTest fun teardown() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey("consent_given")
    }

    @Test
    fun test_ios_동의_키부재_기본true() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey("consent_given")
        assertTrue(
            UserDefaultsConsentStore().given.value,
            "iOS 키부재 첫 실행 동의 기본이 true 아님 — boolForKey false 발산 함정",
        )
    }

    @Test
    fun test_ios_동의_set_persist_왕복() {
        val store = UserDefaultsConsentStore()
        store.set(false)
        assertEquals(false, store.given.value, "set 후 emit 값 불일치")
        // 새 인스턴스가 영속값을 읽는지(objectForKey 존재 → boolForKey 경로)
        assertFalse(UserDefaultsConsentStore().given.value, "NSUserDefaults 영속 왕복 실패")
    }
}
