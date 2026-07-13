package com.robin.devetym.ui.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 플랫폼 seam (M6 §3-9). Compose가 호출하는 인터페이스 — **actual 구현·런타임 검증은 M8 이월**.
 * M6는 no-op/스텁 기본으로 컴파일·조립 green(거짓 green 아님 — 스텁임을 명시).
 */
interface AppActions {
    fun sendMail(to: String, subject: String, body: String)
    fun share(text: String)
    fun requestReview()
    fun copyToClipboard(text: String)
    fun openUrl(url: String)
}

interface AppearanceStore {
    val mode: StateFlow<Int>   // 0=시스템 1=라이트 2=다크(기본)
    fun set(mode: Int)
}

interface DeviceInfo {
    fun appVersion(): String
    suspend fun instanceId(): String?
}

/** 온보딩 최초 1회 게이트 영속 (M8 §3-2·§3-4). */
interface OnboardingStore {
    val completed: Boolean
    fun complete()
}

/**
 * 설정 동의 토글 영속 (M9-후속 셸 재설계 §2-F) — `AppearanceStore`와 동형(StateFlow+set).
 * ⚠️ 현재 표시용(수집 자체가 없음 — 방침 "현재 미수집" 정합, ROADMAP 2026-07-06) — 영속만, 동작 배선 없음.
 */
interface ConsentStore {
    val given: StateFlow<Boolean>   // 기본 true(현행 UI 기본 유지)
    fun set(value: Boolean)
}

/** M6 조립용 no-op 스텁(M8이 실 actual로 대체). */
class NoopAppActions : AppActions {
    override fun sendMail(to: String, subject: String, body: String) {}
    override fun share(text: String) {}
    override fun requestReview() {}
    override fun copyToClipboard(text: String) {}
    override fun openUrl(url: String) {}
}

class StubAppearanceStore(initial: Int = 2) : AppearanceStore {
    private val _mode = MutableStateFlow(initial)
    override val mode: StateFlow<Int> = _mode
    override fun set(mode: Int) { _mode.value = mode }
}

class StubDeviceInfo : DeviceInfo {
    override fun appVersion(): String = "1.0.0"
    override suspend fun instanceId(): String? = null
}

class StubOnboardingStore(private var done: Boolean = false) : OnboardingStore {
    override val completed: Boolean get() = done
    override fun complete() { done = true }
}

class StubConsentStore(initial: Boolean = true) : ConsentStore {
    private val _given = MutableStateFlow(initial)
    override val given: StateFlow<Boolean> = _given
    override fun set(value: Boolean) { _given.value = value }
}
