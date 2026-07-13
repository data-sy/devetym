# M9 서명·빌드·업로드 가이드 (2026-07-05)

> **성격: `[AI→사람]` 지그.** keystore 생성·서명·스토어 업로드 **단계 가이드**(런북 후속). ⚠️ **실제 keystore/인증서
> 생성·서명은 비밀키라 `[사람]`.** AI는 명령·순서를 깔고, 사람이 비밀키를 만들어 실행한다. `[사람]` 잔여 =
> 서명·**심사 제출·게시**(외부 대면, 사람 지시 대기 — §7 Q5).
>
> 현재 상태: `androidApp/build.gradle.kts` release 빌드타입에 **signingConfig 미배선**(`isMinifyEnabled=false`만).
> `versionCode=1`·`versionName="0.1.0"`. 아래 §1이 서명 배선을 안내.

## 1. Android — keystore 생성 & 서명 배선 (`[사람]` 비밀키 생성)

### 1-1. keystore 생성 (사람 — 비밀번호는 안전한 곳에, repo 커밋 금지)
```bash
keytool -genkeypair -v -keystore devetym-release.jks \
  -alias devetym -keyalg RSA -keysize 2048 -validity 10000
# 비밀번호 2개(store/key)·이름·조직 입력. .jks 파일과 비밀번호를 절대 커밋하지 말 것.
```

### 1-2. `~/.gradle/gradle.properties` 또는 환경변수에 자격 주입 (repo 밖)
```properties
DEVETYM_STORE_FILE=/절대경로/devetym-release.jks
DEVETYM_STORE_PASSWORD=****
DEVETYM_KEY_ALIAS=devetym
DEVETYM_KEY_PASSWORD=****
```

### 1-3. `androidApp/build.gradle.kts` signingConfig 배선 (제안 diff — 사람 승인 후 적용)
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(providers.gradleProperty("DEVETYM_STORE_FILE").get())
            storePassword = providers.gradleProperty("DEVETYM_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("DEVETYM_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("DEVETYM_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false          // 또는 R8 켤 때 true + proguard-rules
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```
> ⚠️ 이 배선은 governance/데이터면이 아닌 빌드 구성 변경 — **사람 승인 후** 적용(자율 금지: 비밀키 경로·릴리즈 서명).

### 1-4. AAB 빌드 & 검증
```bash
./gradlew :androidApp:bundleRelease          # AAB 산출: androidApp/build/outputs/bundle/release/
# 서명 확인:
jarsigner -verify -verbose -certs androidApp/build/outputs/bundle/release/androidApp-release.aab
```

### 1-5. Play Console 업로드
1. Play Console → 앱 만들기 → 프로덕션/내부테스트 트랙
2. AAB 업로드 → **Play App Signing**(구글 관리 서명키) 등록 권장
3. 데이터 보안 폼 = [스토어 메타 초안](m9-store-metadata-draft.md) §3-2
4. 개인정보 처리방침 URL 입력(방침 배포 주소)
5. 콘텐츠 등급·타깃 연령(14+)·심사 제출 → **`[사람]` 게시 지시 대기**

## 2. iOS — 서명 & App Store Connect (`[사람]` 인증서 + Xcode)

> iOS 서명·아카이브·appiconset은 **Xcode 빌드**(축 밖, `[사람]`). SKIE 프레임워크는 링크 green(4축) — 앱 셸/서명은 Xcode.

1. **appiconset**: `iosApp/.../Assets.xcassets/AppIcon.appiconset`에 상속 PNG 배치 확인(현재 부재 → 생성 필요).
   [아이콘 렌더 시트](m9-icon-render-sheet.html) §4로 배치 전 모양 컨펌.
2. **서명**: Xcode → Signing & Capabilities → Team 선택 → 자동 서명(Automatically manage signing) 또는 수동 프로비저닝.
3. **버전**: `CFBundleShortVersionString`(=0.1.0)·`CFBundleVersion`(빌드번호) 설정.
4. **아카이브**: Xcode → Product → Archive → Distribute App → App Store Connect 업로드.
5. **App Privacy**: App Store Connect = [스토어 메타 초안](m9-store-metadata-draft.md) §3-3. ATT 프롬프트 불필요(추적 없음).
6. 심사 제출 → **`[사람]` 게시 지시 대기**.

## 3. 게시 전 최종 확인 (사람)
- ☐ keystore/인증서 안전 보관·백업(분실 시 앱 업데이트 불가)
- ☐ `.jks`·비밀번호·프로비저닝 프로파일 **repo 미커밋** 확인(.gitignore)
- ☐ versionCode/Version 정합(Play·App Store)
- ☐ 방침 URL 실배포·접근 가능
- ☐ [실기기 스모크 대본](m9-device-smoke-script.md) 통과 후에만 제출
- ☐ **게시는 사람 지시 대기**(자율 금지 — §7 Q5)
