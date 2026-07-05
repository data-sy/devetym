package com.robin.devetym.di

import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.OnboardingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

/**
 * iOS seam actual (M8 §3-1·§3-2). ⚠️ 런타임 동작(openURL·클립보드·공유)은 실기기 천장 — 컴파일·링크까지 보증.
 * `share`·In-App Review는 presenting VC/scene 제약이라 최소 구현(§7).
 */
class IosAppActions : AppActions {
    override fun sendMail(to: String, subject: String, body: String) {
        open("mailto:$to?subject=${encode(subject)}&body=${encode(body)}")
    }

    override fun share(text: String) {
        // UIActivityViewController는 presenting VC 필요 — 최소 no-op(§7, 실 구현 실기기/별도).
    }

    override fun requestReview() = open("https://apps.apple.com/app/id0000000000")  // App Store url 폴백(§7)

    override fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text   // ⚠️ 세터(쓰기) — 게터 읽기 no-op 금지(§3-1)
    }

    override fun openUrl(url: String) = open(url)

    private fun open(spec: String) {
        NSURL.URLWithString(spec)?.let { UIApplication.sharedApplication.openURL(it) }
    }

    // 최소 인코딩(공백·개행) — 완전한 percent-encoding·실제 mailto 열림은 실기기 천장(§7).
    private fun encode(s: String): String = s.replace(" ", "%20").replace("\n", "%0A")
}

private val defaults: NSUserDefaults get() = NSUserDefaults.standardUserDefaults

class UserDefaultsAppearanceStore : AppearanceStore {
    private val _mode = MutableStateFlow(readMode())
    override val mode: StateFlow<Int> = _mode.asStateFlow()
    override fun set(mode: Int) {
        defaults.setInteger(mode.toLong(), "appearance_mode")
        _mode.value = mode
    }

    // ⚠️ integerForKey는 키 부재 시 0(시스템) 반환 함정 — objectForKey null 판정으로 부재 시 다크(2) 보장(§3-2).
    private fun readMode(): Int =
        if (defaults.objectForKey("appearance_mode") == null) 2
        else defaults.integerForKey("appearance_mode").toInt()
}

class UserDefaultsOnboardingStore : OnboardingStore {
    override val completed: Boolean get() = defaults.boolForKey("onboarding_done")
    override fun complete() { defaults.setBool(true, "onboarding_done") }
}

class UserDefaultsDeviceIdProvider : DeviceIdProvider {
    override fun get(): String {
        defaults.stringForKey("device_id")?.let { return it }
        val id = NSUUID().UUIDString()
        defaults.setObject(id, "device_id")
        return id
    }
}

class IosDeviceInfo : DeviceInfo {
    override fun appVersion(): String =
        NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "?"
    override suspend fun instanceId(): String? = null   // Firebase 미도입(§7)
}
