import { normalizeTermKey } from "./term-key";

// 테스트가 같은 번들에서 정규화를 꺼내 쓸 수 있게 한다 — 테스트용 두 번째 경로를 만들지 않기 위해.
export { normalizeTermKey };

/**
 * 검색 매칭 — 정적 인덱스 위에서 도는 순수 함수. (설계서 §3-2 G3·G4)
 *
 * ⚠️ **앱 `autocomplete` 명세와 의도적으로 갈린다.** 앱은 keyword prefix만 보고 aliases를
 *    제외한다(`BundleDbSource`). 그 명세를 그대로 옮기면 **한글 입력에 자동완성이 전혀 반응하지
 *    않는다** — 표제어 650개가 전부 영어이기 때문이다(설계서 §3-1.4 · D7). 웹은 별칭을 본다.
 *
 * ⚠️ **앱의 first-wins 가림도 고치지 않고 두면 안 된다.** 앱 번들 인덱스는 정규화 충돌 시 뒤
 *    엔트리를 가린다(실측 3건: 집계·분기·샤딩). 웹은 용어마다 자기 URL이 있으므로 **둘 다
 *    보여준다**(설계서 §3-1.3).
 */

export interface IndexRow {
  /** slug (= 정규화 키 = D1 term_key) */
  s: string;
  /** 표제어 원문 */
  k: string;
  /** 별칭 원문 */
  a: string[];
  /** 카테고리 */
  c: string;
  /** 요약 */
  d: string;
}

export interface Match {
  row: IndexRow;
  /** 이 행이 걸린 이유 — 결과에서 "왜 나왔는지"를 보여주기 위해 남긴다. */
  via: string;
  /** 낮을수록 먼저. 0=완전일치 · 1=접두 · 2=부분 · 3=요약 */
  rank: number;
}

/** 행 하나가 가진 모든 표기(표제어 + 별칭). 정규화 전 원문이다. */
function labelsOf(row: IndexRow): string[] {
  return [row.k, ...row.a];
}

/**
 * 질의 → 매치 목록. 정렬은 (rank, 표기 길이, slug)로 **완전히 결정적**이어야 한다 —
 * 같은 질의에 결과 순서가 흔들리면 사용자가 방금 본 것을 다시 못 찾는다.
 */
export function search(rows: IndexRow[], query: string, limit = 30): Match[] {
  const q = normalizeTermKey(query);
  if (!q) return [];

  const out: Match[] = [];
  for (const row of rows) {
    let best: Match | null = null;
    for (const label of labelsOf(row)) {
      const n = normalizeTermKey(label);
      if (!n) continue;
      let rank: number | null = null;
      if (n === q) rank = 0;
      else if (n.startsWith(q)) rank = 1;
      else if (n.includes(q)) rank = 2;
      if (rank === null) continue;
      if (!best || rank < best.rank) best = { row, via: label, rank };
      if (best.rank === 0) break;
    }
    // 요약 본문 매치는 마지막 그물이다 — 표기로 못 찾은 사람을 건진다.
    if (!best && row.d.toLowerCase().includes(query.trim().toLowerCase())) {
      best = { row, via: row.k, rank: 3 };
    }
    if (best) out.push(best);
  }

  out.sort(
    (x, y) =>
      x.rank - y.rank ||
      x.via.length - y.via.length ||
      (x.row.s < y.row.s ? -1 : x.row.s > y.row.s ? 1 : 0),
  );
  return out.slice(0, limit);
}

/**
 * 완전일치 행들. **AI 생성을 열지 말지를 가르는 판정**이다 —
 * 이미 정본에 있는 용어를 다시 생성하면 한도와 돈을 태우고 결과도 같다.
 */
export function exactMatches(rows: IndexRow[], query: string): IndexRow[] {
  const q = normalizeTermKey(query);
  if (!q) return [];
  return rows.filter((row) => labelsOf(row).some((l) => normalizeTermKey(l) === q));
}
