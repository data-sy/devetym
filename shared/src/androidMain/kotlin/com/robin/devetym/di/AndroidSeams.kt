package com.robin.devetym.di

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.OnboardingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Android seam actual (M8 §3-1·§3-2). ⚠️ 런타임 동작(mailto 열림·공유·클립보드)은 실기기 천장 — 여기선
 * 컴파일·조립까지 보증. 모두 Application `context`(FLAG_ACTIVITY_NEW_TASK).
 */
class AndroidAppActions(private val context: Context) : AppActions {
    override fun sendMail(to: String, subject: String, body: String) {
        val uri = Uri.parse("mailto:$to?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
        start(Intent(Intent.ACTION_SENDTO, uri))
    }

    override fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        start(Intent.createChooser(send, null))
    }

    override fun requestReview() =
        openUrl("https://play.google.com/store/apps/details?id=${context.packageName}")  // 스토어 폴백(§7)

    override fun copyToClipboard(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("devetym", text))
    }

    override fun openUrl(url: String) = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun start(intent: Intent) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun prefs(context: Context) = context.getSharedPreferences("devetym", Context.MODE_PRIVATE)

class PrefsAppearanceStore(context: Context) : AppearanceStore {
    private val prefs = prefs(context)
    private val _mode = MutableStateFlow(prefs.getInt("appearance_mode", 2))   // 부재 시 다크(안전)
    override val mode: StateFlow<Int> = _mode.asStateFlow()
    override fun set(mode: Int) {
        prefs.edit().putInt("appearance_mode", mode).apply()
        _mode.value = mode
    }
}

class PrefsOnboardingStore(context: Context) : OnboardingStore {
    private val prefs = prefs(context)
    override val completed: Boolean get() = prefs.getBoolean("onboarding_done", false)
    override fun complete() { prefs.edit().putBoolean("onboarding_done", true).apply() }
}

class PrefsDeviceIdProvider(context: Context) : DeviceIdProvider {
    private val prefs = prefs(context)
    override fun get(): String {
        prefs.getString("device_id", null)?.let { return it }
        val id = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", id).apply()
        return id
    }
}

class AndroidDeviceInfo(private val context: Context) : DeviceInfo {
    override fun appVersion(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    override suspend fun instanceId(): String? = null   // Firebase 미도입(§7)
}
