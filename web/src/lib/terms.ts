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

/** 카테고리 6집합 — AI 경로의 clamp 대상과 같은 집합(설계서 §3-1.6). */
export const CATEGORIES = [
  "동시성",
  "자료구조",
  "네트워크",
  "DB",
  "패턴",
  "기타",
] as const;

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
