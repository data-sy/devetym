# M6 슬라이스 (draft) — Compose UI (디자인 시스템 + 6화면 + 네비게이션)

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** 단일 [`spec.md`](spec.md) Phase 3의 UI 부분을 마일스톤 경계로 떼어낸 것. 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md), 상위 설계는 [`../architecture.md`](../architecture.md)(§4.5·§4.6), 결정 근거는 [`../adr/`](../adr/).
>
> 이 문서는 **자율 구현 전 적대적 비준의 대상**이다. §7 열린 질문을 비준이 판정한다.
>
> **⚠️ 검증 천장(이 마일스톤의 정직 경계).** M6의 green 오라클은 **컴파일·조립·링크**(4축 중 3축) + **순수 헬퍼(토큰·타이포·상대시간·상태매핑) 네이티브 실행**까지다. **Compose 화면의 시각 충실도(간격·폰트 렌더·픽셀·정렬·다크모드 실제 색)·상호작용·애니메이션은 이 마일스톤에서 검증되지 않는다** — 그건 실기기/시뮬레이터 스크린샷·손 검증이 필요하며 **「코드 완료·실기기 시각 검증 필요」로 라벨**해 아침 사람 리뷰로 넘긴다. 이 슬라이스는 green을 '화면이 옳게 보인다'로 **자칭하지 않는다**(구조·상태분기·배선·컴파일만 보증).

- **마일스톤**: M6 · Compose UI
- **브랜치**: `feat/m6-compose-ui` (=`feat/m5-viewmodel` 위에 **스택** 분기)
- **참조**: spec [3-x](spec.md), architecture §4.5·§4.6, [ADR-0002](../adr/0002-code-idiom-principle.md)(반응형·우회 삭제), [ADR-0005](../adr/0005-ios-interop.md), [M5 슬라이스](m5-viewmodel-draft.md)(ViewModel·이월), **디자인 정본**: iOS `~/dev-etymology/DevEtym/DevEtym/Utils/Theme.swift`(토큰 ground truth)·`Features/*/`(화면 구조)·`Resources/Fonts/`(7 ttf)·`docs/design/icon/`(아이콘). 메모리 [ios-design-assets-inheritable].

---

## 1. 목표 (이 슬라이스가 내는 것)

`commonMain/ui/`에 **디자인 시스템**(폰트·색상·타이포·간격/모양 토큰 + 테마 프로바이더)과 그 위에 **6화면**(검색·상세·북마크·히스토리·설정·온보딩) Composable + **네비게이션**(4탭 + 상세 push + 온보딩 cover)을 세운다. 각 화면은 M5 ViewModel의 `StateFlow`를 `collectAsStateWithLifecycle`로 구독해 상태분기(빈/로딩/오류/결과)를 그리기만 한다(architecture §4.5 — UI는 ViewModel만 안다). iOS 검증본의 **디자인 토큰과 화면 구조를 코틀린/Compose 관용으로** 계승하되(하이브리드 폰트·상태머신·네비 흐름), SwiftUI 특유의 우회(수동 재조회·`@Query` 부재 보완)는 옮기지 않는다(ADR-0002 — 목록은 M5 반응형 Flow 소비).

**M5 이월 마감**: 상세 북마크 현재값 상태 소스(DR-4)와 History limit 구체값(DR5-3)을 이 슬라이스가 닫는다(§3-8·§3-9-2). **쓰기 유실창(DR5-2)은 이 슬라이스가 닫지 않고 이월**한다 — 파생 읽기 Flow는 STAY 케이스 피드백만 주고 LEAVE-즉시 쓰기 유실(fire-and-forget 취소)은 미착지다(§3-8).

## 2. 스코프

**IN (M6):**
- **폰트**(§3-1): 7 ttf를 `composeResources/font/`로 이관, `FontFamily` 로더. **하이브리드 전략**: 한글 비중 본문=`FontFamily.Default`(플랫폼 시스템 — iOS SF/Android Roboto, iOS의 "SF 전환" 계승), 영문 코드/키워드=DM Mono, 시그니처 헤더=DM Serif.
- **색상 토큰**(§3-2): 라이트/다크 11토큰(hex 정본=`Assets.xcassets/Theme/*.colorset`; `Theme.swift` Palette는 Asset 참조·토큰 이름만), `AppColors` + `CompositionLocal`, **다크 기본**.
- **타이포 토큰**(§3-3): `Theme.Typography`의 `.typoX` 토큰(정본 파일 실개수, 계승)을 Compose `TextStyle`로 이관(폰트+size+weight 묶음; lineHeight/letterSpacing 정본과 이월은 §3-3).
- **간격/모양 토큰**(§3-4): iOS 인라인 값에서 **신규 추출**(spacing 스케일·corner radius·stroke) — iOS엔 토큰 없음.
- **테마 프로바이더**(§3-5): `AppTheme { }` = MaterialTheme(colorScheme 매핑) + `CompositionLocalProvider`(AppColors·AppTypography·AppDimens).
- **재사용 원자**(§3-6): `CategoryBadge`·`AiBadge`·`FlowChip`·`PulsingDots`(로딩)·`EmptyState`·`RelativeTime`(상대시간 순수 헬퍼).
- **6화면**(§3-7): Search·Detail·Bookmark·History·Settings·Onboarding Composable. M5 VM 구독(있는 4화면)·상태분기·레이아웃 순서·액션 바인딩.
- **네비게이션**(§3-8): 4탭 Scaffold(검색·북마크·히스토리·설정) + 상세 push(keyword) + 온보딩 fullScreen. Compose Navigation.
- **플랫폼 seam**(§3-9): 외관모드 저장·mailto·공유·앱평가·클립보드·기기ID를 **interface/expect로 선언**하고 Compose가 호출. **actual 구현·런타임 검증은 M8 이월**(no-op/스텁 기본 제공으로 컴파일·조립 green).
- **순수 헬퍼 테스트**(§6): 토큰 값·타이포 매핑·상대시간·상태→표시 매핑(네이티브 실행). Compose 화면 자체는 컴파일·조립으로만 검증(천장).

**OUT (다른 마일스톤/트랙):**
- **Koin `viewModel { }` 실배선·`koinViewModel()` 실제 주입·앱 셸(MainActivity/iOS 진입점) 연결** → **M7**(spec 1-4). M6는 화면을 VM 주입 형태로만 정의하고, 프리뷰/테스트는 Fake VM 또는 직접 상태 주입.
- **플랫폼 seam actual 구현**(외관모드 UserDefaults/DataStore·mailto Intent/UIApplication·ShareSheet·StoreKit `requestReview`·Firebase App Instance ID·클립보드)·**그 런타임 검증** → **M8**. M6는 seam 인터페이스 + no-op 기본.
- **앱 아이콘(adaptive/appiconset)·스플래시(런치 스크린)·폰트 라이선스 고지 자산** → **M8**(SVG→플랫폼 자산 내보내기).
- **접근성 세부(TalkBack/VoiceOver 레이블 감사)·Dynamic Type/글꼴 크기 대응·에러 통합 실측** → **M8**.
- **실기기 시각 검증·스크린샷·상호작용·애니메이션 타이밍** → 사람/실기기(검증 천장, §0).

## 3. 산출 명세

### 3-1. 폰트 (`composeResources/font/` + `ui/theme/AppFonts.kt`)

- 7 ttf를 iOS `Resources/Fonts/`에서 `composeResources/font/`로 복사(compose-resources 명명 규칙 — 소문자·언더스코어: `dmsans_regular.ttf` 등). OFL 라이선스 txt도 동반 이관(고지는 M8).
- `dmSerif`(Regular/Italic)·`dmMono`(Light/Regular/Medium)·`dmSans`(Regular/Medium) `FontFamily`를 `Res.font.*`로 구성(`@Composable Font(...)`).
- **하이브리드 원칙**(iOS Theme.swift 계승): `bodyFamily = FontFamily.Default`(한글 시스템 폰트 — 커스텀 폰트 박스에 한글이 작게 끼는 문제 회피), `codeFamily = dmMono`, `serifFamily = dmSerif`. DM Sans는 영문 라벨 한정(잔여 — 대부분 SF로 대체됨).

### 3-2. 색상 토큰 (`ui/theme/AppColors.kt`)

```kotlin
@Immutable data class AppColors(
    val bg, surface, surface2, border, accent, accent2, accentAI, brand, text, textDim, textMuted: Color
)
val LightColors = AppColors(bg=0xFFFAFAFA, surface=0xFFFFFFFF, surface2=0xFFF1F1F1, border=0xFFE4E4E4,
    accent=0xFF3F7A00, accent2=0xFF1E6B94, accentAI=0xFFA35A10, brand=0xFF2E5D3A, text=0xFF0A0A0A, textDim=0xFF555555, textMuted=0xFF6B6B6B)
val DarkColors = AppColors(bg=0xFF0A0A0A, surface=0xFF111111, surface2=0xFF1A1A1A, border=0xFF363636,
    accent=0xFFC8F060, accent2=0xFF60C8F0, accentAI=0xFFF0A060, brand=0xFF2E5D3A, text=0xFFECECEC, textDim=0xFFB4B4B4, textMuted=0xFF8A8A8A)
val LocalAppColors = staticCompositionLocalOf { DarkColors }   // 다크 기본
```
- hex 정본은 `Assets.xcassets/Theme/*.colorset/Contents.json`(라이트=기본 `components`, 다크=`luminosity/dark` appearances). `Theme.swift` Palette는 토큰 이름·Asset 참조(`Color("Theme/bg")`)만 담고 **hex 값은 없다** — 정본을 열어도 대조 대상이 없으므로 색 대조 소스는 colorset이다. 위 hex 표는 그 colorset 실측을 인라인한 11토큰 전량이며, §6 `test_색상토큰_정본일치`가 대표값을 colorset 정본과 대조(순수, 네이티브).

### 3-3. 타이포 토큰 (`ui/theme/AppTypography.kt`)

- iOS `.typoX` 토큰(정본 `Theme.swift` Typography — 2026-07 기준 21종: titleHero·titleTab·bodyLarge·body·bodySub·bodyEmphasis·bodyBlock·bodyNotice·bodyPreview·bodyPreviewSmall·codeHero·codeBody·codeInput·codeValue·badge·badgeAI·codeChip·codeAction·sectionHeader·label·caption)을 `TextStyle`로 이관. **정본에서 직접 얻는 값은 (fontFamily, fontSize, fontWeight)뿐**이다(`Theme.swift`는 `.custom(name,size,relativeTo:)`/`Font.system`만 — lineHeight/letterSpacing 값 없음). letterSpacing/lineHeight의 정본은 **iOS Feature SwiftUI 뷰의 `.tracking()`/`.lineSpacing()` 모디파이어**이며, 이는 토큰 단위로 정리돼 있지 않고 뷰마다 흩어져 있어 **정확값은 시각 천장(§0)으로 이월**한다(아침 실기기 리뷰가 확정). 예(family+size+weight만 정본): `titleHero = TextStyle(serifFamily, 28.sp, W400)`, `codeBody = TextStyle(codeFamily, 17.sp, W500)`, `badge = TextStyle(codeFamily, 12.sp, W500)`(caps는 렌더에서 uppercase). tracking을 얹을 경우 그 sp 값은 정본이 아니라 시각 근사치이며 green이 보증하지 않는다.
- `@Immutable data class AppTypography(val titleHero, titleTab, body, bodySub, bodyEmphasis, bodyBlock, codeHero, codeBody, codeInput, badge, badgeAI, codeChip, codeAction, sectionHeader, label, caption, ... : TextStyle)` + `LocalAppTypography`.
- iOS `.lineSpacing()`(pt) → Compose `lineHeight`, `.tracking()`(pt) → `letterSpacing`(sp)로 얹되, 이 두 값은 정본이 없어(위 참조) **정확 매칭은 검증 천장(실기기)** — §6는 토큰 존재·패밀리·대표 size/weight만 실측하고 lineHeight/letterSpacing은 검증하지 않는다(그 정확성은 아침 리뷰 소관).

### 3-4. 간격/모양 토큰 (`ui/theme/AppDimens.kt`) — 신규 추출

```kotlin
@Immutable data class AppDimens(
    val screenPadding=18.dp, val cardPadding=14.dp, val sectionGap=26.dp, val labelGap=9.dp, val rowVPad=11.dp,
    val radiusCard=12.dp, val radiusAction=10.dp, val radiusBlock=8.dp, val radiusBadge=6.dp,
    val strokeBorder=1.dp, val strokeField=1.5.dp,
)  // + LocalAppDimens
```
- iOS Feature 뷰들의 인라인 관찰값(패딩·라운드·스트로크 리터럴)에서 추출하되, iOS에 토큰 정본이 없으므로 위 data class 값이 M6가 세우는 정본이다. spacing 스케일을 토큰화해 화면들이 매직넘버 대신 참조. 개별 화면의 실측 간격 정확성은 시각 천장(§0).

### 3-5. 테마 프로바이더 (`ui/theme/AppTheme.kt`)

```kotlin
@Composable fun AppTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalAppColors provides colors, LocalAppTypography provides AppTypography(),
        LocalAppDimens provides AppDimens()) {
        MaterialTheme(colorScheme = colors.toMaterialColorScheme(dark)) { content() }
    }
}
object AppScheme { val colors @Composable get() = LocalAppColors.current; val type ...; val dim ... }  // 접근 편의
```
- MaterialTheme colorScheme은 최소 매핑(background/surface/primary=accent/onX). 앱은 주로 `AppScheme.colors`를 직접 참조(커스텀 토큰이 정본, Material은 스캐폴드 컴포넌트 색만).

### 3-6. 재사용 원자 (`ui/components/`)

- `CategoryBadge(category)`: surface2 fill + accent 텍스트, radiusBadge, badge 타이포 uppercase. 카테고리 6종(`Category.CANONICAL`) 색 매핑은 accent 단색(iOS도 단색).
- `AiBadge()`: "✦ AI 생성", accentAI, badgeAI 타이포(source==AI일 때만 상세가 표시).
- `FlowChip(text, onClick)`: Capsule, surface2, codeChip. (검색 최근·자동완성 미리보기용.)
- `PulsingDots()`: 좌→우 pulse 3닷(로딩) — 애니메이션(타이밍은 검증 천장, 컴파일만 보증).
- `EmptyState(icon, message)`: 빈 목록 공통(북마크·히스토리·자동완성 없음).
- `RelativeTime(epochMillis, now)`: **순수 헬퍼** — **경과 diff 기반**(캘린더/타임존/DST 비의존; `now - epochMillis`만 사용): `<1분`="방금 전", `<60분`="N분 전", `<24시간`="N시간 전", `<48시간`="어제", `그 이상`="N일 전"(ko). iOS `RelativeDateTimeFormatter`가 캘린더 기준('어제'=날짜 경계)인 점과 갈릴 수 있으나 M6는 **경과 diff 기반으로 못박아** 자정/DST 경계 모호성을 제거한다(iOS 계승은 라벨 어휘만, 경계 규칙은 diff). `1~23시간` 구간이 정의돼 있어 리터럴 포팅 시 무의미 라벨('300분 전')로 새지 않는다. §6 네이티브 실측(클록 주입).

### 3-7. 6화면 (`ui/screens/`) — 각 화면 구조·상태분기·VM 바인딩

각 화면은 **두 겹**: `XxxScreen(...)`(VM 구독 래퍼) + `XxxContent(state, [파생값,] callbacks)`(순수 상태→UI, 프리뷰/천장검증 대상). 래퍼가 VM `StateFlow`를 `collectAsStateWithLifecycle`로 구독해 **순수값만** `XxxContent`에 주입한다(Content는 VM/Repository를 모른다 — §4). 대부분 화면은 VM 하나지만 `DetailScreen`은 별표 파생을 위해 예외적으로 VM 둘을 받는다(시그니처는 아래 DetailScreen 항에서 확정).

- **SearchScreen**(`SearchViewModel`): header(titleHero "DevEtym" + label) → suggestions 있으면 하단정렬 리스트 / 없으면 recent FlowChips → 하단고정 검색필드(codeInput, 클리어). 액션: `onQueryChanged`(디바운스는 VM), 칩·행 탭→`onNavigateDetail(keyword)`, submit→`commit()?.let(onNavigateDetail)`. **로딩·오류는 이 화면에 없음**(상세로 위임).
- **DetailScreen**(`vm: DetailViewModel`, `bookmarkVm: BookmarkViewModel`) — **시그니처 정본**(§3-8 DR-4 마감): 래퍼가 `vm.state`와 `bookmarkVm.bookmarks`를 각각 `collectAsStateWithLifecycle`로 구독하고, 후자에서 순수 헬퍼 `isBookmarkedFor(bookmarks, entry.keyword)`로 파생한 `isBookmarked: Boolean`을 함께 `DetailContent(state, isBookmarked, callbacks)`로 주입한다(Content는 두 번째 VM/Repository를 직접 모른다 — §4 불변식 유지; placeholder `false` 하드코딩 경로 제거). `state: DetailUiState`(M5) 분기 —
  - `Loading`: `PulsingDots` + keyword(codeHero) + 단계 메시지(시간분할 LoadingPhase는 **M6 애니메이션 소관** — 최소 "어원을 찾고 있어요"만, 타이밍 천장).
  - `Result(Found(entry, src))`: ScrollView[ header(keyword titleHero + `CategoryBadge` + `AiBadge`(src==AI) + summary bodySub) → "어원" 섹션(etymologyBlock: 좌측 2dp accent바 + surface2) → "왜 이 이름인가" 섹션(namingReason) → actionRow(북마크 토글·공유) ] + 하단 오류제보.
  - `Result(NotDevTerm)`: questionmark + "개발 용어를 검색해주세요" + 돌아가기.
  - `Result(PossibleTypo(sug))`: lightbulb + "{sug}을(를) 찾으셨나요?" + suggestion 버튼(→재검색) + 돌아가기.
  - `Error(kind)`: `ErrorKind`(M5)별 한글 메시지(§3-9-3 매핑) 표시 + 돌아가기.
  - 진입 시 `load(keyword)`(`LaunchedEffect(keyword)`), 이탈 시 취소는 VM `onCleared`.
- **BookmarkScreen**(`BookmarkViewModel`): header(titleTab "북마크") → `bookmarks` 빈이면 `EmptyState` / 아니면 목록(keyword codeBody + preview + chevron). 행 탭→상세, 스와이프/버튼 삭제→`removeBookmark`(반응형 Flow 자동 반영, 수동 재조회 없음).
- **HistoryScreen**(`HistoryViewModel`): header(titleTab "히스토리" + "전체 삭제"(항목 있을 때)) → `history` 빈이면 `EmptyState` / 아니면 목록(keyword codeInput + `RelativeTime`). 행 탭→상세, 삭제→`delete`, 전체삭제→확인 후 `clearAll`(반응형 자동 반영).
- **SettingsScreen**(VM 없음 — §3-9 seam 소비): 5섹션(외관 Picker·앱정보·지원(문의/평가/제보)·데이터수집 동의 Toggle+식별자·법적고지). 각 항목은 §3-9 seam 호출(actual은 M8).
- **OnboardingScreen**(VM 없음): 2페이지 pager(인트로 + 데이터수집 동의) → `onComplete(consent)`. 최초 1회 게이트는 M7 셸/seam.

### 3-8. 네비게이션 (`ui/AppRoot.kt`)

- `AppRoot(deps)`: 온보딩 미완료면 `OnboardingScreen`, 아니면 4탭 `Scaffold`(NavigationBar: 검색·북마크·히스토리·설정, tint=accent, surface 배경). 각 탭은 자체 nav 스택, `Detail` route(keyword arg)로 push. Compose Navigation(멀티플랫폼) 또는 단순 상태기반 back stack.
- **⚠️ M5 DR-4 마감 — 상세 북마크 현재값 상태 소스**: `DetailScreen`이 북마크 별표 현재값을 그리려면 상태 소스가 필요하다(M5는 `DetailUiState.Result`에 `isBookmarked` 없음). **M6 결정**: 별표는 `Result(Found(entry, src))` 브랜치 안에서만 그려지므로(다른 상태엔 별표 없음), 교차조회를 **순수 헬퍼 `isBookmarkedFor(bookmarks: List<Term>, keyword: String): Boolean = bookmarks.any { it.keyword == normalizeKeyword(keyword) }`** 로 추출한다(색/타이포/상대시간/에러메시지/상태표시매핑과 동일 부류의 순수 함수 — §6 `test_isBookmarked_교차조회` 네이티브 실측으로 DR-4 종결을 실증). `DetailScreen` 래퍼는 `BookmarkViewModel.bookmarks` `StateFlow`(**반드시 ViewModel 경유** — §4 불변식상 화면은 `repository.bookmarkedTerms()`를 직접 관측하지 않는다)를 `collectAsStateWithLifecycle`로 구독하고 `isBookmarkedFor(bookmarks, entry.keyword)`로 파생 `isBookmarked: Boolean`을 계산해 그린다(시그니처 정본=§3-7). **교차조회 키를 조회 시점에 `normalizeKeyword`(=`trim().lowercase()`)로 정규화하는 것이 근거의 핵심이다 — `entry.keyword`가 이미 정규화돼 있다고 가정하지 않는다.** 정규화는 오직 AI 경로에서만 일어난다(`orchestrate()`가 `TermResult.Found` 저장 시 `copy(keyword=key)`, `TermRepository.kt:83`). **번들 Found 경로의 `entry`는 재정규화·저장되지 않아 `entry.keyword`가 `terms.json` 저작 원문 그대로**다(`orchestrate`는 `bundle.search(key)`가 돌려준 entry를 term 미저장·히스토리만 남기고 그대로 반환, `TermRepository.kt:58-60`; `InMemoryBundleDbSource.search`는 인덱스만 정규화하고 원본 `entry.keyword` 보존, `BundleDbSource.kt:54-57`). 따라서 '저장 시 정규화됨'은 번들 경로에 대해 사실이 아니며, 현재 정규화 일치는 번들 데이터가 전량 소문자로 authored된 미강제 관례로만 성립한다(장래 `OAuth`/`GraphQL`처럼 대소문자 섞인 번들 용어가 저작되면 관례가 깨짐). 저장 측 `toggleBookmark(entry)`도 `val key = normalizeKeyword(entry.keyword)`로 로우를 조회·저장하므로(`TermRepository.kt:116`), 표시 측 교차조회를 **동일하게 `normalizeKeyword(entry.keyword)`로 맞추면 두 경로가 같은 키 파생 위에서 매치**되어 근거가 authoring 관례가 아니라 **코드 불변식**이 된다. **라우트 원형 keyword(`SearchViewModel.commit()`=`trim()`만, lowercase 안 함) 또는 번들 원문 `entry.keyword`를 정규화 없이 교차조회하는 경로는 금지**(그렇게 하면 저장값 `oauth`와 원문 `OAuth`가 불일치→별표가 조용히 오표시되고, 재탭 시 기존 `oauth` 로우를 찾아 un-bookmark로 뒤집히며, STAY 케이스 확정 갱신도 매치 실패로 깨진다). 이로써 **DR-4(상태 소스 부재)만 닫는다**: 반응형이라 STAY(상세 체류) 케이스에서 토글 즉시 별표가 확정 갱신된다.
- **⚠️ DR5-2는 이 슬라이스에서 닫지 않고 이월(carry-forward)한다.** 위 파생 읽기 Flow는 **STAY 케이스의 확정 피드백만** 준다. LEAVE-즉시 케이스(별표 탭 직후 상세 이탈)의 쓰기 유실은 미해결이다 — 정본 `DetailViewModel.toggleBookmark()`가 여전히 `viewModelScope.launch`(fire-and-forget)라 이탈 시 `onCleared→viewModelScope.cancel()`이 아직 디스패치 안 된 toggle launch를 취소→DB 미변경→`bookmarks` 재방출 없음→가시화·자기수정할 것이 없다(사용자는 이미 화면을 떠나 별표를 못 본다). 파생 읽기(표시)는 쓰기 내구성과 직교한다. `toggleBookmark`를 앱/repository 스코프 launch(또는 `NonCancellable`)로 내구화하는 것은 이 슬라이스 밖이므로 **DR5-2는 정직히 이월**한다(§7-Q2 비준 판정).

### 3-9. 플랫폼 seam + 매핑 (`ui/platform/` · `ui/DetailMessages.kt`)

- **3-9-1 seam 인터페이스**: `AppActions { fun sendMail(to,subject,body); fun share(text); fun requestReview(); fun copyToClipboard(text); fun openUrl(url) }` + `AppearanceStore { val mode: StateFlow<Int>; fun set(mode) }` + `DeviceInfo { fun appVersion(); suspend fun instanceId() }`. **M6는 no-op/스텁 기본 구현 제공**(컴파일·조립 green), actual은 M8.
- **3-9-2 DR5-3 마감 — History limit**: `HistoryViewModel.HISTORY_LIMIT`(M5=`Int.MAX_VALUE` 전량)를 UI가 그대로 소비(절단 없음). iOS는 100 cap이었으나 CMP는 전량 노출 결정 유지(관리 화면 목적). **비준이 `Int.MAX_VALUE` 전량 vs 합리적 상한(예: 200)을 판정**(§7).
- **3-9-3 `ErrorKind`→한글 메시지**(순수, `DetailMessages.kt`): `Timeout`→"응답이 지연되고 있어요. 잠시 후 다시 시도해주세요", `Network`→"인터넷 연결을 확인해주세요", `DailyLimitExceeded`→"오늘 사용량을 모두 사용했어요", `InvalidResponse`→"결과를 불러오지 못했어요", `Unknown`→"문제가 발생했어요". §6 `test_errorKind_메시지_전수` 네이티브 실측(when 전수·else 없음 canary).

## 4. 설계 불변식

- **UI는 ViewModel(또는 순수 상태)만 안다**(architecture §4.5): 화면은 Repository·DB·Ktor를 직접 참조하지 않는다. `XxxContent`는 상태+콜백만(프리뷰 가능).
- **상태분기 `when` 전수**: `DetailUiState`·`TermResult`·`ErrorKind` 소비 `when`은 `else` 없이 전수(DR-3 canary).
- **반응형(ADR-0002)**: 목록·북마크 별표는 M5 `StateFlow` 구독. 데이터 변경 후 수동 재조회·수동 리스트 조작 없음.
- **토큰 단일 진입점**: 화면은 색/타이포/간격을 raw 리터럴이 아니라 `AppScheme.colors/type/dim`으로 참조(iOS `.typoX` 단일 진입 계승). 매직 색/크기 금지.
- **검증 천장 정직**(§0): 시각·상호작용·애니메이션은 green이 보증하지 않는다 — 라벨로 넘긴다.
- **다크 기본**: 테마 기본은 다크(iOS `appearanceMode` 기본 2 계승).

## 5. 완료 조건 (DoD) — 하네스 수렴 오라클(천장 명시)

- **컴파일·조립·링크 green(3축)**: `:shared:testDebugUnitTest`(순수 헬퍼 테스트 포함) + `:androidApp:assembleDebug`(모든 Composable·테마·네비 컴파일·조립) + `:shared:linkDebugFrameworkIosSimulatorArm64`(네이티브 링크). 폰트 리소스·compose-navigation·lifecycle-runtime-compose 좌표가 Kotlin 2.3.21·CMP 1.11.1에서 소비됨을 실빌드로 확정(load-bearing).
- **⊕ 순수 헬퍼 네이티브 실행**: `:shared:iosSimulatorArm64Test` green — §6의 토큰 값·타이포 매핑·`RelativeTime`·`ErrorKind`→메시지·상태→표시 매핑이 네이티브 실행으로 실측. **단, Compose 화면 렌더 자체는 이 축이 실행하지 않는다**(UI 테스트 하네스 부재 — 천장).
- §6 테스트 통과.
- **명시적 비-보증(정직)**: 화면 시각 충실도·간격·폰트 렌더·다크 실제색·탭/스와이프/애니메이션은 **이 DoD가 보증하지 않으며** 「코드 완료·실기기 검증 필요」로 아침 리뷰 이월.

## 6. 테스트 (`commonTest/`) — 순수 헬퍼만(Compose 렌더는 천장)

> Compose UI 렌더 테스트(`compose-ui-test`)는 Android 계측/데스크톱 하네스가 필요해 4축(특히 네이티브)에 안 실려 **이 슬라이스 범위 밖**(천장). 대신 화면 로직을 **순수 헬퍼로 뽑아** 네이티브 실측한다.

- `test_색상토큰_정본일치` — Light/Dark 대표값(accent·bg·text) hex가 `Assets.xcassets/Theme/*.colorset` 정본과 일치(`Theme.swift` Palette는 Asset 참조만이라 hex 없음 — colorset이 대조 소스).
- `test_타이포토큰_패밀리매핑` — `codeBody`.fontFamily==codeFamily, `titleHero`==serif, `body`==bodyFamily(Default). 크기/weight 대표 검증.
- `test_errorKind_메시지_전수` — `ErrorKind` 5종 각 한글 메시지 매핑(when 전수, else 없음).
- `test_relativeTime_경계` — now-30s→"방금 전", -5m→"5분 전", **-3h→"3시간 전"**, -1d→"어제", -3d→"3일 전"(클록 주입, ko; 경계 규칙=경과 diff 기반 §3-6).
- `test_detailState_표시매핑` — `DetailUiState`/`TermResult` → 순수 매핑 함수 `detailPresentation(state)`(Loading/Found/NotDevTerm/PossibleTypo/Error 각 라벨·아이콘 키)의 반환을 분기별로 실측(when 전수). **이 테스트는 매핑 함수 자체의 반환 정확성만 보증하며, `DetailContent` 화면이 그 키대로 렌더한다는 것(매핑↔렌더 결속)은 보증하지 않는다** — 그 결속은 시각 천장(§0)으로, 아침 실기기 리뷰 소관이다. 분기 누락만 §4 when 전수 canary가 컴파일 강제한다.
- `test_categoryColor_클램프` — 카테고리 6종+범위밖 → 색 매핑(범위밖은 accent 기본, 크래시 없음).
- `test_isBookmarked_교차조회` — `isBookmarkedFor(bookmarks, keyword)`가 (번들 원문 대소문자 섞임 `OAuth`·AI정규화 `oauth`·미북마크·별칭/원형 경유) 각 케이스에서 저장 로우(`normalizeKeyword` 정규화값)와 `normalizeKeyword(keyword)` 정합으로 매치되는지 네이티브 실측(DR-4 종결 실증 — §3-8 순수 헬퍼).

## 7. 열린 질문 (비준이 판정할 항목)

1. **네비게이션 라이브러리 — Compose Navigation(멀티플랫폼) vs 단순 상태기반 back stack** — 제안: `org.jetbrains.androidx.navigation:navigation-compose`(CMP 정합 버전 실빌드 확정). 대안: sealed route + `StateFlow` 백스택(의존성 0, 테스트 쉬움). 비준이 네이티브 klib 소비·탭별 스택 요구에 비춰 판정.
2. **DR-4 상세 북마크 상태 소스 — `BookmarkViewModel.bookmarks` 파생 교차조회(제안) vs `DetailUiState.Result`에 `isBookmarked` 추가(M5 상태 확장)** — 제안: ViewModel 경유 파생, `normalizeKeyword(entry.keyword)`로 교차조회(조회 시점 정규화 — 번들 Found `entry.keyword`는 저작 원문이라 '저장 시 정규화됨' 가정 불가, 저장 측 `toggleBookmark`의 `normalizeKeyword(entry.keyword)`와 동일 키 파생으로 코드 불변식화; §3-8, M5 상태 불변). 비준이 파생 읽기가 STAY 피드백을 원형/정규화 키 불일치 없이 정확히 주는지, 그리고 DR5-2 쓰기 내구성 이월이 정직한지 판정. 대안(M5 상태에 `isBookmarked`를 얹는 안)이면 원형-정규화 불일치가 사라진다.
3. **History limit(DR5-3) — `Int.MAX_VALUE` 전량(제안) vs 합리적 상한** — §3-9-2. 비준 판정.
4. **플랫폼 seam 기본 구현 — no-op 스텁(제안) vs expect/actual 최소 실구현 당김** — 제안: no-op 기본(M6 조립 green), 실구현·검증 M8. 비준이 이 이월이 정직한지(스텁이 조립을 거짓 green으로 만들지 않는지) 판정.
5. **검증 천장 표현 — 순수 헬퍼 추출 범위** — 화면 로직을 어디까지 순수 함수로 뽑아 네이티브 실측할지(제안: 색/타이포/상대시간/에러메시지/상태표시매핑). 비준이 '천장 위장'(테스트 불가한 걸 테스트한 척) 없이 정직한지 판정.

## 8. 안전·규율

- **검증 천장 정직**(§0·§5): 시각·상호작용은 green 자칭 금지, 라벨로 아침 리뷰 이월. **거짓 green 금지.**
- 마일스톤 경계 **사람 게이트 완화**(메모리 `milestone-human-gate-relaxed`): 적대 비준 후 eyes-open 수용·구현 자율, 사람은 완성물 사후 리뷰. 하네스는 push·머지·`-draft` 제거 안 함.
- **디자인 정본 = iOS `Theme.swift`**(typography-review.md는 stale — 인벤토리 경고). 계승만, 재설계 아님.
- **M5 이월 마감**: DR-4(§3-8)·DR5-3(§3-9-2)를 이 슬라이스가 닫음. **DR5-2(쓰기 내구성)는 이월**(§3-8·§7-Q2). 비준자 확인.
- **브랜치 보존·push 금지·젠더중립 네이밍·진행상태는 ROADMAP(디스크)**.

## Open Questions

> 비준 착수 전 — 비어 있으며 적대 비준이 채운다.

- [ ] (비준 대기) §7 열린 질문 1~5 판정.
- [ ] (선상속·M7) Koin `viewModel { }` 실배선·앱 셸 연결·온보딩 게이트.
- [ ] (선상속·M7/하드닝) **DR5-2 쓰기 내구성 이월분**: `DetailViewModel.toggleBookmark()`를 앱/repository 스코프(또는 `NonCancellable`)로 내구화하고 확정 피드백을 얹는다. 근거: 별표 탭 직후 즉시 이탈 시 fire-and-forget launch가 `onCleared→viewModelScope.cancel()` 취소로 DB 미변경·무피드백으로 **조용히 유실**되는 창(§3-8)은 에러가 없어 시각 천장/아침 사람 게이트가 못 본다 — 명시 트래킹으로 이월분이 프로즈에 묻혀 누락되지 않게 남긴다.
- [ ] (선상속·M8) 플랫폼 seam actual·아이콘/스플래시 자산·폰트 라이선스 고지·접근성 감사·에러 통합 실측·실기기 시각 검증.

### 비준 명시 이월 (carry-forward — "본다는 걸 적어서 넘긴다")

> 비준 종료(RATIFIED) 시점 미탐색이나 **알려진 클래스**로 확인된 항목. 암묵적으로 넘기지 않고 여기 명시 기록한다.

- [ ] **DR-3 (navigation-compose 네이티브 링크 실증 — 구현 착수 시 선실증 필요)** — 주장: `org.jetbrains.androidx.navigation:navigation-compose`(멀티플랫폼)을 CMP 1.11.1·Kotlin 2.3.21에서 소비하고 `iosSimulatorArm64` 링크가 green. 깨짐 시나리오: navigation-compose 멀티플랫폼 아티팩트가 `iosSimulatorArm64` klib로 실제 링크되는지는 **실빌드 전까지 미확정** — 좌표/네이티브 타깃 커버리지가 어긋나면 §5 3축 중 링크 축이 구현 단계에서 깨진다. 다만 스펙이 **의존성-0 대안(sealed route + `StateFlow` 백스택, §7-1)** 을 명시해 두어 깨져도 **안전 폴백**이 있으므로 목표 실패는 아니다. 구현 착수 시 navigation-compose 링크를 **먼저 실증하지 않고** 화면부터 배선하면 후반 재작업 위험(메모리 `green-oracle-native-execution-gap`이 마일스톤마다 재발 기록). → 착수 순서: 코드 0줄에서 3축 빈 빌드로 좌표 확정 후 화면 배선.
