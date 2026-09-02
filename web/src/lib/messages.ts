/**
 * 사용자 문구 — **한 곳에서만 정의한다** (설계서 §3-2 G5).
 *
 * 앱의 `DetailMessages.kt`가 `when` 전수·`else` 없음으로 "종류가 늘면 컴파일이 깨지도록" 해 둔
 * 것과 같은 규율을 TS로 가져온다: `Record<ErrorKind, string>`은 키가 하나라도 빠지면 타입 에러다.
 * `else`/`default`를 쓰면 새 종류가 조용히 "문제가 발생했어요"로 강등된다.
 */

/** 앱 `ErrorKind`와 같은 6종. 이름을 앱과 맞춘다 — 갈라지면 대조가 불가능해진다. */
export type ErrorKind =
  | "Timeout"
  | "Network"
  | "InvalidResponse"
  | "DailyLimitExceeded"
  | "ServiceExhausted"
  | "Unknown";

/** 앱 `errorMessage()`와 같은 문구. 같은 실패를 두 채널이 다르게 부르지 않는다. */
export const ERROR_MESSAGE: Record<ErrorKind, string> = {
  Timeout: "응답이 지연되고 있어요. 잠시 후 다시 시도해주세요",
  Network: "인터넷 연결을 확인해주세요",
  DailyLimitExceeded: "오늘 사용량을 모두 사용했어요",
  ServiceExhausted: "AI 생성에 문제가 있어요. 잠시 후 다시 시도해주세요",
  InvalidResponse: "결과를 불러오지 못했어요",
  Unknown: "문제가 발생했어요",
};

/**
 * 한도 층(429 `scope`) → 화면. **같은 화면을 띄우면 거짓말이 된다**(핸드오프 §2-3).
 *
 * - `browser` = 이 브라우저의 3건을 다 썼다. **사용자가 지금 할 수 있는 일이 있다**(앱).
 * - `ip`·`web` = 남의 사용량 또는 전체 예산이 소진됐다. 앱을 권하면 거짓말이다 — 앱을 받아도
 *   오늘 이 사람의 상황은 바뀌지 않고, 애초에 원인이 이 사람이 아니다.
 *
 * 문구 방향은 2026-09-02 사람 결정(3안 중 「담백 사실형」). 종전 Q4 결정(2026-08-25)의
 * *"한도 화면은 앱 유도를 전면에"*를 **완화한 최신 결정**이며, 이쪽이 이긴다.
 */
export type LimitScope = "browser" | "ip" | "web" | "global";

export interface LimitScreen {
  title: string;
  body: string;
  /** 앱 링크를 보여줄 것인가. `browser`일 때만 참이다. */
  showApp: boolean;
}

export const LIMIT_SCREEN: Record<LimitScope, LimitScreen> = {
  browser: {
    title: "오늘 사용량을 모두 사용했어요",
    body: "웹은 하루 3개까지입니다. 내일 다시 오시거나, iOS 앱에서 이어서 찾아보세요.",
    showApp: true,
  },
  // ⚠️ 여기에 앱 유도를 넣지 않는다. CGNAT 때문에 모바일 사용자는 자기가 쓰지도 않은 한도에
  //    걸릴 수 있다(설계서 §4-5) — 그 사람에게 "앱을 받으세요"는 원인 오귀속이다.
  ip: {
    title: "잠시 후 다시 시도해주세요",
    body: "같은 네트워크에서 오늘 요청이 많았어요. 내일이면 다시 열립니다.",
    showApp: false,
  },
  web: {
    title: "오늘의 AI 생성이 모두 소진됐어요",
    body: "웹 전체의 하루 한도에 도달했습니다. 내일 다시 오시면 이용할 수 있어요.",
    showApp: false,
  },
  // 앱 표면의 전역 캡. 웹에서 보일 일이 없지만 shape이 같아 방어적으로 둔다.
  global: {
    title: "오늘의 AI 생성이 모두 소진됐어요",
    body: "전체 하루 한도에 도달했습니다. 내일 다시 시도해주세요.",
    showApp: false,
  },
};

export const APP_STORE_URL = "https://apps.apple.com/app/id6748814301";

/** 이미 있는 650장은 한도와 무관하다 — 한도 화면에서 이 사실을 알려 준다. */
export const LIMIT_FOOTNOTE = "이미 정리된 650개 용어는 한도 없이 계속 볼 수 있어요.";
