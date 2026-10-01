/**
 * 빌드 산출물 검사 — **구조화 데이터와 내부 링크는 깨져도 페이지가 멀쩡해 보인다** (W2).
 *
 * ⚠️ 이게 이 파일의 존재 이유다. JSON-LD가 이스케이프돼 파서에 통째로 버려져도, 갈래 허브
 *    링크에 오타가 나 404로 가도, **빌드는 성공하고 화면은 정상이며 타입 검사도 녹색이다.**
 *    W1b가 `format: "file"` 함정을 전수 200 오라클로만 잡았던 것과 같은 성격 — 오라클이
 *    없으면 조용히 나간다.
 *
 * 검사하는 것:
 *   1. 모든 페이지의 `<script type="application/ld+json">`이 **JSON으로 파싱된다**
 *   2. 용어 상세 = `DefinedTerm` + `BreadcrumbList`, 갈래 허브 = `DefinedTermSet` + `BreadcrumbList`
 *   3. `DefinedTerm.inDefinedTermSet`이 **실재하는 허브 URL**을 가리킨다
 *   4. 내부 링크(`href="/…"`)가 전부 빌드된 페이지로 간다 — 오타 하나면 660장이 404로 샌다
 *
 *   node scripts/test-structure.mjs
 */
import { readdirSync, readFileSync, existsSync, statSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const here = path.dirname(fileURLToPath(import.meta.url));
const dist = path.join(here, "../dist");
if (!existsSync(dist)) {
  console.error("dist/가 없다 — npm run build 먼저.");
  process.exit(1);
}

/** SSR이 받는 경로. 정적 파일이 없어도 200이므로 링크 검사에서 제외한다. */
const SSR_PATHS = new Set(["/search", "/api/term"]);

const problems = [];
const htmlFiles = [];

(function walk(dir) {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith(".html")) htmlFiles.push(p);
  }
})(dist);

const pagePath = (file) => {
  const rel = path.relative(dist, file).replace(/\\/g, "/");
  const noIndex = rel.replace(/(^|\/)index\.html$/, "");
  return "/" + noIndex.replace(/\.html$/, "");
};

/** 이 빌드가 실제로 내놓은 경로 집합 — 링크 검사의 정답지. */
const built = new Set(htmlFiles.map(pagePath).map((p) => (p === "/" ? "/" : p.replace(/\/$/, ""))));

const LD_RE = /<script[^>]*type="application\/ld\+json"[^>]*>([\s\S]*?)<\/script>/g;
const HREF_RE = /href="(\/[^"#]*)(?:#[^"]*)?"/g;

let ldCount = 0;
let termPages = 0;
let hubPages = 0;
const hubUrls = new Set();

for (const file of htmlFiles) {
  const page = pagePath(file).replace(/\/$/, "") || "/";
  if (page.startsWith("/category/")) hubUrls.add(page);
}

for (const file of htmlFiles) {
  const html = readFileSync(file, "utf8");
  const page = pagePath(file).replace(/\/$/, "") || "/";

  // ── 1·2·3. 구조화 데이터 ──
  const types = [];
  const blocks = [];
  for (const m of html.matchAll(LD_RE)) {
    ldCount++;
    try {
      const obj = JSON.parse(m[1]);
      blocks.push(obj);
      types.push(obj["@type"]);
    } catch (e) {
      problems.push(`${page}: JSON-LD 파싱 실패 — ${String(e).slice(0, 60)}`);
    }
  }

  if (page.startsWith("/term/")) {
    termPages++;
    for (const need of ["DefinedTerm", "BreadcrumbList"]) {
      if (!types.includes(need)) problems.push(`${page}: ${need} 없음 (있는 것: ${types.join(",") || "없음"})`);
    }
    const dt = blocks.find((b) => b["@type"] === "DefinedTerm");
    const setUrl = dt?.inDefinedTermSet?.url;
    if (setUrl) {
      const p = new URL(setUrl).pathname.replace(/\/$/, "");
      if (!hubUrls.has(p)) problems.push(`${page}: inDefinedTermSet이 없는 허브를 가리킨다 — ${p}`);
    } else if (dt) {
      problems.push(`${page}: DefinedTerm에 inDefinedTermSet.url이 없다`);
    }
    if (dt && !(Array.isArray(dt.alternateName) && dt.alternateName.length > 0)) {
      problems.push(`${page}: alternateName이 비었다 — 한글 별칭이 구조화 데이터에서 증발했다`);
    }
  }

  if (page.startsWith("/category/")) {
    hubPages++;
    for (const need of ["DefinedTermSet", "BreadcrumbList"]) {
      if (!types.includes(need)) problems.push(`${page}: ${need} 없음`);
    }
  }

  // ── 4. 내부 링크 ──
  for (const m of html.matchAll(HREF_RE)) {
    const target = m[1].replace(/\/$/, "") || "/";
    if (SSR_PATHS.has(target)) continue;
    if (/\.(json|xml|txt|woff2|css|js|png|svg|ico)$/.test(target)) continue;
    if (built.has(target)) continue;
    problems.push(`${page}: 내부 링크가 빌드에 없다 — ${target}`);
  }
}

// ── 규모 단언: 페이지가 통째로 사라져도 위 검사는 전부 통과한다 ──
if (termPages !== 660) problems.push(`용어 페이지 ${termPages}장 (660이어야 한다)`);
if (hubPages !== 6) problems.push(`갈래 허브 ${hubPages}장 (6이어야 한다)`);

const uniq = [...new Set(problems)];
console.log(
  `  dist ${htmlFiles.length}장 · 용어 ${termPages} · 허브 ${hubPages} · JSON-LD ${ldCount}블록 · 문제 ${uniq.length}건`,
);
if (uniq.length) {
  for (const p of uniq.slice(0, 20)) console.log(`    ${p}`);
  if (uniq.length > 20) console.log(`    ... 외 ${uniq.length - 20}건`);
  console.log("FAIL — 구조 검사 실패");
  process.exit(1);
}
console.log("  PASS — 구조화 데이터·내부 링크 이상 없음");
