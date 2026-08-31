/**
 * 폰트 준비: 앱 번들 TTF → 웹 woff2 (W0a).
 *
 * 앱과 **같은 파일**을 쓴다 (ADR-0009 Decision 6 — 새로 구하지 않는다). 라이선스도 동일(OFL).
 * TTF를 그대로 서빙하지 않는 이유: woff2가 30~50% 작고, ADR-0009가 SSG로 얻으려는
 * Core Web Vitals 이점을 폰트 무게로 반납하면 안 된다.
 *
 * ⚠️ DM Sans는 복사하지 않는다. `AppFonts.sansFamily`는 정의돼 있으나 `appTypography`의
 *    21 토큰 중 **어느 것도 참조하지 않는다** — 앱에서도 실사용 0이다. 웹에 실으면
 *    쓰이지 않는 147KB를 나르게 된다.
 *
 * ⚠️ 본문(bodyFamily)은 커스텀 폰트가 아니라 **시스템 폰트**다. AppFonts.kt 주석의 이유:
 *    한글이 커스텀 라틴 폰트 박스에 작게 끼는 문제를 피한다. 웹도 이 규칙을 따른다
 *    (base.css의 --font-body = 시스템 스택). 이걸 어기면 한글 본문이 앱과 달라 보인다.
 */
import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { compress } from "wawoff2";

const HERE = dirname(fileURLToPath(import.meta.url));
const SRC = resolve(HERE, "../../shared/src/commonMain/composeResources/font");
const OUT = resolve(HERE, "../public/fonts");

/** 웹에서 실제로 참조되는 것만. (family, weight, style) = @font-face 선언과 1:1 */
const FACES = [
  "dmmono_light",
  "dmmono_regular",
  "dmmono_medium",
  "dmserifdisplay_regular",
  "dmserifdisplay_italic",
];

mkdirSync(OUT, { recursive: true });

let before = 0;
let after = 0;
for (const name of FACES) {
  const ttf = readFileSync(resolve(SRC, `${name}.ttf`));
  const woff2 = await compress(ttf);
  writeFileSync(resolve(OUT, `${name}.woff2`), woff2);
  before += ttf.length;
  after += woff2.length;
  console.log(`  ${name}: ${(ttf.length / 1024).toFixed(0)}KB → ${(woff2.length / 1024).toFixed(0)}KB`);
}
console.log(`폰트 ${FACES.length}종 · ${(before / 1024).toFixed(0)}KB → ${(after / 1024).toFixed(0)}KB`);
