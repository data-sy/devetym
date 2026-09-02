import type { APIRoute } from "astro";
import { ALL_TERMS } from "../lib/terms";

/**
 * 검색 인덱스 — **빌드 시점에 굽는 정적 자산**이다.
 *
 * ⚠️ 검색을 SSR로 두지 않은 이유: 그러면 `terms.json`(518KB) 원문이 Worker 번들에 들어가고,
 *    검색 가용성이 D1/Worker에 묶인다. ADR-0013의 Positive *"D1이 죽어도 사이트는 서빙된다"*를
 *    검색까지 지키려면 인덱스가 정적이어야 한다.
 *
 * ⚠️ 본문(etymology·namingReason)은 싣지 않는다 — 실으면 500KB가 되고, 검색 화면은 결과 목록만
 *    보여주면 되기 때문이다. 요약은 싣는다(결과가 무엇인지 알 수 있어야 고른다).
 *    실측: 요약 포함 109KB / gzip 35KB · 요약 제외 60KB / gzip 18KB.
 *
 * 필드를 한 글자로 줄인 것은 650행 × 5필드에서 키 이름이 무시 못 할 비중이기 때문이다.
 *   s=slug · k=keyword · a=aliases · c=category · d=summary
 */
export const prerender = true;

export const GET: APIRoute = () => {
  const index = ALL_TERMS.map((t) => ({
    s: t.slug,
    k: t.keyword,
    a: t.aliases,
    c: t.category,
    d: t.summary,
  }));

  return new Response(JSON.stringify(index), {
    headers: {
      "content-type": "application/json; charset=utf-8",
      // 빌드마다 바뀌는 정적 자산 — 오래 캐시하되 재배포가 이기게 한다(파일명이 고정이라
      // must-revalidate 없이 길게 잡으면 승격된 새 용어가 검색에 한동안 안 뜬다).
      "cache-control": "public, max-age=300, stale-while-revalidate=86400",
    },
  });
};
