/**
 * SITE_URL 단일 지점 (W0a · ROADMAP W 트랙).
 *
 * ⚠️ 이 파일이 도메인의 **유일한** 출처다. canonical·사이트맵·OG·구조화 데이터·
 *    내부 절대링크가 전부 여기서 읽는다. 다른 곳에 호스트명을 박지 않는다.
 *
 * ⚠️ **존재하지 않는 호스트명을 기본값으로 두지 않는다** (2026-08-25 사람 결정).
 *    `devetym.com`은 아직 구매 전이고, 가짜 호스트를 박으면 이 트랙의 완료 오라클
 *    (*배포된 실 URL 전수 200 응답*)이 통째로 실행 불가가 된다. 그래서 기본값은
 *    Cloudflare 미리보기 서브도메인 — 공짜이고 **실존**한다.
 *
 * 도메인을 사면(W0b) `SITE_URL` 환경변수 한 줄 + DNS. 이 파일은 안 고쳐도 된다.
 */

/**
 * 배포 환경이 주입한 정본 URL. 없으면 아래 폴백.
 * ⚠️ **빌드 시점**에만 읽힌다(페이지가 prerender라 값이 HTML에 구워진다).
 *    Worker 런타임에는 `process`가 없을 수 있어 방어적으로 읽는다.
 */
const fromEnv: string | undefined =
  import.meta.env.SITE_URL ??
  (typeof process !== "undefined" ? process.env?.SITE_URL : undefined);

/**
 * 기본값 = 실 도메인. **실존한다** — W0b 완료(2026-08-25), NS는 Cloudflare 위임.
 *
 * 종전 기본값이던 미리보기 서브도메인(`devetym-web.*.workers.dev`)은 **꺼져 있다**
 * (`wrangler.toml workers_dev = false`). 실 도메인이 붙은 뒤에도 켜 두면 같은 내용을
 * 색인 허용 상태로 서빙해 중복 콘텐츠로 경쟁한다 — 2026-08-25 실측으로 확인하고 껐다.
 *
 * 페이지가 prerender라 이 값은 **빌드 시점에 HTML로 구워진다.** 런타임 환경변수
 * (wrangler `[vars]`)로 덮으면 정적 canonical과 어긋난 값이 두 벌 생긴다.
 * 스테이징이 필요해지면 빌드 전에 준다: `SITE_URL=... npm run build`.
 */
const FALLBACK = "https://devetym.com";

/** 정본 사이트 URL. 후행 슬래시 없음. */
export const SITE_URL: string = stripTrailingSlash(fromEnv || FALLBACK);

/** 도메인이 아직 임시(미리보기·로컬)인가 — 색인 억제 판단에 쓴다. */
export const IS_CANONICAL_HOST: boolean =
  /^https:\/\/(www\.)?devetym\.com$/.test(SITE_URL);

/** 사이트 내부 경로를 절대 URL로. 호스트명을 손으로 잇지 않기 위한 유일한 통로. */
export function absoluteUrl(path: string): string {
  return `${SITE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

function stripTrailingSlash(u: string): string {
  return u.endsWith("/") ? u.slice(0, -1) : u;
}

export const SITE_NAME = "DevEtym";
export const SITE_DESCRIPTION =
  "개발 용어의 어원을 찾아봅니다. 650개 큐레이션 용어와 AI 어원 설명.";
export const SITE_LOCALE = "ko_KR";
