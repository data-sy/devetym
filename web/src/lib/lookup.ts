import type { D1Database } from "@cloudflare/workers-types";
import type { Term } from "./terms";

/**
 * D1 **조회 전용** 폴백 (ADR-0013 Decision 2).
 *
 * ⚠️ **여기서 생성을 트리거하지 않는다. 이 파일의 존재 이유가 그 금지다.**
 *    "없으면 만들면 되지 않나"는 ADR-0013이 **명시적으로 기각한 안 3번**이다 —
 *    `/term/<무엇이든>`이 생성을 열면 Googlebot 한 번의 크롤로 웹 전역 캡 30건이 소진되고
 *    (T6), Turnstile·3건 한도는 **사용자 개시 POST**에 걸려 있어 GET을 보호하지 않는다.
 *    구현 중 이 지점으로 되돌아오기 쉬워서 ADR이 기각을 문서에 박아 뒀다.
 *
 * ⚠️ D1이 없거나 죽어도 **사이트는 살아 있어야 한다**(ADR-0013 Positive). 실패는 예외가
 *    아니라 `null`로 접고, 그러면 정상 404가 나간다.
 */

export interface GeneratedTerm extends Term {
  /** 미승격 생성분인가. 승격되면 다음 빌드에서 SSG로 올라가 이 경로를 안 탄다. */
  generated: boolean;
}

interface EntryRow {
  payload: string;
  branch: string;
  origin: string;
}

export async function lookupTerm(
  db: D1Database | undefined,
  termKey: string,
): Promise<GeneratedTerm | null> {
  if (!db || !termKey) return null;

  try {
    // 별칭도 본다 — 사용자가 공유한 링크가 별칭 키일 수 있고, 앱·프록시의 조회 순서와도 맞다.
    const row = await db
      .prepare(
        `SELECT e.payload AS payload, e.branch AS branch, e.origin AS origin
           FROM entries e
          WHERE e.term_key = ?1
          UNION ALL
         SELECT e.payload, e.branch, e.origin
           FROM aliases a JOIN entries e ON e.term_key = a.term_key
          WHERE a.alias_key = ?1
          LIMIT 1`,
      )
      .bind(termKey)
      .first<EntryRow>();

    // 부정 분기(not_dev_term·possible_typo)는 페이지가 아니다 — 404로 둔다.
    // 이걸 200으로 내보내면 "내용 없는 페이지가 무한히 있는 사이트"가 된다(Decision 5).
    if (!row || row.branch !== "term_entry") return null;

    const p = JSON.parse(row.payload);
    for (const field of ["keyword", "category", "summary", "etymology", "namingReason"]) {
      if (typeof p[field] !== "string") return null; // 깨진 payload는 없는 것으로 친다
    }

    return {
      keyword: p.keyword,
      aliases: Array.isArray(p.aliases) ? p.aliases.filter((a: unknown) => typeof a === "string") : [],
      category: p.category,
      summary: p.summary,
      etymology: p.etymology,
      namingReason: p.namingReason,
      generated: row.origin !== "authored",
    };
  } catch {
    return null;
  }
}
