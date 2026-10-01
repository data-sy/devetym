/**
 * 갈래 6종 정본 + 허브 메타 (W2).
 *
 * **어휘 정본은 앱이다** — `shared/.../model/Category.kt`의 `CANONICAL` 6집합. 이 파일은 그
 * 6개에 **URL 조각과 사람이 읽는 소개문**을 붙일 뿐이고, 집합 자체를 늘리거나 줄이지 않는다.
 * 슬러그도 손으로 지은 이름이 아니라 `Category.kt`의 상수명을 소문자·하이픈으로 옮긴 것이다
 * (`DATA_STRUCTURE` → `data-structure`). 그래야 어느 쪽을 봐도 같은 이름이 보인다.
 *
 * ⚠️ **이 모듈은 `terms.json`을 import하지 않는다 — 그게 이 파일이 따로 있는 이유다.**
 *    SSR 폴백 라우트(`term/[...rest].astro`)가 빵부스러기를 그리려면 갈래→URL 표가 필요한데,
 *    그 표를 `terms.ts`에 두면 518KB짜리 번들이 Worker로 통째로 딸려 들어간다.
 *
 * ⚠️ 슬러그는 URL이다 — **한 번 나가면 바꾸는 비용이 색인 손실이다.** 바꿔야 하면 301이 먼저다.
 */

/** 정본 6집합 — AI 경로의 clamp 대상과 같은 집합(설계서 §3-1.6 · `Category.kt`). */
export const CATEGORIES = [
  "동시성",
  "자료구조",
  "네트워크",
  "DB",
  "패턴",
  "기타",
] as const;

export type CategoryName = (typeof CATEGORIES)[number];

export interface CategoryMeta {
  name: CategoryName;
  /** `/category/{slug}`. `Category.kt` 상수명의 소문자·하이픈 표기. */
  slug: string;
  /** 허브 h1. */
  heading: string;
  /** 허브 도입부 = 이 갈래의 이름들이 **어디서 왔는지**. 목록만 있는 페이지를 만들지 않기 위한 것. */
  blurb: string;
}

/**
 * 소개문은 "이 갈래에 무엇이 있다"가 아니라 **"이 갈래의 이름은 어디서 왔다"**를 말한다.
 * 전자는 아래 목록이 이미 하는 일이고, 그것만 있으면 허브는 링크 더미일 뿐이라 얇은 콘텐츠를
 * 6장 더 만드는 셈이 된다(설계서 §5-2가 막으려는 것). 단정할 수 없는 유래는 쓰지 않는다 —
 * 승격 critic이 `shedlock`에서 잡아낸 것과 같은 종류의 오류를 사이트 자체 문구에 심지 않는다.
 */
export const CATEGORY_META: readonly CategoryMeta[] = [
  {
    name: "동시성",
    slug: "concurrency",
    heading: "동시성",
    blurb:
      "여러 흐름이 같은 자원을 두고 겹칠 때 생기는 문제와 그 해법에 붙은 이름들. " +
      "사람 세계의 장치와 사고 실험에서 건너온 말이 유난히 많다 — 상호 배제(mutex), " +
      "신호기(semaphore), 철학자들의 저녁 식사. 추상적인 타이밍 문제를 눈에 보이는 장면으로 " +
      "바꿔 부르는 습관이 이 갈래의 작명법이다.",
  },
  {
    name: "자료구조",
    slug: "data-structure",
    heading: "자료구조",
    blurb:
      "데이터를 담는 모양에 붙은 이름들. 나무·더미·줄·바구니처럼 손에 잡히는 사물의 이름을 " +
      "그대로 가져다 쓴 것이 많고, 그 위에 성질을 나타내는 수식어(균형·이진·순환)가 붙어 " +
      "갈래를 이룬다. 고안한 사람이나 논문 제목이 그대로 굳어 이름이 된 경우도 섞여 있다.",
  },
  {
    name: "네트워크",
    slug: "network",
    heading: "네트워크",
    blurb:
      "기계와 기계 사이를 잇는 일의 어휘. 우편·항해·전화 교환처럼 사람이 먼저 만들어 둔 " +
      "연결망의 말을 물려받은 이름이 많다 — 패킷, 포트, 라우팅, 핸드셰이크. 프로토콜 이름은 " +
      "대개 머리글자라, 그 머리글자를 펼치는 순간 설계 의도가 드러난다.",
  },
  {
    name: "DB",
    slug: "database",
    heading: "데이터베이스",
    blurb:
      "데이터를 오래 두고 다시 꺼내는 일의 어휘. 회계·기록 보관·도서관 색인에서 온 말이 " +
      "많다 — 트랜잭션, 커밋, 저널, 인덱스. 옛 장부의 규율이 이름째로 넘어왔고, 그래서 " +
      "이름을 풀어 보면 그 규율이 왜 필요한지가 같이 나온다.",
  },
  {
    name: "패턴",
    slug: "pattern",
    heading: "패턴·아키텍처",
    blurb:
      "되풀이되는 설계 문제와 그 해법에 붙은 이름들. 이름 자체가 해법의 은유인 경우가 " +
      "대부분이라, 어원을 알면 그 패턴이 무엇을 하는지가 거의 그대로 나온다 — 어댑터, " +
      "브리지, 서킷 브레이커, 방화벽 격실(bulkhead).",
  },
  {
    name: "기타",
    slug: "etc",
    heading: "그 밖의 용어",
    blurb:
      "위 다섯 갈래에 들지 않는 말들 — 도구, 이론, 그리고 이 바닥의 농담과 전설. " +
      "데몬·버그·부트스트랩처럼 유래가 이야기로 남은 이름이 여기 모인다. " +
      "가장 큰 갈래이고, 어원이 가장 안 궁금할 것 같은데 실제로는 가장 재미있는 쪽이다.",
  },
] as const;

const BY_NAME = new Map(CATEGORY_META.map((c) => [c.name as string, c]));
const BY_SLUG = new Map(CATEGORY_META.map((c) => [c.slug, c]));

/** 갈래 이름 → 메타. 6집합 밖 값은 `undefined` — 호출부가 접는다(앱과 같은 pass-through 태도). */
export function categoryMeta(name: string): CategoryMeta | undefined {
  return BY_NAME.get(name);
}

export function categoryBySlug(slug: string): CategoryMeta | undefined {
  return BY_SLUG.get(slug);
}

/** 갈래 허브 경로. 6집합 밖이면 전체 색인으로 접는다 — 죽은 링크를 만들지 않는다. */
export function categoryPath(name: string): string {
  const meta = BY_NAME.get(name);
  return meta ? `/category/${meta.slug}` : "/terms";
}

// ── 빌드 시 단언 ──────────────────────────────────────────────────────────────
// 6집합과 메타가 갈라지면 **허브가 없는 갈래**나 **아무도 안 가리키는 허브**가 생긴다.
// 둘 다 빌드는 성공하고 링크만 조용히 끊긴다.
if (CATEGORY_META.length !== CATEGORIES.length) {
  throw new Error(`갈래 메타 ${CATEGORY_META.length}개 ≠ 정본 ${CATEGORIES.length}개`);
}
for (const name of CATEGORIES) {
  if (!BY_NAME.has(name)) throw new Error(`갈래 '${name}'의 허브 메타가 없다`);
}
if (BY_SLUG.size !== CATEGORY_META.length) {
  throw new Error("갈래 슬러그가 중복이다");
}
for (const c of CATEGORY_META) {
  if (!/^[a-z][a-z-]*[a-z]$/.test(c.slug)) throw new Error(`URL-safe하지 않은 갈래 슬러그: ${c.slug}`);
}
