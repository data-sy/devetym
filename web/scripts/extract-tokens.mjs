/**
 * 디자인 토큰 추출: Kotlin 정본 → CSS 커스텀 프로퍼티 (W0a).
 *
 * 정본은 `docs/design`이 아니라 **코드**다 (ADR-0009 Decision 6):
 *   ui/theme/AppColors.kt     → --c-*  (라이트/다크 11토큰)
 *   ui/theme/AppDimens.kt     → --d-*  (간격·반경·선굵기)
 *   ui/theme/AppTypography.kt → --t-*  (21 토큰의 size/weight/lineHeight/tracking)
 *
 * 손으로 베끼지 않는 이유: 앱 토큰이 바뀌면 웹이 조용히 어긋난다. 이 스크립트가
 * 빌드 전(prebuild)에 돌아 매번 다시 뽑으므로 드리프트가 구조적으로 불가능하다.
 * 산출물 `src/styles/tokens.css`는 **생성물이며 커밋되지만 손편집 금지**다.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const THEME = resolve(HERE, "../../shared/src/commonMain/kotlin/com/robin/devetym/ui/theme");
const OUT = resolve(HERE, "../src/styles/tokens.css");

const read = (f) => readFileSync(resolve(THEME, f), "utf8");

/** kebab-case로. accentAI → accent-ai */
const kebab = (s) => s.replace(/([a-z0-9])([A-Z])/g, "$1-$2").toLowerCase();

// ── 색: Color(0xAARRGGBB) → #rrggbb (알파는 전 토큰 FF라 버린다) ────────────
function colors(scheme) {
  const src = read("AppColors.kt");
  const block = src.match(new RegExp(`val ${scheme}Colors = AppColors\\(([\\s\\S]*?)\\n\\)`));
  if (!block) throw new Error(`AppColors.kt: ${scheme}Colors 블록을 못 찾았다`);
  const out = [];
  for (const [, name, hex8] of block[1].matchAll(/(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)/g)) {
    const alpha = hex8.slice(0, 2).toUpperCase();
    if (alpha !== "FF") throw new Error(`${name}: 알파 ${alpha} — 추출기가 불투명만 가정한다`);
    out.push([`--c-${kebab(name)}`, `#${hex8.slice(2).toLowerCase()}`]);
  }
  if (out.length !== 11) throw new Error(`${scheme}: 색 ${out.length}개 — 11개여야 한다`);
  return out;
}

// ── 치수: 12.dp / 1.5.dp → px ─────────────────────────────────────────────
function dimens() {
  const src = read("AppDimens.kt");
  const block = src.match(/data class AppDimens\(([\s\S]*?)\n\)/);
  if (!block) throw new Error("AppDimens.kt: data class 블록을 못 찾았다");
  const out = [];
  for (const [, name, v] of block[1].matchAll(/val (\w+): Dp = ([\d.]+)\.dp/g)) {
    out.push([`--d-${kebab(name)}`, `${parseFloat(v)}px`]);
  }
  if (!out.length) throw new Error("AppDimens.kt: 추출된 치수 0개");
  return out;
}

// ── 타이포: TextStyle(...)의 size/weight/lineHeight/letterSpacing ──────────
const WEIGHT = { Thin: 100, ExtraLight: 200, Light: 300, Normal: 400, Medium: 500,
                 SemiBold: 600, Bold: 700, ExtraBold: 800, Black: 900 };

function typography() {
  const src = read("AppTypography.kt");
  const block = src.match(/fun appTypography\(f: AppFonts\): AppTypography = AppTypography\(([\s\S]*?)\n\)/);
  if (!block) throw new Error("AppTypography.kt: appTypography 블록을 못 찾았다");
  const out = [];
  let n = 0;
  for (const [, name, args] of block[1].matchAll(/(\w+) = TextStyle\(([^)]*(?:\([^)]*\)[^)]*)*)\)/g)) {
    const k = kebab(name);
    n++;
    const size = args.match(/fontSize = ([\d.]+)\.sp/);
    const line = args.match(/lineHeight = ([\d.]+)\.sp/);
    const weight = args.match(/fontWeight = FontWeight\.(\w+)/);
    const track = args.match(/letterSpacing = \(?(-?[\d.]+)\)?\.sp/);
    const fam = args.match(/fontFamily = f\.(\w+)Family/);

    if (size) out.push([`--t-${k}-size`, `${parseFloat(size[1])}px`]);
    // lineHeight 미지정 토큰은 CSS 기본에 맡긴다 — 임의값을 발명하지 않는다.
    if (line) out.push([`--t-${k}-line`, `${parseFloat(line[1])}px`]);
    if (weight) {
      const w = WEIGHT[weight[1]];
      if (!w) throw new Error(`${name}: 모르는 FontWeight.${weight[1]}`);
      out.push([`--t-${k}-weight`, String(w)]);
    }
    if (track) out.push([`--t-${k}-tracking`, `${parseFloat(track[1])}px`]);
    if (fam) out.push([`--t-${k}-family`, `var(--font-${fam[1]})`]);
  }
  if (n !== 21) throw new Error(`타이포 토큰 ${n}개 — 21개여야 한다(AppTypography 21종)`);
  return out;
}

const fmt = (pairs, indent = "  ") =>
  pairs.map(([k, v]) => `${indent}${k}: ${v};`).join("\n");

const light = colors("Light");
const dark = colors("Dark");

const css = `/* ⚠️ 생성물 — 손으로 고치지 않는다. \`npm run tokens\`가 다시 뽑는다.
 * 정본: shared/src/commonMain/kotlin/com/robin/devetym/ui/theme/{AppColors,AppDimens,AppTypography}.kt
 * 생성기: web/scripts/extract-tokens.mjs
 *
 * 다크가 기본이다 (LocalAppColors 기본값 = DarkColors, iOS appearanceMode 기본 2 계승).
 * 그래서 :root가 다크를 들고, 라이트는 명시 선택/시스템 선호일 때 덮는다.
 */

:root {
  /* ── 색 (다크 · 기본) ────────────────────────────────────────── */
${fmt(dark)}

  /* ── 치수 ──────────────────────────────────────────────────── */
${fmt(dimens())}

  /* ── 타이포 ────────────────────────────────────────────────── */
${fmt(typography())}
}

/* 시스템이 라이트를 선호하고, 사용자가 다크를 명시 선택하지 않았을 때 */
@media (prefers-color-scheme: light) {
  :root:not([data-theme="dark"]) {
${fmt(light, "    ")}
  }
}

/* 사용자 명시 선택이 시스템을 이긴다 (앱 외관 3모드와 같은 규율) */
:root[data-theme="light"] {
${fmt(light)}
}

:root[data-theme="dark"] {
${fmt(dark)}
}
`;

writeFileSync(OUT, css, "utf8");
console.log(`tokens.css 생성 — 색 ${dark.length}×2 · 치수 ${dimens().length} · 타이포 ${typography().length}`);
