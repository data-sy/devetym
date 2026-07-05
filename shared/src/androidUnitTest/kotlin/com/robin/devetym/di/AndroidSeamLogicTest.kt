package com.robin.devetym.di

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * M9 §3-3 (iii)(iv)(i) — Android seam **로직** 계약 테스트(Robolectric).
 * OS 핸드오프(앱 실제 열림·시트 표시·붙여넣기)는 `[사람]` 실기기 잔여지만, Intent **구성**·클립보드 **쓰기**·
 * 외관 set→emit **로직**은 `[AI]`가 닫는다. 순수 JVM에선 `Intent(...)`/`ClipboardManager`가 "not mocked"으로
 * 즉사하므로 Robolectric 필수(§7 Q1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidSeamLogicTest {

    private val app: Application = RuntimeEnvironment.getApplication()

    @Test
    fun test_androidActions_sendMail_intent구성() {
        AndroidAppActions(app).sendMail(to = "a@b.com", subject = "제 목", body = "본 문")
        val intent = shadowOf(app).nextStartedActivity
        assertNotNull(intent, "sendMail이 startActivity 안 함")
        assertEquals(Intent.ACTION_SENDTO, intent.action, "메일은 ACTION_SENDTO여야(메일앱만 매칭)")
        assertEquals("mailto", intent.data?.scheme, "mailto scheme 아님")
    }

    @Test
    fun test_androidActions_share_intent구성() {
        AndroidAppActions(app).share("공유텍스트")
        val chooser = shadowOf(app).nextStartedActivity
        assertNotNull(chooser)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action, "share는 createChooser로 감싸야")
        @Suppress("DEPRECATION")
        val inner = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(inner, "chooser에 내부 SEND intent 없음")
        assertEquals(Intent.ACTION_SEND, inner.action)
        assertEquals("text/plain", inner.type, "공유 MIME text/plain 아님")
        assertEquals("공유텍스트", inner.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun test_clipboard_write() {
        AndroidAppActions(app).copyToClipboard("복사값")
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        assertNotNull(clip, "클립보드 primaryClip null")
        assertEquals("복사값", clip.getItemAt(0).text.toString(), "클립보드 쓰기 값 불일치")
    }

    @Test
    fun test_prefsAppearanceStore_set_emit() {
        val store = PrefsAppearanceStore(app)   // 실 SharedPreferences seam(스텁 아님)
        store.set(1)
        assertEquals(1, store.mode.value, "PrefsAppearanceStore set→emit 불일치")
        // 새 인스턴스가 영속값을 읽는지(SharedPreferences 왕복)
        assertEquals(1, PrefsAppearanceStore(app).mode.value, "SharedPreferences 영속 왕복 실패")
    }
}
