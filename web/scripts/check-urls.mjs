/**
 * **완료 오라클** — 용어 URL 전수 200 (설계서 F6 · ROADMAP W1b).
 *
 * ⚠️ **로컬 빌드 성공은 오라클이 아니다.** 이 프로젝트가 마일스톤마다 겪은
 *    *"빌드는 되는데 실기동은 깨진다"*의 웹 대응물은 **"빌드는 되는데 색인은 안 된다"**이다.
 *    그래서 오라클은 파일 개수가 아니라 **실제 HTTP 응답**이다.
 *
 * 로컬(wrangler dev)에서 먼저 돌려 배포 전에 깨진 것을 잡고, 배포 뒤 실 도메인에 같은
 * 스크립트를 돌려 종결한다.
 *
 *   node scripts/check-urls.mjs http://127.0.0.1:8788
 *   node scripts/check-urls.mjs https://devetym.com
 */
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const base = (process.argv[2] ?? "http://127.0.0.1:8788").replace(/\/$/, "");
const here = path.dirname(fileURLToPath(import.meta.url));
const terms = JSON.parse(
  readFileSync(path.join(here, "../../shared/src/commonMain/composeResources/files/terms.json"), "utf8"),
);

const WS = "\\u0009-\\u000D\\u001C-\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000";
const SEP = new RegExp(`[${WS}\\u002D\\u005F]`, "g");
const slug = (s) => s.replace(SEP, "").toLowerCase();

/**
 * 갈래 허브 6장 (W2). ⚠️ **슬러그를 여기 손으로 적어 둔 것이 의도다** — `categories.ts`에서
 * import하면 그 파일이 틀려도 검사가 같이 틀려서 통과한다. 오라클은 코드 밖에서 온 값이어야 한다.
 */
const CATEGORY_SLUGS = ["concurrency", "data-structure", "network", "database", "pattern", "etc"];

/** 용어 URL + 반드시 살아 있어야 하는 구조 URL. 후자가 죽으면 색인 배선이 끊긴다. */
const urls = [
  ...terms.map((t) => `/term/${slug(t.keyword)}`),
  ...CATEGORY_SLUGS.map((s) => `/category/${s}`),
  "/", "/terms", "/search", "/search-index.json", "/robots.txt", "/sitemap-index.xml",
];

const CONCURRENCY = 16;
const bad = [];
let done = 0;

async function head(url) {
  try {
    // HEAD가 아니라 GET이다 — 정적 자산 서빙이 HEAD만 다르게 처리하는 경우를 안 놓치려고.
    const res = await fetch(base + url, { redirect: "manual" });
    if (res.status !== 200) bad.push(`${url} → ${res.status}`);
  } catch (e) {
    bad.push(`${url} → ${String(e).slice(0, 80)}`);
  }
  if (++done % 100 === 0) process.stderr.write(`  ${done}/${urls.length}\n`);
}

const queue = [...urls];
await Promise.all(
  Array.from({ length: CONCURRENCY }, async () => {
    while (queue.length) await head(queue.shift());
  }),
);

console.log(`  ${base} · ${urls.length}개 요청 · 비200 ${bad.length}건`);
if (bad.length) {
  for (const b of bad.slice(0, 20)) console.log(`    ${b}`);
  if (bad.length > 20) console.log(`    ... 외 ${bad.length - 20}건`);
  console.log("FAIL — 전수 200 아님");
  process.exit(1);
}
console.log("PASS — 용어 URL 전수 200");
