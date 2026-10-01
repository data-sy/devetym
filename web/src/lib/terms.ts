/**
 * 번들 스냅샷 로더 + 인덱스 (W1b).
 *
 * **입력은 D1이 아니라 커밋된 스냅샷이다** — [ADR-0013](../../../docs/adr/0013-web-route-contract.md)
 * Decision 1. 빌드가 D1 가용성에 묶이지 않고, 무엇이 정적으로 나갔는지가 git에 남는다.
 * 정본은 D1이고([ADR-0012](../../../docs/adr/0012-content-canon-d1.md)) 이 파일은 그 스냅샷을 읽는다.
 *
 * ⚠️ **이 모듈을 SSR 라우트에서 import하지 않는다.** 518KB짜리 원문이 Worker 번들에 통째로
 *    들어간다. 정적 라우트(`/term/[slug]`·사이트맵·검색 인덱스 생성)에서만 쓴다. 검색은
 *    빌드가 뽑아 두는 압축 인덱스(`/search-index.json`)를 클라이언트가 가져다 쓴다.
 *
 * ⚠️ **사본을 만들지 않는다.** 앱 번들 파일을 그대로 읽는다 — 웹용 사본을 두면 그 순간
 *    ADR-0012가 막으려던 드리프트가 repo 안에서 재발한다(익스포트가 갱신하는 파일은 하나뿐이다).
 */

import rawTerms from "../../../shared/src/commonMain/composeResources/files/terms.json";
import { normalizeTermKey, slugFor } from "./term-key";
import { CATEGORIES } from "./categories";

export { CATEGORIES } from "./categories";
export type { CategoryName, CategoryMeta } from "./categories";

export interface Term {
  keyword: string;
  aliases: string[];
  category: string;
  summary: string;
  etymology: string;
  namingReason: string;
}

/** 이 용어의 URL 경로 조각. D1 `term_key`와 같은 문자열이다. */
export type Slug = string;

export interface IndexedTerm extends Term {
  slug: Slug;
  /** title·h1에 1급으로 올릴 한글 별칭. 없으면 null. */
  primaryKoreanAlias: string | null;
}

const HANGUL = /[가-힣]/;

/**
 * 한글 별칭 하나를 대표로 고른다 — **이 트랙의 핵심 자산이 여기서 표면으로 올라온다.**
 * 표제어 650개는 전부 영어이고 한글 keyword는 0개다. 한국인은 `뮤텍스 어원`으로 검색하므로
 * 별칭을 title·h1에 올리지 않으면 650장은 한국어 검색에 사실상 존재하지 않는다(설계서 §5-1).
 *
 * 고르는 규칙: 한글이 포함된 별칭 중 **가장 짧은 것**. 별칭 목록은 대개 짧은 순서로 정렬돼
 * 있지 않고, 긴 쪽은 설명형(`읽기-쓰기 잠금 장치`)이라 표제로는 짧은 쪽이 낫다.
 */
function pickKoreanAlias(aliases: string[]): string | null {
  const ko = aliases.filter((a) => HANGUL.test(a));
  if (ko.length === 0) return null;
  return ko.reduce((best, a) => (a.length < best.length ? a : best));
}

const terms = rawTerms as Term[];

export const ALL_TERMS: IndexedTerm[] = terms.map((t) => ({
  ...t,
  slug: slugFor(t.keyword),
  primaryKoreanAlias: pickKoreanAlias(t.aliases),
}));

/** slug → 용어. 660개, 충돌 0(실측). */
export const BY_SLUG: ReadonlyMap<Slug, IndexedTerm> = new Map(
  ALL_TERMS.map((t) => [t.slug, t]),
);

/**
 * 정규화된 별칭·표제어 → 용어들.
 *
 * ⚠️ **앱 번들 인덱스와 의도적으로 다르다.** 앱은 충돌 시 first-wins라 뒤 엔트리가 검색에서
 *    가려진다(실측 교차 충돌 3건: 집계·분기·샤딩). 웹은 용어마다 자기 URL이 있으므로 **가리지
 *    않고 전부 담는다** — 설계서 §3-1.3이 "웹은 이 결함을 구조적으로 고친다"고 적은 지점이다.
 *    순서는 first-wins와 같게 유지해 "대표 하나"가 필요할 때 앱과 같은 답이 나오게 한다.
 */
export const BY_ALIAS: ReadonlyMap<string, IndexedTerm[]> = (() => {
  const m = new Map<string, IndexedTerm[]>();
  for (const t of ALL_TERMS) {
    for (const label of [t.keyword, ...t.aliases]) {
      const key = normalizeTermKey(label);
      if (!key) continue;
      const bucket = m.get(key);
      if (bucket) {
        if (!bucket.includes(t)) bucket.push(t);
      } else {
        m.set(key, [t]);
      }
    }
  }
  return m;
})();

/**
 * 같은 카테고리의 이웃 용어. **얇은 콘텐츠 대응**(설계서 §5-2) — 본문을 새로 쓰지 않고
 * 내부 링크 밀도를 올린다. 317개(48.8%)가 300자 미만이라 이 링크가 페이지의 실질을 채운다.
 *
 * 고르는 규칙은 결정적이어야 한다(빌드마다 달라지면 diff가 무의미해진다) — 카테고리 안에서
 * 자기 위치를 기준으로 순환하며 앞뒤로 집는다. 무작위·해시를 쓰지 않는다.
 */
export function relatedTerms(term: IndexedTerm, count = 6): IndexedTerm[] {
  const peers = ALL_TERMS.filter((t) => t.category === term.category);
  const i = peers.findIndex((t) => t.slug === term.slug);
  const out: IndexedTerm[] = [];
  for (let step = 1; out.length < count && step < peers.length; step++) {
    out.push(peers[(i + step) % peers.length]);
  }
  return out;
}

/** 본문 총 길이 — 얇은 페이지 판정용(설계서 §1-2의 300자 기준). */
export function bodyLength(t: Term): number {
  return t.summary.length + t.etymology.length + t.namingReason.length;
}

/**
 * 갈래별 목록. 사람이 훑는 순서(한글 별칭 우선 사전순)로 고정한다 —
 * 허브·전체 색인·홈이 **같은 순서**를 보여야 어느 입구로 들어와도 같은 사이트로 읽힌다.
 */
export const TERMS_BY_CATEGORY: ReadonlyMap<string, IndexedTerm[]> = new Map(
  CATEGORIES.map((name) => [
    name as string,
    ALL_TERMS.filter((t) => t.category === name).sort((a, b) =>
      (a.primaryKoreanAlias ?? a.keyword).localeCompare(b.primaryKoreanAlias ?? b.keyword, "ko"),
    ),
  ]),
);

// ── 본문이 언급하는 용어 (W2) ────────────────────────────────────────────────
//
// **관련 용어(같은 갈래 순환)만으로는 갈래를 넘는 링크가 하나도 없다.** 그러면 크롤 그래프가
// 서로 안 닿는 6개 덩어리가 되고, 사이트가 660장짜리 한 몸으로 안 읽힌다. 본문이 실제로
// 언급한 용어를 링크로 올리면 그 다리가 생긴다 — 실측 792링크 중 **229건(29%)이 갈래를 넘는다.**
//
// ⚠️ **라벨 하한은 취향이 아니라 실측으로 정했다**(2026-09-03, 660건 전수).
//    한글 별칭을 길이 제한 없이 쓰면 `연결`(58) `가지`(36) `다리`(20) 같은 **평범한 낱말**이
//    걸려 `bridge`가 "다리를 놓는다"라는 비유 한 줄로 링크된다 — 관련 없는 링크는 내부 링크
//    밀도를 올리는 게 아니라 신뢰를 깎는다. 공백 제외 4자 이상으로 자르면 남는 것은
//    `알고리즘`·`트랜잭션`·`연결 리스트`처럼 **실제 용어 언급**뿐이다(육안 확인).
//    영문 표제어는 3자 이상 + 단어 경계 — 한국어 본문에 영단어가 나오면 대개 진짜 인용이다.

const MENTION_MIN_KO = 4;
const MENTION_MIN_EN = 3;
/** 한 페이지에 붙이는 상한. 실측 최대 8건이라 사실상 전량이지만, 본문이 길어져도 목록이 안 넘친다. */
const MENTION_MAX = 8;

function escapeRe(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * 언급 색인. 모듈 로드 시 한 번만 돈다(빌드 시점 · 페이지마다 다시 계산하지 않는다).
 * 순서는 **본문에 처음 나온 자리** 순 — 결정적이고 사람이 읽은 순서와 같다.
 */
const MENTIONS: ReadonlyMap<Slug, IndexedTerm[]> = (() => {
  const labels: { re: RegExp; term: IndexedTerm }[] = [];
  for (const t of ALL_TERMS) {
    if (t.keyword.length >= MENTION_MIN_EN) {
      labels.push({ re: new RegExp(`\\b${escapeRe(t.keyword)}\\b`, "i"), term: t });
    }
    for (const a of t.aliases) {
      if (HANGUL.test(a) && a.replace(/\s/g, "").length >= MENTION_MIN_KO) {
        labels.push({ re: new RegExp(escapeRe(a)), term: t });
      }
    }
  }

  const out = new Map<Slug, IndexedTerm[]>();
  for (const t of ALL_TERMS) {
    const text = `${t.summary} ${t.etymology} ${t.namingReason}`;
    const at = new Map<IndexedTerm, number>();
    for (const { re, term } of labels) {
      if (term === t) continue;
      const m = re.exec(text);
      if (!m) continue;
      const prev = at.get(term);
      if (prev === undefined || m.index < prev) at.set(term, m.index);
    }
    const hits = [...at.entries()]
      .sort((a, b) => a[1] - b[1] || a[0].slug.localeCompare(b[0].slug))
      .slice(0, MENTION_MAX)
      .map(([term]) => term);
    if (hits.length > 0) out.set(t.slug, hits);
  }
  return out;
})();

/**
 * 이 용어의 본문이 언급하는 다른 용어들. 없으면 빈 배열 —
 * **없는 페이지가 절반 가까이 된다**(실측 660 중 언급 보유 430). 호출부는 비면 섹션을 지운다.
 */
export function mentionedTerms(term: IndexedTerm): IndexedTerm[] {
  return MENTIONS.get(term.slug) ?? [];
}

// ── 빌드 시 단언 ──────────────────────────────────────────────────────────────
//
// ⚠️ **성공 디코드로는 안 잡히는 것들이다.** INV-A: `aliases` 키 이름이 다르거나 생략되면
//    예외 없이 빈 배열로 떨어진다(설계서 §3-1.5). 그러면 빌드는 성공하고 전량이 나가지만
//    한글 별칭이 전부 사라진 채로 나간다 — 이 트랙의 핵심 자산이 조용히 증발한다.
//    그래서 "깨지는 검사"로 둔다. 엔트리 수는 승격(W1c)마다 갱신되는 확정값이다.

function assertBundle(): void {
  const problems: string[] = [];

  if (ALL_TERMS.length !== 660) {
    problems.push(`엔트리 660이 아니다: ${ALL_TERMS.length}`);
  }
  if (BY_SLUG.size !== ALL_TERMS.length) {
    problems.push(`slug 충돌: ${ALL_TERMS.length}개 중 고유 ${BY_SLUG.size}개`);
  }

  const aliasCount = ALL_TERMS.reduce((n, t) => n + t.aliases.length, 0);
  if (aliasCount < 1000) {
    problems.push(`별칭이 비었거나 급감했다: ${aliasCount} (INV-A 의심)`);
  }

  const koreanAliased = ALL_TERMS.filter((t) => t.primaryKoreanAlias).length;
  if (koreanAliased < 500) {
    problems.push(`한글 별칭을 가진 용어가 ${koreanAliased}개뿐 — 한국어 검색 표면이 사라진다`);
  }

  const badSlug = ALL_TERMS.filter((t) => !/^[a-z0-9]+$/.test(t.slug));
  if (badSlug.length > 0) {
    problems.push(
      `URL-safe하지 않은 slug ${badSlug.length}건: ${badSlug.slice(0, 5).map((t) => t.slug).join(", ")}`,
    );
  }

  // 갈래 허브 6장이 660장을 나눠 가진다 — 한 갈래가 비면 허브가 빈 페이지로 나간다.
  for (const [name, items] of TERMS_BY_CATEGORY) {
    if (items.length === 0) problems.push(`갈래 '${name}'가 비었다 — 허브가 빈 페이지가 된다`);
  }

  // 언급 링크가 통째로 사라져도 빌드는 성공하고 페이지도 멀쩡해 보인다(섹션만 없어진다).
  // 실측 430건이므로 300 밑으로 떨어지면 라벨 규칙이나 본문이 바뀐 것이다.
  if (MENTIONS.size < 300) {
    problems.push(`본문 언급 링크를 가진 용어가 ${MENTIONS.size}개뿐 — 갈래를 넘는 링크가 끊긴다`);
  }

  const badCategory = ALL_TERMS.filter(
    (t) => !(CATEGORIES as readonly string[]).includes(t.category),
  );
  if (badCategory.length > 0) {
    problems.push(`6집합 밖 카테고리 ${badCategory.length}건: ${badCategory[0].category}`);
  }

  if (problems.length > 0) {
    throw new Error(
      `번들 스냅샷 검증 실패 — 빌드를 세운다:\n  ${problems.join("\n  ")}\n` +
        `  (정본은 D1이다. 스냅샷이 낡았으면 npm run db:export:local로 다시 뽑는다)`,
    );
  }
}

assertBundle();
