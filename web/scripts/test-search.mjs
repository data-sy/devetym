/**
 * 검색 골든 케이스 (설계서 §3-2 G2·G3·G4).
 *
 * ⚠️ **웹이 앱과 등가임이 아니라, 어디서 의도적으로 갈리는지를 고정하는 테스트다.**
 *    앱 `autocomplete`는 keyword prefix만 보고 aliases를 제외한다. 그 명세를 그대로 옮기면
 *    한글 입력에 자동완성이 전혀 반응하지 않는다(표제어 650개가 전부 영어). 여기서 그
 *    갈라짐을 **단언**해 둬야, 나중에 "앱과 맞춘다"며 되돌리는 변경이 테스트를 깬다.
 *
 * 실행: node scripts/test-search.mjs
 *
 * ⚠️ 소스는 확장자 없는 import를 쓴다(Vite/Astro 관례). node ESM은 그걸 못 푼다 —
 *    그래서 esbuild로 한 번 번들해서 돌린다. 테스트 때문에 소스의 import 스타일을
 *    바꾸지 않는다(빌드가 정본이고 테스트가 거기 맞춘다).
 */
import { readFileSync, mkdtempSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { tmpdir } from "node:os";
import path from "node:path";
import esbuild from "esbuild";

const bundleDir = mkdtempSync(path.join(tmpdir(), "devetym-search-"));
const entry = path.join(bundleDir, "entry.mjs");
await esbuild.build({
  entryPoints: [path.join(path.dirname(fileURLToPath(import.meta.url)), "../src/lib/search.ts")],
  bundle: true,
  format: "esm",
  platform: "node",
  outfile: entry,
  logLevel: "silent",
});
const { search, exactMatches, normalizeTermKey } = await import(entry);

const here = path.dirname(fileURLToPath(import.meta.url));
const BUNDLE = path.join(here, "../../shared/src/commonMain/composeResources/files/terms.json");
const terms = JSON.parse(readFileSync(BUNDLE, "utf8"));

const rows = terms.map((t) => ({
  s: normalizeTermKey(t.keyword),
  k: t.keyword,
  a: t.aliases,
  c: t.category,
  d: t.summary,
}));

const failures = [];
const ok = (cond, label) => { if (!cond) failures.push(label); };
const slugs = (q, n = 5) => search(rows, q).slice(0, n).map((m) => m.row.s);

// ── G3 · 매칭 ────────────────────────────────────────────────────────────────
ok(slugs("mutex")[0] === "mutex", "G3 표제어 완전일치가 1위");
ok(slugs("MUTEX")[0] === "mutex", "G3 대문자 변형");
ok(slugs("뮤텍스")[0] === "mutex", "G3 한글 별칭 → 영문 표제어 페이지");
ok(slugs("aa tree")[0] === "aatree", "G3 공백 표기 변이(N1 삭제 정규화)");
ok(slugs("aa-tree")[0] === "aatree", "G3 하이픈 표기 변이");

// ⚠️ 앱 번들 인덱스가 **가리는** 3건. 웹은 둘 다 보여야 한다(설계서 §3-1.3).
for (const collided of ["집계", "분기", "샤딩"]) {
  const hits = search(rows, collided).filter((m) => normalizeTermKey(m.via) === normalizeTermKey(collided));
  ok(hits.length >= 2, `G3 교차 충돌 '${collided}' 전부 노출 (앱은 first-wins로 가림) — 실제 ${hits.length}건`);
}

// ── G4 · 자동완성이 별칭을 본다 (앱과 의도적으로 다름) ──────────────────────────
const koPrefix = search(rows, "뮤텍");
ok(koPrefix.length > 0, "G4 한글 접두 입력에 결과가 있다 — 앱 명세(keyword only)면 0건이어야 한다");
ok(
  terms.every((t) => !/[가-힣]/.test(t.keyword)),
  "G4 전제: 표제어에 한글이 0개다(그래서 별칭을 봐야 한다)",
);

// ── 생성 게이트 · 완전일치가 있으면 AI를 열지 않는다 ──────────────────────────
ok(exactMatches(rows, "뮤텍스").length === 1, "정본에 있는 별칭은 완전일치로 잡힌다(생성 안 염)");
ok(exactMatches(rows, "존재하지않는용어xyz").length === 0, "정본에 없으면 완전일치 0 → 생성 어포던스");

// ── 결정성 ───────────────────────────────────────────────────────────────────
ok(
  JSON.stringify(search(rows, "lock")) === JSON.stringify(search(rows, "lock")),
  "같은 질의에 같은 순서(결정적)",
);

// ── 빈 입력 ──────────────────────────────────────────────────────────────────
ok(search(rows, "").length === 0 && search(rows, "   ").length === 0, "빈 질의는 0건");

console.log(`  케이스 ${11 + 3}건 · 행 ${rows.length}`);
if (failures.length) {
  console.log(`FAIL — ${failures.length}건`);
  for (const f of failures) console.log(`  ${f}`);
  process.exit(1);
}
console.log("PASS — 검색 골든 케이스");
