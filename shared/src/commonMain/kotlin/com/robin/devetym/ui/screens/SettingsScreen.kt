package com.robin.devetym.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.theme.AppScheme

/** 설정 화면 (M6 §3-7). VM 없음 — 플랫폼 seam 소비(actual은 M8). */
@Composable
fun SettingsScreen(
    actions: AppActions,
    appearance: AppearanceStore,
    device: DeviceInfo,
    consentGiven: Boolean,
    onConsentChange: (Boolean) -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val mode by appearance.mode.collectAsStateWithLifecycle()
    SettingsContent(mode, appearance::set, device.appVersion(), consentGiven, onConsentChange, actions, onOpenLicenses)
}

@Composable
fun SettingsContent(
    mode: Int,
    onModeChange: (Int) -> Unit,
    appVersion: String,
    consentGiven: Boolean,
    onConsentChange: (Boolean) -> Unit,
    actions: AppActions,
    onOpenLicenses: () -> Unit,
) {
    val dim = AppScheme.dim
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = dim.screenPadding),
    ) {
        Text("설정", style = AppScheme.type.titleTab, color = AppScheme.colors.text,
            modifier = Modifier.padding(vertical = 24.dp))

        Section("외관") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("시스템" to 0, "라이트" to 1, "다크" to 2).forEach { (label, value) ->
                    Text(
                        label,
                        style = AppScheme.type.codeAction,
                        color = if (mode == value) AppScheme.colors.accent else AppScheme.colors.textMuted,
                        modifier = Modifier.clickable { onModeChange(value) },
                    )
                }
            }
        }
        Section("앱 정보") {
            InfoRow("버전", appVersion)
        }
        Section("지원") {
            ActionRow("개발자에게 문의") { actions.sendMail("data.sy.2@gmail.com", "DevEtym 문의", "") }
            ActionRow("앱 평가하기") { actions.requestReview() }
            ActionRow("오류 제보") { actions.sendMail("data.sy.2@gmail.com", "DevEtym 오류 제보", "") }
        }
        Section("데이터 수집") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("사용 데이터 수집 동의", style = AppScheme.type.bodyLarge, color = AppScheme.colors.text,
                    modifier = Modifier.weight(1f))
                Switch(checked = consentGiven, onCheckedChange = onConsentChange)
            }
            ActionRow("개인정보 처리방침") { actions.openUrl("https://devetym.app/privacy") }
        }
        Section("법적 고지") {
            ActionRow("오픈소스 라이선스", onOpenLicenses)
            Text("✦ 어원 정보는 AI가 생성하며 부정확할 수 있어요", style = AppScheme.type.caption,
                color = AppScheme.colors.textMuted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(title.uppercase(), style = AppScheme.type.sectionHeader, color = AppScheme.colors.accent,
            modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = AppScheme.type.bodyLarge, color = AppScheme.colors.text, modifier = Modifier.weight(1f))
        Text(value, style = AppScheme.type.codeValue, color = AppScheme.colors.textMuted)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(label, style = AppScheme.type.bodyLarge, color = AppScheme.colors.text,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp))
}
