package com.robin.devetym.di

import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.ConsentStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.OnboardingStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

/**
 * mailto URL 조립 (M9-후속 셸 재설계 §2-D) — 순수 함수(iosTest 대상). `NSURLComponents`+`NSURLQueryItem`이
 * percent-encoding을 담당해 **한글 제목도 nil이 안 된다** — 종전 공백·개행만 치환하던 수동 encode는
 * "DevEtym 문의" 같은 한글 subject에서 `NSURL.URLWithString` nil → 조용한 no-op였다(실기기 3-5 전멸의 확정 결함).
 */
internal fun mailtoUrl(to: String, subject: String, body: String): NSURL? {
    val components = NSURLComponents()
    components.scheme = "mailto"
    components.path = to
    components.queryItems = listOf(
        NSURLQueryItem(name = "subject", value = subject),
        NSURLQueryItem(name = "body", value = body),
    )
    return components.URL
}

/**
 * iOS seam actual (M8 §3-1 → M9-후속 셸 재설계 §2-D 전면 재작성). 스텁·deprecated API를 실구현으로 교체:
 * 비동기 `openURL:options:completionHandler:`(동기 `openURL:`은 iOS 26 실기기에서 https 포함 전멸 관찰)·
 * `UIActivityViewController` 공유·씬 기반 StoreKit 리뷰·메일 폴백(클립보드+알럿).
 * ⚠️ 메일 실전송·공유시트 실동작·리뷰 프롬프트는 실기기 게이트 이월(§3).
 */
class IosAppActions : AppActions {
    override fun sendMail(to: String, subject: String, body: String) {
        val url = mailtoUrl(to, subject, body) ?: return mailFallback(to)
        // completionHandler는 메인 큐 실행(UIKit 보증) — 알럿 present 안전.
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>()) { opened ->
            if (!opened) mailFallback(to)
        }
    }

    override fun share(text: String) {
        val presenter = topPresentedViewController() ?: return
        val activityVc = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        presentWithPopoverGuard(activityVc, presenter)
        presenter.presentViewController(activityVc, animated = true, completion = null)
    }

    override fun requestReview() {
        // 씬 기반 StoreKit — 미출시(실 앱 ID 부재) 상태에서도 안전. 스토어 URL 폴백(id0000000000) 폐지.
        foregroundWindowScene()?.let { SKStoreReviewController.requestReviewInScene(it) }
    }

    override fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text   // ⚠️ 세터(쓰기) — 게터 읽기 no-op 금지(M8 §3-1)
    }

    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    }

    /** 메일 앱을 못 여는 기기(계정 미설정 등) — 주소 클립보드 복사 + 네이티브 알럿 안내(§2-D 폴백, 승인 채택). */
    private fun mailFallback(to: String) {
        copyToClipboard(to)
        val alert = UIAlertController.alertControllerWithTitle(
            title = "메일 앱을 열 수 없어요",
            message = "지원 이메일 주소를 클립보드에 복사했어요\n$to",
            preferredStyle = UIAlertControllerStyleAlert,
        )
        alert.addAction(UIAlertAction.actionWithTitle("확인", style = UIAlertActionStyleDefault, handler = null))
        topPresentedViewController()?.presentViewController(alert, animated = true, completion = null)
    }

    /** iPad에서 UIActivityViewController는 popover source 없으면 크래시 — 뷰 중앙 anchor 방어. */
    @OptIn(ExperimentalForeignApi::class)
    private fun presentWithPopoverGuard(vc: UIViewController, presenter: UIViewController) {
        vc.popoverPresentationController?.let { popover ->
            val view = presenter.view
            popover.sourceView = view
            popover.sourceRect = view.bounds.useContents {
                CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
            }
        }
    }

    private fun foregroundWindowScene(): UIWindowScene? =
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }

    /** 최상위 presented VC 탐색 — present 중인 시트/알럿 위에도 안전하게 얹는다. */
    private fun topPresentedViewController(): UIViewController? {
        var top = foregroundWindowScene()?.windows
            ?.filterIsInstance<UIWindow>()
            ?.firstOrNull { it.keyWindow }
            ?.rootViewController
            ?: return null
        while (true) top = top.presentedViewController ?: return top
    }
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

/**
 * 동의 토글 영속 (M9-후속 셸 재설계 §2-F) — `UserDefaultsAppearanceStore`와 동형.
 * ⚠️ boolForKey는 키 부재 시 false 함정 — objectForKey null 판정으로 부재 시 true(현행 UI 기본) 보장.
 */
class UserDefaultsConsentStore : ConsentStore {
    private val _given = MutableStateFlow(readConsent())
    override val given: StateFlow<Boolean> = _given.asStateFlow()
    override fun set(value: Boolean) {
        defaults.setBool(value, "consent_given")
        _given.value = value
    }

    private fun readConsent(): Boolean =
        if (defaults.objectForKey("consent_given") == null) true
        else defaults.boolForKey("consent_given")
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
