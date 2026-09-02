/**
 * `normalizeTermKey` — **웹 지점의 이식본** (W1b).
 *
 * 같은 입력에 같은 키를 내야 하는 지점이 이제 넷이다:
 *
 *   1. 앱        shared/src/commonMain/kotlin/.../AppJson.kt   normalizeKeyword
 *   2. Worker    ~/devetym-proxy/src/index.js                  normalizeTermKey
 *   3. 파이프라인 Scripts/db-expand/term_key.py                  normalize_term_key
 *   4. 웹        이 파일
 *
 * ⚠️ **네 번째 구현을 만드는 것은 설계서 §3-1.2가 경고한 드리프트 지점 증설이다.**
 *    repo가 갈라져 있어(`~/devetym` vs `~/devetym-proxy`) import로 공유할 수 없으므로,
 *    공유 대신 **교차 실행 오라클**로 갚는다 — `Scripts/db-expand/test_term_key.py`가 이 파일을
 *    실제로 실행해 번들 650의 keyword·aliases 전량 + 유니코드 경계 문자에서 파이썬·JS 구현과
 *    바이트 비교한다. 이 파일을 옮기거나 이름을 바꾸면 그 테스트가 깨진다(의도된 결합).
 *
 * ⚠️ 갈라져도 **조용하다**. 각 구현은 자기 일관적이라 캐시 미스로 드러나지 않는다.
 *    증상은 "웹이 만든 term_key로 조회하면 있는데 앱이 만든 키로는 없다" 같은 누수로 나타난다.
 */

// Kotlin `Char.isWhitespace()`의 정확한 복제. 정본은 devetym `NormalizeKeywordTest`가
// U+0000~U+FFFF를 전수 실측해 고정한 집합이다(JVM·네이티브 동일).
//
// ⚠️ JS `trim()`도 `\s`도 쓰면 안 된다. 세 규격의 집합이 전부 다르다:
//   - Kotlin만 자름:      U+001C-U+001F
//   - JS `trim()`만 자름: U+FEFF (BOM)
//   - 둘 다 안 자름:      U+0085 (NEL)
const WS =
  "\\u0009-\\u000D\\u001C-\\u0020\\u00A0\\u1680" +
  "\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000";

// 삭제 대상 = 공백류 ∪ {하이픈, 언더스코어}. 양끝만이 아니라 **내부까지 전부** 지운다.
const SEPARATORS = new RegExp(`[${WS}\\u002D\\u005F]`, "g");

/**
 * 구분자(공백류·하이픈·언더스코어)를 전부 삭제한 뒤 lowercase.
 *
 *     "aa-tree" · "AA tree" · "aatree"  → "aatree"
 *     "추상 팩토리" · "추상팩토리"        → "추상팩토리"
 *
 * ⚠️ `toLocaleLowerCase()` 금지 — 터키어 I 등에서 Kotlin `lowercase()`와 갈라진다.
 */
export function normalizeTermKey(s: string): string {
  return s.replace(SEPARATORS, "").toLowerCase();
}

/**
 * URL 슬러그 = 정규화 키 그대로.
 *
 * 설계서 §5-1은 *"normalizeKeyword 결과의 URL-safe 변환(공백→`-`)"*로 적혀 있으나, 그 문장은
 * 정규화가 **트림**이던 시절(N0)의 것이다. W0c에서 정의가 **삭제**(N1)로 확정되면서 결과 키에
 * 공백·하이픈·언더스코어가 남을 수 없게 됐고, 실측상 번들 650의 정규화 키는 **전부 `[a-z0-9]`**
 * (비영숫자 0자·중복 0건)다. 따라서 추가 변환이 필요 없고, 슬러그 = 키 = D1 `term_key`가 되어
 * **URL과 정본 조회 키가 같은 문자열**이 된다 — SSR 폴백·승격 잡이 URL을 재해석할 필요가 없다.
 */
export function slugFor(keyword: string): string {
  return normalizeTermKey(keyword);
}
